"use strict";
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {getFirestore} = require("firebase-admin/firestore");
const {onRequest} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {createHandler} = require("./service");

const app = initializeApp();
const key = defineSecret("GOOGLE_PLACES_API_KEY");
const db = getFirestore(app, "cafe-photo-guard");

exports.cafePhoto = onRequest({region: "asia-northeast3", minInstances: 0, maxInstances: 1,
  concurrency: 10, timeoutSeconds: 45, memory: "256MiB", secrets: [key], cors: false,
  invoker: "public"}, createHandler({db,
  verifyToken: token => getAuth(app).verifyIdToken(token, true),
  apiKey: () => key.value(), enabled: () => process.env.PHOTOS_ENABLED === "true"
}));
