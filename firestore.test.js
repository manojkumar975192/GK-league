const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const DATE_KEY = "2026-11-25";
const MONTH_KEY = "2026-11";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinus(ms = 2000) {
  return new Date(Date.now() - ms);
}

function validStudentPayload(uid) {
  const ts = nowMinus(5000);
  return {
    uid,
    displayName: "தமிழ் மாணவர்",
    schoolName: "அரசு மேல்நிலைப் பள்ளி",
    studentIdNumber: "TN-2026-8421",
    studentIdHash: "abcdef0123456789abcdef0123456789",
    gradeLevel: "உயர்நிலைப் பள்ளி",
    verificationStatus: "verified",
    rewardPoints: 50,
    totalScore: 0,
    tournamentsCompleted: 0,
    currentStreak: 0,
    bestStreak: 0,
    adRewardCountToday: 0,
    categoryStats: {},
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDailyTournamentPayload(creatorUid, count = 50) {
  const questions = [];
  for (let i = 1; i <= count; i++) {
    questions.push({
      id: `q${i}`,
      category: "தமிழ் இலக்கியம்",
      difficulty: "நடுத்தரம்",
      prompt: `திருக்குறள் வினா எண் ${i}`,
      options: ["விடை 1", "விடை 2", "விடை 3", "விடை 4"],
      correctIndex: i % 4,
      explanation: "தெளிவான தமிழ் விளக்கம்.",
    });
  }
  return {
    dateKey: DATE_KEY,
    title: "தமிழ் GK தினசரி தேர்வு",
    theme: "50 வினாக்கள் • 25 நிமிடம்",
    questions,
    createdByUid: creatorUid,
    createdAt: nowMinus(4000),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("students").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("students").doc(ALICE_UID).set(validStudentPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("daily_tournaments").doc(DATE_KEY).get());
  await assertFails(
    unauthDb
      .collection("tournament_submissions")
      .where("visibility", "==", "public_leaderboard")
      .where("dateKey", "==", DATE_KEY)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("monthly_rankings")
      .where("visibility", "==", "public_leaderboard")
      .where("monthKey", "==", MONTH_KEY)
      .get()
  );
});

test("Student profile: owner create/read/update succeeds; PII & spoofing blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("students").doc(ALICE_UID).get());

  // Spoofing points on signup fails
  await assertFails(
    aliceDb.collection("students").doc(ALICE_UID).set({
      ...validStudentPayload(ALICE_UID),
      rewardPoints: 99999,
    })
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).set(validStudentPayload(ALICE_UID))
  );

  // Cross-user PII read & list denied
  await assertFails(bobDb.collection("students").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("students").get());

  // Action A: 50-question tournament finish (+50 max rank points, +40 correct answers) succeeds
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).update({
      rewardPoints: 100,
      totalScore: 40,
      tournamentsCompleted: 1,
      currentStreak: 1,
      bestStreak: 1,
      lastPlayedDate: DATE_KEY,
      categoryStats: { "தமிழ் இலக்கியம்": { correct: 10, total: 12 } },
      updatedAt: nowMinus(1000),
    })
  );

  // Action A cheating (+60 points > 50 max) fails
  await assertFails(
    aliceDb.collection("students").doc(ALICE_UID).update({
      rewardPoints: 160,
      totalScore: 80,
      tournamentsCompleted: 2,
      currentStreak: 2,
      bestStreak: 2,
      lastPlayedDate: DATE_KEY,
      categoryStats: {},
      updatedAt: nowMinus(500),
    })
  );

  // Action B: Opt-in rewarded ad (+25 points, count=1) succeeds
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).update({
      rewardPoints: 125,
      adRewardCountToday: 1,
      adRewardDate: DATE_KEY,
      updatedAt: nowMinus(500),
    })
  );

  // Action C: Reward store claim deduction succeeds
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).update({
      rewardPoints: 75,
      updatedAt: nowMinus(400),
    })
  );
});

test("50-question daily exam, negative marking, pass/fail rank rewards & monthly rankings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).set(validStudentPayload(ALICE_UID))
  );
  await assertSucceeds(
    bobDb.collection("students").doc(BOB_UID).set(validStudentPayload(BOB_UID))
  );

  // Publish 50-question daily tournament
  await assertSucceeds(
    aliceDb
      .collection("daily_tournaments")
      .doc(DATE_KEY)
      .set(validDailyTournamentPayload(ALICE_UID, 50))
  );

  // Cannot overwrite published 50-question paper
  await assertFails(
    bobDb.collection("daily_tournaments").doc(DATE_KEY).update({ title: "Hacked" })
  );

  // Valid passing submission with negative marking (40 right, 8 wrong => 38.0 >= 35, Rank #1 => 50 pts)
  const aliceSubId = `${DATE_KEY}_${ALICE_UID}`;
  await assertSucceeds(
    aliceDb.collection("tournament_submissions").doc(aliceSubId).set({
      uid: ALICE_UID,
      dateKey: DATE_KEY,
      monthKey: MONTH_KEY,
      studentName: "தமிழ் மாணவர்",
      schoolName: "அரசு மேல்நிலைப் பள்ளி",
      gradeLevel: "உயர்நிலைப் பள்ளி",
      score: 38.0,
      correctCount: 40,
      wrongCount: 8,
      unansweredCount: 2,
      totalQuestions: 50,
      passed: true,
      rank: 1,
      durationMillis: 720000,
      pointsEarned: 50,
      visibility: "public_leaderboard",
      submittedAt: nowMinus(1000),
    })
  );

  // Failing submission claiming non-zero points MUST FAIL
  const bobSubId = `${DATE_KEY}_${BOB_UID}`;
  await assertFails(
    bobDb.collection("tournament_submissions").doc(bobSubId).set({
      uid: BOB_UID,
      dateKey: DATE_KEY,
      monthKey: MONTH_KEY,
      studentName: "மாணவர் 2",
      schoolName: "அரசு பள்ளி",
      gradeLevel: "கல்லூரி",
      score: 28.5,
      correctCount: 30,
      wrongCount: 6,
      unansweredCount: 14,
      totalQuestions: 50,
      passed: false,
      rank: 2,
      durationMillis: 900000,
      pointsEarned: 50,
      visibility: "public_leaderboard",
      submittedAt: nowMinus(900),
    })
  );

  // Valid failing submission with 0 points succeeds (even negative net mark)
  await assertSucceeds(
    bobDb.collection("tournament_submissions").doc(bobSubId).set({
      uid: BOB_UID,
      dateKey: DATE_KEY,
      monthKey: MONTH_KEY,
      studentName: "மாணவர் 2",
      schoolName: "அரசு பள்ளி",
      gradeLevel: "கல்லூரி",
      score: -2.5,
      correctCount: 5,
      wrongCount: 30,
      unansweredCount: 15,
      totalQuestions: 50,
      passed: false,
      rank: 2,
      durationMillis: 1500000,
      pointsEarned: 0,
      visibility: "public_leaderboard",
      submittedAt: nowMinus(800),
    })
  );

  // Duration exceeding 25 minutes (1,500,000 ms) fails
  await assertFails(
    bobDb.collection("tournament_submissions").doc(`${DATE_KEY}_${BOB_UID}_extra`).set({
      uid: BOB_UID,
      dateKey: DATE_KEY,
      monthKey: MONTH_KEY,
      studentName: "மாணவர் 2",
      schoolName: "அரசு பள்ளி",
      gradeLevel: "கல்லூரி",
      score: 40.0,
      correctCount: 40,
      wrongCount: 0,
      unansweredCount: 10,
      totalQuestions: 50,
      passed: true,
      rank: 1,
      durationMillis: 1600000,
      pointsEarned: 50,
      visibility: "public_leaderboard",
      submittedAt: nowMinus(700),
    })
  );

  // Filtered daily leaderboard query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb
      .collection("tournament_submissions")
      .where("visibility", "==", "public_leaderboard")
      .where("dateKey", "==", DATE_KEY)
      .get()
  );
  await assertFails(bobDb.collection("tournament_submissions").get());

  // Monthly ranking create (+50 pts) & update (+20 pts)
  const monthlyId = `${MONTH_KEY}_${ALICE_UID}`;
  await assertSucceeds(
    aliceDb.collection("monthly_rankings").doc(monthlyId).set({
      uid: ALICE_UID,
      monthKey: MONTH_KEY,
      studentName: "தமிழ் மாணவர்",
      schoolName: "அரசு மேல்நிலைப் பள்ளி",
      gradeLevel: "உயர்நிலைப் பள்ளி",
      monthlyScore: 50,
      tournamentsPlayed: 1,
      totalDurationMillis: 720000,
      visibility: "public_leaderboard",
      lastSubmittedAt: nowMinus(1000),
      createdAt: nowMinus(1000),
      updatedAt: nowMinus(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("monthly_rankings").doc(monthlyId).update({
      studentName: "தமிழ் மாணவர்",
      schoolName: "அரசு மேல்நிலைப் பள்ளி",
      gradeLevel: "உயர்நிலைப் பள்ளி",
      monthlyScore: 70,
      tournamentsPlayed: 2,
      totalDurationMillis: 1400000,
      lastSubmittedAt: nowMinus(500),
      updatedAt: nowMinus(500),
    })
  );
});

test("Private 50-question quiz attempts and reward claims are isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).set(validStudentPayload(ALICE_UID))
  );

  const answers50 = Array.from({ length: 50 }, (_, idx) => (idx < 48 ? idx % 4 : -1));
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).collection("attempts").doc(DATE_KEY).set({
      dateKey: DATE_KEY,
      score: 38.0,
      correctCount: 40,
      wrongCount: 8,
      unansweredCount: 2,
      totalQuestions: 50,
      passed: true,
      rank: 1,
      durationMillis: 720000,
      pointsEarned: 50,
      selectedAnswers: answers50,
      createdAt: nowMinus(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).collection("attempts").get()
  );
  await assertFails(
    bobDb.collection("students").doc(ALICE_UID).collection("attempts").get()
  );

  await assertSucceeds(
    aliceDb.collection("students").doc(ALICE_UID).collection("reward_claims").doc("claim_1").set({
      uid: ALICE_UID,
      rewardId: "gc_amazon_10",
      rewardTitle: "₹500 புத்தகப் பரிசு அட்டை",
      rewardCategory: "gift_card",
      pointsCost: 500,
      claimCode: "GKL-2026-9AF42",
      status: "confirmed",
      monthKey: MONTH_KEY,
      claimedAt: nowMinus(1000),
    })
  );
  await assertFails(
    bobDb.collection("students").doc(ALICE_UID).collection("reward_claims").get()
  );
});
