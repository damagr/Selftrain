# SelfTrain

[![Release](https://img.shields.io/github/v/release/damagr/Selftrain?style=flat-square&logo=github&label=release)](https://github.com/damagr/Selftrain/releases)
[![License](https://img.shields.io/github/license/damagr/Selftrain?style=flat-square&color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android_8+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)

App Android para registrar entrenamientos de gimnasio. Soporta **Método Bilbo** (series de activación explosiva + series de trabajo), Full Body y Push-Pull-Legs.

## ¿Qué es SelfTrain?

SelfTrain te guía durante el entreno ejercicio a ejercicio, registra cada serie al instante y guarda tu progreso automáticamente. Así puedes concentrarte en entrenar: la app recuerda tus pesos, te sugiere la carga de cada ejercicio, controla los descansos y te muestra cómo evolucionas con calendarios y gráficos.

## Características principales

### Entrenamiento guiado
- Navega entre ejercicios con anterior/siguiente o salta directamente a cualquiera
- Cada ejercicio tiene un **GIF animado** (botón `i` junto al nombre) para ver cómo se hace
- La app te **sugiere el peso y las repeticiones** según tu última sesión
- **Descanso automático**: al registrar una serie, el temporizador arranca solo con el tiempo del ejercicio (ajustable ±30s), y la app te avisa con una vibración
- **Calculadora de discos** (botón junto al peso): cómo cargar la barra exactamente, por lado
- **Notas por serie** ("fallé en la última", "subir peso la próxima"...)
- Cada serie se registra al instante y **se guarda automáticamente**: si la app se cierra a mitad de entreno, al volver recupera tu sesión donde la dejaste (series y ejercicio)

### Método Bilbo (automático)
- Si tu rutina es Bilbo, la app te ofrece la **serie de activación explosiva** antes de las series de trabajo
- Sugiere peso y reps según la **progresión del método**: al llegar a 50 reps limpias, sube el peso y reinicia
- Ajuste intra-sesión: si rindes bien te sugiere subir peso en la siguiente serie, si te quedaste corto, bajar
- Las sugerencias de mancuernas se redondean al paso de 2.5kg

### Temporizador de descanso
- Arranca solo al registrar serie (o inícialo con un toque)
- La cuenta **sigue en una notificación aunque uses otra app, apagues la pantalla o el sistema cierre la app**
- Pausa/reanudar desde la notificación o desde la app — siempre sincronizados
- **Aviso sonoro** cuando termina
- El tiempo de descanso se ajusta por defecto por tipo de ejercicio, y puedes fijarlo **por ejercicio concreto** al editar la rutina

### Historial y progreso
- **Calendario mensual** con los días que entrenaste
- Detalle de cada entreno: series, peso, RIR, notas y **fecha — todo editable**
- **Filtros** por rutina y/o ejercicio para ver solo lo que te interese
- **1RM estimado** y gráficos de progresión por ejercicio
- **Comparativa con la semana anterior**

### Estadísticas
- **Racha semanal**: semanas consecutivas entrenando
- **Gráfico de reps de trabajo**: lo que conseguiste vs el objetivo (12 por serie) cada semana — toca una barra para ver la cifra exacta
- **Volumen total acumulado** y entrenos completados
- **Todos tus récords personales** en una lista: 1RM estimado de cada ejercicio y su **fuerza relativa** (1RM por kg de peso corporal)
- Acceso desde la pantalla de Historial

### Rutinas
- **6 programas predefinidos** (PPL, Full Body y variantes Bilbo) cargables con un toque
- Tarjeta **"Continuar"** en la pantalla principal: te dice qué día de tu programa toca hoy
- Etiqueta **"Te toca"** en cada programa según la rotación de días
- Crea tus propias rutinas y añade/quita/reordena ejercicios
- **Comparte rutinas por QR** (o cópialas como texto) para importarlas en otro dispositivo

### Biblioteca de ejercicios
- **60 ejercicios pre-cargados** con grupo muscular, categoría y equipamiento
- Crea ejercicios propios
- Borra los que no uses (la app avisa si están en uso)

### Atajos y personalización
- **Long-press del icono de la app**: repetir tu última rutina, ir al historial o a estadísticas
- **Modo claro** para entrenar con mucha luz (Ajustes), junto al tema oscuro por defecto
- **Comparte el resumen** de tu entreno como texto al finalizar
- Feedback háptico al registrar series y terminar descansos

### Backup y datos
- **Backup automático diario** en segundo plano
- Exporta/importa tus datos manualmente en JSON
- **Exporta tus series a CSV** para analizarlas en Excel o Google Sheets
- Elige la carpeta donde guardar los backups

### Actualizaciones integradas
- La app detecta nuevas versiones y **las instala desde la propia app**

### Dashboard web (PC)
Estudia tu progresión desde el navegador del PC:

```bash
# Exporta el backup desde la app (Ajustes → Exportar)
# Copia el JSON al PC y ejecuta:
python3 dashboard/dashboard.py selftrain_backup.json
# Abre http://localhost:8080
```

Solo necesita Python 3 (sin dependencias extra).

## Cómo funciona: el workflow

1. **Prepara tus rutinas** — el primer día, la app te da la bienvenida y ofrece cargar los programas predefinidos; también puedes crear las tuyas. Te pedirá permiso de notificaciones (para el aviso del temporizador).
2. **Empieza el entreno** — la tarjeta "Continuar" te dice qué día toca; elige rutina y pulsa **Empezar**. Si tienes dudas sobre cómo funciona, pulsa el botón **?** arriba.
3. **Registra las series** — sigue los ejercicios en orden: serie de activación (si es Bilbo) + series de trabajo. La app sugiere los pesos y guarda todo al instante; el **descanso arranca solo** tras cada serie. Si te equivocas, puedes deshacer la última serie.
4. **Descansa** — la cuenta sigue fuera de la app aunque apagues la pantalla; el aviso sonoro te avisa al terminar.
5. **Finaliza** — pulsa **Finalizar** y revisa el resumen: volumen por grupo muscular, 1RM estimado, nuevos récords, duración y peso corporal. Se guarda en el historial automáticamente y puedes compartirlo como texto.
6. **Consulta tu progreso** — calendario con filtros, gráficos de 1RM, **estadísticas** (racha, volumen, PRs) o el dashboard interactivo en el PC.
7. **Protege tus datos** — backup automático diario, exportación manual o CSV desde Ajustes.

## Descarga e instalación

1. Descarga el APK de la última versión desde [GitHub Releases](https://github.com/damagr/Selftrain/releases)
2. Instálalo (si te lo pide, activa "Instalar apps de fuentes desconocidas")
3. Las actualizaciones llegarán avisadas dentro de la propia app (Ajustes → Buscar actualización)

## Para desarrolladores

```bash
./gradlew assembleDebug          # compilar
./gradlew testDebugUnitTest      # tests (lógica + ViewModels)
```

## Licencia

MIT — ver [LICENSE](LICENSE).
