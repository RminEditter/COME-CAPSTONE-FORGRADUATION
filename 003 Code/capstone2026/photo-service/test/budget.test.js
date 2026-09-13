"use strict";
const {test} = require("node:test");
const assert = require("node:assert/strict");
const {reserve, buckets, limitFor} = require("../budget");

// Serial transaction fake exercises the contract; optional real Firestore test below.
function memoryDb(config = {enabled: true}) {
  const data = new Map([["config/control", config]]);
  let queue = Promise.resolve();
  const snapshot = key => ({exists: data.has(key), data: () => data.get(key)});
  return {data, doc: key => key, runTransaction: fn => {
    const next = queue.then(async () => {
      const writes = [];
      const result = await fn({get: async key => snapshot(key),
        getAll: async (...keys) => keys.map(snapshot), set: (key, value) => writes.push([key, value])});
      writes.forEach(([key, value]) => data.set(key, value));
      return result;
    });
    queue = next.catch(() => {});
    return next;
  }};
}
const now = new Date("2026-09-15T12:00:00Z");

test("concurrent users cannot exceed shared daily limit", async () => {
  const db = memoryDb();
  const results = await Promise.all(Array.from({length: 100}, (_, i) => reserve(db, `user${i}`, "photo", now)));
  assert.equal(results.filter(Boolean).length, 25);
  assert.equal(db.data.get("usage/photo-day-2026-09-15").count, 25);
});
test("monthly boundary, new device and UTC/Pacific mismatch cannot bypass cap", async () => {
  const db = memoryDb();
  const boundary = new Date("2026-10-01T01:00:00Z");
  db.data.set("usage/photo-month-pacific-2026-09", {count: 800});
  assert.equal(await reserve(db, "new-device", "photo", boundary), false);
  assert.equal(await reserve(db, "new-device", "photo", new Date("2026-10-01T12:00:00Z")), true);
});
test("server count survives clients and failed upstream attempts; five per user", async () => {
  const db = memoryDb();
  for (let i = 0; i < 5; i++) assert.equal(await reserve(db, "same-user", "details", now), true);
  assert.equal(await reserve(db, "same-user", "details", now), false);
  assert.equal(await reserve(db, "other-user", "details", now), true);
});
test("disabled, zero cap, corrupt counters, transaction errors fail closed", async () => {
  assert.equal(await reserve(memoryDb({enabled: false}), "u", "photo", now), false);
  assert.equal(await reserve(memoryDb({enabled: true, month: 0}), "u", "photo", now), false);
  const db = memoryDb();
  db.data.set(`usage/${buckets(now, "u", "photo")[0].id}`, {count: "0"});
  assert.equal(await reserve(db, "u", "photo", now), false);
  db.runTransaction = async () => { throw new Error("offline"); };
  await assert.rejects(reserve(db, "u", "photo", now));
});
test("remote configuration cannot raise hard limits or accept invalid limits", () => {
  assert.equal(limitFor({month: 9000}, "month"), 800);
  assert.equal(limitFor({day: -1}, "day"), 0);
  assert.equal(limitFor({day: "25"}, "day"), 0);
});

test("real Firestore transactions respect cap under contention", {
  skip: !process.env.FIRESTORE_EMULATOR_HOST
}, async () => {
  const {initializeApp, deleteApp} = require("firebase-admin/app");
  const {getFirestore} = require("firebase-admin/firestore");
  const app = initializeApp({projectId: "demo-cafefit-photos"}, "budget-test");
  const db = getFirestore(app, "cafe-photo-guard");
  const denied = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/demo-cafefit-photos/databases/cafe-photo-guard/documents/usage/attack`, {
    method: "PATCH", headers: {"Content-Type": "application/json"},
    body: JSON.stringify({fields: {count: {integerValue: "0"}}})
  });
  assert.equal(denied.status, 403, "Client must not be able to reset counters");
  await db.recursiveDelete(db.collection("usage"));
  await db.doc("config/control").set({enabled: true, day: 3});
  const results = await Promise.all(Array.from({length: 12}, (_, i) => reserve(db, `u${i}`, "photo", now).catch(() => false)));
  assert.equal(results.filter(Boolean).length, 3);
  assert.equal((await db.doc("usage/photo-day-2026-09-15").get()).data().count, 3);
  await deleteApp(app);
});
