# Demo 06 — Luxury Traveler App

**Design concept:** Cinematic, high-end tourism product. Full-bleed hero with gradient
"photography", gold + deep-charcoal palette, premium card shadows, generous type. Makes
GoNature feel like a luxury booking platform rather than a school project.

**Target user:** Traveler / Subscriber (marketing-grade customer experience).

**Included screens / views**
- Cinematic hero ("Masada at Sunrise") with primary CTA
- Featured-park cards with gradient imagery, ratings, and price
- Upcoming-journeys timeline with date chips and status dots
- Membership card (Gold Guide tier, family size, perks)
- **Confirmation modal** — click any "Reserve" button to open it; click the backdrop or
  "Not now" to dismiss

**Why it fits GoNature:** real parks, subscriber 10%-off perk, `isGuide`/`familySize`,
and the real statuses (Confirmed / Pending Confirm / Booked) on the journey timeline.

**Run**
```bat
cd Demo_06_Luxury_Traveler_App
java --module-path "..\..\GoNatureClient\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.graphics,javafx.base Demo_06_Luxury_Traveler_App.java
```

**Notes:** "Photography" is gradient panels (no shipped image assets). The modal is a real
interactive overlay. Mock data only.
