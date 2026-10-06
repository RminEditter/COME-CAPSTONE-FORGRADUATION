# Visit tag Firestore integration test

Uses demo-cafefit-visits, Firestore at port 8181, and a separately named FirebaseApp in Android. No production credentials or records are used. Test documents are deleted in finally. Rules here are an emulator fixture, not production authorization rules; never deploy them.

Start an Android emulator, set JAVA_HOME to the Android Studio JBR, and add its bin directory to PATH. From the project root, run:

    node build/firebase-tools/node_modules/firebase-tools/lib/bin/firebase.js emulators:exec --only firestore --project demo-cafefit-visits --config firebase.tests.json "gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.capstone2026.VisitFirestoreTest -Pandroid.testInstrumentationRunnerArguments.firestoreEmulator=true --offline"

The build/firebase-tools CLI is local tooling and is not checked into Git. Install Firebase CLI separately if it is absent.

The test checks server round-trip, legacy documents, user-scoped queries, tag preference scoring, transaction edits preserving identity/date, and deletion. It is skipped during ordinary connected tests unless firestoreEmulator=true. It does not verify deployed security rules, Auth authorization, or the full activity UI.
