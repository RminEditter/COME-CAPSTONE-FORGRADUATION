"use strict";
const {FieldValue} = require("firebase-admin/firestore");
const TAGS = new Set(["BEAN_NUTTY","BEAN_ACIDIC","INTERIOR_PRETTY","DRINK_TASTY","HIP","WORK_FRIENDLY","DESSERT","SPECIALTY_DRIP","SMALL_CAFE","LARGE_CAFE","SOLO","COUPLE","FRIEND","FAMILY","COLLEAGUE","OUTLET_MANY","WIFI_FAST","LAPTOP_OK"]);
const POINTS = 100;
const validId = value => typeof value === "string" && value.length > 0 && value.length <= 128 && !value.includes("/") && value !== "." && value !== "..";
function eligible(data) {
  return data && validId(data.userUid) && validId(data.cafeId)
    && typeof data.rating === "number" && Number.isFinite(data.rating) && data.rating >= 0 && data.rating <= 5
    && Array.isArray(data.observedTags) && data.observedTags.length > 0 && data.observedTags.length <= TAGS.size
    && new Set(data.observedTags).size === data.observedTags.length && data.observedTags.every(tag => TAGS.has(tag));
}
// Requires production rules to bind visit_records.userUid to request.auth.uid.
// No client-supplied amounts; all three writes commit atomically.
async function awardVisit(db, recordId, eventData, {dryRun = false, requireEnabled = true} = {}) {
  if (!validId(recordId) || !eligible(eventData)) return {status: "ineligible"};
  const {userUid: uid, cafeId} = eventData;
  return db.runTransaction(async tx => {
    const userRef = db.doc('users/' + uid);
    const awardRef = db.doc('users/' + uid + '/point_awards/' + cafeId);
    const historyRef = db.doc('users/' + uid + '/point_history/' + recordId);
    const [control, user, cafe, award, history, current] = await tx.getAll(
      db.doc('point_config/control'), userRef, db.doc('cafes/' + cafeId), awardRef, historyRef,
      db.doc('visit_records/' + recordId));
    if (requireEnabled && (!control.exists || control.get('enabled') !== true)) return {status: "disabled"};
    if (award.exists || history.exists) return {status: "already_awarded"};
    if (!user.exists || !cafe.exists || !current.exists || !eligible(current.data())
        || current.get('userUid') !== uid || current.get('cafeId') !== cafeId
        || cafe.get('excludedFromDiscovery') === true || cafe.get('isCafe') === false) return {status: "ineligible"};
    const balance = user.get('pointBalance') ?? 0;
    if (!Number.isSafeInteger(balance) || balance < 0 || balance > Number.MAX_SAFE_INTEGER - POINTS)
      throw new Error('Invalid point balance; refusing to overwrite');
    if (dryRun) return {status: "would_award", points: POINTS};
    const now = FieldValue.serverTimestamp();
    tx.update(userRef, {pointBalance: balance + POINTS});
    tx.create(awardRef, {visitRecordId: recordId, points: POINTS, createdAt: now});
    tx.create(historyRef, {type: 'VISIT_TAG_REWARD', points: POINTS, cafeId,
      cafeName: cafe.get('name') || '', visitRecordId: recordId, createdAt: now, balanceAfter: balance + POINTS});
    return {status: "awarded", points: POINTS};
  });
}
module.exports = {awardVisit, eligible, POINTS};
