# Demo 05 — Clean Government Service

**Design concept:** The deliberate light/accessible contrast option. A clean, official
public-service portal: high contrast, generous labels, simple aligned forms, navy + green
institutional palette. Reliable and serious rather than flashy.

**Target user:** General public + staff — accessibility-first, "easy for everyone".

**Included screens / views**
- Government-style banner (emblem, language, accessibility, sign-in)
- Service navigation bar
- Hero with primary/secondary calls to action
- Four service tiles (Book, Membership, Track, Reports)
- Booking form with required-field markers + capacity/price confirmation note
- Reservation status tracker (Submitted → Booked → Pending Confirm → Confirmed → Complete)
- Report download list (Visit / Cancellations / Usage)

**Why it fits GoNature:** reflects the real GoNature status pipeline and the four real report
types (S10 Visit, S11 Cancellations, S13 Usage), plus subscriber-rate pricing.

**Run**
```bat
cd Demo_05_Clean_Government_Service
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_05_Clean_Government_Service.java
```

**Notes:** This is the one intentionally *light* demo — included so you can compare a clean
institutional direction against the dark premium ones. Mock data only.
