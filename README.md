# Drone Dashboard - Farm Setup

JavaFX dashboard to set up items on a farm, visualize them on an 800 x 600
plot, and animate a drone visiting an item or scanning the whole farm.
Eclipse Java project, Java 25.

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

Click **Load Test Scenario** to build the scenario from the description (barn
with livestock area, cow and milk storage; storage building with tractor and
tiller; command center with drone; soy crop). You can also build it by hand:
select `Farm`, pick **Add item-container**, and so on.

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
| `app` | `Launcher`, `FarmApplication` (JavaFX entry), `Dashboard` (singleton window), `application.css` |
| `farm` | `FarmComponent`, `AbstractFarmComponent`, `Item`, `ItemContainer`, `Property`, `SampleFarm` |
| `farm.commands` | `Command`, `AddCommand`, `DeleteCommand`, `ChangePropertyCommand` |
| `ui` | `FarmView` (800 x 600 visualization + drone animation), `AddDialog` |
