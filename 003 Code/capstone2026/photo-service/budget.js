"use strict";

const {createHash} = require("node:crypto");
// Server-side hard ceilings; Firestore configuration may lower, NEVER raise them.
const LIMITS = Object.freeze({month: 800, day: 25, userDay: 5});

function calendar(date, timeZone) {
  const parts = Object.fromEntries(new Intl.DateTimeFormat("en-US", {
    timeZone, year: "numeric", month: "2-digit", day: "2-digit"
  }).formatToParts(date).map(p => [p.type, p.value]));
  return `${parts.year}-${parts.month}-${parts.day}`;
}

function buckets(now, uid, kind) {
  if (!["details", "photo"].includes(kind)) throw new Error("Invalid budget kind");
  const utc = calendar(now, "UTC");
  const pacific = calendar(now, "America/Los_Angeles");
  const user = createHash("sha256").update(uid).digest("hex");
  return [
    {id: `${kind}-month-utc-${utc.slice(0, 7)}`, key: "month"},
    {id: `${kind}-month-pacific-${pacific.slice(0, 7)}`, key: "month"},
    {id: `${kind}-day-${utc}`, key: "day"},
    {id: `${kind}-user-${user}-${utc}`, key: "userDay"}
  ];
}

function limitFor(config, key) {
  const n = config[key];
  if (n === undefined) return LIMITS[key];
  return Number.isSafeInteger(n) && n >= 0 ? Math.min(n, LIMITS[key]) : 0;
}

async function reserve(db, uid, kind, now = new Date()) {
  const entries = buckets(now, uid, kind);
  const refs = entries.map(entry => db.doc(`usage/${entry.id}`));
  // One atomic transaction across ALL users/devices/instances. No network call inside it:
  // Firestore may retry this callback. Never refund failed/ambiguous upstream attempts.
  return db.runTransaction(async tx => {
    const config = await tx.get(db.doc("config/control"));
    if (!config.exists || config.data().enabled !== true) return false;
    const state = config.data();
    const snapshots = await tx.getAll(...refs);
    const counts = snapshots.map(snapshot => snapshot.exists ? snapshot.data().count : 0);
    if (counts.some((count, i) => !Number.isSafeInteger(count) || count < 0
        || count >= limitFor(state, entries[i].key))) return false;
    refs.forEach((ref, i) => tx.set(ref, {count: counts[i] + 1}));
    return true;
  });
}

module.exports = {LIMITS, buckets, limitFor, reserve};
