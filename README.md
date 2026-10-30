# BedWars1058-GeneratorFix

Small addon for [BedWars1058](https://github.com/andrei1058/BedWars1058) that stops the generator base from
disappearing at sudden death, which leaves a hole for resources to fall through.

## Cause
In BedWars1058's `GamePlayingTask` (the `ENDER_DRAGON` event) every block from the team generator down 20 blocks is
set to `AIR` right before the dragons spawn. It is not the dragon destroying anything.

## How it works
`Arena.setNextEvent` fires `NextEventChangeEvent` (old event `ENDER_DRAGON`) in the same tick, just before that
removal. The plugin snapshots those columns at that moment and puts them back one tick later. Nothing else is
touched, so blocks players break or place are never reverted.

BedWars is accessed by reflection (no compile-time dependency, since the old Maven repo is gone), so one jar covers
Minecraft 1.8 → 26.x and also works with BedWars2023. Only the Bukkit API is used (no NMS).

## Build
```
gradle wrapper --gradle-version 9.1.0   # first time only
./gradlew shadowJar
```
Install `build/libs/BedWars1058-GeneratorFix-<version>.jar` (not the `-plain` one). Java 8 bytecode, Kotlin 2.2.21 shaded
and relocated. Needs a JDK 21 toolchain to build.

## Config
`depth` (default 20, must be ≥ BedWars' 20), `restore-delay-ticks` (default 1), `debug`.
Set `debug: true` once to see how many blocks are saved/restored at sudden death.

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.