# 관리자 PC에서 포인트 적립 (Cloud Functions 배포 없음)

## 사용법
프로젝트 폴더의 PowerShell에서:

    .\scripts\points.ps1

위 명령은 미리보기만 합니다. 지급 후보 수, 총 포인트, 보고서 경로를 출력합니다.
내용을 확인한 뒤 실제 지급:

    .\scripts\points.ps1 -Apply

Node가 PATH에 없어도 현재 PC의 Codex 런타임 경로를 찾습니다. Firebase CLI 로그인이 필요하며 토큰은 기존 CLI 저장소에서만 사용하고 출력하거나 저장소에 복사하지 않습니다.
다른 PC에서는 Node 및 photo-service 의존성, build/firebase-tools의 Firebase CLI를 설치해야 합니다.
로그인한 관리자 계정은 Firestore 읽기/쓰기 IAM 권한이 있어야 합니다.

## 동작
- 현재 존재하는 전체 방문 기록을 조회합니다. 과거 기록도 대상이므로 미리보기를 먼저 확인하세요.
- observedTags가 유효하고 하나 이상인 평가: 100P. likedTags 및 높은 별점은 조건이 아닙니다.
- 같은 계정/카페는 최초 1회만 적립합니다. 기존 원장이 있으면 건너뜁니다.
- 수정·삭제 후 재등록, 프로그램 재실행 및 동시 실행으로 중복 적립되지 않습니다.
- 사용자와 카페 존재, 제외 상태, 현재 평가 내용, 정상 잔액을 다시 검증합니다.
- 잔액, 적립 내역, 중복 방지 원장은 하나의 Firestore 트랜잭션으로 저장됩니다.
- 명령 중간 실패 시 이미 완료한 지급은 유지됩니다. 다시 실행하면 남은 대상만 처리합니다.
- build/points-audit에 실행 보고서를 저장합니다. 보고서는 Git에 포함되지 않습니다.
- 프로그램을 실행할 때만 적립됩니다. 자동 스케줄러와 Cloud Functions는 사용하지 않습니다.
- 실제 방문 여부를 GPS/결제로 증명하는 기능은 아닙니다.

## Firebase 경로
- users/{uid}.pointBalance
- users/{uid}/point_history/{visitRecordId}
- users/{uid}/point_awards/{cafeId}

리워드샵/프로필은 잔액을 실시간 구독하고, 내역 화면은 열 때 최근 100건을 조회합니다.
실제 상품 교환은 비활성 상태로 유지됩니다.
관리자 프로그램은 point_config/control 스위치를 사용하지 않습니다. 관리자 실행 자체가 지급 승인입니다.
공용 awardVisit 함수는 기본적으로 enabled 스위치를 요구하며, 관리자 프로그램만 이를 명시적으로 해제합니다.

## 보안 규칙
firestore.rules 및 firebase.points.json은 기본 데이터베이스용입니다.
잔액 생성/변경 및 사용자 문서 삭제, 포인트 내역/원장 쓰기는 앱에서 불가능합니다.
프로필/배지/평가/커뮤니티/피드백은 로그인한 계정 소유권을 검사합니다.
카페 조회는 로그인 사용자에게 허용하고 수정은 관리자만 가능합니다.
기존 공개 읽기·쓰기 규칙을 대체합니다. 앱의 관리자용 일괄 카페 수정 코드는 이 규칙에서 차단됩니다.

    node build/firebase-tools/node_modules/firebase-tools/lib/bin/firebase.js deploy --only firestore:rules --project capstone-8ca90 --config firebase.points.json

사진용 별도 데이터베이스 규칙은 변경하지 않습니다.

## 검증

    node build/firebase-tools/node_modules/firebase-tools/lib/bin/firebase.js emulators:exec --only firestore --project demo-cafefit-points --config firebase.points-tests.json "node --test photo-service/test/points.test.js"

Java/JBR bin을 PATH에 추가해야 합니다. 실제 데이터는 건드리지 않고 로컬 Firestore에서 동시 지급, 재실행, 계정 분리, 보안 규칙을 검증합니다.
운영 데이터 조회·쓰기에는 기존 Firestore 할당량이 사용됩니다. 결제 계정은 연결하지 않습니다.
