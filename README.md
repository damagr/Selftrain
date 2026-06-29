# EntrenaGuay

App Android para registrar entrenamientos de gimnasio. Soporta el **Método Bilbo** (series de activación explosiva + series de trabajo), Full Body y Push-Pull-Legs.

## Método Bilbo

Sistema creado por Jesús María Varela Goicochea:

- **1 serie Bilbo**: 15–50 reps al ~50% 1RM, fase concéntrica explosiva, excéntrica controlada, RIR 1–3
- **3–4 series de trabajo**: 8–12 reps con ~40% más peso, descanso 90–120s
- Progresión automática: al llegar a 50 reps limpias → +10% peso y reinicio

Solo se aplica a ejercicios compuestos (press, sentadilla, remo, peso muerto…). Los de aislamiento van directos a series de trabajo.

## Funcionalidades

- Crear rutinas con selector de método (Bilbo / Full Body / Push-Pull-Legs)
- Biblioteca de 40+ ejercicios pre-cargados + crear/eliminar ejercicios propios
- Entreno paso a paso con navegación lineal y salto entre ejercicios
- Temporizador de descanso (90s configurable)
- Sugerencias automáticas de peso/reps basadas en el historial
- PRs de la sesión anterior visibles durante el entreno
- Historial con drill-down: resumen → lista de entrenos → detalle por ejercicio
- 1RM estimado (fórmula Epley)
- Exportar/importar backup en JSON

## Stack

Kotlin + Jetpack Compose + Material 3 + Room + Hilt + Navigation Compose

## Build

```bash
./gradlew assembleDebug
```

APK en `app/build/outputs/apk/debug/app-debug.apk`
