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
- Cada serie se registra al instante y **se guarda automáticamente**: si la app se cierra a mitad de entreno, al volver recupera tu sesión donde la dejaste (series y ejercicio)

### Método Bilbo (automático)
- Si tu rutina es Bilbo, la app te ofrece la **serie de activación explosiva** antes de las series de trabajo
- Sugiere peso y reps según la **progresión del método**: al llegar a 50 reps limpias, sube el peso y reinicia
- Las sugerencias de mancuernas se redondean al paso de 2.5kg

### Temporizador de descanso
- Inicia el descanso con un toque (ajustable ±30s)
- La cuenta **sigue en una notificación aunque uses otra app, apagues la pantalla o el sistema cierre la app**
- Pausa/reanudar desde la notificación o desde la app — siempre sincronizados
- **Aviso sonoro** cuando termina

### Historial y progreso
- **Calendario mensual** con los días que entrenaste
- Detalle de cada entreno: series, peso y RIR — **editables**
- **1RM estimado** y gráficos de progresión por ejercicio
- **Comparativa con la semana anterior**

### Rutinas
- **6 programas predefinidos** (PPL, Full Body y variantes Bilbo) cargables con un toque
- Crea tus propias rutinas y añade/quita/reordena ejercicios
- **Comparte rutinas por QR** (o cópialas como texto) para importarlas en otro dispositivo

### Biblioteca de ejercicios
- **60 ejercicios pre-cargados** con grupo muscular, categoría y equipamiento
- Crea ejercicios propios
- Borra los que no uses (la app avisa si están en uso)

### Backup y datos
- **Backup automático diario** en segundo plano
- Exporta/importa tus datos manualmente en JSON
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

1. **Prepara tus rutinas** — al abrir por primera vez, carga los programas predefinidos (botón "Cargar rutinas") o crea las tuyas. Al iniciar, la app pedirá permiso de notificaciones (para el aviso del temporizador).
2. **Empieza el entreno** — elige la rutina y pulsa **Empezar**.
3. **Registra las series** — sigue los ejercicios en orden: serie de activación (si es Bilbo) + series de trabajo. La app sugiere los pesos y guarda todo al instante. Si te equivocas, puedes deshacer la última serie.
4. **Descansa** — usa el temporizador entre series; la cuenta sigue fuera de la app aunque apagues la pantalla.
5. **Finaliza** — pulsa **Finalizar** y revisa el resumen: volumen por grupo muscular, 1RM estimado, nuevos récords y duración. Se guarda en el historial automáticamente.
6. **Consulta tu progreso** — calendario, gráficos de 1RM y comparativas en la app; o el dashboard interactivo en el PC.
7. **Protege tus datos** — backup automático diario o exportación manual desde Ajustes.

## Descarga e instalación

1. Descarga el APK de la última versión desde [GitHub Releases](https://github.com/damagr/Selftrain/releases)
2. Instálalo (si te lo pide, activa "Instalar apps de fuentes desconocidas")
3. Las actualizaciones llegarán avisadas dentro de la propia app (Ajustes → Buscar actualización)

## Para desarrolladores

```bash
./gradlew assembleDebug
```

## Licencia

MIT — ver [LICENSE](LICENSE).
