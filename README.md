# AgriDrone Farm Dashboard (skeleton)

JavaFX skeleton built from the locked scope in the team's `CLAUDE.md` / `SKILL.md`
(5 actors, UC-01..UC-10, screens S1..S10). Eclipse Java project, Java 25.

## Running

JavaFX comes from the JDK itself: **BellSoft Liberica JDK 25 Full**
(`C:\Users\roman\Java\jdk-25.0.5.0.3-full`). The separate Gluon JavaFX SDK is
not used, because Windows Smart App Control blocks its unsigned DLLs.

1. Window > Preferences > Java > Installed JREs > Add... > Standard VM, and pick the Liberica folder.
2. Installed JREs > Execution Environments > JavaSE-25 > tick the Liberica JDK.
3. Run `app.Launcher` (Run As > Java Application). No VM arguments needed.

## Layout

| Package   | What it holds |
| --------- | ------------- |
| `model`   | `Actor`, `UseCase`, `Screen` enums: the single source of truth for names, IDs, actors, relationships and the screen -> use case map. |
| `screens` | `BaseScreen` (header + use-case summary) and one class per screen with a TODO placeholder. |
| `app`     | `DroneDashboardApp` (top bar, role picker, sidebar), `Launcher`, `application.css`. |

## Traceability

| Screen | Class | Use cases | Shown to |
| ------ | ----- | --------- | -------- |
| S1 Home | `HomeScreen` | - | both roles |
| S2 Farm and Fields | `FarmFieldsScreen` | UC-01 | Farm Manager |
| S3 Flight Planning | `FlightPlanningScreen` | UC-02, UC-03 | Farm Manager |
| S4 Soil Moisture | `SoilMoistureScreen` | UC-04 | Farm Manager |
| S5 Crop Growth | `CropGrowthScreen` | UC-05 | Farm Manager |
| S6 Harvest Readiness | `HarvestReadinessScreen` | UC-06 | Farm Manager |
| S7 Equipment | `EquipmentScreen` | UC-07 | Farm Manager |
| S8 Alerts | `AlertsScreen` | UC-08 | Farm Manager |
| S9 Reports | `ReportsScreen` | UC-09 | Farm Manager |
| S10 Users and Roles | `UsersRolesScreen` | UC-10 | System Administrator |

## Rules (from CLAUDE.md)

- No extra use cases, roles, screens or platforms without scope approval.
- Names are identical everywhere: change them only in the `model` enums.
- IDs stay contiguous (UC-01..UC-10, S1..S10).
