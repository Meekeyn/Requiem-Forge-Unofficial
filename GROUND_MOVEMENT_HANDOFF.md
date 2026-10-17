# Ground Player Body — validated movement, pending manual client check

Starting checkpoint: `690d921`; preserve FOV fix `c2bca99`.

The standard MoveControl path now transfers linear controller speed and scales player input like Mob.setSpeed. Vanilla travel retains acceleration, friction, normalization and stopping. Ground-body air acceleration is 0.02, including sprint. The sprint flag is mirrored before speed calculation. Villager uses vanilla SPEED_MODIFIER=0.5; Cat sprint uses its native controller mode 1.33. Other ordinary directed movement uses MOVE_TO at 1.0. Special controllers, jumping profiles, immovable bodies, protected Skeleton/Zombie/Blaze and riding retain their existing paths. Water/lava movement is not changed.

GroundMovementGameTests compares actual native aiStep/MoveControl/travel against Player Body, with goals and Brain behaviors disabled. The 9x8x256 structure reserves both tracks. Cases: walk, sprint, Speed I, Slowness I, additive modifier, large base attribute, cold start, diagonal, dynamic modifiers/sprint, stop/resume. 13 species x 10 cases x 80 ticks passed; maximum per-tick error 3.214e-8 blocks (tolerance 2e-6 absolute + 2e-6 relative).

| Species | Walk blocks/s | Sprint blocks/s |
|---|---:|---:|
| Spider, Enderman | 3.885464 | 6.566433 |
| Donkey | 1.322137 | 2.234411 |
| Cow | 1.726873 | 2.918415 |
| Cat | 3.885464 | 11.615363 |
| Pig, Iron Golem, Creeper, Endermite, Villager, Alex's Mobs Grizzly Bear | 2.698238 | 4.560022 |
| Sheep | 2.283789 | 3.859603 |
| Horse (test instance) | 2.185573 | 3.693618 |

Both bodies match at the displayed precision except insignificant final-digit float rounding. These are directed controller modes at 20 TPS, not the maximum of every AI goal or mounted speed.

The old Spider 5.4978/5.4566 discrepancy was not float noise: a cold start gives an air tick with input 0.98 on the old player vs 0.294 on the mob. The excess 0.01372 acceleration plus friction accounts for 0.0412204 blocks over 30 ticks. The old sprint mapping also gave Spider 5.0511 instead of 6.5664 blocks/s.

Enderman's test used the 1x1x1 empty template but placed the target outside it. An isolated arena now checks the exact raycast target, obstructed-destination rejection, exact arrival and proxy sync. The gameplay teleport code is unchanged. The original failing run did not capture the precise interfering block; a diagnostic old-layout rerun in a different placement successfully teleported, confirming placement dependence. Attack and climb regression fixtures also use isolated arenas instead of overlapping adjacent tests.

Validation: `gradlew.bat compileJava reobfJar runGameTestServer --offline --console=plain`, Forge 47.4.16. Compilation/reobfuscation completed. 261 GameTests: 253 passed, 8 failed. Overall Gradle status FAILED, server exit 8, expected from the known failures:

- vagrantvisualsclearwhenstatechanges
- possessedhealingrulesparse
- filledvesselreleasestypedsoulandreturnsemptyvessel
- possesseditemusefindscameracontrolledhost
- soulvesseldataroundtrips
- possessedoffhanditemuseuseshostoverride
- possessedeaterstoresseparatehungerdata
- possessedswimmerbodykeepshostwatervelocity — user explicitly deferred this test and all swimming work.

Do not claim all ground categories are calibrated: Rabbit, Slime, Magma Cube, Frog, Strider, Fox and other special controllers/profiles are not speed-validated; their WIP mapping remains. Of Alex's Mobs, only active ground movement of Grizzly Bear was measured.

STOP here for manual Minecraft testing: first five bodies, Villager and Grizzly; start/stop, diagonal, sprint, jump/landing, effects, release/repossess, Spider climbing, Enderman teleport, multiplayer corrections. Recheck protected Skeleton/Zombie/Blaze smoothness and FOV. GameTest does not prove client visual smoothness. Do not start swimming, flying, donkey camera, animation or hunger work without a new user request.

## Changed files in this stage

- `common/possession/PlayerBodyMovement.java` — new `usesGroundMoveControl`; ground branch in `prepareTravel` transfers `player.setSpeed(attr)` with the 0.98 input clamp, matching `Mob.setSpeed`.
- `common/possession/PossessionManager.java` — sprint flag mirrored to the host before the speed calculation; `beta16PlayerBodyMovementSpeed` uses the native controller speed for ground bodies (`Villager.SPEED_MODIFIER` 0.5, Cat sprint 1.33, otherwise 1.0) instead of the old squared-attribute shortcut.
- `mixin/PlayerMixin.java` — `getFlyingSpeed` returns 0.02 for a non-fluid ground body, including sprint.
- `gametest/GroundMovementGameTests.java` (new) — 13 species x 10 case parity harness against native aiStep/MoveControl/travel.
- `gametest/PossessionMovementGameTests.java`, `gametest/CoreDataGameTests.java` — updated sprint/attribute expectations; isolated arenas for attack, climb and Enderman teleport.
- `resources/data/requiemunofficial/structures/ground_movement.nbt`, `.../possession_arena.nbt` (new test structures).
- `GROUND_MOVEMENT_HANDOFF.md` (this file).

## Re-verification (no source changes)

Re-ran `gradlew.bat compileJava reobfJar runGameTestServer --offline` on the committed tree: 261 tests, 8 required failures = the same 7 baseline plus deferred `possessedswimmerbodykeepshostwatervelocity`. `endermanAbilityTeleportsPlayerBody` and the attack/climb fixtures passed. Ground parity re-measured at 130 cases (13 x 10) with maximum per-tick error 3.2139e-8 blocks, matching this report.
