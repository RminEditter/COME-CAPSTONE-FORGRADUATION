"use strict";
const {test} = require("node:test");
const assert = require("node:assert/strict");
const {createHandler, imageUrl} = require("../service");

function fixture(overrides = {}) {
  const calls = []; const reservations = [];
  const deps = {
    db: {doc: () => ({get: async () => ({exists: true, data: () => ({enabled: true, googlePlaceId: "place123"})})})},
    verifyToken: async () => ({uid: "user1"}), enabled: () => true, apiKey: () => "server-secret",
    reserveSlot: async (_db, _uid, kind) => { reservations.push(kind); return true; },
    fetchImpl: async (url, options) => {
      calls.push({url, options});
      if (url.endsWith("/places/place123")) return Response.json({photos: [{name: "places/place123/photos/ref123",
        authorAttributions: [{displayName: "Photographer", uri: "https://www.google.com/maps/contrib/123"}]}]});
      if (url.includes("/media?")) return Response.json({photoUri: "https://lh3.googleusercontent.com/photo"});
      return new Response(Buffer.from("test-image"), {headers: {"content-type": "image/jpeg"}});
    }, ...overrides
  };
  const handler = createHandler(deps);
  const invoke = async (body = {cafeId: "cafe1"}, auth = "Bearer valid") => {
    const res = {code: 200, headers: {}, set(k, v) {this.headers[k] = v;},
      status(code) {this.code = code; return this;}, json(value) {this.body = value; return this;}};
    await handler({method: "POST", body, get: () => auth}, res);
    return res;
  };
  return {calls, reservations, invoke};
}
test("successful photo reserves each SKU once, uses narrow fields, preserves attribution", async () => {
  const f = fixture(); const response = await f.invoke();
  assert.equal(response.body.status, "ok");
  assert.deepEqual(f.reservations, ["details", "photo"]);
  assert.equal(f.calls.length, 3);
  assert.equal(f.calls[0].options.headers["X-Goog-FieldMask"], "photos,attributions");
  assert.equal(f.calls[2].options.headers, undefined);
  assert.equal(response.body.authors[0].name, "Photographer");
  assert.match(response.headers["Cache-Control"], /no-store/);
  assert.ok(!JSON.stringify(response.body).includes("server-secret"));
  assert.ok(!JSON.stringify(response.body).includes("ref123"));
});
test("quota denial prevents all Google requests", async () => {
  const f = fixture({reserveSlot: async () => false});
  assert.equal((await f.invoke()).body.status, "limit");
  assert.equal(f.calls.length, 0);
  const namedCafe = fixture();
  assert.equal((await namedCafe.invoke({cafeId: "카페 온유 (궁동점)"})).body.status, "ok");
});
test("photo quota denial between stages prevents billable media request", async () => {
  const f = fixture({reserveSlot: async (_db, _uid, kind) => kind === "details"});
  assert.equal((await f.invoke()).body.status, "limit");
  assert.equal(f.calls.length, 1);
});
test("unconfigured, unauthenticated and invalid cafe requests cannot spend quota", async () => {
  for (const overrides of [{enabled: () => false}, {apiKey: () => ""},
    {verifyToken: async () => {throw new Error("bad token");}}]) {
    const f = fixture(overrides); await f.invoke(); assert.equal(f.calls.length, 0);
  }
  const f = fixture();
  assert.equal((await f.invoke({cafeId: "../other"})).code, 400);
  assert.equal((await f.invoke(undefined, "")).code, 401);
  assert.equal(f.calls.length, 0);
});
test("upstream errors and unavailable budget store never retry or refund", async () => {
  let attempts = 0;
  const f = fixture({fetchImpl: async () => {attempts++; throw new Error("timeout");}});
  assert.equal((await f.invoke()).body.status, "unavailable");
  assert.equal(attempts, 1);
  assert.deepEqual(f.reservations, ["details"]);
  const g = fixture({reserveSlot: async () => {throw new Error("Firestore unavailable");}});
  assert.equal((await g.invoke()).body.status, "unavailable");
  assert.equal(g.calls.length, 0);
});
test("unmapped cafe and no-photo results do not request media", async () => {
  const f = fixture({db: {doc: () => ({get: async () => ({exists: false})})}});
  assert.equal((await f.invoke()).body.status, "unmapped");
  assert.equal(f.calls.length, 0);
  const g = fixture({fetchImpl: async () => Response.json({})});
  assert.equal((await g.invoke()).body.status, "no_photo");
  assert.deepEqual(g.reservations, ["details"]);
});
test("image source validation rejects external hosts and HTTP", () => {
  assert.equal(imageUrl("https://lh3.googleusercontent.com/photo"), "https://lh3.googleusercontent.com/photo");
  assert.equal(imageUrl("https://googleusercontent.com.attacker.test/x"), null);
  assert.equal(imageUrl("http://lh3.googleusercontent.com/photo"), null);
  assert.equal(imageUrl("https://127.0.0.1/x"), null);
});
