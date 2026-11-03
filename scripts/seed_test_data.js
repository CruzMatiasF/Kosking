/**
 * Script de datos de prueba para Kosking
 *
 * Crea 4 locales con empleados, productos y clientes de ejemplo.
 *
 * ANTES DE CORRER:
 * 1. Bajá la service account key desde Firebase Console:
 *    https://console.firebase.google.com/project/kosking-dev/settings/serviceaccounts/adminsdk
 *    → "Generar nueva clave privada" → guardala como scripts/serviceAccountKey.json
 * 2. npm install firebase-admin
 * 3. node scripts/seed_test_data.js
 */

const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: "kosking-dev",
});

const db = admin.firestore();

// ── Helpers ──────────────────────────────────────────────────────────────────

function uid() {
  return db.collection("_").doc().id;
}

// ── Datos de prueba ───────────────────────────────────────────────────────────

const locales = [
  {
    id: uid(),
    nombre: "Kiosco Don Raúl",
    duenoUid: "demo_dueno_1",
    codigo: "RAUL01",
    tipo: "Kiosco",
    telefono: "5491122334455",
    direccion: "Av. San Martín 1234, Buenos Aires",
    abierto: true,
    imagenUrl: "",
    descripcion: "Kiosco de barrio, abierto desde las 7am",
    lat: -34.6037,
    lng: -58.3816,
    permisos: { anotarFiados: true, registrarPagos: true, verTotal: false },
    horarios: {
      lunes: { apertura: "07:00", cierre: "22:00" },
      martes: { apertura: "07:00", cierre: "22:00" },
      miercoles: { apertura: "07:00", cierre: "22:00" },
      jueves: { apertura: "07:00", cierre: "22:00" },
      viernes: { apertura: "07:00", cierre: "23:00" },
      sabado: { apertura: "08:00", cierre: "23:00" },
      domingo: { apertura: "09:00", cierre: "21:00" },
    },
  },
  {
    id: uid(),
    nombre: "Almacén La Esquina",
    duenoUid: "demo_dueno_2",
    codigo: "ESQUI2",
    tipo: "Almacén",
    telefono: "5491133445566",
    direccion: "Rivadavia 567, Buenos Aires",
    abierto: true,
    imagenUrl: "",
    descripcion: "Almacén familiar con todo para el hogar",
    lat: -34.6118,
    lng: -58.4023,
    permisos: { anotarFiados: true, registrarPagos: true, verTotal: true },
    horarios: {
      lunes: { apertura: "08:00", cierre: "20:00" },
      martes: { apertura: "08:00", cierre: "20:00" },
      miercoles: { apertura: "08:00", cierre: "20:00" },
      jueves: { apertura: "08:00", cierre: "20:00" },
      viernes: { apertura: "08:00", cierre: "21:00" },
      sabado: { apertura: "09:00", cierre: "20:00" },
      domingo: { apertura: "", cierre: "" },
    },
  },
  {
    id: uid(),
    nombre: "Despensa El Barrio",
    duenoUid: "demo_dueno_3",
    codigo: "BARRI3",
    tipo: "Despensa",
    telefono: "5491144556677",
    direccion: "Corrientes 890, Rosario",
    abierto: false,
    imagenUrl: "",
    descripcion: "Despensa con delivery al barrio",
    lat: -32.9442,
    lng: -60.6505,
    permisos: { anotarFiados: true, registrarPagos: false, verTotal: false },
    horarios: {
      lunes: { apertura: "09:00", cierre: "19:00" },
      martes: { apertura: "09:00", cierre: "19:00" },
      miercoles: { apertura: "09:00", cierre: "19:00" },
      jueves: { apertura: "09:00", cierre: "19:00" },
      viernes: { apertura: "09:00", cierre: "19:00" },
      sabado: { apertura: "10:00", cierre: "14:00" },
      domingo: { apertura: "", cierre: "" },
    },
  },
  {
    id: uid(),
    nombre: "Mini Market Sol",
    duenoUid: "demo_dueno_4",
    codigo: "SOL004",
    tipo: "Kiosco",
    telefono: "5491155667788",
    direccion: "Belgrano 321, Córdoba",
    abierto: true,
    imagenUrl: "",
    descripcion: "Abierto las 24hs, los 365 días",
    lat: -31.4201,
    lng: -64.1888,
    permisos: { anotarFiados: true, registrarPagos: true, verTotal: true },
    horarios: {
      lunes: { apertura: "00:00", cierre: "23:59" },
      martes: { apertura: "00:00", cierre: "23:59" },
      miercoles: { apertura: "00:00", cierre: "23:59" },
      jueves: { apertura: "00:00", cierre: "23:59" },
      viernes: { apertura: "00:00", cierre: "23:59" },
      sabado: { apertura: "00:00", cierre: "23:59" },
      domingo: { apertura: "00:00", cierre: "23:59" },
    },
  },
];

const productosPorLocal = [
  // Kiosco Don Raúl
  [
    { nombre: "Coca Cola 500ml", precio: 1200, descripcion: "Botella personal", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Agua mineral 500ml", precio: 600, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Alfajor Oreo", precio: 850, descripcion: "", habilitado: true, esOferta: true, precioAnterior: 950 },
    { nombre: "Cigarrillos Marlboro", precio: 2800, descripcion: "Paquete x20", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Chicles Tic Tac", precio: 400, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Pepsi 1.5L", precio: 2100, descripcion: "", habilitado: true, esOferta: true, precioAnterior: 2400 },
    { nombre: "Papas fritas Lays", precio: 1100, descripcion: "Bolsa grande", habilitado: true, esOferta: false, precioAnterior: 0 },
  ],
  // Almacén La Esquina
  [
    { nombre: "Leche La Serenísima 1L", precio: 1400, descripcion: "Entera", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Pan lactal Bimbo", precio: 1800, descripcion: "Grande", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Yerba Rosamonte 1kg", precio: 3200, descripcion: "", habilitado: true, esOferta: true, precioAnterior: 3600 },
    { nombre: "Aceite girasol 1.5L", precio: 2600, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Arroz Gallo 1kg", precio: 1900, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Azúcar 1kg", precio: 1500, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Fideos Matarazzo 500g", precio: 1200, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Detergente Magistral", precio: 2200, descripcion: "Limón", habilitado: true, esOferta: false, precioAnterior: 0 },
  ],
  // Despensa El Barrio
  [
    { nombre: "Empanadas (docena)", precio: 4800, descripcion: "Carne o pollo", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Medialunas (6u)", precio: 1800, descripcion: "Dulces o saladas", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Coca Cola 1.5L", precio: 2300, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Fernet Branca 750ml", precio: 8500, descripcion: "", habilitado: true, esOferta: true, precioAnterior: 9200 },
    { nombre: "Cerveza Quilmes 1L", precio: 2800, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Queso en fetas 200g", precio: 2100, descripcion: "", habilitado: false, esOferta: false, precioAnterior: 0 },
  ],
  // Mini Market Sol
  [
    { nombre: "Red Bull 250ml", precio: 2800, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Monster 473ml", precio: 3200, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Sandwich de miga", precio: 2500, descripcion: "Jamón y queso", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Chocolatines x3", precio: 1400, descripcion: "", habilitado: true, esOferta: true, precioAnterior: 1800 },
    { nombre: "Gaseosa 354ml lata", precio: 1600, descripcion: "Coca / Pepsi / Sprite", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Agua con gas 600ml", precio: 900, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Turrones x4", precio: 1200, descripcion: "", habilitado: true, esOferta: false, precioAnterior: 0 },
    { nombre: "Cigarrillos Lucky Strike", precio: 2600, descripcion: "Paquete x20", habilitado: true, esOferta: false, precioAnterior: 0 },
  ],
];

const empleadosPorLocal = [
  // Kiosco Don Raúl
  [
    { nombre: "Carlos López", uid: "demo_emp_raul_1" },
    { nombre: "María González", uid: "demo_emp_raul_2" },
  ],
  // Almacén La Esquina
  [
    { nombre: "Diego Pérez", uid: "demo_emp_esquina_1" },
  ],
  // Despensa El Barrio
  [
    { nombre: "Ana Rodríguez", uid: "demo_emp_barrio_1" },
    { nombre: "Lucas Fernández", uid: "demo_emp_barrio_2" },
  ],
  // Mini Market Sol
  [
    { nombre: "Valentina Torres", uid: "demo_emp_sol_1" },
    { nombre: "Martín Ruiz", uid: "demo_emp_sol_2" },
    { nombre: "Sofía Morales", uid: "demo_emp_sol_3" },
  ],
];

const clientesPorLocal = [
  // Kiosco Don Raúl
  [
    { nombre: "Ramón García", telefono: "1165432100", pin: "1234", deudaTotal: 3500 },
    { nombre: "Susana Méndez", telefono: "1176543210", pin: "5678", deudaTotal: 0 },
    { nombre: "Jorge Blanco", telefono: "", pin: "9012", deudaTotal: 12000 },
  ],
  // Almacén La Esquina
  [
    { nombre: "Patricia Sosa", telefono: "1187654321", pin: "1111", deudaTotal: 6800 },
    { nombre: "Roberto Núñez", telefono: "1198765432", pin: "2222", deudaTotal: 0 },
  ],
  // Despensa El Barrio — sin clientes aún
  [],
  // Mini Market Sol
  [
    { nombre: "Claudia Herrera", telefono: "1110987654", pin: "3333", deudaTotal: 2200 },
    { nombre: "Fernando Castro", telefono: "1121098765", pin: "4444", deudaTotal: 15500 },
    { nombre: "Natalia Vargas", telefono: "", pin: "5555", deudaTotal: 0 },
    { nombre: "Andrés Lima", telefono: "1132109876", pin: "6666", deudaTotal: 8900 },
  ],
];

// ── Escritura en Firestore ────────────────────────────────────────────────────

async function seed() {
  console.log("🌱 Iniciando seed de datos de prueba...\n");

  const batch = db.batch();

  for (let i = 0; i < locales.length; i++) {
    const local = locales[i];

    // Local
    batch.set(db.collection("locales").doc(local.id), local);
    console.log(`📍 Local: ${local.nombre} (código: ${local.codigo})`);

    // Productos
    for (const prod of productosPorLocal[i]) {
      const prodId = uid();
      batch.set(db.collection("productos").doc(prodId), {
        id: prodId,
        localId: local.id,
        imagenUrl: "",
        ...prod,
      });
    }
    console.log(`   📦 ${productosPorLocal[i].length} productos`);

    // Empleados
    for (const emp of empleadosPorLocal[i]) {
      const empId = uid();
      batch.set(db.collection("empleados").doc(empId), {
        id: empId,
        localId: local.id,
        uid: emp.uid,
        nombre: emp.nombre,
      });
    }
    console.log(`   👷 ${empleadosPorLocal[i].length} empleados`);

    // Clientes
    const clientes = clientesPorLocal[i];
    for (const cli of clientes) {
      const cliId = uid();
      batch.set(db.collection("clientes").doc(cliId), {
        id: cliId,
        localId: local.id,
        nombre: cli.nombre,
        telefono: cli.telefono,
        domicilio: "",
        pin: cli.pin,
        habilitado: true,
        deudaTotal: cli.deudaTotal,
        codigoAcceso: local.codigo,
        limiteCredito: 0,
      });
    }
    console.log(`   👥 ${clientes.length} clientes`);
    console.log();
  }

  await batch.commit();
  console.log("✅ Datos creados exitosamente en Firestore (proyecto: kosking-dev)");
}

seed().catch((e) => {
  console.error("❌ Error:", e.message);
  process.exit(1);
});
