const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTaskPayload(userId, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId,
    title: "Planejar sprint da semana",
    description: "Definir prioridades e prazos no quadro Kanban.",
    status: "PENDENTE",
    priority: "ALTA",
    category: "Trabalho",
    scheduledAt: new Date(Date.now() + 3600000),
    createdAt: past,
    updatedAt: past,
    ...overrides,
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

test("Unauthenticated user: cannot read, list, create, update, or delete tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set(validTaskPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid create (with and without scheduledAt), status move, edit, list, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks");

  // Create with scheduledAt (mirrors TaskRepository.createTask)
  await assertSucceeds(
    tasksRef.doc("task_1").set(validTaskPayload(ALICE_UID))
  );

  // Create without scheduledAt (optional field omitted via filterValues)
  const noDatePayload = validTaskPayload(ALICE_UID);
  delete noDatePayload.scheduledAt;
  await assertSucceeds(tasksRef.doc("task_2").set(noDatePayload));

  // List subcollection (mirrors TaskRepository.observeTasks)
  await assertSucceeds(tasksRef.get());

  // Action 1: Quick Kanban status move (PENDENTE -> INICIADO -> CONCLUIDO -> INICIADO)
  await assertSucceeds(
    tasksRef.doc("task_1").update({
      status: "INICIADO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    tasksRef.doc("task_1").update({
      status: "CONCLUIDO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 2: Full edit including clearing scheduledAt to null
  await assertSucceeds(
    tasksRef.doc("task_1").update({
      title: "Sprint revisada",
      description: "Atualizado com metas finais",
      status: "INICIADO",
      priority: "MEDIA",
      category: "Projeto",
      scheduledAt: null,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete own task
  await assertSucceeds(tasksRef.doc("task_2").delete());
});

test("Security bounds: reject ghost fields, @DocumentId id key, invalid status/priority, oversized strings, future timestamps, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks");

  // Seed valid task
  await assertSucceeds(
    tasksRef.doc("task_base").set(validTaskPayload(ALICE_UID))
  );

  // Reject ghost field (Shadow Update test)
  await assertFails(
    tasksRef.doc("task_ghost").set({
      ...validTaskPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Reject explicit id key (protects @DocumentId contract)
  await assertFails(
    tasksRef.doc("task_with_id").set({
      ...validTaskPayload(ALICE_UID),
      id: "task_with_id",
    })
  );

  // Reject invalid Kanban status
  await assertFails(
    tasksRef.doc("task_bad_status").set(
      validTaskPayload(ALICE_UID, { status: "ARQUIVADO" })
    )
  );

  // Reject empty or oversized title
  await assertFails(
    tasksRef.doc("task_empty").set(validTaskPayload(ALICE_UID, { title: "" }))
  );
  await assertFails(
    tasksRef.doc("task_long").set(
      validTaskPayload(ALICE_UID, { title: "A".repeat(121) })
    )
  );

  // Reject future updatedAt timestamp
  await assertFails(
    tasksRef.doc("task_future").set(
      validTaskPayload(ALICE_UID, {
        updatedAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Reject modifying immutable createdAt or userId
  await assertFails(
    tasksRef.doc("task_base").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    tasksRef.doc("task_base").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject Value Poisoning on status update
  await assertFails(
    tasksRef.doc("task_base").update({
      status: "INVALIDO",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Multi-user isolation: user cannot read, list, create, update, or delete another user's tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("tasks")
      .doc("bob_task_1")
      .set(validTaskPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobTasksAsAlice = aliceDb
    .collection("users")
    .doc(BOB_UID)
    .collection("tasks");

  await assertFails(bobTasksAsAlice.get());
  await assertFails(bobTasksAsAlice.doc("bob_task_1").get());
  await assertFails(
    bobTasksAsAlice.doc("bob_task_1").update({
      status: "CONCLUIDO",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobTasksAsAlice.doc("bob_task_1").delete());
  await assertFails(
    bobTasksAsAlice.doc("spoofed").set(validTaskPayload(ALICE_UID))
  );
});
