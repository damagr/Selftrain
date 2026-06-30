#!/usr/bin/env python3
"""SelfTrain Dashboard — local web charts from backup JSON. Zero deps, stdlib only.
Usage: python dashboard.py [selftrain_backup.json]
"""

import json
import sys
import math
from datetime import datetime, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler

HOST, PORT = "localhost", 8080
EPLEY = lambda w, r: w * (1 + r / 30)


def load_data(path):
    with open(path) as f:
        return json.load(f)


def process(data):
    exercises = {e["id"]: e for e in data["exercises"] if not e.get("isDeleted")}
    workouts = {w["id"]: w for w in data["workouts"] if w.get("completed")}
    sets = data["workoutSets"]

    # Build: exercise_id -> [{date, e1rm, weight, volume}]
    history = {}
    for s in sets:
        eid = s["exerciseId"]
        wid = s["workoutId"]
        w = workouts.get(wid)
        if not w:
            continue
        date = datetime.fromtimestamp(w["date"] / 1000).strftime("%Y-%m-%d")
        e1rm = EPLEY(s["weightKg"], s["reps"])
        vol = s["reps"] * s["weightKg"]
        history.setdefault(eid, []).append(
            {"date": date, "e1rm": round(e1rm, 1), "weight": s["weightKg"], "volume": vol, "reps": s["reps"]}
        )

    # Aggregate max per session (since multiple sets per session)
    per_exercise = {}
    for eid, rows in history.items():
        ex = exercises[eid]
        sessions = {}
        for r in rows:
            d = r["date"]
            if d not in sessions:
                sessions[d] = {"e1rm": 0, "weight": 0, "volume": 0}
            sessions[d]["e1rm"] = max(sessions[d]["e1rm"], r["e1rm"])
            sessions[d]["weight"] = max(sessions[d]["weight"], r["weight"])
            sessions[d]["volume"] += r["volume"]
        sorted_sessions = sorted(sessions.items())
        per_exercise[eid] = {
            "name": ex["name"],
            "muscle": ex.get("muscleGroup", ""),
            "dates": [d for d, _ in sorted_sessions],
            "e1rm": [v["e1rm"] for _, v in sorted_sessions],
            "weight": [v["weight"] for _, v in sorted_sessions],
            "volume": [v["volume"] for _, v in sorted_sessions],
        }

    # Workout frequency per week
    week_counts = {}
    for w in workouts.values():
        dt = datetime.fromtimestamp(w["date"] / 1000)
        wk = dt.strftime("%Y-W%W")
        week_counts[wk] = week_counts.get(wk, 0) + 1
    freq = dict(sorted(week_counts.items()))

    return per_exercise, freq


HTML = """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SelfTrain Dashboard</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
  body{font-family:system-ui,sans-serif;background:#121212;color:#e0e0e0;margin:0;padding:16px}
  h1{text-align:center;color:#4caf50}
  select{display:block;margin:0 auto 16px;padding:8px 12px;font-size:16px;background:#1e1e1e;color:#e0e0e0;border:1px solid #333;border-radius:6px}
  .charts{display:grid;grid-template-columns:repeat(auto-fit,minmax(500px,1fr));gap:16px;max-width:1200px;margin:0 auto}
  .card{background:#1e1e1e;border-radius:8px;padding:16px}
  canvas{width:100%!important}
  .freq-grid{max-width:1200px;margin:16px auto}
</style>
</head>
<body>
<h1>SelfTrain Dashboard</h1>
<select id="exerciseSelect"><option value="">-- Selecciona ejercicio --</option></select>

<div class="freq-grid"><div class="card"><canvas id="freqChart"></canvas></div></div>
<div class="charts">
  <div class="card"><canvas id="e1rmChart"></canvas></div>
  <div class="card"><canvas id="weightChart"></canvas></div>
  <div class="card"><canvas id="volumeChart"></canvas></div>
</div>

<script>
const exercises = EXERCISES_JSON;
const freq = FREQ_JSON;

const sel = document.getElementById("exerciseSelect");
Object.entries(exercises).forEach(([id, ex]) => {
  const opt = document.createElement("option");
  opt.value = id;
  opt.textContent = ex.name;
  sel.appendChild(opt);
});

const ctx = (id) => document.getElementById(id).getContext("2d");
let charts = {};

function makeChart(id, label, color) {
  if (charts[id]) charts[id].destroy();
  charts[id] = new Chart(ctx(id), {
    type: "line",
    data: { labels: [], datasets: [{ label, data: [], borderColor: color, tension: 0.2, pointRadius: 3 }] },
    options: {
      responsive: true,
      plugins: { legend: { labels: { color: "#e0e0e0" } } },
      scales: {
        x: { ticks: { color: "#999" } },
        y: { ticks: { color: "#999" }, beginAtZero: false }
      }
    }
  });
}

// Frequency chart (always visible)
makeChart("freqChart", "Entrenos por semana", "#ff9800");
charts.freqChart.data.labels = Object.keys(freq);
charts.freqChart.data.datasets[0].data = Object.values(freq);
charts.freqChart.options.scales.y.beginAtZero = true;
charts.freqChart.update();

function loadExercise(eid) {
  const ex = exercises[eid];
  if (!ex) return;
  makeChart("e1rmChart", "1RM estimado (Epley) — " + ex.name, "#4caf50");
  charts.e1rmChart.data.labels = ex.dates;
  charts.e1rmChart.data.datasets[0].data = ex.e1rm;
  charts.e1rmChart.update();

  makeChart("weightChart", "Peso max (kg) — " + ex.name, "#2196f3");
  charts.weightChart.data.labels = ex.dates;
  charts.weightChart.data.datasets[0].data = ex.weight;
  charts.weightChart.update();

  makeChart("volumeChart", "Volumen (kg) — " + ex.name, "#e91e63");
  charts.volumeChart.data.labels = ex.dates;
  charts.volumeChart.data.datasets[0].data = ex.volume;
  charts.volumeChart.options.scales.y.beginAtZero = true;
  charts.volumeChart.update();
}

sel.addEventListener("change", () => loadExercise(sel.value));
if (Object.keys(exercises).length > 0) {
  sel.value = Object.keys(exercises)[0];
  loadExercise(sel.value);
}
</script>
</body>
</html>"""


def make_html(per_exercise, freq):
    ex_json = json.dumps(per_exercise)
    freq_json = json.dumps(freq)
    return HTML.replace("EXERCISES_JSON", ex_json).replace("FREQ_JSON", freq_json)


class Handler(BaseHTTPRequestHandler):
    content = ""

    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(self.content.encode())

    def log_message(self, fmt, *args):
        pass  # silent


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "selftrain_backup.json"
    print(f"Loading {path}...")
    data = load_data(path)
    per_exercise, freq = process(data)
    print(f"  {len(per_exercise)} exercises, {len(freq)} weeks of data")

    Handler.content = make_html(per_exercise, freq)

    server = HTTPServer((HOST, PORT), Handler)
    print(f"\nDashboard: http://{HOST}:{PORT}\nPress Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBye.")


if __name__ == "__main__":
    main()
