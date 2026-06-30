#!/usr/bin/env python3
"""SelfTrain Dashboard — local web charts from backup JSON. Zero deps, stdlib only.
Usage: python dashboard.py [selftrain_backup.json]
"""

import json
import sys
from collections import defaultdict
from datetime import datetime, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler

HOST, PORT = "localhost", 8080
EPLEY = lambda w, r: w * (1 + r / 30)
LABELS = {
    "pecho": "Pecho", "piernas": "Piernas", "espalda": "Espalda",
    "hombros": "Hombros", "brazos": "Brazos", "core": "Core",
}


def load_data(path):
    with open(path) as f:
        return json.load(f)


def process(data):
    exercises = {e["id"]: e for e in data["exercises"] if not e.get("isDeleted")}
    workouts = {w["id"]: w for w in data["workouts"] if w.get("completed")}
    sets = data["workoutSets"]

    # Per-exercise: split bilbo vs work, aggregate max per session
    per_exercise = {}
    for eid, ex in exercises.items():
        sessions = defaultdict(lambda: {"work_e1rm": 0, "work_weight": 0,
                                         "bilbo_e1rm": 0, "bilbo_weight": 0, "bilbo_reps": 0,
                                         "volume": 0})
        for s in sets:
            if s["exerciseId"] != eid:
                continue
            w = workouts.get(s["workoutId"])
            if not w:
                continue
            date = datetime.fromtimestamp(w["date"] / 1000).strftime("%Y-%m-%d")
            e1rm = round(EPLEY(s["weightKg"], s["reps"]), 1)
            sessions[date]["volume"] += s["reps"] * s["weightKg"]

            if s["setType"] == "bilbo":
                sessions[date]["bilbo_e1rm"] = max(sessions[date]["bilbo_e1rm"], e1rm)
                sessions[date]["bilbo_weight"] = max(sessions[date]["bilbo_weight"], s["weightKg"])
                sessions[date]["bilbo_reps"] = max(sessions[date]["bilbo_reps"], s["reps"])
            else:
                sessions[date]["work_e1rm"] = max(sessions[date]["work_e1rm"], e1rm)
                sessions[date]["work_weight"] = max(sessions[date]["work_weight"], s["weightKg"])

        sorted_sessions = sorted(sessions.items())
        per_exercise[eid] = {
            "name": ex["name"],
            "muscle": LABELS.get(ex.get("muscleGroup", ""), ex.get("muscleGroup", "")),
            "hasBilbo": any(v["bilbo_reps"] > 0 for _, v in sorted_sessions),
            "dates": [d for d, _ in sorted_sessions],
            "work_e1rm": [v["work_e1rm"] or None for _, v in sorted_sessions],
            "work_weight": [v["work_weight"] or None for _, v in sorted_sessions],
            "bilbo_e1rm": [v["bilbo_e1rm"] or None for _, v in sorted_sessions],
            "bilbo_weight": [v["bilbo_weight"] or None for _, v in sorted_sessions],
            "bilbo_reps": [v["bilbo_reps"] or None for _, v in sorted_sessions],
            "volume": [round(v["volume"], 1) for _, v in sorted_sessions],
        }

    # Workouts view
    workout_list = []
    for w in sorted(workouts.values(), key=lambda x: x["date"], reverse=True):
        dt = datetime.fromtimestamp(w["date"] / 1000)
        routine = next((r for r in data["routines"] if r["id"] == w["routineId"]), None)
        w_sets = [s for s in sets if s["workoutId"] == w["id"]]
        ex_names = {exercises[s["exerciseId"]]["name"] for s in w_sets if s["exerciseId"] in exercises}
        total_vol = round(sum(s["reps"] * s["weightKg"] for s in w_sets), 1)
        workout_list.append({
            "id": w["id"],
            "date": dt.strftime("%Y-%m-%d"),
            "weekday": dt.strftime("%a"),
            "routine": routine["name"] if routine else "?",
            "duration": w.get("durationMinutes", 0),
            "exercises": sorted(ex_names),
            "n_sets": len(w_sets),
            "volume": total_vol,
        })

    # Volume trend per week (for workouts view)
    weekly_vol = defaultdict(float)
    weekly_count = defaultdict(int)
    for w in workouts.values():
        dt = datetime.fromtimestamp(w["date"] / 1000)
        wk = dt.strftime("%Y-W%W")
        w_sets = [s for s in sets if s["workoutId"] == w["id"]]
        weekly_vol[wk] += sum(s["reps"] * s["weightKg"] for s in w_sets)
        weekly_count[wk] += 1
    vol_trend = dict(sorted(weekly_vol.items()))

    return per_exercise, workout_list, dict(sorted(weekly_count.items())), vol_trend


HTML = r"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SelfTrain Dashboard</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@3.0.1/dist/chartjs-plugin-annotation.min.js"></script>
<style>
  *{box-sizing:border-box}
  body{font-family:system-ui,sans-serif;background:#121212;color:#e0e0e0;margin:0;padding:16px}
  h1{text-align:center;color:#4caf50;margin:0 0 8px 0}
  .tabs{display:flex;justify-content:center;gap:8px;margin-bottom:20px}
  .tab{
    padding:8px 24px;border:none;border-radius:6px 6px 0 0;cursor:pointer;
    font-size:16px;background:#1e1e1e;color:#888;border-bottom:2px solid transparent;
    transition:all .2s
  }
  .tab.active{color:#4caf50;border-bottom-color:#4caf50;background:#2a2a2a}
  .tab:hover{color:#ccc}
  .tab-content{display:none}
  .tab-content.active{display:block}
  select{display:block;margin:0 auto 16px;padding:8px 12px;font-size:16px;background:#1e1e1e;color:#e0e0e0;border:1px solid #333;border-radius:6px}
  .charts{display:grid;grid-template-columns:repeat(auto-fit,minmax(500px,1fr));gap:16px;max-width:1300px;margin:0 auto}
  .card{background:#1e1e1e;border-radius:8px;padding:16px}
  canvas{width:100%!important}
  .wide{grid-column:1/-1}
  .workout-table{max-width:1300px;margin:0 auto;border-collapse:collapse;width:100%}
  .workout-table th,.workout-table td{padding:10px 12px;text-align:left;border-bottom:1px solid #333}
  .workout-table th{color:#4caf50;font-weight:600}
  .workout-table tr:hover{background:#2a2a2a}
  .workout-table .ex-list{font-size:13px;color:#999;max-width:300px}
  .stat-row{display:flex;gap:16px;max-width:1300px;margin:0 auto 16px;flex-wrap:wrap}
  .stat-card{flex:1;min-width:200px;background:#1e1e1e;border-radius:8px;padding:12px 16px;text-align:center}
  .stat-card .val{font-size:28px;font-weight:700;color:#4caf50}
  .stat-card .lbl{font-size:13px;color:#888;margin-top:4px}
</style>
</head>
<body>
<h1>SelfTrain Dashboard</h1>
<div class="tabs">
  <button class="tab active" onclick="switchTab('exercises')">Ejercicios</button>
  <button class="tab" onclick="switchTab('workouts')">Entrenamientos</button>
</div>

<!-- EXERCISES TAB -->
<div id="tab-exercises" class="tab-content active">
  <select id="exerciseSelect"><option value="">-- Selecciona ejercicio --</option></select>
  <div class="charts">
    <div class="card"><canvas id="e1rmChart"></canvas></div>
    <div class="card"><canvas id="weightChart"></canvas></div>
    <div class="card" id="bilboRepsCard" style="display:none"><canvas id="bilboRepsChart"></canvas></div>
    <div class="card"><canvas id="volumeChart"></canvas></div>
  </div>
</div>

<!-- WORKOUTS TAB -->
<div id="tab-workouts" class="tab-content">
  <div class="stat-row" id="workoutStats"></div>
  <div class="charts">
    <div class="card"><canvas id="globalVolChart"></canvas></div>
    <div class="card"><canvas id="globalFreqChart"></canvas></div>
  </div>
  <table class="workout-table" id="workoutTable">
    <thead><tr><th>Fecha</th><th>Rutina</th><th>Duración</th><th>Ejercicios</th><th>Series</th><th>Volumen</th></tr></thead>
    <tbody></tbody>
  </table>
</div>

<script>
const exercises = EXERCISES_JSON;
const workoutList = WORKOUTS_JSON;
const freqData = FREQ_JSON;
const volTrend = VOLTREND_JSON;
const ctx = (id) => document.getElementById(id)?.getContext("2d");
let charts = {};

function destroyChart(id) { if (charts[id]) { charts[id].destroy(); delete charts[id]; } }

function baseChart(type) {
  return {
    type, data: {labels:[], datasets:[]},
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: "#e0e0e0" } } },
      scales: {
        x: { ticks: { color: "#999", maxTicksLimit: 20 } },
        y: { ticks: { color: "#999" }, beginAtZero: false }
      }
    }
  };
}

function lineChart(id, datasets) {
  destroyChart(id);
  const c = baseChart("line");
  c.data.datasets = datasets;
  charts[id] = new Chart(ctx(id), c);
}

function barChart(id, labels, data, color, label) {
  destroyChart(id);
  const c = baseChart("bar");
  c.data.labels = labels;
  c.data.datasets = [{ label, data, backgroundColor: color, borderRadius: 4 }];
  c.options.scales.y.beginAtZero = true;
  charts[id] = new Chart(ctx(id), c);
}

// ---- EXERCISES TAB ----

const sel = document.getElementById("exerciseSelect");
Object.entries(exercises).forEach(([id, ex]) => {
  const opt = document.createElement("option");
  opt.value = id; opt.textContent = ex.name + " (" + ex.muscle + ")";
  sel.appendChild(opt);
});

function loadExercise(eid) {
  const ex = exercises[eid];
  if (!ex) return;
  const dates = ex.dates;

  lineChart("e1rmChart", [
    { label: "1RM estimado (Epley)", data: ex.work_e1rm, borderColor: "#4caf50", tension: 0.2, pointRadius: 3, spanGaps: true }
  ]);
  charts.e1rmChart.data.labels = dates;
  charts.e1rmChart.update();

  lineChart("weightChart", [
    { label: "Peso max (kg)", data: ex.work_weight, borderColor: "#2196f3", tension: 0.2, pointRadius: 3, spanGaps: true }
  ]);
  charts.weightChart.data.labels = dates;
  charts.weightChart.update();

  // Bilbo dual-axis: reps (left y, orange) + weight (right y, blue)
  const bilboCard = document.getElementById("bilboRepsCard");
  if (ex.hasBilbo) {
    bilboCard.style.display = "";
    destroyChart("bilboRepsChart");
    charts.bilboRepsChart = new Chart(ctx("bilboRepsChart"), {
      type: "line",
      data: {
        labels: dates,
        datasets: [
          { label: "Reps Bilbo", data: ex.bilbo_reps, borderColor: "#ff9800", backgroundColor: "rgba(255,152,0,0.1)", fill: true, tension: 0.2, pointRadius: 3, spanGaps: true, yAxisID: "y" },
          { label: "Peso Bilbo (kg)", data: ex.bilbo_weight, borderColor: "#42a5f5", borderDash: [6,3], tension: 0.2, pointRadius: 2, spanGaps: true, yAxisID: "y1" }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: {
          legend: { labels: { color: "#e0e0e0" } },
          annotation: {
            annotations: {
              line50: {
                type: "line", yMin: 50, yMax: 50,
                borderColor: "rgba(255,152,0,0.5)", borderWidth: 2, borderDash: [8,4],
                label: { display: true, content: "50 reps → subir peso +10%", position: "start", backgroundColor: "rgba(255,152,0,0.2)", color: "#ffcc80", font: { size: 11 } }
              }
            }
          }
        },
        scales: {
          x: { ticks: { color: "#999", maxTicksLimit: 20 } },
          y: {
            type: "linear", position: "left", beginAtZero: true,
            title: { display: true, text: "Reps", color: "#ff9800" },
            ticks: { color: "#ff9800" }
          },
          y1: {
            type: "linear", position: "right", beginAtZero: false,
            title: { display: true, text: "kg", color: "#42a5f5" },
            ticks: { color: "#42a5f5" },
            grid: { drawOnChartArea: false }
          }
        }
      }
    });
  } else {
    bilboCard.style.display = "none";
  }

  lineChart("volumeChart", [
    { label: "Volumen (kg) — " + ex.name, data: ex.volume, borderColor: "#e91e63", backgroundColor: "rgba(233,30,99,0.08)", fill: true, tension: 0.2, pointRadius: 3 }
  ]);
  charts.volumeChart.data.labels = dates;
  charts.volumeChart.options.scales.y.beginAtZero = true;
  charts.volumeChart.update();
}

sel.addEventListener("change", () => { if (sel.value) loadExercise(sel.value); });
if (Object.keys(exercises).length > 0) { sel.value = Object.keys(exercises)[0]; loadExercise(sel.value); }

// ---- WORKOUTS TAB ----

function renderWorkouts() {
  // Stats
  const total = workoutList.length;
  const totalVol = workoutList.reduce((a,w) => a + w.volume, 0);
  const avgDur = total ? Math.round(workoutList.reduce((a,w) => a + w.duration, 0) / total) : 0;
  const nEx = new Set(workoutList.flatMap(w => w.exercises)).size;
  document.getElementById("workoutStats").innerHTML = [
    ["Entrenos", total], ["Volumen total", Math.round(totalVol) + " kg"],
    ["Duración media", avgDur + " min"], ["Ejercicios distintos", nEx]
  ].map(([l,v]) => '<div class="stat-card"><div class="val">'+v+'</div><div class="lbl">'+l+'</div></div>').join("");

  // Global volume chart
  barChart("globalVolChart", Object.keys(volTrend), Object.values(volTrend).map(v=>Math.round(v)), "#4caf50", "Volumen semanal (kg)");

  // Frequency chart
  barChart("globalFreqChart", Object.keys(freqData), Object.values(freqData), "#ff9800", "Entrenos por semana");

  // Table
  let rows = "";
  workoutList.forEach(w => {
    rows += `<tr>
      <td>${w.date} <span style="color:#666;font-size:12px">${w.weekday}</span></td>
      <td>${w.routine}</td>
      <td>${w.duration} min</td>
      <td class="ex-list">${w.exercises.join(", ")}</td>
      <td>${w.n_sets}</td>
      <td>${w.volume} kg</td>
    </tr>`;
  });
  document.querySelector("#workoutTable tbody").innerHTML = rows;
}

// ---- TABS ----

function switchTab(name) {
  document.querySelectorAll(".tab").forEach(t => t.classList.toggle("active", t.textContent.trim().toLowerCase().includes(name)));
  document.querySelectorAll(".tab-content").forEach(c => {
    const active = c.id === "tab-" + name;
    c.classList.toggle("active", active);
    if (active && name === "workouts") renderWorkouts();
  });
}
</script>
</body>
</html>"""


def make_html(per_exercise, workout_list, freq, vol_trend):
    return (HTML
        .replace("EXERCISES_JSON", json.dumps(per_exercise))
        .replace("WORKOUTS_JSON", json.dumps(workout_list))
        .replace("FREQ_JSON", json.dumps(freq))
        .replace("VOLTREND_JSON", json.dumps(vol_trend)))


class Handler(BaseHTTPRequestHandler):
    content = ""

    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(self.content.encode())

    def log_message(self, fmt, *args):
        pass


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "selftrain_backup.json"
    print(f"Loading {path}...")
    data = load_data(path)
    per_exercise, workout_list, freq, vol_trend = process(data)
    print(f"  {len(per_exercise)} exercises, {len(workout_list)} workouts, {len(freq)} weeks")

    Handler.content = make_html(per_exercise, workout_list, freq, vol_trend)

    server = HTTPServer((HOST, PORT), Handler)
    print(f"\nDashboard: http://{HOST}:{PORT}\nPress Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBye.")


if __name__ == "__main__":
    main()
