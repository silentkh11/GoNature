# Demo 08 — Role-Based Control Hub

**Design concept:** One screen that demonstrates the entire GoNature permission hierarchy. A role
switcher at the top swaps the whole workspace, and every role view clearly separates **allowed**
actions from **restricted** ones (locked, struck-through, red "Restricted" tags).

**Target user:** Stakeholders evaluating the actor model + anyone who needs to see permissions clearly.

**Included screens / views** (click the role tabs to switch)
- **Boss** (Park Manager): parameters, promotions, reports — can't approve other parks
- **Admin** (Department Manager): approval queue, all reports, force-disconnect — can't edit daily bookings
- **Employee** (Service Rep / Gate): register/lookup subscribers, gate admit, walk-ins — can't change capacity or approve promos
- **Traveler** (Subscriber): book, edit while "Booked", update profile, join waitlist — can't see others' bookings or staff reports

**Why it fits GoNature:** mirrors the exact real roles and the real server-enforced rules — e.g.
only Park Manager edits capacity, only Dept Manager approves, a booking is editable only while
its status is `"Booked"`.

**Run**
```bat
cd Demo_08_Role_Based_Control_Hub
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_08_Role_Based_Control_Hub.java
```

**Notes:** Role tabs are fully interactive. Mock data only.
