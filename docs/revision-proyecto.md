# Revisión de PetGo

## Correcciones funcionales

- El buscador de mascotas perdidas y los filtros por especie tenían apariencia de controles, pero no tenían lógica. Ahora filtran los datos reales y el contador refleja las publicaciones cargadas.
- La búsqueda de adopciones incluye especie y ubicación, ignora tildes y muestra un estado vacío y un contador de resultados. El botón del proceso de adopción abre una explicación.
- Los favoritos locales se guardaban en un conjunto compartido entre cuentas. Ahora se separan por UID; una sincronización antigua no sobrescribe una selección reciente. La lista permite abrir y quitar favoritos.
- La autenticación utilizaba solicitudes REST y un UID local, sin iniciar la sesión del SDK. Ahora registro, login y cierre usan Firebase Authentication; las preferencias antiguas no bastan para autenticar una cuenta.
- Los errores de carga se capturan y las respuestas no actualizan actividades cerradas. Se eliminaron lecturas repetidas al abrir varias listas y el perfil.
- La consulta de notificaciones requería un índice compuesto no declarado. Ahora consulta por destinatario y ordena los resultados localmente.
- Crear un avistamiento y su aviso al dueño se confirma en un mismo lote. Un fallo del aviso local no convierte un guardado exitoso en un error que invite a repetirlo.
- Actualizar un avistamiento modificaba la colección de publicaciones. Ahora actualiza el avistamiento y verifica su autor.
- Las fotos se cargan con el mismo mecanismo en todos los listados y detalles. Se cancelan peticiones de imágenes al reciclar tarjetas, se limita la decodificación y no se guarda una URI privada cuando falla la lectura.
- Ambos formularios de denuncia requieren motivo y descripción. Las fotos se capturan al enviar para que la tarea no use una selección posterior.
- Los permisos aceptan ubicación aproximada. La búsqueda de ubicación y la transición de bienvenida se cancelan al salir; los mapas liberan sus recursos al cerrar la actividad.
- Se eliminaron las inserciones automáticas de alertas y albergues ficticios al abrir pantallas.

## Interfaz

Se conserva el estilo de fondo crema, Poppins, bordes negros y sombras desplazadas. Los controles comparten estilos, las fotos conservan sus bordes y esquinas y las listas vacías tienen mensajes contextualizados. Los filtros largos se desplazan horizontalmente. Se corrigió el contraste del login, el tamaño de textos muy pequeños y el tema de navegación para Android 8.0.

## Seguridad

Las reglas anteriores permitían lectura y escritura sin autenticación. Las reglas locales nuevas restringen el acceso por sesión y propietario, protegen favoritos y denuncias y limitan las actualizaciones de notificaciones al campo de lectura. Storage limita las imágenes por UID, formato y tamaño.

**Estas reglas todavía deben publicarse en el proyecto Firebase real.** La validación utiliza emuladores locales y cuentas ficticias. No se desplegaron cambios ni se escribieron datos de prueba en producción. Las cuentas anteriores conservan su UID en Firebase, pero deben volver a iniciar sesión para crear la sesión del SDK.

## Validación reproducible

Resultado final del 16 de octubre de 2026: compilación correcta, 10 pruebas unitarias aprobadas, 3 pruebas de interfaz y 4 de integración Android aprobadas en emulador, y 22 comprobaciones de reglas aprobadas. Lint: 0 errores y 395 advertencias pendientes.

```powershell
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Las pruebas unitarias cubren búsquedas con tildes, especie, ubicación y filtros combinados, y validación de denuncias. Las pruebas de interfaz verifican navegación, login vacío y la carga de nueve pantallas a 360×640, 360×800, 390×844 y 412×915. También se revisaron capturas de login y registro en un emulador a 360×640.

`FirebaseFlujoTest` utiliza exclusivamente el proyecto `demo-petgo-review` cuando se pasa `firebaseEmulators=true`: Auth en el puerto 9190 y Firestore en 8180. Comprueba sesión, favoritos entre cuentas, avistamientos, avisos al dueño, fotos y errores de credenciales. `tests/firebase-rules.test.cjs` comprueba los permisos en Firestore y Storage, con Storage en 9299. Para ejecutar este script se instalan las herramientas de pruebas en `.review`:

```powershell
npm install --prefix .review firebase-tools @firebase/rules-unit-testing firebase
node tests/firebase-rules.test.cjs
```

Los emuladores deben estar iniciados previamente con esas reglas y puertos. Los archivos temporales de `.review` no se incluyen en Git.

## Pendientes

- Publicar las reglas en Firebase real y verificar los flujos en dos dispositivos con la configuración definitiva.
- Quedan advertencias de lint, principalmente textos escritos directamente en layouts/código y recursos sin uso. No se ocultaron ni se creó una baseline.
- Las fotos siguen dentro de documentos de Firestore. Una migración a Storage requiere actualizar la ruta y gestionar cargas y borrados.
- Los avisos entre cuentas son internos a la app; las notificaciones push requieren un backend de envío.

Referencias utilizadas: [Firebase Authentication para Android](https://firebase.google.com/docs/auth/android/password-auth), [consultas protegidas por reglas](https://firebase.google.com/docs/firestore/security/rules-query), [condiciones de Storage](https://firebase.google.com/docs/storage/security/rules-conditions) y [permisos de ubicación de Android](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime).
