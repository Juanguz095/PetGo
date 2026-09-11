Rediseña la pantalla inicial de mi aplicación Android. Trabaja únicamente sobre esta pantalla y no modifiques ninguna otra funcionalidad.

## Objetivo

Crear una pantalla de bienvenida moderna, profesional y memorable para una aplicación dedicada a mascotas perdidas, encontradas, adopción, avistamientos, albergues y denuncias de maltrato animal.

La pantalla debe transmitir inmediatamente:

- mascotas
- ayuda
- comunidad
- ubicación
- esperanza
- volver a casa

## Estructura exacta

La pantalla debe estar compuesta verticalmente por:

1. Ilustración principal
2. Título
3. Descripción
4. Botón "Continuar"
5. Texto de inicio de sesión

### 1. Ilustración

Colocar una ilustración grande en la parte superior, ocupando aproximadamente el 40% de la pantalla.

La ilustración debe mostrar una escena de rescate/comunidad animal.

Debe incluir visualmente:
- perros y gatos
- una persona ayudando o buscando una mascota
- un pin de ubicación
- elementos relacionados con mapas
- huellas de animales
- sensación de comunidad

Estilo:
- ilustración 2D
- cartoon moderno
- limpio
- amigable
- profesional
- colores agradables
- formas redondeadas
- no fotorealista
- no excesivamente infantil

La ilustración debe tener bordes redondeados y estar integrada con el diseño general.

### 2. Título

Mostrar exactamente:

"Cada mascota merece
volver a casa"

Características:
- blanco
- negrita o semibold
- centrado
- grande
- máximo 2 líneas
- alta legibilidad

### 3. Descripción

Mostrar exactamente:

"Ayuda a encontrar mascotas perdidas, reporta avistamientos y dale una segunda oportunidad a quienes buscan un hogar."

Características:
- gris claro
- centrado
- tamaño aproximadamente 17-18sp
- interlineado cómodo
- máximo 4 líneas
- separación clara respecto al título

### 4. Botón principal

Botón grande con el texto:

"Continuar"

Características:
- ancho: 85-90% de la pantalla
- altura: aproximadamente 64dp
- esquinas muy redondeadas
- color naranja/coral suave
- texto negro
- semibold
- ubicado cerca de la parte inferior del contenido principal

Debe mantener y utilizar la navegación existente de "Continuar".

No crear una navegación nueva.

### 5. Inicio de sesión

Debajo del botón mostrar:

"¿Ya tienes una cuenta? Iniciar sesión"

"¿Ya tienes una cuenta?" debe utilizar color gris.

"Iniciar sesión" debe utilizar el color principal naranja/coral y tener apariencia de enlace.

Al pulsarlo debe utilizar la navegación existente hacia la pantalla de inicio de sesión.

## Estilo general

Fondo:
- negro o casi negro
- limpio
- sin degradados innecesarios
- sin elementos decorativos excesivos

La combinación debe generar contraste entre el fondo oscuro, el texto blanco y el botón naranja/coral.

## Diseño

La pantalla debe sentirse como una aplicación real publicada en Google Play, no como un prototipo.

Priorizar:
- jerarquía visual
- espacios amplios
- alineación
- consistencia
- legibilidad
- sensación de confianza
- estética moderna

No llenar la pantalla con información.

## Responsive

Debe funcionar correctamente en diferentes tamaños de Android.

Comprobar:
- 360x640
- 360x800
- 390x844
- 412x915

Evitar:
- texto cortado
- elementos fuera de pantalla
- solapamientos
- botones deformados
- espacios excesivos
- scroll innecesario

## Restricciones

NO modificar:
- Firebase
- autenticación
- base de datos
- mapa
- publicaciones
- adopciones
- albergues
- denuncias
- favoritos
- lógica de negocio
- navegación existente fuera de esta pantalla

No cambies la arquitectura del proyecto.

Primero inspecciona cómo está implementada actualmente esta pantalla y reutiliza los componentes, estilos y recursos existentes cuando sea posible.

Si ya existe una ilustración adecuada en el proyecto, reutilízala. Si no existe, crea/prepara un recurso visual adecuado.

## Resultado esperado

Quiero que esta pantalla sea inmediatamente reconocible como una aplicación de ayuda para mascotas.

La sensación general debe ser:

"Una comunidad que ayuda a que las mascotas vuelvan a casa."

Al terminar:

1. Indica qué archivos modificaste.
2. Verifica que compile.
3. Verifica que "Iniciar sesión" siga funcionando.
4. No modifiques funcionalidades fuera de esta pantalla.
