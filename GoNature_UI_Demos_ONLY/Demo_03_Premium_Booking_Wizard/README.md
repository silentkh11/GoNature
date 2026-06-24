# Demo 03 — Premium Booking Wizard

**Design concept:** The most usable, mistake-proof reservation flow. A single focused dark card,
a 5-step progress rail, generous spacing, and clear inline validation at every step.

**Target user:** Anyone making a reservation (traveler, or a Service Rep booking on a visitor's behalf).

**Included screens / steps** (the Back / Continue buttons actually navigate)
1. **Choose Park** — selectable list with live occupancy
2. **Date & Time** — date + time-slot grid with full/disabled slots struck through
3. **Group Size** — +/- stepper with capacity + rate validation
4. **Your Details** — contact form, with a note that the booking stays editable until confirmed
5. **Confirm** — full summary, total price, and final status explanation

**Why it fits GoNature:** encodes the real rules — capacity checks vs `Park.maxCapacity`,
subscriber vs guided-group rates, and the project's actual status rule that a new order is
`"Booked"` (editable) until the visitor confirms.

**Run**
```bat
cd Demo_03_Premium_Booking_Wizard
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_03_Premium_Booking_Wizard.java
```

**Notes:** Step rail and footer rebuild on each navigation to reflect progress. Mock data only.
