# Demo 01 — Dark Nature Command Center

**Design concept:** A serious operational command center for managing national parks.
Charcoal/graphite base, premium nature-green + cyan accents, soft shadows, strong card hierarchy,
persistent left sidebar with a live server-status pill.

**Target user:** Park Manager (boss) & Department Manager (admin).

**Included screens / views**
- Operations dashboard (KPI row: visitors, reservations, occupancy, pending approvals)
- Live park-occupancy panel with capacity progress bars (green/amber/red by load)
- Alerts & notifications panel (capacity, promotions, parameter requests, report submissions)
- Upcoming-reservations table with hover rows and status chips
- Sidebar navigation + live OCSF server status indicator

**Why it fits GoNature:** mirrors the real actors and data — `Park.maxCapacity` vs `currentVisitors`,
`VisitOrder` statuses (Booked / Pending Confirm / Confirmed / Waitlisted), pending `ParameterRequest`
and `Promotion` approvals, and the client-server connection state.

**Run**
```bat
cd Demo_01_Dark_Nature_Command_Center
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_01_Dark_Nature_Command_Center.java
```
(Adjust the `--module-path` if your JavaFX SDK lives elsewhere.)

**Notes:** Self-contained single file, all styling inline, mock data only. Every sidebar item routes to
the dashboard in this prototype; in production each would load its own view.
