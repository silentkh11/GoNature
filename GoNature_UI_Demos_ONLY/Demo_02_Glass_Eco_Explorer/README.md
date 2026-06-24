# Demo 02 — Glass Eco Explorer

**Design concept:** Dark glassmorphism with soft nature gradients and glowing accent blobs.
Frosted translucent panels, premium green/cyan, emotional but still professional — a high-end
travel product feel.

**Target user:** Traveler / Subscriber booking visits.

**Included screens / views**
- Discovery hero with greeting, search bar, and subscriber messaging
- Featured-reserve cards with gradient "photo" headers, live availability bars, and price
- Quick-booking stepper (Date → Group → Details → Confirm) with capacity-OK validation
- Live price summary with subscriber discount applied
- Membership card (Guide tier, family size) + next-visit panel

**Why it fits GoNature:** uses real parks, the subscriber −10% discount logic, `familySize`,
the `isGuide` flag, capacity validation against `Park.maxCapacity`, and the booking flow.

**Run**
```bat
cd Demo_02_Glass_Eco_Explorer
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_02_Glass_Eco_Explorer.java
```

**Notes:** Self-contained, inline styling, mock data. Glass effect uses translucent fills + blur-like
soft shadows (JavaFX has no native backdrop-blur, so depth is faked with layered radial glows).
