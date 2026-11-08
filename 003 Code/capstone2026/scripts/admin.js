"use strict";
let token = "";
let requests = [];
const $ = id => document.getElementById(id);
const labels = { PENDING: "승인 대기", APPROVED: "지급 대기", COMPLETED: "지급 완료", REJECTED: "거절" };
function notice(message) { $("notice").textContent = message; }
async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "요청 실패");
  return result;
}
function cell(row, value, className = "") {
  const td = document.createElement("td");
  td.textContent = String(value);
  td.className = className;
  row.appendChild(td);
  return td;
}
function actionButton(parent, label, action, id, style) {
  const button = document.createElement("button");
  button.textContent = label;
  if (style) button.className = style;
  button.addEventListener("click", async () => {
    const description = action === "approve" ? "승인하고 포인트를 차감" : action === "reject" ? "거절하고 필요 시 환불" : "기프티콘 발송 완료로 기록";
    if (!confirm(`${description}할까요?\n신청 ID: ${id}`)) return;
    if (action === "complete" && !confirm("실제로 기프티콘을 발송했나요? 발송한 경우에만 확인을 누르세요.")) return;
    button.disabled = true;
    try {
      await api("/api/action", { method: "POST", body: JSON.stringify({ action, id }) });
      notice(`${label} 처리 완료`);
      await load();
    } catch (e) { notice(`오류: ${e.message}`); button.disabled = false; }
  });
  parent.appendChild(button);
}
function render() {
  $("pending").textContent = requests.filter(r => r.status === "PENDING").length;
  $("approved").textContent = requests.filter(r => r.status === "APPROVED").length;
  $("completed").textContent = requests.filter(r => r.status === "COMPLETED").length;
  const tbody = $("requests");
  tbody.replaceChildren();
  const shown = requests.filter(r => $("filter").value === "ALL" || r.status === $("filter").value);
  for (const item of shown) {
    const tr = document.createElement("tr");
    cell(tr, item.createdAt ? new Date(item.createdAt).toLocaleString("ko-KR") : "-");
    cell(tr, item.name);
    cell(tr, `${Number(item.cost).toLocaleString("ko-KR")} P`);
    cell(tr, labels[item.status] || item.status);
    cell(tr, item.id, "id");
    const actions = document.createElement("td");
    actions.className = "actions";
    if (item.status === "PENDING") {
      actionButton(actions, "승인", "approve", item.id);
      actionButton(actions, "거절", "reject", item.id, "danger");
    } else if (item.status === "APPROVED") {
      actionButton(actions, "지급 완료", "complete", item.id);
      actionButton(actions, "거절·환불", "reject", item.id, "danger");
    } else { actions.textContent = "처리 완료"; }
    tr.appendChild(actions);
    tbody.appendChild(tr);
  }
  if (!shown.length) {
    const tr = document.createElement("tr");
    const td = cell(tr, "신청 내역이 없습니다."); td.colSpan = 6; tbody.appendChild(tr);
  }
}
async function load() {
  const result = await api("/api/requests");
  requests = result.requests;
  render();
}
$("connect").addEventListener("click", async () => {
  token = $("token").value.trim();
  try {
    await load();
    $("token").value = "";
    $("login").classList.add("hidden");
    $("dashboard").classList.remove("hidden");
    $("logout").classList.remove("hidden");
    notice("관리자 연결 성공");
  } catch (e) { token = ""; notice(`접속 실패: ${e.message}`); }
});
$("logout").addEventListener("click", () => location.reload());
$("refresh").addEventListener("click", async () => { try { await load(); notice("새로고침 완료"); } catch(e) { notice(e.message); } });
$("filter").addEventListener("change", render);
