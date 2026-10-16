"use strict";
// Runs only on an administrator's PC. Never ship credentials with the Android app.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const cli = path.join(root, 'build/firebase-tools/node_modules/firebase-tools/lib');
const serviceRequire = require('node:module').createRequire(path.join(root,'photo-service/package.json'));
const adminRequire = require('node:module').createRequire(serviceRequire.resolve('firebase-admin/firestore'));
const {Firestore} = adminRequire('@google-cloud/firestore');
const {awardVisit, eligible} = require('../photo-service/points');
const PROJECT = 'capstone-8ca90';
async function main() {
 const args = process.argv.slice(2);
 if(args.some(a=>!['--apply','--help'].includes(a))) throw new Error('Allowed options: --apply, --help');
 if(args.includes('--help')) { console.log('Default: preview only. --apply: award 100P per account/cafe, once. Scans existing visit records; no scheduler.'); return; }
 if(process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) throw new Error('Unset emulator environment variables before using this production tool');
 const account = require(path.join(cli,'auth')).getGlobalDefaultAccount();
 if(!account) throw new Error('Firebase login required. Run the login command first.');
 await require(path.join(cli,'requireAuth')).requireAuth({project:PROJECT,...account});
 const api=require(path.join(cli,'api'));
 const db=new Firestore({projectId:PROJECT,credentials:{type:'authorized_user',
   client_id:api.clientId(),client_secret:api.clientSecret(),refresh_token:account.tokens.refresh_token}});
 const summary={project:PROJECT,mode:args.includes('--apply')?'apply':'preview',at:new Date().toISOString(),counts:{},points:0,records:[]};
 try {
  // Read-only until --apply. Stable ordering makes a repeated preview reproducible.
  const snapshot=await db.collection('visit_records').get();
  const docs=snapshot.docs.sort((a,b)=>(Number(a.get('visitedAt'))||0)-(Number(b.get('visitedAt'))||0)||a.id.localeCompare(b.id));
  const proposed=new Set();
  for(const doc of docs) {
   const data=doc.data();
   if(!eligible(data)) {summary.counts.ineligible=(summary.counts.ineligible||0)+1;continue;}
   const key=JSON.stringify([data.userUid,data.cafeId]);
   let result;
   if(summary.mode==='preview' && proposed.has(key)) result={status:'duplicate_candidate'};
   else result=await awardVisit(db,doc.id,data,{dryRun:summary.mode==='preview',requireEnabled:false});
   summary.counts[result.status]=(summary.counts[result.status]||0)+1;
   if(['would_award','awarded'].includes(result.status)) {
    proposed.add(key);summary.points+=result.points;
    summary.records.push({recordId:doc.id,cafeId:data.cafeId,cafeName:data.cafeName||'',points:result.points,status:result.status});
   }
  }
  const folder=path.join(root,'build/points-audit');fs.mkdirSync(folder,{recursive:true});
  const file=path.join(folder,summary.mode+'-'+Date.now()+'.json');fs.writeFileSync(file,JSON.stringify(summary,null,2));
  console.log(JSON.stringify({project:PROJECT,mode:summary.mode,counts:summary.counts,points:summary.points,report:file},null,2));
 } finally { await db.terminate(); }
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
