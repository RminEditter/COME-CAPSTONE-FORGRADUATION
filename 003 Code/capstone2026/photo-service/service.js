"use strict";

const {reserve} = require("./budget");
const MAX_BYTES = 2 * 1024 * 1024;

function httpsUrl(value) {
  try { const url = new URL(value); return url.protocol === "https:" ? url.href : null; }
  catch { return null; }
}

function imageUrl(value) {
  const safe = httpsUrl(value);
  if (!safe) return null;
  const host = new URL(safe).hostname;
  return ["googleusercontent.com", "ggpht.com"].some(domain => host === domain || host.endsWith(`.${domain}`)) ? safe : null;
}

async function limitedBytes(response) {
  if (!response.ok || Number(response.headers.get("content-length")) > MAX_BYTES) throw new Error("Invalid image");
  const chunks = [];
  let size = 0;
  for await (const chunk of response.body) {
    size += chunk.length;
    if (size > MAX_BYTES) throw new Error("Image too large");
    chunks.push(Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}

function createHandler({db, verifyToken, apiKey, enabled, fetchImpl = fetch, reserveSlot = reserve}) {
  return async (req, res) => {
    res.set("Cache-Control", "private, no-store, max-age=0");
    res.set("X-Content-Type-Options", "nosniff");
    if (req.method !== "POST") return res.status(405).json({status: "method_not_allowed"});
    if (!enabled()) return res.status(200).json({status: "disabled"});
    let user;
    try {
      const token = /^Bearer (\S+)$/.exec(req.get("authorization") || "")?.[1];
      if (!token) throw new Error("No token");
      user = await verifyToken(token);
      if (!user.uid || user.firebase?.sign_in_provider === "anonymous") throw new Error("Sign in required");
    } catch { return res.status(401).json({status: "unauthorized"}); }
    const cafeId = req.body?.cafeId;
    if (typeof cafeId !== "string" || !cafeId.trim() || Buffer.byteLength(cafeId, "utf8") > 1500
        || cafeId.includes("/") || cafeId === "." || cafeId === ".." || /^__.*__$/.test(cafeId)) {
      return res.status(400).json({status: "invalid_cafe"});
    }
    try {
      // Mapping is in a separate DENY-ALL Firestore database, never client-controlled.
      const mapping = await db.doc(`cafes/${cafeId}`).get();
      const placeId = mapping.exists && mapping.data().enabled === true ? mapping.data().googlePlaceId : null;
      if (typeof placeId !== "string" || !/^[A-Za-z0-9_-]{1,255}$/.test(placeId)) {
        return res.status(200).json({status: "unmapped"});
      }
      const key = apiKey();
      if (!key) return res.status(200).json({status: "disabled"});
      if (!await reserveSlot(db, user.uid, "details")) return res.status(200).json({status: "limit"});
      // photos + attributions ONLY: Place Details Essentials IDs Only SKU.
      // There are intentionally no automatic search, broad masks, or HTTP retries.
      const detail = await fetchImpl(`https://places.googleapis.com/v1/places/${placeId}`, {
        headers: {"X-Goog-Api-Key": key, "X-Goog-FieldMask": "photos,attributions"},
        redirect: "error", signal: AbortSignal.timeout(8000)
      });
      if (!detail.ok) throw new Error("Details failed");
      const data = await detail.json();
      const photo = data.photos?.[0];
      if (!photo) return res.status(200).json({status: "no_photo"});
      if (typeof photo.name !== "string" || !photo.name.startsWith(`places/${placeId}/photos/`)
          || !/^places\/[A-Za-z0-9_-]+\/photos\/[A-Za-z0-9_-]+$/.test(photo.name)) throw new Error("Invalid photo name");
      if (!await reserveSlot(db, user.uid, "photo")) return res.status(200).json({status: "limit"});
      const media = await fetchImpl(`https://places.googleapis.com/v1/${photo.name}/media?maxWidthPx=800&maxHeightPx=600&skipHttpRedirect=true`, {
        headers: {"X-Goog-Api-Key": key}, redirect: "error", signal: AbortSignal.timeout(8000)
      });
      if (!media.ok) throw new Error("Photo failed");
      const url = imageUrl((await media.json()).photoUri);
      if (!url) throw new Error("Invalid image host");
      // No API key forwarded to Google's image CDN. No URLs or images persisted.
      const download = await fetchImpl(url, {redirect: "error", signal: AbortSignal.timeout(8000)});
      const mime = download.headers.get("content-type")?.split(";")[0];
      if (!["image/jpeg", "image/png", "image/webp", "image/gif"].includes(mime)) throw new Error("Invalid image type");
      const bytes = await limitedBytes(download);
      const authors = (photo.authorAttributions || []).map(a => ({name: String(a.displayName || ""), url: httpsUrl(a.uri)}));
      const providers = (data.attributions || []).map(a => ({name: String(a.provider || ""), url: httpsUrl(a.providerUri)}));
      return res.status(200).json({status: "ok", imageBase64: bytes.toString("base64"),
        authors, providers, googleMapsUrl: `https://www.google.com/maps/search/?api=1&query=cafe&query_place_id=${placeId}`});
    } catch {
      // Fail closed if Firestore, quota reservation, Google, or decoding is unavailable.
      // Do not log upstream URLs, keys, user tokens, or Google content.
      return res.status(200).json({status: "unavailable"});
    }
  };
}

module.exports = {createHandler, imageUrl, limitedBytes};
