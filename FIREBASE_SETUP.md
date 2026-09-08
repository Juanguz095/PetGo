# Configuración de Firebase para PetGo

La app ya contiene la integración de Auth, Firestore y Storage. Falta asociarla al proyecto Firebase real de la feria:

1. En Firebase Console crea un proyecto y registra una app Android con el paquete com.example.practicafinal.
2. Activa Authentication > Sign-in method > Email/Password.
3. Crea Firestore Database en modo producción y Storage. Publica las reglas incluidas en firestore.rules y storage.rules.
4. Descarga el google-services.json generado por Firebase y colócalo en app/google-services.json. El Gradle del proyecto activa automáticamente el plugin Google Services cuando ese archivo existe.
5. Ejecuta ./gradlew :app:assembleDebug y prueba registro, publicación con foto, avistamiento, favoritos y cierre de sesión en dos dispositivos.

La aplicación no contiene credenciales inventadas ni un google-services.json falso: Firebase genera ese archivo exclusivamente para el proyecto y el identificador de Android que se registre en la consola.
