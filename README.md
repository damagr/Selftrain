# SelfTrain

App Android para registrar entrenamientos de gimnasio. Soporta **Método Bilbo** (series de activación explosiva + series de trabajo), Full Body y Push-Pull-Legs.

## Funcionalidades

### Entreno en vivo
- **Navegación paso a paso** entre ejercicios con anterior/siguiente y salto directo
- **GIF demostrativo** de cada ejercicio — botón `i` junto al nombre → popup con animación del movimiento (vía CDN, sin peso en la app)
- **Sugerencias automáticas** de peso y reps basadas en el historial de la sesión anterior
- **PRs de la mejor sesión anterior** visibles durante el entreno (card colapsable para no ocupar espacio)
- **Series Bilbo + series de trabajo** con prellavereo inteligente del peso
- **Progresión intra-sesión**: ajuste automático de peso según reps del set anterior
- **Temporizador de descanso** configurable (±30s, 30–300s) con **aviso sonoro al terminar** (notificación heads-up, funciona aunque estés en otra app)
- **Resumen al finalizar**: volumen por grupo muscular, 1RM estimado (Epley), comparativa con la semana anterior, nuevos récords

### Método Bilbo
Sistema creado por Jesús María Varela Goicochea:
- **1 serie Bilbo**: 15–50 reps al ~50% 1RM, concéntrica explosiva, excéntrica controlada, RIR 1–3
- **3–4 series de trabajo**: 8–12 reps con ~40% más peso, descanso 90–120s
- **Progresión automática**: al llegar a 50 reps limpias → +10% peso y reinicio a 15–20 reps

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

- **Jerarquía padre/hijo**: los programas se expanden para mostrar sus días, las rutinas sueltas conviven sin interferir
- Crear rutinas personalizadas con selector de método
- Añadir/quitar/reordenar ejercicios (↑↓ con animación)
- Reemplazar ejercicios sin borrar y volver a añadir
- Buscador en el selector de ejercicios (filtro en tiempo real)

### Biblioteca de ejercicios
- **54 ejercicios pre-cargados** con grupo muscular, categoría (compuesto/aislamiento) y equipamiento
- **GIF demostrativo** de cada ejercicio cargado desde CDN (no ocupa espacio en la app)
- Crear ejercicios propios (nombre, grupo muscular, categoría, equipamiento)
- Eliminación suave (soft delete) con restauración desde Ajustes
- Validación: no permite borrar ejercicios en uso en rutinas o histórico

### Historial
- Vista resumen con volumen por grupos musculares
- Drill-down: lista de entrenos → detalle por ejercicio
- 1RM estimado (fórmula Epley) con evolución
- Gráfico de progresión de 1RM por ejercicio
- Comparativa con la semana anterior

### Backup y datos
- **Backup automático diario** (WorkManager) en segundo plano
- **Backup preventivo antes de cada actualización** de la app
- **Carpeta configurable**: elige dónde guardar los backups desde Ajustes (accesible desde el gestor de archivos)
- Conserva los últimos 5 backups automáticos
- Exportar/importar manual en JSON

### Actualización integrada
- Detecta automáticamente nuevas versiones en GitHub
- Descarga e instala el APK con backup previo automático
- Avisa si ya estás en la última versión

### Cuadro de mando web
Dashboard interactivo para estudiar progresión desde el navegador del PC:

```bash
# Exporta el backup desde la app (Ajustes → Exportar)
# Copia el JSON al PC y ejecuta:
python3 dashboard/dashboard.py selftrain_backup.json
# Abre http://localhost:8080
```

Gráficas con Chart.js: 1RM estimado (Epley), peso máximo por sesión, volumen total, frecuencia semanal. Solo necesita Python 3 (sin dependencias extra).

## Stack

Kotlin + Jetpack Compose + Material 3 + Room + Hilt + Navigation Compose + Coil

### Dependencias principales
- **Coil** — carga de GIFs demostrativos desde CDN
- **Room** — persistencia local SQLite
- **Hilt** — inyección de dependencias
- **Navigation Compose** — navegación tipo single-activity
- **Gson** — parsing de ejercicios semilla y backups JSON
- **WorkManager** — backup automático diario

## Build

```bash
./gradlew assembleDebug
```

APK en `app/build/outputs/apk/debug/selftrain-debug.apk`
