# Configuración de Firebase para PetGo

La app ya contiene la integración de Auth, Firestore y Storage. Falta asociarla al proyecto Firebase real de la feria:

1. En Firebase Console crea un proyecto y registra una app Android con el paquete com.example.practicafinal.
2. Activa Authentication > Sign-in method > Email/Password.
3. Crea Firestore Database en modo producción y Storage. Publica las reglas incluidas en firestore.rules y storage.rules.
4. Descarga el google-services.json generado por Firebase y colócalo en app/google-services.json. El Gradle del proyecto activa automáticamente el plugin Google Services cuando ese archivo existe.
5. Ejecuta ./gradlew :app:assembleDebug y prueba registro, publicación con foto, avistamiento, favoritos y cierre de sesión en dos dispositivos.

La sesión usa el SDK de Firebase Authentication. No se guardan tokens manualmente ni se usan claves de API fijas en el repositorio Kotlin.

Las reglas nuevas exigen una sesión autenticada. Las publicaciones y los avistamientos son visibles para la comunidad autenticada; su autor conserva el permiso para modificarlos. Los perfiles, favoritos, denuncias y notificaciones se leen por propietario o destinatario. Publica las reglas de este repositorio después de instalar la versión actualizada de la app. Las versiones anteriores que guardaban solo un UID local deberán iniciar sesión de nuevo.

Las fotos de la app se comprimen y se guardan en Firestore como JPEG en base64. Las reglas de Storage permiten imágenes de hasta 5 MB bajo `usuarios/{uid}/...`, pero la app todavía no usa esa ruta de almacenamiento.

Abrir la app o la lista de albergues ya no inserta datos de ejemplo automáticamente. Los emuladores y pruebas utilizan el proyecto ficticio `demo-petgo-review`; nunca necesitan escribir en el proyecto real.

La revisión local no publica reglas ni modifica datos en Firebase real. Antes de usar la app con otras personas, publica `firestore.rules` y `storage.rules` en el proyecto correspondiente.
