"use strict";

// CafeFit reward administration
// Run on a trusted administrator PC only.
// Requires firebase-admin and Google credentials.

const { initializeApp, applicationDefault } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const readline = require("node:readline/promises");

const { stdin, stdout } = require("node:process");

const PROJECT_ID = process.env.CAFEFIT_PROJECT_ID;

const REWARDS = Object.freeze({
  coffee: { name: "커피 교환권", cost: 3000 },
  dessert: { name: "디저트 교환권", cost: 5000 },
  cafe: { name: "카페 이용권", cost: 10000 }
});

if (!PROJECT_ID) {
  console.error(
    "CAFEFIT_PROJECT_ID 환경 변수를 설정하세요."
  );
  process.exit(1);
}

if (
  process.env.FIRESTORE_EMULATOR_HOST ||
  process.env.FIREBASE_AUTH_EMULATOR_HOST
) {
  console.error(
    "에뮬레이터 환경에서는 이 프로그램을 실행하지 않습니다."
  );
  process.exit(1);
}

initializeApp({
  credential: applicationDefault(),
  projectId: PROJECT_ID
});

const db = getFirestore();

const STATUS = Object.freeze({
  PENDING: "PENDING",
  APPROVED: "APPROVED",
  COMPLETED: "COMPLETED",
  REJECTED: "REJECTED"
});

function getReward(data) {
  const reward = REWARDS[data.rewardId];

  if (!reward) {
    throw new Error("등록되지 않은 교환 상품입니다.");
  }

  if (
    !Number.isSafeInteger(data.cost) ||
    data.cost !== reward.cost
  ) {
    throw new Error("교환 상품 가격이 올바르지 않습니다.");
  }

  return reward;
}

function validateRequest(data) {
  if (!data || typeof data.uid !== "string" ||
      !data.uid.trim()) {
    throw new Error("신청자 UID가 올바르지 않습니다.");
  }

  return getReward(data);
}

function validRequestId(id, data) {
  const prefix = data.uid + "_" + data.rewardId;
  if (id === prefix) return true; // 기존 신청 호환
  return id.startsWith(prefix + "_") &&
    /^[0-9a-f]{32}$/.test(id.slice(prefix.length + 1));
}

function requestRef(id) {
  if (
    typeof id !== "string" ||
    !/^[A-Za-z0-9_-]+$/.test(id)
  ) {
    throw new Error("신청 ID가 올바르지 않습니다.");
  }

  return db.collection("reward_requests").doc(id);
}

function displayDate(value) {
  if (!value) return "-";

  if (typeof value.toDate === "function") {
    return value.toDate().toLocaleString("ko-KR", {
      timeZone: "Asia/Seoul"
    });
  }

  return String(value);
}

async function listRequests() {
  const snapshot = await db
    .collection("reward_requests")
    .get();

  if (snapshot.empty) {
    console.log("교환 신청 내역이 없습니다.");
    return;
  }

  const rows = snapshot.docs.map(doc => {
    const data = doc.data();
    const reward = REWARDS[data.rewardId];

    return {
      requestId: doc.id,
      uid: data.uid || "-",
      reward: reward ? reward.name : "알 수 없음",
      cost: data.cost,
      status: data.status,
      createdAt: displayDate(data.createdAt)
    };
  });

  console.table(rows);
}

async function showRequest(id) {
  const snapshot = await requestRef(id).get();

  if (!snapshot.exists) {
    throw new Error("신청 문서를 찾을 수 없습니다.");
  }

  const data = snapshot.data();

  console.log({
    requestId: snapshot.id,
    uid: data.uid,
    rewardId: data.rewardId,
    cost: data.cost,
    status: data.status,
    createdAt: displayDate(data.createdAt),
    approvedAt: displayDate(data.approvedAt),
    completedAt: displayDate(data.completedAt),
    rejectedAt: displayDate(data.rejectedAt)
  });
}

async function approve(id) {
  const ref = requestRef(id);

  const result = await db.runTransaction(async tx => {
    const requestSnap = await tx.get(ref);

    if (!requestSnap.exists) {
      throw new Error("신청이 존재하지 않습니다.");
    }

    const data = requestSnap.data();

    if (data.status !== STATUS.PENDING) {
      throw new Error(
        "대기 중인 신청만 승인할 수 있습니다."
      );
    }

    const reward = validateRequest(data);

    if (!validRequestId(id, data)) {
      throw new Error("신청 ID와 사용자 정보가 다릅니다.");
    }

    const userRef = db.collection("users").doc(data.uid);
    const userSnap = await tx.get(userRef);

    if (!userSnap.exists) {
      throw new Error("사용자 문서가 존재하지 않습니다.");
    }

    const userData = userSnap.data();
    const balance = userData.pointBalance;

    if (
      !Number.isSafeInteger(balance) ||
      balance < 0
    ) {
      throw new Error(
        "포인트 잔액이 없거나 올바르지 않습니다."
      );
    }

    if (balance < reward.cost) {
      throw new Error(
        "포인트 부족: 현재 " + balance + "P"
      );
    }

    const historyRef = userRef
      .collection("point_history")
      .doc("reward_debit_" + id);

    const historySnap = await tx.get(historyRef);

    if (historySnap.exists) {
      throw new Error(
        "이미 차감 내역이 존재합니다. 수동 확인이 필요합니다."
      );
    }

    tx.update(userRef, {
      pointBalance: FieldValue.increment(-reward.cost)
    });

    tx.create(historyRef, {
      type: "REWARD_DEBIT",
      requestId: id,
      rewardId: data.rewardId,
      points: -reward.cost,
      createdAt: FieldValue.serverTimestamp()
    });

    tx.update(ref, {
      status: STATUS.APPROVED,
      approvedAt: FieldValue.serverTimestamp(),
      chargedPoints: reward.cost
    });

    return {
      uid: data.uid,
      deducted: reward.cost,
      previousBalance: balance,
      newBalance: balance - reward.cost
    };
  });

  console.log("승인 및 포인트 차감 완료");
  console.log(result);
}

async function complete(id) {
  const ref = requestRef(id);

  await db.runTransaction(async tx => {
    const snapshot = await tx.get(ref);

    if (!snapshot.exists) {
      throw new Error("신청이 존재하지 않습니다.");
    }

    const data = snapshot.data();

    if (data.status !== STATUS.APPROVED) {
      throw new Error(
        "승인된 신청만 지급 완료 처리할 수 있습니다."
      );
    }

    const reward = validateRequest(data);

    if (data.chargedPoints !== reward.cost) {
      throw new Error("차감 포인트 기록이 일치하지 않습니다.");
    }

    tx.update(ref, {
      status: STATUS.COMPLETED,
      completedAt: FieldValue.serverTimestamp()
    });
  });

  console.log("기프티콘 지급 완료 처리되었습니다.");
}

async function reject(id) {
  const ref = requestRef(id);

  const result = await db.runTransaction(async tx => {
    const requestSnap = await tx.get(ref);

    if (!requestSnap.exists) {
      throw new Error("신청이 존재하지 않습니다.");
    }

    const data = requestSnap.data();
    const reward = validateRequest(data);

    if (data.status === STATUS.REJECTED) {
      throw new Error("이미 거절된 신청입니다.");
    }

    if (data.status === STATUS.COMPLETED) {
      throw new Error(
        "지급 완료된 신청은 거절할 수 없습니다."
      );
    }

    if (data.status === STATUS.PENDING) {
      tx.update(ref, {
        status: STATUS.REJECTED,
        rejectedAt: FieldValue.serverTimestamp(),
        refundedPoints: 0
      });

      return { refunded: 0 };
    }

    if (data.status !== STATUS.APPROVED) {
      throw new Error("처리할 수 없는 신청 상태입니다.");
    }

    if (data.chargedPoints !== reward.cost) {
      throw new Error("차감 포인트가 일치하지 않습니다.");
    }

    const userRef = db.collection("users").doc(data.uid);

    const refundRef = userRef
      .collection("point_history")
      .doc("reward_refund_" + id);

    const debitRef = userRef
      .collection("point_history")
      .doc("reward_debit_" + id);

    const userSnap = await tx.get(userRef);
    const debitSnap = await tx.get(debitRef);
    const refundSnap = await tx.get(refundRef);

    if (!userSnap.exists) {
      throw new Error("사용자 문서가 없습니다.");
    }

    if (!debitSnap.exists ||
        debitSnap.get("points") !== -reward.cost ||
        debitSnap.get("requestId") !== id) {
      throw new Error(
        "원본 차감 내역을 확인할 수 없습니다."
      );
    }

    if (refundSnap.exists) {
      throw new Error(
        "이미 환불 내역이 존재합니다."
      );
    }

    const balance = userSnap.get("pointBalance");

    if (!Number.isSafeInteger(balance) ||
        balance < 0 ||
        !Number.isSafeInteger(balance + reward.cost)) {
      throw new Error("포인트 잔액이 올바르지 않습니다.");
    }

    tx.update(userRef, {
      pointBalance: FieldValue.increment(reward.cost)
    });

    tx.create(refundRef, {
      type: "REWARD_REFUND",
      requestId: id,
      rewardId: data.rewardId,
      points: reward.cost,
      createdAt: FieldValue.serverTimestamp()
    });

    tx.update(ref, {
      status: STATUS.REJECTED,
      rejectedAt: FieldValue.serverTimestamp(),
      refundedPoints: reward.cost
    });

    return { refunded: reward.cost };
  });

  console.log("거절 처리 완료");
  console.log(result);
}

async function confirmAction(command, id) {
  const rl = readline.createInterface({
    input: stdin,
    output: stdout
  });

  try {
    console.log("\n프로젝트:", PROJECT_ID);
    console.log("작업:", command);
    console.log("신청 ID:", id);

    const answer = await rl.question(
      "실행하려면 YES를 입력하세요: "
    );

    if (answer !== "YES") {
      throw new Error("작업을 취소했습니다.");
    }
  } finally {
    rl.close();
  }
}

function printHelp() {
  console.log(`
CafeFit Reward Admin

Usage:
  node reward-admin.cjs list
  node reward-admin.cjs show <requestId>
  node reward-admin.cjs approve <requestId>
  node reward-admin.cjs complete <requestId>
  node reward-admin.cjs reject <requestId>

approve:
  Verify balance and deduct points.

complete:
  Run ONLY after the gift coupon has been sent.

reject:
  Reject pending request or refund approved request.
`);
}

async function main() {
  const [command, id, ...extra] = process.argv.slice(2);

  if (!command || command === "help") {
    printHelp();
    return;
  }

  if (extra.length > 0) {
    throw new Error("인자가 너무 많습니다.");
  }

  if (command === "list" && !id) {
    await listRequests();
    return;
  }

  if (command === "show" && id) {
    await showRequest(id);
    return;
  }

  if (!id) {
    throw new Error("신청 ID를 입력하세요.");
  }

  if (!["approve", "complete", "reject"].includes(command)) {
    throw new Error("지원하지 않는 명령어입니다.");
  }

  await showRequest(id);

  await confirmAction(command, id);

  if (command === "approve") {
    await approve(id);
  } else if (command === "complete") {
    await complete(id);
  } else if (command === "reject") {
    await reject(id);
  }
}

// Export trusted server operations for the local web dashboard.
module.exports = { db, REWARDS, STATUS, approve, reject, complete, validRequestId };

if (require.main === module) {
  main().catch(error => {
    console.error("오류:", error.message);
    process.exitCode = 1;
  });
}
