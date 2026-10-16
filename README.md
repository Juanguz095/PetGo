# Descripcion

Aplicación Android para conectar a la comunidad con mascotas perdidas, encontradas y en adopción. Permite publicar alertas con ubicación en el mapa, reportar avistamientos de mascotas perdidas y gestionar adopciones.

## Funcionalidades

### Mapa de alertas
- Mapa basado en OpenStreetMap (sin API keys ni servicios de pago)
- Pines de colores según el tipo de alerta:
  - Rojo: mascota perdida
  - Verde: mascota encontrada
  - Naranja: mascota en adopción
  - Amarillo: avistamiento reportado
  - Gris: caso resuelto
- Círculo de búsqueda de 1.5 km alrededor de cada mascota perdida
- Botones de recentrar ubicación y zoom propio

### Publicaciones
- Mantener presionado el mapa para publicar en el punto exacto
- Foto desde la galería, nombre, descripción, especie y último lugar visto
- Estados: activa / resuelta

### Avistamientos comunitarios
- Cualquier usuario puede reportar dónde vio una mascota perdida
- Foto y descripción opcionales
- El dueño visualiza el contador de avistamientos en la lista y el detalle de cada uno
- Los avistamientos aparecen como pines amarillos en el mapa

### Adopciones
- Lista de animales en adopción con foto, especie y descripción
- Buscador y filtros por especie (perros / gatos / todos)
- Pantalla de detalle con favoritos y contacto por WhatsApp
- Enlace para ver la ubicación en el mapa de la aplicación

### Usuarios
- Registro e inicio de sesión con Firebase Authentication
- Sesión persistente en el dispositivo
- Perfil de usuario y cierre de sesión

### Contacto y compartición
- Compartir alertas por WhatsApp con texto y enlace a la ubicación
- Compartir por cualquier otra aplicación instalada

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin (Android nativo) |
| Mapas | osmdroid (OpenStreetMap) |
| Base de datos | Cloud Firestore (DatabaseHelper conserva la API anterior) |
| Autenticación | Firebase Authentication, correo y contraseña |
| Sesión y favoritos | Sesión del SDK; favoritos locales por cuenta y sincronizados con Firestore |
| Imágenes | Glide; fotos comprimidas guardadas en Firestore |

## Requisitos

- Android 8.0 (API 26) o superior
- Conexión a internet para cargar los mapas
- Android Studio compatible con la versión de AGP declarada en el proyecto
- Configuración de Firebase según [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

## Instalación

1. Abrir el proyecto en Android Studio (File → Open → seleccionar la carpeta del proyecto)
2. Esperar a que Gradle sincronice las dependencias
3. Conectar un dispositivo físico o iniciar un emulador
4. Ejecutar la aplicación (Run)

## Flujo de demostración

1. Registrar una cuenta con correo y contraseña
2. Mantener presionado un punto del mapa y publicar una mascota perdida con foto
3. Verificar que el pin rojo aparece en el mapa y que el panel de detalle muestra el círculo de búsqueda
4. Acceder a "Mascotas perdidas" y reportar un avistamiento sobre la publicación
5. Verificar el contador de avistamientos en la lista y el detalle de cada reporte
6. En "Adopciones", abrir el detalle de una mascota, agregarla a favoritos y compartirla por WhatsApp

## Estructura del proyecto

```
app/src/main/java/com/example/practicafinal/
├── MainActivity.kt                 Mapa principal con alertas
├── LoginActivity.kt                Inicio de sesión
├── RegistroActivity.kt             Creación de cuenta
├── AdopcionesActivity.kt           Lista de adopciones
├── DetalleAdopcionActivity.kt      Detalle con favoritos y WhatsApp
├── CrearPublicacionActivity.kt     Formulario de publicación
├── MascotasPerdidasActivity.kt     Lista de perdidas y avistamientos
├── ReportarAvistamientoActivity.kt Reporte de avistamiento
├── MenuOpcionesActivity.kt         Menú de navegación
├── PerfilActivity.kt               Perfil del usuario
├── model/                          Modelos de datos
├── db/                             DatabaseHelper (lógica SQLite)
├── session/                        Gestión de sesión y favoritos
├── adapters/                       Adaptadores RecyclerView
└── util/                           Utilidades
```

## Limitaciones conocidas

- Las operaciones compartidas requieren conexión con Firebase; los favoritos también tienen una copia local por cuenta.
- Las fotos se guardan comprimidas dentro del documento de Firestore. Storage está reservado para una futura migración; sus reglas ya limitan las cargas por propietario.
- No hay notificaciones push entre dispositivos. Los avisos al dueño aparecen en la sección de notificaciones de la app.
- Proyecto académico: práctica final.

## Documentación

- [Revisión del proyecto](docs/revision-proyecto.md): correcciones, comprobaciones y pendientes
- [Configuración de Firebase](FIREBASE_SETUP.md): configuración y publicación de reglas
