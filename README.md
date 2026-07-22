# SelfTrain

[![Release](https://img.shields.io/github/v/release/damagr/Selftrain?style=flat-square&logo=github&label=release)](https://github.com/damagr/Selftrain/releases)
[![License](https://img.shields.io/github/license/damagr/Selftrain?style=flat-square&color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android_8+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)

App Android para registrar entrenamientos de gimnasio. Soporta **Método Bilbo** (series de activación explosiva + series de trabajo), Full Body y Push-Pull-Legs.

## Funcionalidades

### Entreno en vivo
- **Navegación paso a paso** entre ejercicios con anterior/siguiente y salto directo
- **GIF demostrativo** de cada ejercicio — botón `i` junto al nombre → popup con animación del movimiento (vía CDN, sin peso en la app)
- **Sugerencias automáticas** de peso y reps basadas en el historial de la sesión anterior
- **PRs de la mejor sesión anterior** visibles durante el entreno (card colapsable para no ocupar espacio)
- **Series Bilbo + series de trabajo** con prellavereo inteligente del peso
- **Progresión intra-sesión**: ajuste automático de peso según reps del set anterior (tarjeta verde +5% si reps >10, roja −10% si <8)
- **Temporizador de descanso** configurable (±30s, 30–300s) con:
  - **Servicio en primer plano** con notificación persistente y botones Pausa/Parar
  - **Aviso sonoro al terminar** (canal de alta prioridad con sonido+ vibración)
  - Pausa/reanudar/reiniciar desde la propia tarjeta del timer
  - Funciona aunque estés en otra app
- **Resumen al finalizar**: volumen por grupo muscular, 1RM estimado (Epley), comparativa con la semana anterior, nuevos récords con flechas de evolución, duración editable

### Recuperación de sesiones
- Si la app se cierra durante un entreno, al volver aparece un diálogo para **reanudar o descartar** la sesión incompleta
- El progreso se persiste ejercicio a ejercicio (`lastExerciseIndex` en la entidad Workout)

### Método Bilbo
Sistema creado por Jesús María Varela Goicochea:
- **1 serie Bilbo**: 15–50 reps al ~50% 1RM, concéntrica explosiva, excéntrica controlada, RIR 1–3
- **3–4 series de trabajo**: 8–12 reps con ~40% más peso, descanso 90–120s
- **Progresión automática**: al llegar a 50 reps limpias → +10% peso y reinicio a 15–20 reps
- **Progresión entre sesiones**: al iniciar un ejercicio, muestra tarjeta con sugerencia de subir/bajar peso según las series de la sesión anterior

Solo aplica a ejercicios compuestos (press, sentadilla, remo, peso muerto…). Los de aislamiento van directos a series de trabajo.

### Rutinas
- **6 programas predefinidos** cargables con un toque (botón "Cargar rutinas"):

| Programa | Días | Método |
|----------|------|--------|
| PPL 5 Días | Push / Pull / Legs / Push / Pull | PPL |
| PPL 3 Días | Push / Pull / Legs | PPL |
| Full Body 3 Días | Día 1 / 2 / 3 | Full Body |
| Bilbo Upper/Lower 4 Días | Upper A / B + Lower A / B | Bilbo |
| Bilbo Full Body 3 Días | Día 1 / 2 / 3 | Bilbo |
| Bilbo PPL 3 Días | Push / Pull / Legs | Bilbo |

- **Jerarquía padre/hijo**: los programas se expanden para mostrar sus días; las rutinas sueltas conviven sin interferir
- Crear rutinas personalizadas con selector de método (Bilbo / Full Body / PPL)
- Añadir/quitar/reordenar ejercicios (↑↓ con `animateItem()`)
- Reemplazar ejercicios sin borrar y volver a añadir
- Buscador en el selector de ejercicios (filtro en tiempo real)
- Añadir días a programas existentes; eliminar programa con todos sus hijos

### Biblioteca de ejercicios
- **54 ejercicios pre-cargados** con grupo muscular, categoría (compuesto/aislamiento) y equipamiento (barra, mancuerna, cable, máquina, peso corporal)
- **GIF demostrativo** de cada ejercicio cargado desde CDN
- Crear ejercicios propios (nombre, grupo muscular, categoría, equipamiento)
- **Eliminación suave** (soft delete): modo borrado con toggle en la barra superior; muestra conteo exacto de usos antes de borrar
- Validación: no permite borrar ejercicios en uso en rutinas o histórico (avisa con snackbar)
- Restauración de ejercicios eliminados desde Ajustes

### Historial
- **Calendario mensual** navegable con dots en días con entreno, día seleccionado con borde
- Vista resumen con volumen por grupos musculares
- Drill-down: pulsar entreno → detalle por ejercicio con sets expandibles
- **Edición de sets**: modificar reps, peso, RIR, tipo Bilbo/Trabajo desde el detalle
- **Añadir sets y ejercicios** a entrenos ya completados
- 1RM estimado (fórmula Epley) con evolución
- Gráfico de progresión de 1RM por ejercicio
- Comparativa con la semana anterior
- Borrado de entreno con doble confirmación

### Backup y datos
- **Backup automático diario** (WorkManager) en segundo plano
- **Backup preventivo antes de cada actualización** de la app
- **Carpeta configurable**: selector SAF (Storage Access Framework) — elige dónde guardar los backups desde Ajustes
- Conserva los últimos 5 backups automáticos (limpia los antiguos)
- Exportar/importar manual en JSON con feedback toast

### Actualización integrada
- Detecta automáticamente nuevas versiones en GitHub
- Diálogo de progreso con porcentaje durante la descarga del APK
- Gestión de permisos `REQUEST_INSTALL_PACKAGES` en Android 8+
- Backup automático antes de instalar la actualización

### Temática (Material 3 Expressive)
- **Material You** (Android 12+): esquemas de color dinámicos claro/oscuro con paleta pastel azul de respaldo
- **Google Fonts**: Oswald para titulares + Inter para cuerpo/textos
- **Esquinas redondeadas**: 8 niveles de forma personalizada (4–32dp)
- **Animaciones**: transiciones slide+fade entre pantallas, `animateItem()` en reordenación de ejercicios

### Cuadro de mando web
Dashboard interactivo para estudiar progresión desde el navegador del PC:

```bash
# Exporta el backup desde la app (Ajustes → Exportar)
# Copia el JSON al PC y ejecuta:
python3 dashboard/dashboard.py selftrain_backup.json
# Abre http://localhost:8080
```

Dos pestañas con Chart.js v4 + plugin de anotaciones:
- **Ejercicios**: 1RM estimado (Epley), peso máximo, volumen; gráfico Bilbo dual-axis (reps + peso) con línea de anotación en 50 reps
- **Entrenamientos**: total de entrenos, volumen total, duración media, ejercicios distintos; barras de volumen semanal y frecuencia semanal; tabla ordenable con fecha, rutina, duración, ejercicios, series, volumen

Solo necesita Python 3 (sin dependencias extra).

## Stack

Kotlin + Jetpack Compose + Material 3 Expressive + Room + Hilt + Navigation Compose + Coil

### Dependencias principales
- **Coil** — carga de GIFs demostrativos desde CDN (con `ImageDecoderDecoder`)
- **Room** — persistencia local SQLite (v5, 5 entidades)
- **Hilt** — inyección de dependencias (con integración WorkManager)
- **Navigation Compose** — navegación tipo single-activity
- **Gson** — parsing de ejercicios semilla y backups JSON
- **WorkManager** — backup automático diario
- **Material Icons Extended** — iconos Material
- **Google Fonts** — Oswald + Inter tipografía

### Tests
- 19 tests unitarios (JUnit 4, solo JVM) en `app/src/test/` para lógica Bilbo: progresión, Epley 1RM, reglas de ajuste

## Build

```bash
./gradlew assembleDebug
```

APK en `app/build/outputs/apk/debug/selftrain-debug.apk`

```bash
# Release (firmado con keystore)
./gradlew assembleRelease
```

APK firmado en `app/build/outputs/apk/release/selftrain-release.apk`

## CI/CD

GitHub Actions en `.github/workflows/release.yml`: detecta cambio de versión → auto-tag → build APK release → crea GitHub Release.

## Licencia

MIT — ver [LICENSE](LICENSE).
