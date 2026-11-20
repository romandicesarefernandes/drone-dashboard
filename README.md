# AgriDrone Farm Dashboard

JavaFX dashboard for Green Valley Farms. Eclipse Java project, Java 25.

## Pages

The sidebar has the screens from the team's wireframes:

| Page | Use case | State |
| ---- | -------- | ----- |
| Dashboard | Overview (UC-04, UC-05, UC-07, UC-08) | Blank |
| Fields | UC-01: farm layout setup, visualization, drone flights | Implemented (see below) |
| Missions | UC-02, UC-03 | Blank |
| Soil Moisture | UC-04 Monitor Soil Moisture | Demo |
| Crop Health | UC-05 Monitor Crop Growth | Demo |
| Harvest | UC-06 Determine Harvest Readiness | Demo |
| Equipment | UC-07 Track Equipment Location and Inventory | Implemented |
| Alerts | UC-08 | Blank |
| Reports | UC-09 Generate Farm Reports | Implemented |
| Users & Settings | UC-10 Manage Users and Roles | Implemented |

**Soil Moisture (demo):** pick a field and scan date for a heat map of 96
zones (hover or click a zone for its value), a summary, the 30-day trend and
the zones below the low threshold. **Save Thresholds** changes the field's
low / high limits, **Request Scan** schedules a soil-moisture mission and
**Export** saves the zone readings as CSV. Opens on South Field, 22 Oct (dry spell).

**Crop Health (demo):** pick a field and flight for an NDVI map, the field's
growth stage, actual vs expected NDVI, and growth by zone (NW, NE, SW, SE).
Zones more than 15% below expected are flagged. **Request Scan** schedules a
crop-growth mission. Opens on East Orchard, whose NE corner lags behind.

**Harvest (demo):** readiness by field from the latest crop flight, with the
recommended harvest window, estimated yield and data age (older than 7 days is
flagged). Select a field in the plan, then **Mark Ready to Harvest** (fields
at 60% or more) or **Schedule Harvest Scan**.

Zone values on these three pages come from each field's single sample reading
plus a fixed pattern per field, since the sample data has one value per scan.

**Reports:** pick report type (Farm Summary, Soil Moisture, Crop Growth,
Harvest Readiness, Equipment Usage, Drone Missions), field and period, then
**Generate Report** for a preview with summary, chart and table. **Export PDF** /
**Export CSV** save the report; **Schedule Report** sets a weekly or monthly
schedule. Every generated report is kept in **Report History** and can be reopened.

**Equipment:** farm map with each item's last known location (Active, Idle,
Missing shown as X, orange ring = outside its geofence), counts by type,
search and Type/Status/Field filters, and the selected item's details with its
location history. **Add Equipment** registers an item and its GPS tag (unique
name and tag). Tags silent for 24 hours are marked Missing; **Locate with
Drone** (Missing items only) flies a drone from the base pad to the last known
spot, reads the tag and logs an equipment-scan mission. Items outside their
geofence raise a geofence alert.

**Users & Settings:** list of users with role, status and last login.
**Invite User** (name, email, phone, role, field access; duplicate emails are
rejected) adds a Pending user and sends an invitation; **Edit Role** changes
role and field access; **Deactivate** blocks login but keeps the account. The
last active System Administrator cannot be demoted or deactivated.

Sample data comes from the scenario documents (Green Valley Farms, its four
fields, Alex Morgan and Dana Whitfield, drone data from Sep to mid-Nov 2026) and
lives in memory: changes are lost when the app closes. Email notifications are
simulated (printed to the console).

## Fields page: farm setup

## Use cases in this part

| Use case | Actors | Where in the dashboard |
| -------- | ------ | ---------------------- |
| Manage farm items | Farmer | Tree + Details + Commands panel (add, delete, change name/price/location/size) |
| Visit item | Drone, Farmer | Select an object, click **Visit Item/Container** |
| Scan farm | Drone, Farmer | Click **Scan Farm** |
| Create farm layout | Farmer | Adding containers and items builds the layout; the visualization draws it |

## Design patterns

- **Composite**: `farm.FarmComponent` is the common interface. `farm.Item` is
  the leaf and `farm.ItemContainer` the composite (it holds a list of `FarmComponent`).
  Total price, drawing and listing all recurse through the tree.
- **Singleton**: `app.Dashboard` has a private constructor and `getInstance()`.
  Calling `show()` again only brings the existing window to the front.
- **Commands** (`farm.commands`): `AddCommand`, `DeleteCommand` and
  `ChangePropertyCommand`. Each object lists its own commands (`getCommands()`):
  - Item: Delete item, Change name, price, location-x, location-y, length, width, height
  - Item-container: Add item-container, Add item, Delete item-container, plus the same Change commands
    (the root "Farm" cannot be deleted)

## Coordinates

Screen coordinates, 1 unit = 1 pixel: the origin (0, 0) is the top-left of the
farm, x grows to the right and y grows **down**. location-x / location-y are
the top-left corner. **length** is the horizontal size and **width** the
vertical size on the plot. height is stored but not drawn (2D plot).

## Drone

The drone is the first item named `Drone`. Following the description, put it
in a `Command Center` item-container on the root. Every flight starts and ends
at the drone item's center. Without a drone, the flight buttons explain what to add.

## Testing scenario

Click **Load Test Scenario** to build the Green Valley Farms farm items.
Equipment is not part of it: tractors, sprayers and so on are managed on the
Equipment page.

- The four fields (North 40, East Orchard, South Field, River Plot) are
  item-containers at the same places as on the Equipment map (scaled by 0.8),
  each holding its crop: 200 Corn, 60 Apple Trees, 300 Wheat, 150 Soybeans.
- Barn > Livestock Area > 5 Cows, 3 Chickens; Barn > Milk Storage.
- Storage Building > 40 Feed Bags, 25 Seed Bags.
- Command Center > Drone, in the farmyard below South Field.

Items have no quantity variable, so a group (e.g. 5 Cows) is one item with
the count in its name and the price of the whole group.

The project description's own scenario (barn > livestock area > cow, milk
storage; storage building > tractor, tiller; command center > drone; soy crop)
can still be built by hand: select `Farm`, pick **Add item-container**, and so on.

## Running

JavaFX comes from the JDK itself: **BellSoft Liberica JDK 25 Full**
(`C:\Users\roman\Java\jdk-25.0.5.0.3-full`). The separate Gluon JavaFX SDK is
not used, because Windows Smart App Control blocks its unsigned DLLs.

1. Window > Preferences > Java > Installed JREs > Add... > Standard VM, and pick the Liberica folder.
2. Installed JREs > Execution Environments > JavaSE-25 > tick the Liberica JDK.
3. Run `app.Launcher` (Run As > Java Application). No VM arguments needed.

## Packages

| Package | Contents |
| ------- | -------- |
| `app` | `Launcher`, `FarmApplication` (JavaFX entry), `Dashboard` (singleton window with sidebar), `application.css` |
| `boundary` | Pages (`FarmSetupUI`, `SoilMoistureUI`, `CropHealthUI`, `HarvestReadinessUI`, `EquipmentTrackerUI`, `ReportsUI`, `UserManagementUI`, `PlaceholderUI`), `NotificationInterface`, `EquipmentGPSTagInterface` and their simulated services |
| `control` | `EquipmentController`, `ReportController`, `UserManagementController`, `SoilMoistureController`, `CropGrowthController`, `HarvestReadinessController`, `ReportExporter` (+ `SimplePdf`) |
| `entity` | `Farm`, `Field`, `FieldBoundary`, `User`, `Report`, `SoilMoistureReading`, `CropGrowthRecord`, `Alert`, `Equipment`, `Mission`, `ReportSchedule`, `DataStore`, `SampleData` |
| `farm` | `FarmComponent`, `AbstractFarmComponent`, `Item`, `ItemContainer`, `Property`, `SampleFarm` |
| `farm.commands` | `Command`, `AddCommand`, `DeleteCommand`, `ChangePropertyCommand` |
| `ui` | `FarmView` (800 x 600 visualization + drone animation), `AddDialog` |

`boundary` / `control` / `entity` follow the team's UML class diagram.
