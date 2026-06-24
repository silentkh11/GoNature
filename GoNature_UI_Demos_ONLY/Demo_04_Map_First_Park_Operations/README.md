# Demo 04 — Map-First Park Operations

**Design concept:** A control-room / logistics view built around a stylised map. Dark canvas,
faint grid, a landmass silhouette, and clickable park markers colour-coded by capacity heat
(green → amber → red). A right-hand details panel reacts to the selected marker.

**Target user:** Operations / Department Manager monitoring all parks at once.

**Included screens / views**
- Map canvas with 4 real parks positioned geographically (Galilee → Eilat)
- **Interactive markers** — click any marker to load that park into the panel
- Live occupancy gauge with capacity bar + state chip
- Entrance / check-in status (gates, walk-in acceptance) that adapts to load
- Next-arrivals list with statuses

**Why it fits GoNature:** visualises `Park.currentVisitors` vs `maxCapacity` across all parks,
plus the gate/entrance and walk-in logic the GateWorker role handles. Walk-ins auto-pause as a
park fills — mirroring real capacity rules.

**Run**
```bat
cd Demo_04_Map_First_Park_Operations
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_04_Map_First_Park_Operations.java
```

**Notes:** The "map" is drawn with JavaFX shapes (no external map library / tiles). Marker positions
are illustrative. Mock data only.
