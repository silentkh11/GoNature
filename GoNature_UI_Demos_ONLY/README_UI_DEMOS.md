# GoNature — UI/UX Demo Collection (DEMO-ONLY)

> **10 standalone visual prototypes** exploring premium, dark, modern UI directions for the
> GoNature park-reservation system. These are **design demos only** — they do **not** touch,
> import, modify, or connect to the real project, its database, or its config.

---

## 1. What I scanned

I inspected the real GoNature project **read-only** before designing:

| Area | Finding |
|------|---------|
| Language / build | Java + **Eclipse** project (`bin/` output, `module-info.java` per module) |
| GUI framework | **JavaFX** (FXML + CSS), JavaFX 21 |
| Networking | OCSF client/server framework, TCP port 5555 |
| Client packages | `gui.auth`, `gui.guest`, `gui.management`, `gui.service`, `gui.gate`, `gui.core`, `entities`, `client` |
| Actors / roles | `ParkManager` (boss), `DeptManager` (admin), `ServiceRep`, `GateWorker`, `Subscriber`/Guest (traveler) |
| Entities | `Park`, `VisitOrder`, `Employee`, `Subscriber`, `Promotion`, `ParameterRequest`, `ReportData` |
| Real parks | Masada National Park, Caesarea National Park, Timna Valley Park, Nahal Ayun Nature Reserve |
| Order statuses | Booked → Pending Confirm → Confirmed → In Park → Completed; Cancelled; Waitlisted |
| Reports | Visit (S10), Cancellations (S11), Visitor Count (S12), Usage (S13) |
| Existing theme | Dark `#1e1e1e` base, `#2b2b2b` cards, `#0984e3` blue accent, Segoe UI, borderless window chrome |

All mock data in the demos uses this **real terminology** so the prototypes feel like GoNature, not generic filler.

---

## 2. Framework chosen & why

**JavaFX**, to match the real project exactly — so any direction you like can be ported back with
minimal friction.

Each demo is a **single self-contained `.java` file** with:
- **All styling inline** (programmatic CSS via `setStyle(...)`) — no external `.css`/`.fxml`,
  so there are **no broken references** and nothing to misconfigure.
- **Hardcoded mock data** — no DB, no network, no OCSF, fully offline and safe.
- **Internal navigation** between 4–6 screens inside one window.

No external libraries beyond the JavaFX that the project already uses.

---

## 3. How to run

You need a JDK (11+) and the **JavaFX SDK** (the same one your GoNature project already uses).

### Option A — Command line (single-file launch, JDK 11+)
Your project already bundles **JavaFX SDK 26.0.1** at
`GoNatureClient\javafx-sdk-26.0.1\lib`, so from inside any demo folder you can run:
```bat
cd GoNature_UI_Demos_ONLY\Demo_01_Dark_Nature_Command_Center
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_01_Dark_Nature_Command_Center.java
```
Repeat for any demo folder (swap the folder + file name). *(All 10 demos were
compile-verified against this exact SDK.)*

### Option B — Eclipse (recommended for you, since JavaFX is already set up)
1. Create a **new** Java project (e.g. `GoNatureDemos`) — keep it separate from GoNature.
2. Add your existing **JavaFX user library** to its build path.
3. In *Run Configurations → Arguments → VM arguments*, add:
   ```
   --module-path "C:\path\to\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
   ```
4. Drop any demo `.java` file into `src` (default package), right-click → **Run As → Java Application**.

> Each demo has its own `main()` — run whichever one you want, independently.

---

## 4. The 10 demos at a glance

| # | Demo | Concept | Best for |
|---|------|---------|----------|
| 01 | **Dark Nature Command Center** | Sidebar ops dashboard, KPIs, live occupancy, alerts | Admin/Boss operations |
| 02 | **Glass Eco Explorer** | Glassmorphism discovery + booking for travelers | Traveler experience |
| 03 | **Premium Booking Wizard** | Guided 5-step reservation flow with validation | Booking flow |
| 04 | **Map-First Park Operations** | Map canvas + details panel, capacity heat | Operations / logistics |
| 05 | **Clean Government Service** | Official accessible public-service portal | Institutional / accessibility |
| 06 | **Luxury Traveler App** | Cinematic featured parks, membership, timeline | Traveler / marketing |
| 07 | **Admin Analytics Studio** | Charts, filters, segmentation, export | Reports & analytics |
| 08 | **Role-Based Control Hub** | One screen, four role dashboards + permissions | Actor hierarchy demo |
| 09 | **Modern Kiosk Check-In** | Big-touch entrance/check-in terminal | Gate / entrance control |
| 10 | **Future Web App Desktop** | Web/SaaS-style navbar + drawer + settings | Future full redesign |

### Recommended mapping
- **Traveler experience:** Demo 06 (Luxury Traveler App), Demo 02 (Glass Eco Explorer)
- **Admin dashboard:** Demo 01 (Dark Nature Command Center)
- **Reports:** Demo 07 (Admin Analytics Studio)
- **Booking flow:** Demo 03 (Premium Booking Wizard)
- **Entrance / check-in:** Demo 09 (Modern Kiosk Check-In)
- **Future full redesign:** Demo 10 (Future Web App Desktop), Demo 08 (Role-Based Control Hub)

---

## 5. Files created

```
GoNature_UI_Demos_ONLY/
├─ README_UI_DEMOS.md                          (this file)
├─ Demo_01_Dark_Nature_Command_Center/  ├─ Demo_01_Dark_Nature_Command_Center.java  + README.md
├─ Demo_02_Glass_Eco_Explorer/          ├─ Demo_02_Glass_Eco_Explorer.java          + README.md
├─ Demo_03_Premium_Booking_Wizard/      ├─ Demo_03_Premium_Booking_Wizard.java      + README.md
├─ Demo_04_Map_First_Park_Operations/   ├─ Demo_04_Map_First_Park_Operations.java   + README.md
├─ Demo_05_Clean_Government_Service/     ├─ Demo_05_Clean_Government_Service.java     + README.md
├─ Demo_06_Luxury_Traveler_App/         ├─ Demo_06_Luxury_Traveler_App.java         + README.md
├─ Demo_07_Admin_Analytics_Studio/      ├─ Demo_07_Admin_Analytics_Studio.java      + README.md
├─ Demo_08_Role_Based_Control_Hub/      ├─ Demo_08_Role_Based_Control_Hub.java      + README.md
├─ Demo_09_Modern_Kiosk_CheckIn/        ├─ Demo_09_Modern_Kiosk_CheckIn.java        + README.md
└─ Demo_10_Future_Web_App_Desktop/      └─ Demo_10_Future_Web_App_Desktop.java      + README.md
```

---

## 6. Original project — untouched ✅

- ❌ No original `.java`, `.fxml`, `.css`, `.sql`, or config file was modified, renamed, or deleted.
- ❌ No demo imports or references original GoNature classes.
- ❌ No database connection (read or write). All data is hardcoded mock data.
- ✅ Everything lives **only** inside `GoNature_UI_Demos_ONLY/`.

You can delete this entire folder at any time and the GoNature project is exactly as it was.

---

## 7. Limitations

- Demos are **visual/UX prototypes**, not wired to logic — buttons navigate and toggle local state, but do not persist data.
- "Maps" (Demo 04) and "charts" use JavaFX shapes / built-in JavaFX charts — no external map/chart libraries.
- Images are represented with gradient panels + icons/emoji glyphs (no binary image assets shipped), to keep each demo a single portable file.
- Window default size targets **1366×768**; all demos are resizable and look good up to 1440×900+.
```