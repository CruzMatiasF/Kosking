const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

async function enviarNotificacion(uid, titulo, cuerpo, tipo, batch) {
  const notifRef = db.collection("notificaciones").doc();
  batch.set(notifRef, {
    id: notifRef.id,
    uid,
    titulo,
    cuerpo,
    leida: false,
    fecha: Timestamp.now(),
    tipo,
  });

  const usuarioSnap = await db.collection("usuarios").doc(uid).get();
  if (!usuarioSnap.exists) return null;
  const token = usuarioSnap.data().fcmToken;
  return token || null;
}

// ── Notificación cuando se anota un fiado o pago ─────────────────────────────
exports.notificarMovimiento = onDocumentCreated(
  "movimientos/{movId}",
  async (event) => {
    const mov = event.data.data();
    if (!mov || mov.anulado) return;

    const { clienteId, localId, tipo, monto, producto, anotadoPorNombre } = mov;

    const localSnap = await db.collection("locales").doc(localId).get();
    const nombreLocal = localSnap.exists ? localSnap.data().nombre : "Tu local";

    const montoStr = `$${Number(monto).toFixed(2)}`;
    const quien = anotadoPorNombre ? ` · por ${anotadoPorNombre}` : "";

    let titulo, cuerpo;
    if (tipo === "cargo") {
      titulo = `Nuevo fiado en ${nombreLocal}`;
      cuerpo = producto ? `${producto} — ${montoStr}${quien}` : `${montoStr}${quien}`;
    } else {
      titulo = `Pago registrado en ${nombreLocal}`;
      cuerpo = `${montoStr} registrado${quien}`;
    }

    const batch = db.batch();
    const tokens = [];

    // Notificar al cliente que tiene este fiado guardado
    const clienteLocalesSnap = await db.collectionGroup("localesGuardados")
      .where("localId", "==", localId)
      .where("clienteId", "==", clienteId)
      .get();

    for (const doc of clienteLocalesSnap.docs) {
      const uid = doc.ref.parent.parent.id;
      const token = await enviarNotificacion(uid, titulo, cuerpo, "local", batch);
      if (token) tokens.push(token);
    }

    // Notificar también al dueño del local
    if (localSnap.exists) {
      const duenoUid = localSnap.data().duenoUid;
      if (duenoUid) {
        const token = await enviarNotificacion(duenoUid, titulo, cuerpo, "local", batch);
        if (token) tokens.push(token);
      }
    }

    await batch.commit();

    if (tokens.length > 0) {
      await getMessaging().sendEachForMulticast({
        tokens,
        notification: { title: titulo, body: cuerpo },
        android: {
          priority: "high",
          notification: { channelId: "kosking_general" },
        },
      });
    }
  }
);

// ── Notificación al cliente cuando el dueño marca una oferta ─────────────────
exports.notificarOferta = onDocumentCreated(
  "productos/{prodId}",
  async (event) => {
    const prod = event.data.data();
    if (!prod || !prod.esOferta || !prod.habilitado) return;

    const { localId, nombre, precio } = prod;

    const localSnap = await db.collection("locales").doc(localId).get();
    const nombreLocal = localSnap.exists ? localSnap.data().nombre : "Un local";

    const titulo = `Oferta en ${nombreLocal}`;
    const cuerpo = `${nombre} — $${Number(precio).toFixed(2)}`;

    const usuariosSnap = await db.collectionGroup("localesGuardados")
      .where("localId", "==", localId)
      .get();

    if (usuariosSnap.empty) return;

    const batch = db.batch();
    const tokens = [];

    for (const doc of usuariosSnap.docs) {
      const uid = doc.ref.parent.parent.id;
      const token = await enviarNotificacion(uid, titulo, cuerpo, "oferta", batch);
      if (token) tokens.push(token);
    }

    await batch.commit();

    if (tokens.length > 0) {
      await getMessaging().sendEachForMulticast({
        tokens,
        notification: { title: titulo, body: cuerpo },
        android: {
          priority: "normal",
          notification: { channelId: "kosking_general" },
        },
      });
    }
  }
);
