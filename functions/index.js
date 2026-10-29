const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

// ── Notificación al cliente cuando se le agrega un fiado o pago ──────────────
exports.notificarMovimiento = onDocumentCreated(
  "movimientos/{movId}",
  async (event) => {
    const mov = event.data.data();
    if (!mov || mov.anulado) return;

    const { clienteId, localId, tipo, monto, producto, anotadoPorNombre } = mov;

    // Buscar al cliente para obtener su codigoAcceso y localidad
    const clienteSnap = await db.collection("clientes").doc(clienteId).get();
    if (!clienteSnap.exists) return;
    const cliente = clienteSnap.data();

    // Buscar el uid del cliente mediante su codigoAcceso
    const codigoAcceso = cliente.codigoAcceso;
    if (!codigoAcceso) return;

    const codigoSnap = await db.collection("codigosCliente").doc(codigoAcceso).get();
    if (!codigoSnap.exists) return;
    const clienteUid = codigoSnap.data().clienteId
      ? null // codigoAcceso doc tiene clienteId del cliente, no uid del usuario
      : null;

    // Buscar usuarios que tienen este cliente guardado
    const usuariosSnap = await db.collectionGroup("localesGuardados")
      .where("localId", "==", localId)
      .where("clienteId", "==", clienteId)
      .get();

    if (usuariosSnap.empty) return;

    // Buscar el local para el nombre
    const localSnap = await db.collection("locales").doc(localId).get();
    const nombreLocal = localSnap.exists ? localSnap.data().nombre : "Tu local";

    const montoStr = `$${Number(monto).toFixed(2)}`;
    const quien = anotadoPorNombre ? ` · por ${anotadoPorNombre}` : "";

    let titulo, cuerpo, tipoNotif;
    if (tipo === "cargo") {
      titulo = `Nuevo fiado en ${nombreLocal}`;
      cuerpo = producto
        ? `${producto} — ${montoStr}${quien}`
        : `${montoStr}${quien}`;
      tipoNotif = "local";
    } else {
      titulo = `Pago registrado en ${nombreLocal}`;
      cuerpo = `${montoStr} registrado${quien}`;
      tipoNotif = "local";
    }

    // Para cada usuario que tiene este local guardado, enviar notif y guardar en Firestore
    const batch = db.batch();
    const tokens = [];

    for (const doc of usuariosSnap.docs) {
      // El doc está en usuarios/{uid}/localesGuardados/{localId}
      const uid = doc.ref.parent.parent.id;

      // Guardar notificación en Firestore
      const notifRef = db.collection("notificaciones").doc();
      batch.set(notifRef, {
        id: notifRef.id,
        uid,
        titulo,
        cuerpo,
        leida: false,
        fecha: Timestamp.now(),
        tipo: tipoNotif,
      });

      // Obtener FCM token del usuario
      const usuarioSnap = await db.collection("usuarios").doc(uid).get();
      if (usuarioSnap.exists) {
        const token = usuarioSnap.data().fcmToken;
        if (token) tokens.push(token);
      }
    }

    await batch.commit();

    // Enviar push notifications
    if (tokens.length > 0) {
      const messaging = getMessaging();
      await messaging.sendEachForMulticast({
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

    // Buscar todos los usuarios que tienen este local guardado
    const usuariosSnap = await db.collectionGroup("localesGuardados")
      .where("localId", "==", localId)
      .get();

    if (usuariosSnap.empty) return;

    const localSnap = await db.collection("locales").doc(localId).get();
    const nombreLocal = localSnap.exists ? localSnap.data().nombre : "Un local";

    const titulo = `🔖 Oferta en ${nombreLocal}`;
    const cuerpo = `${nombre} — $${Number(precio).toFixed(2)}`;

    const batch = db.batch();
    const tokens = [];

    for (const doc of usuariosSnap.docs) {
      const uid = doc.ref.parent.parent.id;

      const notifRef = db.collection("notificaciones").doc();
      batch.set(notifRef, {
        id: notifRef.id,
        uid,
        titulo,
        cuerpo,
        leida: false,
        fecha: Timestamp.now(),
        tipo: "oferta",
      });

      const usuarioSnap = await db.collection("usuarios").doc(uid).get();
      if (usuarioSnap.exists) {
        const token = usuarioSnap.data().fcmToken;
        if (token) tokens.push(token);
      }
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
