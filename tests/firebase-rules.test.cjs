const { createRequire } = require('node:module');
const { resolve } = require('node:path');
const { readFileSync } = require('node:fs');
const { strict: assert } = require('node:assert');
const localRequire = createRequire(resolve('.review/package.json'));
const { initializeTestEnvironment, assertFails, assertSucceeds } = localRequire('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc } = localRequire('firebase/firestore');
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
    const publication = { usuarioId: 'owner', tipo: 'Perdida', estado: 'Activa' };
    await assertSucceeds(setDoc(doc(owner.firestore(), 'publicaciones/pet'), publication));
    await assertFails(getDoc(doc(guest.firestore(), 'publicaciones/pet')));
    await assertFails(setDoc(doc(other.firestore(), 'publicaciones/spoof'), publication));
    await assertFails(updateDoc(doc(other.firestore(), 'publicaciones/pet'), { estado: 'Resuelta' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'publicaciones/pet'), { usuarioId: 'other' }));
    await assertSucceeds(setDoc(doc(other.firestore(), 'avistamientos/seen'), { usuarioId: 'other', publicacionId: 'pet' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'avistamientos/seen'), { latitud: -12 }));
    await assertSucceeds(updateDoc(doc(other.firestore(), 'avistamientos/seen'), { latitud: -12 }));
    await assertSucceeds(setDoc(doc(owner.firestore(), 'usuarios/owner/favoritos/pet'), { publicacionId: 'pet' }));
    await assertFails(getDoc(doc(other.firestore(), 'usuarios/owner/favoritos/pet')));
    await assertSucceeds(setDoc(doc(owner.firestore(), 'denuncias/report'), { usuarioId: 'owner', descripcion: 'Necesita ayuda' }));
    await assertFails(getDoc(doc(other.firestore(), 'denuncias/report')));
    await assertFails(setDoc(doc(owner.firestore(), 'denuncias/empty'), { usuarioId: 'owner', descripcion: '' }));
    const notification = { usuarioOrigenId: 'other', usuarioDestinoId: 'owner', publicacionId: 'pet', tipo: 'avistamiento', leida: false };
    await assertSucceeds(setDoc(doc(other.firestore(), 'notificaciones/notice'), notification));
    await assertFails(getDoc(doc(other.firestore(), 'notificaciones/notice')));
    await assertSucceeds(updateDoc(doc(owner.firestore(), 'notificaciones/notice'), { leida: true }));
    await assertFails(updateDoc(doc(owner.firestore(), 'notificaciones/notice'), { usuarioDestinoId: 'other' }));
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/spoof'), { ...notification, usuarioDestinoId: 'other' }));
    await assertSucceeds(uploadBytes(ref(owner.storage(), 'usuarios/owner/photo.jpg'), new Uint8Array([1, 2]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(other.storage(), 'usuarios/owner/other.jpg'), new Uint8Array([1]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(owner.storage(), 'usuarios/owner/file.txt'), new Uint8Array([1]), { contentType: 'text/plain' }));
    await assertFails(getBytes(ref(guest.storage(), 'usuarios/owner/photo.jpg')));
    assert(true);
    console.log('22 comprobaciones de reglas Firestore/Storage completadas.');
  } finally { await env.cleanup(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
