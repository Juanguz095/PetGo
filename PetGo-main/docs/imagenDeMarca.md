# paleta de colores:

#32161f
#472026
#623430
#7a463c
#8e5849
#ac7764
#531c25
#772c2f
#94483b
#af654f
#7a3d2e
#9a583d
#ac6a43
#c4834e
#deb661
#e7da83
#eaeadf
#182f1a
#294a2a
#40653b
#547749
#719161
#97af7f
#1d1427
#271c38
#30274d
#3e386b
#545894
#5c6dac
#7393d0
#9fbbe1
#c6daed

**Descripción**

Aplicación Android para conectar a la comunidad con mascotas perdidas, encontradas y en adopción. Permite publicar alertas con ubicación en el mapa, reportar avistamientos, gestionar adopciones, registrar albergues y denunciar casos de maltrato animal.

**Funcionalidades**

**Mapa de alertas**
- Mapa basado en OpenStreetMap (sin API keys ni servicios de pago)
- Pines de colores según el tipo de alerta: rojo (perdida), verde (encontrada), naranja (adopción), amarillo (avistamiento), azul (albergue)
- Círculo de búsqueda de 1.5 km alrededor de cada mascota perdida
- Botones de recentrar ubicación y zoom
- Diálogo de leyenda que explica los colores de los pines
- Diálogo de alertas cercanas con las 20 más próximas ordenadas por distancia

**Publicaciones**
- Mantener presionado el mapa para publicar en el punto exacto
- Foto desde la galería, nombre, descripción, especie y último lugar visto
- Tipos: mascota perdida, encontrada o en adopción
- Estados: activa / resuelta

**Avistamientos comunitarios**
- Cualquier usuario puede reportar dónde vio una mascota perdida
- Foto y descripción opcionales
- El dueño visualiza el contador de avistamientos y el detalle de cada uno
- Los avistamientos aparecen como pines amarillos en el mapa

**Adopciones**
- Lista de animales en adopción con foto, especie y descripción
- Buscador y filtros por especie (perros / gatos / todos)
- Pantalla de detalle con favoritos y contacto por WhatsApp
- Enlace para ver la ubicación en el mapa de la aplicación

**Albergues**
- Lista de albergues con nombre, dirección, teléfono y distancia
- Búsqueda y ordenamiento (por distancia o alfabético)
- Pantalla de detalle con mapa, WhatsApp y llamada directa

**Denuncias**
- Reportes de maltrato, abandono o venta ilegal
- Foto, descripción y ubicación en el mapa
- Lista de denuncias del usuario con detalle individual

**Usuarios**
- Registro e inicio de sesión con Firebase Authentication
- Sesión persistente en el dispositivo
- Perfil con estadísticas de publicaciones, denuncias y favoritos
- Cierre de sesión

**Favoritos**
- Marcar publicaciones como favoritas
- Sincronización local y en la nube
- Lista de favoritos accesible desde el perfil

**Contacto y compartición**
- Compartir alertas por WhatsApp con texto y enlace a la ubicación
- Compartir por cualquier otra aplicación instalada
