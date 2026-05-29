# Assembly Table

Assembly Table is a NeoForge mod for Minecraft 1.21.1 that brings back a BuildCraft-inspired assembly workflow with modern FE-based automation.

The mod adds Assembly Table machines, configurable lasers, chipset items, and scripting support for custom recipes through both KubeJS and CraftTweaker.

## Machines

### Assembly Table

- Crafts recipes using stored FE
- Supports a recipe queue instead of a single fixed recipe
- Accepts energy from lasers
- Pushes crafted items into the inventory above the block when possible

### Powered Assembly Table

- Works like the normal Assembly Table
- Can receive FE directly from cables or other energy providers
- Does not require lasers to operate

### Lasers

- Search for valid targets in front of the block
- Require line of sight to transfer energy
- Support optional warmup behavior
- Can expose laser stats in the tooltip by holding `Shift`

## Mining and Drops

- Assembly Table and Laser blocks are mineable with a pickaxe
- They require an iron-tier tool to drop themselves

## How to Use

The mod supports both KubeJS and CraftTweaker for recipe creation.

### KubeJS

The custom recipe type is:

```text
assemblytable:assembly_table
```

Recipe format:

```json
{
  "type": "assemblytable:assembly_table",
  "result": { "item": "minecraft:diamond" },
  "ingredients": [
    { "ingredient": { "item": "minecraft:iron_ingot" }, "count": 2 },
    { "ingredient": { "item": "minecraft:redstone" }, "count": 1 }
  ],
  "energy": 1000
}
```

Example:

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'assemblytable:assembly_table',
    result: Item.of('minecraft:diamond'),
    ingredients: [
      { ingredient: { item: 'minecraft:iron_ingot' }, count: 2 },
      { ingredient: { item: 'minecraft:redstone' }, count: 1 }
    ],
    energy: 1000
  })
})
```

### CraftTweaker

Assembly Table recipes can be added through the global `bc_assemblyTable` recipe manager.

Example:

```ts
bc_assemblyTable.addRecipe(
    "my_recipe_new_ct",
    <item:mekanism:ultimate_universal_cable>,
    [
        <item:minecraft:iron_ingot> * 4,
        <item:minecraft:diamond>,
        <item:minecraft:netherite_scrap> * 6
    ],
    5000000
);
```

Method signature:

```ts
bc_assemblyTable.addRecipe(
    recipeId as String,
    outItem as IItemStack,
    ingredient as IIngredientWithAmount[],
    energyRequired as int
);
```

## Developer API

### Laser Configuration

Laser variants can be created through `LaserConfig.builder()`.

Supported builder options:

- `targetRange(int)` - search range in front of the laser
- `energyBuffer(int)` - internal FE buffer
- `maxTransferPerTick(int)` - maximum FE sent per tick
- `maxSpeedTransferPerTick(int)` - alias for max transfer, useful for warmup-based lasers
- `maxReceivePerTick(int)` - maximum FE the laser can receive per tick
- `beamColor(...)` - default beam color when warmup is disabled
- `warmupDisabled()` - keeps classic instant transfer behavior
- `warmup(int)` - enables warmup for the specified number of ticks
- `rampUpTicks(int)` - alias for `warmup(int)`
- `warmupColors(Vec4i low, Vec4i mid, Vec4i max)` - colors for low, medium, and max speed stages

Example:

```java
LaserConfig config = LaserConfig.builder()
        .targetRange(6)
        .energyBuffer(1000)
        .maxSpeedTransferPerTick(1000)
        .maxReceivePerTick(5000)
        .rampUpTicks(60)
        .warmupColors(
                new Vec4i(255, 40, 20, 200),
                new Vec4i(255, 220, 30, 200),
                new Vec4i(30, 100, 255, 200)
        )
        .build();
```

### Laser Targets

Block entities that want to receive power from lasers should implement `LaserTarget`.

Contract:

- `getRequiredLaserPower()` - returns how much FE the target currently wants
- `receiveLaserPower(int)` - accepts FE and returns leftover energy
- `isInvalidTarget()` - tells lasers whether the target should be ignored

## Compatibility

- JEI
- CraftTweaker
- KubeJS

## Requirements

- Minecraft 1.21.1
- NeoForge

## License

GNU LGPL 3.0
