"use strict";

// Local-only CafeFit reward dashboard. NEVER expose this server to the Internet.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const { db, REWARDS, approve, reject, complete } = require("./reward-admin.cjs");

const HOST = "127.0.0.1";
const PORT = 8787;
const ACCESS_TOKEN = crypto.randomBytes(24).toString("hex");
const MAX_BODY = 4096;
const HTML = fs.readFileSync(path.join(__dirname, "admin.html"), "utf8");

function send(res, code, data) {
  const body = JSON.stringify(data);
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer"
  });
  res.end(body);
}

function sameOrigin(req) {
  const origin = req.headers.origin;
  return origin === `http://${HOST}:${PORT}`;
}

function authorized(req) {
  const provided = req.headers.authorization;
  const expected = `Bearer ${ACCESS_TOKEN}`;
  return typeof provided === "string" &&
    Buffer.byteLength(provided) === Buffer.byteLength(expected) &&
    crypto.timingSafeEqual(Buffer.from(provided), Buffer.from(expected));
}

async function parseBody(req) {
  let body = "";
  for await (const chunk of req) {
    body += chunk;
    if (Buffer.byteLength(body) > MAX_BODY) throw new Error("요청 크기 초과");
  }
  return JSON.parse(body);
}

async function getRequests() {
  const snapshot = await db.collection("reward_requests").get();
  const rows = snapshot.docs.map(doc => {
    const d = doc.data();
    const reward = REWARDS[d.rewardId];
    return {
      id: doc.id,
      uid: d.uid || "",
      name: reward ? reward.name : "알 수 없는 상품",
      cost: d.cost,
      status: d.status,
      createdAt: d.createdAt && d.createdAt.toDate ? d.createdAt.toDate().toISOString() : null
    };
  });
  rows.sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));
  return rows;
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${HOST}:${PORT}`);
    const allowedHost = [`${HOST}:${PORT}`, `localhost:${PORT}`];
    if (!allowedHost.includes(req.headers.host)) return send(res, 403, { error: "허용되지 않은 호스트" });

    if (req.method === "GET" && url.pathname === "/") {
      res.writeHead(200, {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-store",
        "Content-Security-Policy": "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'; base-uri 'none'; form-action 'none'",
        "X-Content-Type-Options": "nosniff",
        "Referrer-Policy": "no-referrer"
      });
      return res.end(HTML);
    }

    if (req.method === "GET" && url.pathname === "/admin.js") {
      const js = fs.readFileSync(path.join(__dirname, "admin.js"));
      res.writeHead(200, { "Content-Type": "application/javascript; charset=utf-8", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" });
      return res.end(js);
    }

    if (url.pathname.startsWith("/api/")) {
      if (!authorized(req)) return send(res, 401, { error: "접속 코드가 올바르지 않습니다." });
      if (req.method === "GET" && url.pathname === "/api/requests") {
        return send(res, 200, { requests: await getRequests() });
      }
      if (req.method === "POST" && url.pathname === "/api/action") {
        if (!sameOrigin(req)) return send(res, 403, { error: "잘못된 요청 출처" });
        const body = await parseBody(req);
        const { action, id } = body;
        if (typeof id !== "string" || !/^[A-Za-z0-9_-]{1,200}$/.test(id)) {
          return send(res, 400, { error: "신청 ID가 올바르지 않습니다." });
        }
        if (action === "approve") await approve(id);
        else if (action === "reject") await reject(id);
        else if (action === "complete") await complete(id);
        else return send(res, 400, { error: "지원하지 않는 작업" });
        return send(res, 200, { ok: true });
      }
    }
    return send(res, 404, { error: "페이지를 찾을 수 없습니다." });
  } catch (error) {
    console.error("관리자 작업 오류:", error);
    return send(res, 400, { error: error.message || "요청 실패" });
  }
});

server.listen(PORT, HOST, () => {
  console.log(`\nCafeFit 관리자 페이지: http://${HOST}:${PORT}`);
  console.log(`접속 코드: ${ACCESS_TOKEN}`);
  console.log("관리자 PC에서만 접속하세요. 종료: Ctrl+C\n");
});
