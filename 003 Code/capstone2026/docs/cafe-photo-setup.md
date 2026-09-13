# CafeFit 메인 화면 · Google 사진 연결

## 현재 구현

- 브라운·아이보리 메인, 상황 선택(공부/데이트/혼자), 추천 태그, 즐겨찾기, 취향 설문 바로가기, 카페 상세, 지도 보기, 최근 본 카페, 실제 방문 평점 순위.
- 상황 선택은 이번 탐색에만 적용합니다. 설문 DB는 수정하지 않습니다. '추천 전체 보기'와 검색에도 선택한 상황을 전달합니다.
- 대표 추천 카드 **한 장**만 사진 API를 호출합니다. 목록 썸네일은 컵 아이콘으로 유지하여 한도를 아낍니다. 시안의 카페 이름·평점·사진을 운영 데이터로 넣지 않았습니다.
- 연결 설정이 비어 있으면 Google 또는 사진 서버에 요청하지 않고 기본 이미지를 표시합니다. 나머지 기능은 기존 Firebase 데이터로 작동합니다.
- Google 사진 카드의 지도 버튼은 Google Maps 웹/앱으로 연결합니다. 기존 MapLibre 지도 탭에는 Google 사진/데이터를 넣지 않습니다.

## 무료 한도 보호의 범위

Google Places Photos의 2026-09-13 공식 가격표는 월 1,000건 무료입니다. 서버는 이를 전부 사용하지 않고 아래 상한에서 차단합니다.

| 범위 | 상한 |
|---|---:|
| 전체 사용자 합산, 월 | 800회 |
| 전체 사용자 합산, UTC 하루 | 25회 |
| 한 계정, UTC 하루 | 5회 |

장소 상세 요청과 사진 요청에 각각 적용합니다. 월 카운터는 UTC와 미국 태평양 시간 둘 다 검사합니다. 낮은 일 한도도 추가 안전 여유를 제공합니다. 예약을 먼저 성공시킨 후에만 Google을 호출합니다. 동시 요청은 Firestore 트랜잭션으로 직렬화되며, 실패하거나 타임아웃된 호출도 차감한 채 유지하고 자동 재시도하지 않습니다. 카운터 조회/쓰기 실패, 잘못된 카운터, 한도 도달, 비활성화 상태에서는 요청을 차단합니다.

이 제한은 **이 서버를 거친 요청에 대한 제한**입니다. Google의 무료 사용량은 결제 계정의 다른 프로젝트 사용량과 합산될 수 있으므로, 같은 결제 계정에서 다른 서비스가 동일 SKU를 사용하고 있으면 전체 무료 한도를 보장할 수 없습니다. 이번 기능 전용 API 키를 쓰고 다른 경로로 사진 API를 호출하지 마세요. 이미 사용 중인 양이 있다면 그보다 낮게 상한을 설정하거나 연결을 비활성화해야 합니다.

또한 Google 사진 API 무료 한도와 **Firebase Functions 실행·네트워크·Secret Manager·Firestore 운영 비용은 별개**입니다. 별도 이름의 Firestore DB는 기존 기본 DB의 무료 할당량에 포함되지 않을 수 있습니다. 이 구성은 전체 클라우드 청구액 0원을 보장하지 않습니다. 결제 계정 연결이 필요하며, 예산 알림 자체는 지출을 차단하는 기능이 아닙니다.

## 사용자가 준비해야 하는 항목

1. Firebase 프로젝트 `capstone-8ca90`의 관리자 접근 권한과 Blaze/결제 연결.
2. Google Cloud에서 **Places API (New)** 활성화 및 API 키 생성. 키의 API 제한은 Places API로 지정합니다. 앱이나 Git에 키를 넣지 않습니다.
3. 동일 결제 계정의 사진 SKU 사용량을 확인하고, 이 서버 외의 사용이 무료 여유를 소진하지 않는지 확인합니다.
4. 먼저 연결할 카페의 **Firestore 카페 문서 ID ↔ Google Place ID** 목록. 이름이 같은 매장을 잘못 연결하지 않도록 주소도 확인합니다. [공식 Place ID 안내/찾기 도구](https://developers.google.com/maps/documentation/places/web-service/place-id)를 사용할 수 있습니다.
5. 사용자에게 공개할 앱 이용약관·개인정보처리방침. [Google Places 정책](https://developers.google.com/maps/documentation/places/web-service/policies)에 맞는 내용을 준비합니다.

키를 대화에 붙여넣지 마세요. 아래 Secret Manager 명령이 로컬에서 안전하게 입력받습니다. 앱 설정에 필요한 값은 배포된 **서버 URL**뿐입니다.

## 배포 순서

Node.js 22, Firebase CLI, Google Cloud CLI가 설치된 환경에서 저장소 루트를 기준으로 실행합니다. 아래 명령은 가이드이며 이번 작업에서 실제 클라우드 배포나 결제 변경은 하지 않았습니다.

### 1. 보호 DB 만들기

기존 카페 DB는 그대로 두고, 카운터와 Place ID 매핑만 별도 DB에 보관합니다. 기존 운영 DB의 허용 규칙이 카운터에 영향을 주지 않도록 하기 위한 분리입니다.

```powershell
gcloud firestore databases create --project=capstone-8ca90 --database=cafe-photo-guard --location=asia-northeast3 --type=firestore-native
firebase deploy --project capstone-8ca90 --config firebase.photos.json --only firestore
```

`firebase.photos.json`의 Firestore 대상은 `cafe-photo-guard` 하나뿐입니다. 기본 DB 규칙에 이 deny-all 파일을 덮어쓰면 앱의 기존 기능이 차단되므로 대상 DB를 바꾸지 마세요. 서버 서비스 계정은 해당 DB를 읽고 쓸 IAM 권한이 필요합니다. 클라이언트 규칙은 모든 읽기·쓰기를 거부합니다.

### 2. 키와 서버 등록

```powershell
firebase functions:secrets:set GOOGLE_PLACES_API_KEY --project capstone-8ca90
cd photo-service
npm install
Copy-Item .env.example .env
cd ..
firebase deploy --project capstone-8ca90 --config firebase.photos.json --only functions:cafe-photos
```

처음 `.env`의 `PHOTOS_ENABLED=false`를 유지합니다. 이 상태에서는 Google 호출이 없습니다. 배포 시 출력된 `cafePhoto` HTTPS URL을 기록합니다. Functions는 기존 Firebase Auth의 로그인 토큰을 검증합니다. Cloud Run 호출 권한은 public이어야 하지만, 실제 사진 요청은 앱 로그인 토큰 없이는 거절됩니다.

### 3. 매장 매핑과 상한 설정

Google Cloud Firestore 콘솔에서 **`cafe-photo-guard` DB**를 선택한 뒤 다음 문서를 관리자로 만듭니다.

```text
config/control
  enabled: false       (boolean)
  month: 800           (number)
  day: 25              (number)
  userDay: 5           (number)

cafes/{기존 앱의 카페 문서 ID}
  enabled: true        (boolean)
  googlePlaceId: "실제 Google Place ID"   (string)
```

문서 ID가 기존 `cafes` 문서와 정확히 같아야 합니다. Google 검색을 앱 실행마다 자동 수행하지 않습니다. 매핑이 없거나 비활성화된 카페는 기본 이미지를 표시합니다. 사진 URL이나 Google 응답 전문을 문서에 저장하지 않습니다.

상한은 코드의 800/25/5를 초과하도록 설정해도 올라가지 않습니다. 더 낮은 정수로는 낮출 수 있습니다. 0이면 해당 범위의 호출을 막습니다. `usage` 문서들은 서버가 생성하며, 삭제/초기화하면 보호가 깨지므로 운영 중 수정하지 마세요. 월/일이 바뀌면 새 문서가 자동 사용됩니다.

### 4. 앱에 서버 URL 연결

Git에서 제외된 루트 `local.properties`에 추가합니다.

```properties
cafe.photo.endpoint=https://asia-northeast3-capstone-8ca90.cloudfunctions.net/cafePhoto
```

위 주소는 형식 예시입니다. **실제 배포 결과 URL을 사용**하세요. URL은 HTTPS이며 끝 경로까지 입력합니다. API 키는 이 파일에도 넣지 않습니다.

이후 앱을 다시 빌드합니다.

```powershell
./gradlew.bat :app:assembleDebug
```

### 5. 활성화 및 실제 연결 확인

설정·요금·매장 매핑을 확인한 다음 `photo-service/.env`를 `PHOTOS_ENABLED=true`로 바꾸고 Functions만 다시 배포합니다. 마지막으로 보호 DB의 `config/control.enabled`를 `true`로 바꿉니다.

실제 계정으로 로그인해서 매핑된 카페가 추천될 때 사진/작성자 출처가 표시되는지 확인합니다. `usage`의 details/photo 카운터가 각각 1씩 증가해야 합니다. 사진이 없는 카페는 details만 증가할 수 있습니다. 앱은 서버가 반환한 이미지를 현재 화면에만 표시하며, 파일 저장·디스크 캐시·백그라운드 미리 불러오기를 하지 않습니다.

한도를 시험할 때는 `day: 1`로 낮춰 이미 한 번 사용한 날의 후속 요청이 차단되는지 확인하세요. 카운터를 지워서 테스트하지 마세요. 중단하려면 `config/control.enabled=false`로 바꾸면 다음 예약부터 차단됩니다. 이미 외부에 전송된 호출은 취소되지 않습니다.

## 검증 명령

```powershell
node --test photo-service/test/*.test.js
firebase emulators:exec --only firestore --project demo-cafefit-photos --config firebase.photos.json "node --test photo-service/test/*.test.js"
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.capstone2026.HomeLayoutTest"
```

일반 Node 테스트는 동시 요청, 월 경계, 계정별 제한, 잘못된 카운터, 실패 시 차감 유지, 무인증 차단, Google 응답/작성자 전달, 재시도 금지를 검사합니다. Firestore 에뮬레이터가 있으면 실제 트랜잭션 경합과 클라이언트 카운터 쓰기 거절도 검사합니다. 테스트 데이터는 demo 프로젝트/에뮬레이터만 사용합니다.

## 구현 검증 결과 (2026-09-13)

- Debug APK 빌드 성공, Android Lint 오류 0개. 프로젝트 전체에는 기존 경고를 포함해 282개의 경고가 남아 있습니다.
- Android JVM 단위 테스트 18개 통과: 기존 추천/계정 식별 테스트와 상황 선택의 설문 보존·잘못된 인자 처리를 포함합니다.
- Android 15 에뮬레이터에서 메인 레이아웃 테스트 4개 통과: 추천/즐겨찾기/로딩 상태, 설문·실패 시 액션, 320dp·글꼴 1.6배·야간 설정, 사진 상태 초기화.
- Node 서버 테스트 13개 모두 통과. Firestore 에뮬레이터에서 실제 동시 트랜잭션 경합과 카운터 클라이언트 쓰기 거절을 확인했습니다. Google 응답은 테스트용 대체 응답이며 실제 유료 API를 호출하지 않았습니다.
- 미리보기는 실제 XML에 예시 데이터를 바인딩한 이미지로 `app/build/home-previews/home-360-top.png`와 `home-360-bottom.png`에 있습니다. 예시 카페·평점은 운영 DB에 기록하지 않았습니다.
- 실제 Google API 키/Cloud Functions 배포/카페 Place ID 매핑은 아직 설정하지 않았습니다. 따라서 실계정 사진 로딩과 카페 매칭 정확도는 연결 후 확인해야 합니다. 현재 APK의 `CAFE_PHOTO_ENDPOINT`는 빈 값으로, 사진 서버 요청이 비활성화되어 있습니다.

## 공식 근거 (2026-09-13 확인)

- [사진 요금표](https://developers.google.com/maps/billing-and-pricing/pricing): Place Details Photos 월 무료 1,000건.
- [장소 상세조회 필드](https://developers.google.com/maps/documentation/places/web-service/place-details): `photos,attributions`는 Essentials IDs Only. 이름/평점 등 유료 상위 필드는 요청하지 않습니다.
- [사진 요청과 저장 제한](https://developers.google.com/maps/documentation/places/web-service/place-photos): 사진 식별자는 매번 조회하고 작성자 정보를 표시합니다.
- [Google 지도 정책](https://developers.google.com/maps/documentation/places/web-service/policies): 출처 표시, Place ID 저장 예외, Google 콘텐츠 표시 조건.
- [Firestore 가격](https://firebase.google.com/docs/firestore/pricing): 무료 할당량은 프로젝트당 하나의 DB에 적용됩니다.
