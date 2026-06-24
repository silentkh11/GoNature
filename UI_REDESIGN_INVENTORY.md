# GoNature — UI Redesign Inventory
# Design: Glass Eco Explorer (Demo 02)
# Branch: uiux-glass-eco-explorer-redesign

| # | File | Screen | Role | FXML Update | CSS Driven | Status |
|---|------|--------|------|-------------|-----------|--------|
| 1 | `gui/auth/Login.fxml` | Login Portal | All | errorLabel color | ✅ full | ✅ Done |
| 2 | `gui/auth/Register.fxml` | Self-Registration | Guest | — | ✅ full | ✅ Done |
| 3 | `gui/guest/MainMenu.fxml` | Home / Welcome | Guest | ✅ full inline rewrite | partial | ✅ Done |
| 4 | `gui/guest/GuestPortal.fxml` | My Bookings + Profile | Subscriber | 4 label colors | ✅ full | ✅ Done |
| 5 | `gui/guest/CreateOrder.fxml` | Plan Your Visit / Booking | Guest/Sub | — | ✅ full | ✅ Done |
| 6 | `gui/management/ParkManagerDashboard.fxml` | PM Dashboard | ParkManager | ✅ full inline rewrite | ✅ full | ✅ Done |
| 7 | `gui/management/ParkManagerReports.fxml` | Statistical Reports | ParkManager | — | ✅ full | ✅ Done |
| 8 | `gui/management/DeptManagerDashboard.fxml` | Dept Admin Dashboard | DeptManager | — | ✅ full | ✅ Done |
| 9 | `gui/management/DeptManagerReports.fxml` | System Reports | DeptManager | — | ✅ full | ✅ Done |
| 10 | `gui/service/ServiceRepDashboard.fxml` | Subscriber Enrollment / Lookup | ServiceRep | — | ✅ full | ✅ Done |
| 11 | `gui/gate/ParkEntrance.fxml` | Gate Terminal / Check-In | GateWorker | — | ✅ full | ✅ Done |
| 12 | `gui/ServerPort.fxml` (Server) | Server Control Panel | Admin/Server | — | ✅ full | ✅ Done |

## CSS Files Changed
| File | Change |
|------|--------|
| `GoNatureClient/src/gui/assets/dark-theme.css` | Full rewrite — Glass Eco Explorer palette |
| `GoNatureClient/src/gui/assets/light-theme.css` | Full rewrite — Glass Eco day variant |
| `GoNatureServer/src/gui/dark-theme.css` | Full rewrite — Glass Eco server variant |
| `GoNatureServer/src/gui/light-theme.css` | Full rewrite — Glass Eco server light |

## Java Files Changed
| File | Change |
|------|--------|
| `GoNatureClient/src/gui/core/ThemeManager.java` | Default to dark mode (true) |
| `GoNatureServer/src/gui/ThemeManager.java` | Default to dark mode (true) |

## Functionality Preserved
- ✅ All FXML fx:id bindings preserved
- ✅ All onAction handlers preserved
- ✅ All fx:controller references unchanged
- ✅ All visible/managed toggle panels preserved
- ✅ All navigation logic unchanged
- ✅ No business logic touched
- ✅ No server/client communication changed
- ✅ No database queries changed
- ✅ No entity classes changed
