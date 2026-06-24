# Demo 07 — Admin Analytics Studio

**Design concept:** A dark, SaaS-grade analytics workspace (Power-BI energy) for managers who
make decisions from data. Filter bar, KPI cards, **real JavaFX charts**, export actions, and a
clean per-park data table.

**Target user:** Department Manager / Park Manager reviewing reports.

**Included screens / views**
- Filter bar (period / park / visitor type / granularity) + Apply
- KPI cards (visitors, occupancy, cancellation rate, waitlist conversions)
- **Bar chart** — daily visitor trend
- **Pie chart** — visitor segmentation by order type (Subscriber / Solo-Family / Guided Group / Walk-in)
- Export row (PDF / CSV / Print)
- Per-park breakdown table with status chips

**Why it fits GoNature:** maps directly onto the four real reports (Visit, Cancellations,
Visitor Count, Usage) and the real order types and pricing tiers.

**Run**
```bat
cd Demo_07_Admin_Analytics_Studio
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_07_Admin_Analytics_Studio.java
```

**Notes:** Charts are genuine `javafx.scene.chart` controls, dark-themed via a **base64 data-URI
stylesheet built in code** — so there is still no external `.css` file to misplace. Mock data only.
