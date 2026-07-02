# SelfTrain

App Android para registrar entrenamientos de gimnasio. Soporta el **Método Bilbo** (series de activación explosiva + series de trabajo), Full Body y Push-Pull-Legs.

## Método Bilbo

Sistema creado por Jesús María Varela Goicochea:

- **1 serie Bilbo**: 15–50 reps al ~50% 1RM, fase concéntrica explosiva, excéntrica controlada, RIR 1–3
- **3–4 series de trabajo**: 8–12 reps con ~40% más peso, descanso 90–120s
- Progresión automática: al llegar a 50 reps limpias → +10% peso y reinicio

Solo se aplica a ejercicios compuestos (press, sentadilla, remo, peso muerto…). Los de aislamiento van directos a series de trabajo.

## Rutinas predefinidas

La app incluye **6 programas** listos para cargar en un toque (botón "Cargar rutinas"):

| Programa | Días | Método |
|----------|------|--------|
| PPL 5 Días | Push / Pull / Legs / Push / Pull | PPL |
| PPL 3 Días | Push / Pull / Legs | PPL |
| Full Body 3 Días | Día 1 / 2 / 3 | Full Body |
| Bilbo Upper/Lower 4 Días | Upper A / B + Lower A / B | Bilbo |
| Bilbo Full Body 3 Días | Día 1 / 2 / 3 | Bilbo |
| Bilbo PPL 3 Días | Push / Pull / Legs | Bilbo |

Los programas se organizan en **jerarquía padre/hijo**: cada programa es una tarjeta expandible que contiene sus días. Las rutinas sueltas conviven sin interferir.

## Funcionalidades

- Crear rutinas con selector de método (Bilbo / Full Body / Push-Pull-Legs)
- **6 programas predefinidos** cargables con un toque, agrupados en jerarquía expandible
- **Biblioteca de 54 ejercicios** pre-cargados con equipamiento (Barra, Mancuerna, Polea, Máquina, Peso corporal) + crear/eliminar ejercicios propios
- **Buscador** en el selector de ejercicios (filtra por nombre en tiempo real)
- **Reemplazar** ejercicios en la rutina sin necesidad de borrar y volver a añadir
- **Reordenar** rutinas y ejercicios con ↑↓ y animación de deslizamiento
- Entreno paso a paso con navegación lineal y salto entre ejercicios
- Temporizador de descanso configurable (±30s, 30–300s) con **aviso sonoro al terminar** (notificación heads-up + sonido predeterminado, aunque estés en otra app)
- Sugerencias automáticas de peso/reps basadas en el historial
- PRs de la sesión anterior visibles durante el entreno
- Historial con drill-down: resumen → lista de entrenos → detalle por ejercicio
- 1RM estimado (fórmula Epley)
- Exportar/importar backup en JSON
- **Carpeta de backups configurable**: elige dónde guardar los backups automáticos desde Ajustes (accesible desde el gestor de archivos)
- **Backup automático diario** + **backup previo a cada actualización** (conserva los últimos 5)
- **Actualización integrada**: detecta nuevas versiones, descarga e instala con backup previo; avisa si ya estás en la última versión
- **Cuadro de mando web** para estudiar progresión con gráficas (1RM, peso, volumen, frecuencia) desde el navegador del PC

## Cuadro de mando (dashboard)

```bash
# Exporta el backup desde la app (Ajustes → Exportar)
# Copia el JSON al PC y ejecuta:
python3 dashboard/dashboard.py selftrain_backup.json
# Abre http://localhost:8080
```

Gráficas interactivas con Chart.js: 1RM estimado (Epley), peso máximo por sesión, volumen total, frecuencia semanal de entrenos. Solo necesita Python 3 (sin dependencias extra).

## Stack

Kotlin + Jetpack Compose + Material 3 + Room + Hilt + Navigation Compose

## Build

```bash
./gradlew assembleDebug
```

APK en `app/build/outputs/apk/debug/app-debug.apk`
