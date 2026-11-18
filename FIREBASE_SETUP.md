# Configuración de Firebase para PetGo

PetGo utiliza Firebase Authentication y Cloud Firestore. El proyecto está configurado para funcionar en el plan gratuito Spark sin depender de Firebase Storage ni de Cloud Functions.

## Configuración inicial

1. Registra una aplicación Android en el proyecto Firebase con el paquete `com.example.practicafinal`.
2. Activa Authentication > Sign-in method > Email/Password.
3. Crea la base de datos predeterminada de Cloud Firestore.
4. Descarga el `google-services.json` de esa aplicación y colócalo en `app/google-services.json`.
5. Inicia sesión en Firebase CLI con `firebase.cmd login`. El archivo `.firebaserc` ya apunta al proyecto `petgo-325bb`.
6. Publica las reglas e índices de Firestore con `firebase.cmd deploy --only firestore --project petgo-325bb`.

El despliegue de reglas reemplaza las reglas de Firestore que estén publicadas en la consola. Revisa `firestore.rules` antes de cambiar el proyecto de destino.

La versión actual de la app guarda los puntos exactos de las publicaciones en `usuarios/{uid}/ubicaciones_publicaciones/{id}` y muestra coordenadas aproximadas al resto de usuarios. También añade campos opcionales de color, tamaño y señas particulares. Para que estas escrituras funcionen en el proyecto Firebase, sus reglas publicadas deben incluir las reglas correspondientes de `firestore.rules`; publicar reglas no habilita facturación ni cambia el plan Spark.

## Fotos en el plan gratuito

Las fotos nuevas se reducen a un máximo de 80 KiB y se guardan como una cadena `data:image/jpeg;base64,...` dentro del documento de Firestore. El campo `foto` no se indexa. Si no se puede leer o comprimir una imagen, el aviso se guarda sin foto y la app muestra el marcador de posición. Por el tamaño limitado de Firestore, las fotos se comprimen de forma agresiva; las lecturas de listas también descargan estas fotos junto con cada documento.

Las publicaciones antiguas que guardan una URL de Firebase Storage pueden mostrar un marcador en Spark. Firebase requiere Blaze para acceder a los buckets de Storage desde el 3 de febrero de 2026; sus datos permanecen en el bucket, pero Spark no puede leerlos ni escribirlos. Consulta [los requisitos actuales de Storage](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024).

## Notificaciones sin Cloud Functions

Las notificaciones dentro de la app se guardan en Firestore y se consultan desde la bandeja de PetGo. El envío automático de avisos push tras nuevos avistamientos o al resolver una alerta depende de `functions/`; Firebase exige Blaze para desplegar Cloud Functions. No ejecutes `firebase deploy --only functions` si quieres mantener el proyecto en Spark.

La app no requiere desplegar Storage ni Functions para crear publicaciones, guardar avistamientos, consultar mapas o revisar notificaciones dentro de PetGo. Authentication y Firestore siguen sujetos a las cuotas incluidas en Spark.

## Moderación de denuncias

La bandeja de moderación aparece únicamente para cuentas con el custom claim `moderator: true`. Asigna ese claim con Firebase Admin SDK desde un entorno confiable; nunca lo guardes en un campo editable por el usuario en Firestore. Las cuentas sin ese claim solo pueden leer sus propios folios. La app permite actualizar los estados `Recibida`, `En revisión`, `Atendida` y `No procede`.
