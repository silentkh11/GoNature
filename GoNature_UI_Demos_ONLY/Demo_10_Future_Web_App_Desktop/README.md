# Demo 10 — Future Web App Desktop

**Design concept:** What GoNature could look like as a modern web / SaaS product, recreated in
JavaFX. Top navbar with page routing, responsive-style card grids, a global search, and a
right-hand booking drawer — startup-quality and scalable.

**Target user:** Forward-looking direction for a future full redesign (staff + power users).

**Included screens / views** (navbar items route between pages)
- **Dashboard** — KPI cards + a responsive park grid with occupancy bars
- **Bookings** — filter chips + a clean reservations table with status chips
- **Reports** — cards for all four GoNature report types (Generate / Download)
- **Settings** — profile + preference toggles (dark mode, notifications…)
- **Booking drawer** — click "+ New booking" to slide in a side panel; click the backdrop or ✕ to close

**Why it fits GoNature:** the four report cards, real statuses, subscriber pricing, and per-park
occupancy all map to the existing system — just reorganised into a scalable web-style IA.

**Run**
```bat
cd Demo_10_Future_Web_App_Desktop
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_10_Future_Web_App_Desktop.java
```

**Notes:** Page routing and the drawer are fully interactive. `FlowPane` grids reflow when you
resize the window (the "responsive-like" behaviour). Mock data only.
