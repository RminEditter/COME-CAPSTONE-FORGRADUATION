"use strict";
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {awardVisit, eligible} = require('../points');
const data = (uid='u', cafeId='c') => ({userUid:uid,cafeId,rating:3,observedTags:['DESSERT']});
test('eligibility accepts honest low ratings and rejects malformed rewards', () => {
 assert(eligible(data())); assert(eligible({...data(),rating:0}));
 for (const patch of [{observedTags:[]},{observedTags:['FAKE']},{observedTags:['DESSERT','DESSERT']},{rating:NaN},{userUid:'x/y'},{cafeId:''}]) assert(!eligible({...data(),...patch}));
});
test('real Firestore: concurrent awards, replay, delete/recreate, account isolation and rules',
 {skip: !process.env.FIRESTORE_EMULATOR_HOST}, async () => {
 const {initializeApp,deleteApp} = require('firebase-admin/app');
 const {getFirestore} = require('firebase-admin/firestore');
 const projectId='demo-cafefit-points';
 const app=initializeApp({projectId},'points-test'); const db=getFirestore(app);
 await db.doc('point_config/control').set({enabled:true});
 await db.doc('users/u').set({nickname:'tester'}); await db.doc('users/v').set({pointBalance:0});
 await db.doc('cafes/c').set({name:'카페'}); await db.doc('cafes/d').set({name:'다른 카페'});
 try {
  for(let i=0;i<8;i++) await db.doc('visit_records/r'+i).set(data());
  await db.doc('point_config/control').set({enabled:false});
  assert.equal((await awardVisit(db,'r0',data(),{dryRun:true,requireEnabled:false})).status,'would_award');
  assert.equal((await db.doc('users/u').get()).get('pointBalance'),undefined);
  assert.equal((await db.collection('users/u/point_history').get()).size,0);
  await db.doc('point_config/control').set({enabled:true});
  const outcomes=await Promise.all(Array.from({length:8},(_,i)=>awardVisit(db,'r'+i,data())));
  assert.equal(outcomes.filter(r=>r.status==='awarded').length,1);
  assert.equal((await db.doc('users/u').get()).get('pointBalance'),100);
  assert.equal((await db.collection('users/u/point_history').get()).size,1);
  await db.doc('visit_records/r0').delete(); await db.doc('visit_records/r0').set(data());
  assert.equal((await awardVisit(db,'r0',data())).status,'already_awarded');
  await db.doc('visit_records/v1').set(data('v')); await awardVisit(db,'v1',data('v'));
  assert.equal((await db.doc('users/v').get()).get('pointBalance'),100);
  await db.doc('visit_records/d1').set(data('u','d')); await awardVisit(db,'d1',data('u','d'));
  assert.equal((await db.doc('users/u').get()).get('pointBalance'),200);
  const jwt = uid => [Buffer.from(JSON.stringify({alg:'none',typ:'JWT'})).toString('base64url'),
    Buffer.from(JSON.stringify({sub:uid,user_id:uid,iss:'https://securetoken.google.com/'+projectId,aud:projectId,
      iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'password'}})).toString('base64url'),''].join('.');
  const request=async(path,method,fields,uid='u') => fetch('http://'+process.env.FIRESTORE_EMULATOR_HOST+'/v1/projects/'+projectId+'/databases/(default)/documents/'+path,
    {method,headers:{Authorization:'Bearer '+jwt(uid),'Content-Type':'application/json'},body:fields?JSON.stringify({fields}):undefined});
  assert.equal((await request('users/u','PATCH',{pointBalance:{integerValue:'999999'}})).status,403);
  assert.equal((await request('users/u','DELETE')).status,403);
  assert.equal((await request('users/u/point_awards/c','DELETE')).status,403);
  assert.equal((await request('users/u/point_history/forged','PATCH',{points:{integerValue:'999'}})).status,403);
  assert.equal((await request('users/v/point_history','GET')).status,403);
  assert.equal((await request('point_config/control','PATCH',{enabled:{booleanValue:true}})).status,403);
  const fields={userUid:{stringValue:'v'},cafeId:{stringValue:'c'}};
  assert.equal((await request('visit_records/forged','PATCH',fields)).status,403);
  fields.userUid={stringValue:'u'};
  assert.equal((await request('visit_records/legit','PATCH',fields)).status,200);
  await db.doc('point_config/control').set({enabled:false});
  assert.equal((await awardVisit(db,'r0',data())).status,'disabled');
 } finally {
  for(const collection of ['users','cafes','visit_records','point_config']) await db.recursiveDelete(db.collection(collection));
  await deleteApp(app);
 }
});
