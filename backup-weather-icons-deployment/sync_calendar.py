import subprocess, json, datetime, os

def get_events(cal_id=None):
    cmd = ["gcalendar", "readonly", "events", "--max", "40", "--json"]
    if cal_id:
        cmd.extend(["--cal", cal_id])
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", shell=True)
    if res.returncode == 0:
        return json.loads(res.stdout)
    return []

primary = get_events()
holidays = get_events("en.indian#holiday@group.v.calendar.google.com")

all_events = []
today = datetime.date.today()

for e in primary:
    summary = e.get("summary", "")
    start = e.get("start", {}).get("date") or e.get("start", {}).get("dateTime", "")[:10]
    event_type = e.get("eventType", "default")
    if start:
        is_bday = event_type == "birthday" or "birthday" in summary.lower()
        is_anni = "anniversary" in summary.lower()
        t = "birthday" if is_bday else ("anniversary" if is_anni else "event")
        all_events.append({
            "title": summary,
            "date": start,
            "type": t
        })

for e in holidays:
    summary = e.get("summary", "")
    start = e.get("start", {}).get("date") or e.get("start", {}).get("dateTime", "")[:10]
    if start:
        all_events.append({
            "title": summary,
            "date": start,
            "type": "festival"
        })

future = []
for ev in all_events:
    try:
        ev_date = datetime.date.fromisoformat(ev["date"])
        if ev_date >= today:
            diff = (ev_date - today).days
            ev["days_away"] = diff
            future.append(ev)
    except:
        pass

future.sort(key=lambda x: (x["days_away"], x["title"]))

out_path = r"C:\Users\bhask\.gemini\antigravity\scratch\cleanyourtv\launcher\app\src\main\assets\calendar_events.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(future[:25], f, indent=2)

print(f"Saved {len(future[:25])} events to {out_path}")
