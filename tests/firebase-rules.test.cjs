const { createRequire } = require('node:module');
const { resolve } = require('node:path');
const { readFileSync } = require('node:fs');
const { strict: assert } = require('node:assert');
const localRequire = createRequire(resolve('.review/package.json'));
const { initializeTestEnvironment, assertFails, assertSucceeds } = localRequire('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc, writeBatch } = localRequire('firebase/firestore');
const { ref, uploadBytes, getBytes } = localRequire('firebase/storage');

(async () => {
  const env = await initializeTestEnvironment({
    projectId: 'demo-petgo-review',
    firestore: { host: '127.0.0.1', port: 8180, rules: readFileSync('firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9299, rules: readFileSync('storage.rules', 'utf8') },
  });
  try {
    const owner = env.authenticatedContext('owner');
    const other = env.authenticatedContext('other');
    const guest = env.unauthenticatedContext();
    const publication = {
      usuarioId: 'owner', usuarioLegacyId: 1, tipo: 'Perdida', estado: 'Activa',
      nombre: 'Luna', descripcion: 'Collar rojo', latitud: -12, longitud: -77,
      fechaCreacion: Date.now(),
    };
    await assertSucceeds(setDoc(doc(owner.firestore(), 'publicaciones/pet'), publication));
    await assertFails(getDoc(doc(guest.firestore(), 'publicaciones/pet')));
    await assertFails(setDoc(doc(other.firestore(), 'publicaciones/spoof'), publication));
    await assertFails(updateDoc(doc(other.firestore(), 'publicaciones/pet'), { estado: 'Resuelta' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'publicaciones/pet'), { usuarioId: 'other' }));
    const sighting = {
      usuarioId: 'other', usuarioLegacyId: 2, publicacionId: 'pet',
      latitud: -12.1, longitud: -77.1, descripcion: 'La vi en el parque', fecha: Date.now(),
    };
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/invalid'), {
      ...sighting, latitud: -91,
    }));
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/missing-fields'), {
      usuarioId: 'other', publicacionId: 'pet',
    }));
    await assertSucceeds(setDoc(doc(owner.firestore(), 'usuarios/owner/favoritos/pet'), { publicacionId: 'pet' }));
    await assertFails(getDoc(doc(other.firestore(), 'usuarios/owner/favoritos/pet')));
    await assertSucceeds(setDoc(doc(owner.firestore(), 'denuncias/report'), { usuarioId: 'owner', descripcion: 'Necesita ayuda' }));
    await assertFails(getDoc(doc(other.firestore(), 'denuncias/report')));
    await assertFails(setDoc(doc(owner.firestore(), 'denuncias/empty'), { usuarioId: 'owner', descripcion: '' }));
    const notification = {
      id: 'seen', usuarioOrigenId: 'other', usuarioDestinoId: 'owner',
      publicacionId: 'pet', avistamientoId: 'seen', tipo: 'avistamiento',
      mensaje: 'Vieron a Luna', latitud: -12.1, longitud: -77.1,
      fecha: Date.now(), leida: false,
    };
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/fake'), {
      ...notification, id: 'fake', avistamientoId: 'missing',
    }));
    const batch = writeBatch(other.firestore());
    batch.set(doc(other.firestore(), 'avistamientos/seen'), sighting);
    batch.set(doc(other.firestore(), 'notificaciones/seen'), notification);
    await assertSucceeds(batch.commit());
    await assertFails(updateDoc(doc(owner.firestore(), 'avistamientos/seen'), { latitud: -12.3 }));
    await assertSucceeds(updateDoc(doc(other.firestore(), 'avistamientos/seen'), { latitud: -12.2 }));
    await assertFails(getDoc(doc(other.firestore(), 'notificaciones/seen')));
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/replay'), {
      ...notification, id: 'replay', avistamientoId: 'seen',
    }));
    await assertSucceeds(updateDoc(doc(owner.firestore(), 'notificaciones/seen'), { leida: true }));
    await assertFails(updateDoc(doc(owner.firestore(), 'notificaciones/seen'), { usuarioDestinoId: 'other' }));
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/spoof'), {
      ...notification, id: 'spoof', usuarioDestinoId: 'other',
    }));
    await assertSucceeds(uploadBytes(ref(owner.storage(), 'usuarios/owner/photo.jpg'), new Uint8Array([1, 2]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(other.storage(), 'usuarios/owner/other.jpg'), new Uint8Array([1]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(owner.storage(), 'usuarios/owner/file.txt'), new Uint8Array([1]), { contentType: 'text/plain' }));
    await assertFails(getBytes(ref(guest.storage(), 'usuarios/owner/photo.jpg')));
    assert(true);
    console.log('Comprobaciones de reglas Firestore/Storage completadas.');
  } finally { await env.cleanup(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
