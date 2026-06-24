# Demo 09 — Modern Kiosk Check-In

**Design concept:** A large, high-contrast, touch-friendly entrance terminal. Big targets, minimal
reading, fast flow — built for a busy park gate or a self-service kiosk.

**Target user:** Gate Worker (entrance control) or self-service visitors.

**Included screens / views**
- Park header with live capacity badge and clock
- **Working numeric keypad** — tap digits / ⌫ to edit the order code (QR resets to a sample)
- Reservation preview (visitor, party size, slot, type)
- **Capacity warning** before admitting when the park is filling up
- Large "Check in" action → **success overlay** ("Welcome to Masada!")
- Today's-arrivals rail with live statuses

**Why it fits GoNature:** this is the GateWorker `ParkEntrance` flow — look up a `VisitOrder` by id,
verify it's `Confirmed`, check capacity against `Park.maxCapacity`, and admit. Walk-in/QR included.

**Run**
```bat
cd Demo_09_Modern_Kiosk_CheckIn
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_09_Modern_Kiosk_CheckIn.java
```

**Notes:** Keypad and check-in are interactive. For a true kiosk, run the window maximised /
fullscreen. Mock data only.
