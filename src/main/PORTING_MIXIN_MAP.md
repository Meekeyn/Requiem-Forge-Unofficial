# Requiem 2.0.0-beta.16 mixin functional map

Reference original: `requiem-2.0.0-beta.16`.
Forge runtime port: `java`, `resources`.

This is a working map for porting behavior, not Fabric architecture. Existing Forge systems must be extended, not removed, when they already cover the original feature.

Legend: `IMPLEMENTED`, `PARTIAL`, `MISSING`, `NOT_NEEDED_ON_FORGE`.

## Current Forge equivalents

| Forge area | Existing implementation |
| --- | --- |
| Player/entity persistent soul data | `common/soul/PlayerSoulProvider`, `PlayerSoul`, `SoulManager`, `EntitySoulManager` capability/persistent NBT |
| Possession lifecycle | `common/possession/PossessionManager`, `PossessionInputPacket`, `PossessionActionPacket`, `SyncSoulDataPacket`, `PossessionFxPacket` |
| Possessed item handling | `common/possession/PossessedItemManager` data reload listener and Forge right-click event hooks |
| Possessed inventory restrictions | `PossessionInventoryManager`, `SlotMixin`, `InventoryScreenMixin` |
| Remnant/vagrant behavior | `VagrantRules`, `VagrantEvents`, `EntityMixin`, `LivingEntityMixin`, `WeightedPressurePlateBlockMixin`, `BlockStateBaseMixin` |
| Client possession/remnant render hints | `ClientPossessionVision`, `ClientPossessionInput`, `ClientPossessionRenderHelper`, HUD/client soul handlers and render mixins |
| First-death world freeze FX | `ClientDeathSuspensionFx`, vanilla `PostChain`, `PostChainAccessor`, `assets/requiemunofficial/shaders/post/za_warudo.json`, `za_warudo`/`fish_eye` shader programs |
| Opus lectern behavior | `OpusLecternRules`, `LecternBlockEntityMixin` |

## First-death ZaWorld FX parity

| Original element | Forge port | Status | Notes |
| --- | --- | --- | --- |
| `client.ZaWorldFx` Satin post chain | `ClientDeathSuspensionFx` uses vanilla `PostChain` on `RenderLevelStageEvent.Stage.AFTER_LEVEL` | `IMPLEMENTED` | Replaces the previous GUI overlay approximation. The two-pass chain renders `za_warudo` into `swap`, then `fish_eye` back to `minecraft:main`. |
| `ReadableDepthFramebuffer#getStillDepthMap()` for `DepthSampler` | Post-chain aux target `minecraft:main:depth` | `IMPLEMENTED` | The shader samples the main framebuffer depth texture directly as `DepthSampler`; no separate Satin framebuffer helper is used. |
| `GlMatrices.getInverseTransformMatrix(projectionMatrix)` | `inverse(projection * cameraViewRotation)` from Forge render event projection and active camera rotation | `IMPLEMENTED/PARTIAL` | Matches the original depth-to-world reconstruction contract: shader transforms clip coordinates back to camera/world-relative coordinates, then adds `CameraPosition`. Manual visual validation should confirm exact wave alignment against nearby block edges. |
| `Center`, `CameraPosition`, `Radius`, `OuterSat`, `STime`, `ViewPort` uniforms | Updated before each `PostChain.process(partialTick)` call | `IMPLEMENTED` | Timings preserved: ticks 1-24 expand by 4, tick 25 sets `OuterSat=0.3`, ticks 26-49 collapse by 4, finished after tick 60. |
| `effect.time.stop` world sound | `ClientLevel.playLocalSound(player position, EFFECT_TIME_STOP, PLAYERS, 1, 1, false)` | `IMPLEMENTED` | No longer uses `SimpleSoundInstance.forUI`; `sounds.json` still references the original three `block/bell/resonate` variants. |
| `BlabberDialogueScreenMixin` hides dialogue until freeze animation finishes | `InitialSoulChoiceScreen` creates/renders controls only after `ClientDeathSuspensionFx.hasFinishedAnimation()` | `IMPLEMENTED` | UI is closer to Blabber: no central panel, text at left with ~300px wrap, Blabber-like text choices without vanilla button backgrounds, centered bottom instruction. |

## Verified injection details

| Original mixin | Minecraft target | Method/injection | Behavior | Forge status |
| --- | --- | --- | --- | --- |
| `common.possession.gameplay.LivingEntityMixin` | `LivingEntity` | `travel` `@ModifyVariable(HEAD,argsOnly)` | Redirect possessed movement input through possessor/body state. | `IMPLEMENTED/FORGE_ADAPTATION`: Forge possession is not literal riding, but normal walking hosts now copy possessor input/rotation into the host and route movement through `host.travel(...)` so vanilla movement handles gravity, friction, collision, step-up, falling, `fallDistance`, and jump lifecycle. Special mobility profiles stay on the explicit movement path. |
| `common.possession.gameplay.LivingEntityMixin` | `LivingEntity` | `damage` `@Inject(INVOKE DamageSource.getAttacker)` | Fire possessed-hit advancement/context while vanilla damage continues. | `PARTIAL`: Forge has `LivingHurtEvent`/criteria hooks; compare exact trigger timing. |
| `common.possession.gameplay.LivingEntityMixin` | `LivingEntity` | `sleep`, `wakeUp`, `pushAway` `@Inject(RETURN)` | Keep possession state coherent around sleep/wake/push. | `PARTIAL`: current Forge handles push/collision and lifecycle, sleep/wake still needs explicit audit. |
| `common.remnant.EntityMixin` | `Entity` | `fall` `@Inject(FIELD fallDistance GETFIELD)` | Incorporeal remnants suppress fall effects/fall distance. | `PARTIAL`: Forge uses vagrant collision/fall events; verify exact fall-distance reset. |

## Server-side possession bytecode audit

| Original mixin/injection | Vanilla target and point | Original conditions and behavior | Forge equivalent | Gap/action |
| --- | --- | --- | --- | --- |
| `common.possession.gameplay.ServerPlayNetworkHandlerMixin#swapJumpingMount` | `ServerPlayNetworkHandler.onClientCommand`, `@Inject(HEAD, cancellable=true)` | Runs only for client commands matching riding-jump start/stop. If the real player is not already riding a vanilla `JumpingMount`, gets the player's possessed host through `PossessionComponent.getHost(player)`, asks `JumpingMountEvents.MOUNT_CHECK` for a controlled jumping mount, then forwards jump strength to `setJumpStrength(int)` or calls stop-jump. It does not cancel vanilla packet handling. | Forge port uses `ClientPossessionInput` plus `PossessionInputPacket`/`PossessionActionPacket`; `PossessionManager.controlHost` redirects movement to `controlledMovementEntity(host)`, including skeleton-on-spider, and jump input reaches the movement host on tick. | `PARTIAL`: functionally covered for current camera-control path. No new packet layer needed. Manual test skeleton/stray riding spider jump/dismount. |
| `common.possession.gameplay.MobEntityMixin#requiem$travelStart` | `MobEntity.travel`, inherited `LivingEntityMixin @ModifyVariable(method="travel", at=HEAD,argsOnly=true)` | Restores temporary step height, checks `JumpingMountEvents`, handles `ExternalJumpingMount.attemptJump`, then if mob is alive/ridden and `MobTravelRidingCallback.canBeControlled(mob, rider)` is true, copies rider yaw/pitch and side/forward input, raises `stepHeight` to at least 1, adjusts flying speed/saddled speed, and returns redirected movement input. Otherwise returns vanilla movement input. | Forge port does not use vanilla riding for possession. `PossessionManager.controlHost` sets one rotation authority, suppresses autonomous mob target/navigation each tick, temporarily raises `maxUpStep`, and calls `travel(...)` for ordinary walking hosts. Hosts that were inert soulless mobs are restored to `NoAI=false` while possessed so `Mob.travel` enters vanilla physics; the original NoAI value is saved and restored on release. | `IMPLEMENTED/FORGE_ADAPTATION`: normal walking now follows `PLAYER INPUT -> HOST INPUT -> host.travel(...) -> VANILLA PHYSICS`. Direct `move(MoverType.SELF, ...)` remains only for special mobility paths such as phasing/flying/custom fall behavior. |
| `common.possession.gameplay.MobEntityMixin#requiem$pushed` | `LivingEntity.pushAway`, `@Inject(RETURN)` inherited/overridden | After a mob is pushed, calls `DetectionHelper.attemptDetection(self, pushed, BUMP)`. | `LivingEntityMixin` now calls `PossessionDetectionManager.attemptDetection(self, pushedMob, BUMP)` after vanilla push returns. | `IMPLEMENTED/PARTIAL`: bump detection now reaches the existing Forge detection mode/crowd logic. Manual test exact monster aggro behavior in normal/hard detection modes. |
| `common.possession.gameplay.MobEntityMixin#requiem$damaged` | `LivingEntity.damage`, inherited `@Inject(INVOKE DamageSource.getAttacker)` | If the attacker is a mob, calls detection twice: victim detects attacker with `ATTACKING`, attacker detects victim with `ATTACKED`. | Forge `PossessionManager.tryAttributeAttack` and `PossessionDetectionManager` cover some attack detection. `HumanityKillManager` and criteria hooks also exist. | `PARTIAL`: attack routing works, exact bidirectional detection parity still needs focused detection pass. |
| `common.possession.gameplay.PlayerEntityMixin#generateArrow` | `PlayerEntity.getArrowType`, `@Inject(RETURN, cancellable=true)` | If vanilla returns empty, the player has a possessed host, and host type is in `ARROW_GENERATORS`, marks the weapon NBT with `requiem:infinity_shot=true` and returns a new arrow stack. | Forge `PlayerMixin#getProjectile` returns a generated arrow for possessed `ARROW_GENERATORS`, stores a temporary generated-ammo marker through `PossessionProjectileHooks.INFINITY_SHOT_TAG`, and `PossessionManager.onEntityJoinLevel` marks arrows shot by such possessors as `CREATIVE_ONLY` pickup. | `IMPLEMENTED/PARTIAL`: generated ammo and pickup protection work; custom skeleton arrow entity creation remains not fully ported. |
| `common.possession.gameplay.ItemStackMixin#use` | `ItemStack.use`, `@Inject(HEAD, cancellable=true)` | Delegates to `PossessionItemOverrideWrapper.tryUseOverride(world, player, stack, hand)` and uses returned `TypedActionResult` when present. Applies to both hands because it receives the actual `hand`. | Forge `PossessedItemManager` handles right-click item through data-driven cure/healing/diet rules. | `FIXED`: Forge handler now accepts both main hand and offhand, preserving actual hand for host swing. Full wrapper semantics remain future item override work. |
| `common.possession.gameplay.ItemStackMixin#getMaxUseTime` | `ItemStack.getMaxUseTime`, `@Inject(HEAD, cancellable=true)` | Returns a temporary overridden use time stored on the stack when an override requested it. | `ItemStackMixin#getUseDuration` reads `OverridablePossessedItemStack`; `PossessedItemManager` writes duration before `startUsingItem`. | `IMPLEMENTED`: long-use cure/healing duration is restored through the current Forge pipeline. |
| `common.possession.gameplay.ItemStackMixin#finishUsing` | `ItemStack.finishUsing`, `@Inject(HEAD, cancellable=true)` | If user is a player, delegates to `PossessionItemOverrideWrapper.tryFinishUsingOverride(world, player, stack, user.getActiveHand())`; maps `TypedActionResult` to resulting stack. | `ItemStackMixin#finishUsingItem` delegates to `PossessedItemManager.finishPossessedItemUse`. | `IMPLEMENTED/PARTIAL`: cure/healing/witch-diet finish lifecycle is restored; generic old action registry remains intentionally unported except current gameplay-compatible cases. |
| `common.possession.gameplay.ItemStackMixin#useOnEntity` | `ItemStack.useOnEntity`, `@Inject(RETURN, cancellable=true)` | If target entity is possessed and the possessor's `PossessionComponent.canBeCured(stack)` is true, consumes one item from the user unless creative, starts curing on the possessor's possession component, and returns success. | `FIXED/PARTIAL`: Forge `PossessedItemManager` now handles both self-use while possessing and external `PlayerInteractEvent.EntityInteract` against an already-possessed host, using the existing data-driven cure rules and pending cure tick. | Exact original CCA `canBeCured` semantics are approximated by the current Forge cure rules: reagent match, cure gamerule, weakness effect, cooldown, and pending cure completion. Full long-use item override pipeline remains later work. |

### Server-side possession audit notes

- The audited original `ServerPlayNetworkHandlerMixin` contains only one injection, `swapJumpingMount`, into `onClientCommand` at `HEAD`. It handles `START_RIDING_JUMP` and `STOP_RIDING_JUMP` only. It does **not** redirect vanilla attack, use item, use block, interact entity, drop, swap-hand, selected-slot, or player-action packets.
- Original generic possessed movement in this group comes from `MobEntityMixin`/`LivingEntityMixin` riding-style travel mutation, not from a broad packet router. The Forge port intentionally implements camera-controlled possession through `PossessionInputPacket`, `PossessionActionPacket`, and `PossessionManager.controlHost`.
- The confirmed server-side item dependency in `ItemStackMixin` is the item override entrypoint. Only the cure-on-entity branch was added in this pass because it can be expressed safely with existing Forge events and `PossessedItemManager`. `getMaxUseTime` and `finishUsing` still require the larger item override stage.
- Current Forge possession already has explicit systems for movement, rotation/look, sneak, jump, attacks, mob abilities, AI suppression, targeting, possessed host death, player damage suppression while possessing, teleport coupling, logout suspend/restore, reconnect resume, host food data, inventory/equipment transfer/restrictions, client camera/input sync, and soul data sync.
- Host-death release now has an explicit Forge death-release path: the player remains a free vagrant at the possessed body's death position, vanilla death XP loss is applied with `keepInventory` respected, and ordinary voluntary release still uses the safe adjacent release position. Player clone/respawn after a possessed player body death stores the host death position and reapplies it after vanilla respawn positioning.
- Selected hotbar hand sync is player-authoritative and mirrors copies into the host only when `ItemStack.matches` changes against the last synced equipment snapshot, avoiding shared mutable stack state and repeated equipment packets for unchanged selected items.
- Server/client possessed rotation has one authority during possession: packet yaw/pitch is applied to host, head, body, and previous rotation fields together. Manual validation is still required for client arm/item jitter and spontaneous spin.
- Beta.16 Mortician exposes seven trade listings; `ICHOR_VESSEL_EMANCIPATION` is produced by one listing. Forge now removes the extra piglin Emancipation listing and tests the result item count directly.
- Beta.16 wall phasing keeps the 60-tick wall charge and then lets no-clip movement proceed through normal player motion. Forge keeps the existing delay and raises the post-activation speed cap to `0.10D`; particles are unchanged.
- Beta.16 remnant targeting suppresses ordinary targeting of incorporeal players; it does not provide Blaze/Zombie/Skeleton same-type faction disguise. Forge leaves Blaze-style aggro semantics unchanged and keeps the existing undead-only possessed-host disguise.
- Possession start FX is bounded client state: `START` now begins a 12-tick particle burst and `ACK` only extends the short burst instead of leaving ambiguous target state.
- Remaining manual parity risks for this server-side slice are long-use eating/healing/cure timing, vanilla block-use semantics while possessing item-using bodies, and exact skeleton/spider jumping-mount behavior.

## Group map

| Original mixin | Purpose | Existing Forge implementation | Status | Required action |
| --- | --- | --- | --- | --- |
| `common.possession.PlayerManagerMixin` | Player login/logout/dimension lifecycle for possessed hosts. | `PossessionManager` player clone/login/logout/dimension event logic, `PlayerSoulEvents`. | `PARTIAL` | Audit disconnect/login and cross-dimension host restore against original. |
| `common.possession.gameplay.AbstractPiglinEntityMixin` | Possessed piglin gameplay override. | `PossessionManager`, entity tags, possessed abilities. | `PARTIAL` | Verify piglin-specific anger/barter/equipment behavior. |
| `common.possession.gameplay.BlockItemMixin` | Possessed body item/block use override. | `PossessedItemManager`, `PlayerInteractEvent` hooks. | `PARTIAL` | Compare block placement restrictions and hand routing. |
| `common.possession.gameplay.BowItemMixin` | Bow use for possessed bodies. | `PlayerMixin#getProjectile`, `PossessionManager` attacks/abilities. | `PARTIAL` | Verify ammo generation and host-as-shooter damage source. |
| `common.possession.gameplay.CatEntityMixin` | Cat-specific possessed behavior. | General possession manager. | `MISSING` | Decompile/audit exact cat behavior before adding Forge hook. |
| `common.possession.gameplay.CatLikeEntityMixin` | Cat/ocelot possessed movement should not have vanilla AI clear sprint/pose each server AI step. | `CatLikeEntityMixin` cancels `customServerAiStep` for possessed cats/ocelots. | `IMPLEMENTED/PARTIAL` | Restores sprint retention while possessed. Cat bed/sleep pose callback remains separate and still needs audit. |
| `common.possession.gameplay.CraftingScreenHandlerMixin` | Crafting availability while possessed. | `PossessionInventoryManager`, `SlotMixin`, inventory screen mixin. | `PARTIAL` | Verify recipe/crafting-result restrictions. |
| `common.possession.gameplay.CreeperEntityMixin` | Possessed creeper priming ability. | `PossessionManager` ability code. | `PARTIAL` | Compare fuse timing/cancel behavior. |
| `common.possession.gameplay.CrossbowItemMixin` | Crossbow use for possessed body. | General item/attack handling. | `PARTIAL` | Verify projectile owner, cooldown, ammo. |
| `common.possession.gameplay.EndermanEntityMixin` | Enderman possession abilities/restrictions. | `PossessionManager` ability code and tags. | `PARTIAL` | Compare teleport/pickup/sensitivity behavior. |
| `common.possession.gameplay.FoxEntityMixin` | Fox possessed behavior. | `FoxMixin` clears fox action state when possession begins and syncs crouching to possessed sneak input; general possession manager and item diet rules handle the rest. | `IMPLEMENTED/PARTIAL` | Crouch/action reset covered. Exact sleep/wake and item-holding/eating interactions remain manual. |
| `common.possession.gameplay.FrogEntityMixin` | Frog tongue/catch ability. | `PossessionManager` has frog-related imports/ability path. | `PARTIAL` | Compare target selection and cooldown. |
| `common.possession.gameplay.HorseBaseEntityMixin` | Horse body riding/inventory/movement. | `PossessionInventoryManager`, movement profiles, horse jump now uses `AbstractHorse#getCustomJump`, and `AbstractHorseMixin` lets undead possessed riders count as saddled on undead horses. | `IMPLEMENTED/PARTIAL` | Jump strength and undead-horse saddle rule covered. Chest/inventory and full vanilla riding UX remain manual. |
| `common.possession.gameplay.ItemStackMixin` | Item override pipeline entry for use, max-use-time, finish-use, and cure-on-entity. | `PossessedItemManager`. | `PARTIAL` | Offhand use fixed. Port missing `OverridableItemStack` use-time/finish-use/cure-on-entity semantics in the item override stage without deleting current manager. |
| `common.possession.gameplay.LivingEntityMixin` | Core possessed body movement/damage/sleep/push hooks. | `PossessionManager`, Forge living events, current mixins. | `PARTIAL` | Complete exact lifecycle audit. |
| `common.possession.gameplay.MerchantEntityMixin` | Merchant interaction while possessed. | Vagrant possession interaction now yields to vanilla merchant interaction when the merchant is not possessable; `VagrantEvents` does not cancel merchant entity interaction. | `IMPLEMENTED/PARTIAL` | Verify trading screens in vagrant and possessed states; original also pretends soulless non-possessed merchants have a customer for trading state. |
| `common.possession.gameplay.MilkBucketItemMixin` | Milk cure/diet override. | `PossessedItemManager` diet/cure rules. | `PARTIAL` | Compare vanilla milk effect removal and host restrictions. |
| `common.possession.gameplay.MobEntityMixin` | Host AI/control/targeting while possessed. | `PossessionManager`, `LivingChangeTargetEvent`. | `PARTIAL` | Verify AI suppression and targeting parity. |
| `common.possession.gameplay.MooshroomEntityMixin` | Mooshroom shearing conversion keeps possession on the spawned cow. | `MushroomCowMixin` transfers existing possession to the sheared cow before vanilla adds it to the level. | `IMPLEMENTED/PARTIAL` | Shear conversion is covered and triggers transformed-possessed criterion. Bowl/stew interaction remains vanilla/player-context and should be audited separately if a concrete break appears. |
| `common.possession.gameplay.PandaEntityMixin` | Panda possessed behavior. | `PandaMixin` prevents eating animation for non-food held items, clears lying-on-back while possessed, maps possessed sneak to scared state, and `EntityMixin` scales scared possessed panda dimensions. | `IMPLEMENTED/PARTIAL` | Confirmed hooks ported/adapted for 1.20.1 Panda. Roll/sneeze edge cases remain manual. |
| `common.possession.gameplay.PigEntityMixin` | Pig movement/boost/riding behavior and lightning conversion transfer. | Movement profiles/general possession; `PigMixin` transfers possession from struck pig to spawned zombified piglin. | `IMPLEMENTED/PARTIAL` | Lightning conversion covered and triggers transformed-possessed criterion. Carrot-on-stick/saddle riding parity still needs manual test. |
| `common.possession.gameplay.PlayerEntityMixin` | Player methods affected by possession. | `PlayerMixin`, `PossessionManager`, `SoulManager`. | `PARTIAL` | Audit use/attack/sleep/damage hooks. |
| `common.possession.gameplay.RavagerEntityMixin` | Ravager possessed behavior. | `RavagerMixin` suppresses client-side ravager horizontal-collision griefing during `aiStep`; general possession manager covers attack/movement. | `IMPLEMENTED/PARTIAL` | Client griefing hook covered. Roar/stun/shield behavior remains manual. |
| `common.possession.gameplay.ServerPlayNetworkHandlerMixin` | Server packet routing from possessor to host. | Forge packets `PossessionInputPacket`, `PossessionActionPacket`. | `PARTIAL` | Verify attack/interact/block-use packet parity. |
| `common.possession.gameplay.TridentItemMixin` | Trident use for possessed body. | General projectile/attack handling. | `PARTIAL` | Verify owner, loyalty, riptide restrictions. |
| `common.possession.gameplay.VillagerEntityMixin` | Villager possessed cure/trade/gossip behavior and lightning conversion transfer. | `PossessedItemManager`, cured entities; `VillagerMixin` transfers possession from struck villager to spawned witch. | `IMPLEMENTED/PARTIAL` | Lightning conversion covered and triggers transformed-possessed criterion. Trading/gossip/cure duration restrictions remain partial. |
| `common.possession.gameplay.WitchEntityMixin` | Witch potion behavior around possessed/cure flows. | `WitchMixin` prefers Weakness splash potions against undead targets without Weakness and restores a glass bottle to possessed witches after drinking. | `IMPLEMENTED/PARTIAL` | Matches the two audited beta.16 hooks. Full witch held-item UX and bottle use while directly controlled still need manual testing. |
| `common.possession.gameplay.ZombieVillagerEntityMixin` | Zombie villager cure/body restoration. | `PossessedItemManager` cure path. | `PARTIAL` | Compare cure duration and conversion target. |
| `common.possession.gameplay.ability.*Accessor` | Access private vanilla ability fields/methods. | Direct Forge/Mojang methods plus `GuardianAccessor`, `ShulkerAccessor`. | `PARTIAL` | Add only accessors still required by missing abilities. |
| `common.possession.gameplay.ability.PufferfishEntityMixin` | Pufferfish inflation ability. | `PossessionManager` pufferfish path not confirmed. | `PARTIAL` | Verify inflation state and damage aura. |
| `common.possession.gameplay.ability.WololoGoalMixin` | Evoker sheep wololo ability. | `PossessionManager` evoker path. | `PARTIAL` | Verify custom target and cooldown. |
| `client.possession.*RendererMixin`, `FeatureRendererMixin`, `EyesFeatureRendererMixin`, `ArmorFeatureRendererMixin` | Hide/alter rendering for possessor/host and possessed features. | `ClientPossessionRenderHelper`, `PlayerRendererMixin`, `LivingEntityRendererMixin`, `EntityRenderDispatcherMixin`. | `IMPLEMENTED/PARTIAL` | Free vagrant player skin is translucent and now suppresses vanilla feature/layers; local soul player render is cancelled at dispatcher entry while possessing, which also suppresses third-party renderer/layer hooks attached to that player. Compare armor/features/eyes/passengers by body type manually, especially with Citadel installed. |
| `client.possession.GameRendererMixin`, `MinecraftClientMixin`, `ClientEntityMixin` | Camera/control POV while possessed. | Camera possession state and client packets. | `PARTIAL` | Verify camera, crosshair target, disconnect cleanup. |
| `client.possession.nametag.*` | Name tag rendering for host/possessor. | `PossessionNameTagPacket`, `ClientPossessionNameTags`, `EntityRendererNameTagMixin`, `LivingEntityRendererNameTagMixin`, plus existing server custom-name fallback. | `IMPLEMENTED/PARTIAL` | Client render path now substitutes the possessor name and forces host nametag visibility while `showPossessorNameTag` is active, with periodic resync for gamerule changes. Manual multiplayer tracking/late-join validation still needed. |
| `client.possession.nightvision.*` | Body-specific night vision/lightmap. | `ClientPossessionVision` shaders. | `PARTIAL` | Compare lightmap/fog behavior; avoid Satin dependency. |
| `client.possession.render.GuardianEntityRendererMixin` | Guardian beam visual while possessed. | `PossessionManager` guardian beam state, accessor. | `PARTIAL` | Verify client beam render. |
| `client.possession.render.ShulkerEntityModelMixin` | Hide possessed shulker head in first-person so the model does not occlude the camera. | `ShulkerModelMixin` hides/restores the model head for the local player's possessed shulker in first-person. | `IMPLEMENTED` | Server peek/open ability remains handled by `ShulkerAccessor`/`PossessionManager`; this row covers the missing client model parity hook. |
| `client.possession.riding.ClientPlayerEntityMixin` | Client riding input integration. | Camera possession avoids mounting. | `PARTIAL` | Verify old riding-specific cases are obsolete or reimplemented. |
| `common.remnant.AbstractBlockStateMixin` | Remnant block collision/suffocation/material interaction. | `BlockStateBaseMixin`, `VagrantRules`. | `PARTIAL` | Compare all collision and suffocation branches. |
| `common.remnant.DamageTrackerMixin` | Remnant death/damage message behavior. | Soul lifecycle events. | `MISSING` | Audit death messages/damage attribution. |
| `common.remnant.EntityMixin` | Fall effects for incorporeal remnants. | `EntityMixin`, Forge fall handling. | `PARTIAL` | Verify fall distance reset. |
| `common.remnant.EntityPredicatesMixin` | Entity predicates ignore/handle remnants. | `EntitySelectorMixin` wraps `NO_CREATIVE_OR_SPECTATOR`; `VagrantRules` filters free vagrants from ordinary target predicates. | `IMPLEMENTED/PARTIAL` | Free-vagrant vanilla predicate parity restored for the audited predicate; other predicates remain manual-only unless a concrete beta.16 hook is found. |
| `common.remnant.EntityShapeContextMixin`, `ShapeContextMixin` | Block shape/collision context for remnant. | `EntityMixin` redirects block collision. | `PARTIAL` | Verify context-sensitive blocks: powder snow, doors, fluids. |
| `common.remnant.HungerManagerMixin` | Hunger restriction while remnant. | `VagrantEvents.refreshSoulHunger` sets vagrant food level to 20 and exhaustion to 0 during server tick. | `IMPLEMENTED` | Covered by Forge tick handling rather than a HungerManager mixin. |
| `common.remnant.LivingEntityMixin` | Damage/visibility/targeting for remnant living entities. | `VagrantEvents`, `LivingEntityMixin`. | `PARTIAL` | Audit damage immunity and AI target rules. |
| `common.remnant.NearestPlayersSensorMixin`, `TargetPredicateMixin` | AI sensors should ignore/incorporate remnants correctly. | `TargetingConditionsMixin` rejects free-vagrant targets before vanilla target checks; `NearestLivingEntitySensorMixin` filters free vagrants from 1.20.1 brain nearest-living memories; `LivingChangeTargetEvent` remains as cleanup. | `IMPLEMENTED/PARTIAL` | Forge/Mojmap 1.20.1 has no separate `NearestPlayersSensor`; nearest-living brain memory is the applicable hook. Manual mob sensor regression still required. |
| `common.remnant.PlayerEntityMixin`, `ServerPlayerEntityMixin`, `ServerPlayNetworkHandler1Mixin` | Player interaction restrictions and network handling for remnant. | `VagrantRules`, `VagrantEvents`, network packets. | `PARTIAL` | Verify use/attack/block breaking/login persistence. |
| `common.remnant.WeightedPressurePlateBlockMixin` | Remnants do not trigger weighted plates. | `WeightedPressurePlateBlockMixin`. | `IMPLEMENTED` | Keep current Forge mixin. |
| `client.remnant.*` | Remnant HUD, camera, interaction manager and rendering. | HUD/client state/render mixins; `EntityMixin#isVisuallyCrawling` suppresses crawling pose for restricted vagrants. | `PARTIAL` | Verify server remains authoritative and client only visual/input. |
| `common.inventory.*Accessor` | Access screen handler internals. | Forge inventory APIs and current `SlotMixin`. | `NOT_NEEDED_ON_FORGE` | Do not port unless exact access is required. |
| `common.inventory.CraftingResultSlotMixin`, `PlayerScreenHandlerMixin`, `SlotMixin` | Inventory/crafting restrictions for possessed body anatomy. | `PossessionInventoryManager`, `SlotMixin`. | `PARTIAL` | Verify all slot activity/mayPlace/mayPickup branches. |
| `common.inventory.balance.EntityPredicatesEquipableMixin` | Equipment slot/body anatomy predicate. | `PossessionInventoryManager` body tags. | `PARTIAL` | Compare equipable predicate behavior. |
| `client.inventory.*` | Alternate inventory screen layout and recipe book behavior. | `InventoryScreenMixin`. | `PARTIAL` | Verify crafting screen/recipe book still need client hooks. |
| `common.data.DynamicRegistryManagerMixin`, `EntityPredicateMixin`, `ItemPredicateMixin`, `LocationPredicateMixin` | Custom datapack predicates/powers support. | Some `ModCriteria`; missing listed predicates/powers. | `MISSING` | Implement/adapt Forge predicate/power data before enabling JSON. |
| `common.attrition.*` | Attrition effect persistence, commands, HUD/status behavior. | `AttritionMobEffect`, `ModEffects`, partial HUD. | `PARTIAL` | Compare effect command, reapply, death/clone persistence. |
| `client.attrition.*` | Attrition HUD and inventory visuals. | `HudOverlay` and client effects. | `PARTIAL` | Verify icon/heart overlays. |
| `client.opus.EditBookScreenAccessor`, `LecternScreenMixin`, `WrittenBookScreenContentsMixin` | Opus book/lectern client GUI behavior. | `OpusDaemoniumItem`, `OpusLecternRules`, server lectern mixin. | `PARTIAL` | Audit client GUI pages/buttons; add only missing client hooks. |

## Item Override Pipeline

Audited original classes/methods from `requiem-2.0.0-beta.16` with `javap`, using `Requiem-1.20` source only as readable reference: `ItemStackMixin`, `PossessionItemOverrideWrapper`, `OverridableItemStack`, `InstancedItemOverride`, `OverrideFailure`, `CureItemOverride`, `DietItemOverride`, `HealingItemOverride`, and `VanillaRequiemPlugin` actions `cure`, `healWithFood`, `replaceBone`, `eatWitchFood`.

Original runtime flow:

1. Item override wrappers are datapack registry entries under `requiem:mob_items`, sorted by descending `priority`.
2. A wrapper first checks `enabled`, then wrapper-level `mob`, then the concrete override predicate.
3. `findOverride` returns the first short-circuiting override, otherwise the first fallback failure.
4. `ItemStackMixin#use` clears any temporary use duration, finds the possessed host for the real player, and delegates to override `use`.
5. `ItemStackMixin#getMaxUseTime` returns the temporary duration written by `OverridableItemStack`.
6. `ItemStackMixin#finishUsing` clears the temporary duration, finds the same host, delegates to override `finishUsing`, and replaces the returned stack when the result is not `PASS`.
7. `DietItemOverride#use` returns `PASS`, so vanilla food starts and finishes normally unless a filter handles `finishUsing`.
8. `HealingItemOverride#use` is instant when `use_time <= 0`; otherwise it overrides use duration and calls `player.setCurrentHand`.
9. `CureItemOverride#use` always overrides use duration to 48 ticks and calls `player.setCurrentHand`.
10. Interruption has no special override callback. If vanilla use is interrupted before `finishUsing`, no consume/effect/cooldown happens.

| Component | Original behavior | Forge equivalent | Status | Implementation |
| --- | --- | --- | --- | --- |
| `ItemStackMixin#use` | Entry point for possessed player item override. Starts long use for cure/healing or returns fail/pass according to selected override. | `PossessedItemManager.onUseItem` handles possessed `RightClickItem`. | `PARTIAL` -> `IMPLEMENTED` for cure/healing/diet override entry. | Now clears transient duration, blocks invalid cure/healing/diet states, starts real player `startUsingItem(hand)` for long cure/healing, and lets valid diet food pass to vanilla. |
| `ItemStackMixin#getMaxUseTime` | Returns temporary overridden use time from `OverridableItemStack`. | New Forge `ItemStackMixin#getUseDuration` plus `OverridablePossessedItemStack`. | `IMPLEMENTED` | Needed because Forge events cannot change vanilla active item duration after use has started. |
| `ItemStackMixin#finishUsing` | Delegates completion to override; only completion consumes item, heals/cures, and applies cooldown. | New Forge `ItemStackMixin#finishUsingItem` delegates to `PossessedItemManager.finishPossessedItemUse`. | `IMPLEMENTED/PARTIAL` | Long-use completion is restored. `replace_bone` now tracks replaced bones in persistent entity data and transforms replaceable skeletons after the original difficulty thresholds. |
| `ItemStackMixin#useOnEntity` | External cure: if target has possessor and possessor can be cured with stack, consume unless creative and start curing. | `PossessedItemManager.onUseItemOnPossessedEntity`. | `PARTIAL` | Existing helper covers external cure with reagent, Weakness, cooldown, pending cure, particles/sound. It intentionally keeps Forge cure rules rather than adding a second cure system. |
| `PossessionItemOverrideWrapper` | Datapack codec, priority sorting, fallback failure handling, tooltip collection. | `PossessedItemManager` reloads equivalent healing/diet/cure JSON subsets from Forge resources; `ClientPossessedItemTooltips` adds tooltip lines. | `IMPLEMENTED/PARTIAL` | Runtime priority is honored for healing/diet; tooltip text, strikethrough fallback, and schema 0 cure-catalyst pass tooltips are restored. Generic old action registry remains unported because current beta.16 gameplay data only needs pass catalyst tooltip plus explicit cure/healing/diet behavior. |
| `OverridableItemStack` | Transient per-stack use duration field, cleared at use/finish. | `OverridablePossessedItemStack` + `ItemStackMixin`. | `IMPLEMENTED` | No persistent NBT and no client-only state. |
| `CureItemOverride` | Test: possessor may cure host, stack matches reagent, possessed host has Weakness via `possessed_state`. Use: 48 ticks. Finish: `startCuring`, decrement one item, success. | `PossessedItemManager` cure rules plus existing pending body cure flow. | `PARTIAL` -> `IMPLEMENTED/PARTIAL` | Self-use now has 48-tick long use and consumes/starts pending cure on finish. External cure remains immediate. Final transformation still uses existing Forge `restorePlayerBodyFromHost` path, not original CCA component internals. |
| `DietItemOverride` | Allows only configured diets for host groups; matching diet returns `PASS`, nonmatching vanilla food becomes fallback failure. `remove_harmful_effects` filter manually finishes item and reverts harmful food effects. | `PossessedItemManager` diet rules and player food data already swapped by `PossessionManager`; client tooltip event mirrors override feedback. | `PARTIAL` -> `IMPLEMENTED/PARTIAL` | Valid diet food now uses vanilla lifecycle and host-backed player food data. Invalid food is blocked. Witch harmful-effect filter is implemented at finish. Diet tooltips and strikethrough invalid-food feedback are restored. |
| `HealingItemOverride` | If item matches and host damaged, either instant finish or long use with configured `use_time`; `eat_to_heal` heals host by food hunger and calls player food consumption; `replace_bone` consumes a bone via skeleton bone component; cooldown applies after finish. Full-health host returns fallback failure. | `PossessedItemManager` healing rules. | `PARTIAL` -> `IMPLEMENTED/PARTIAL` | Long use, original use times, cooldown-after-finish, host healing by food nutrition, full-HP blocking, tooltip feedback, and `replace_bone` difficulty-threshold transformation are restored. |

### DietItemOverride Details

Original configured diet hosts:

- `#requiem:illagers` -> `requiem:food/illager_diet`
- `#requiem:mushroom_folk` -> `requiem:food/mushroom_based`
- `#requiem:piglins` -> `requiem:food/piglin_diet`, plus lower-priority always-edible vanilla food
- `#requiem:villager_folk` -> `requiem:food/villager_base_diet`
- `#requiem:villagers` -> `requiem:food/villager_diet`
- `#requiem:witches` -> `requiem:food/witch_diet` with harmful-effect filter, plus lower-priority always-edible vanilla food

Diet does not heal the host directly. It gates vanilla food use. Hunger and saturation use the real player food system; in this Forge port that food data is already swapped to host-backed data by `PossessionManager.applyHostFoodData`. Vanilla decides main hand/offhand, container item, creative handling, duration, animation, interruption, and food effects. Nonmatching vanilla food is blocked for hosts with diet rules, matching non-food items are ignored, and always-edible rules are now parsed.

### HealingItemOverride Details

Original healing rules:

- `#requiem:humanoid_skeletons` + `#requiem:bones`: `replace_bone`, `use_time=0`, `cooldown=60`
- `minecraft:drowned` + `#requiem:raw_fishes`: `eat_to_heal`, `use_time=32`, `cooldown=150`
- `minecraft:fox` + sweet berries: `eat_to_heal`, `use_time=16`, `cooldown=48`
- `#requiem:zombies` + `#requiem:raw_meats`: `eat_to_heal`, `use_time=32`, `cooldown=150`

This Forge port also has extra Alex's Mobs and enderman pearl JSON rules. The restored generic lifecycle applies to them, but non-food `eat_to_heal` items cannot heal through original `healWithFood` semantics unless a future rule adds a dedicated usage.

### CureItemOverride Details

Original self cure requires `RemnantComponent.canCurePossessed(host)`, matching reagent, and Weakness on host. It uses 48 ticks, then calls `PossessionComponent.startCuring()` and decrements one item. The original `useOnEntity` external cure is immediate after vanilla entity interaction return and consumes unless the external user is creative.

Forge self cure now mirrors the long-use timing and still feeds the existing pending cure flow: reagent + Weakness + cure gamerule + cooldown check, consume on completion, start sound/particles, wait until Weakness duration expires, restore player body from host, set player to `REMNANT`, clear possession, and heal player. External cure continues to share the same helper and pending cure path.

### Long-Use Semantics

Authoritative use state is the real `ServerPlayer` active item state, matching the original design where `player.setCurrentHand(hand)` owns `getActiveHand`, `getUseItem`, and remaining ticks. The possessed host does not store a separate active item state. Effects are applied to the possessed host only at `finishUsingItem`. If the use is interrupted through vanilla `stopUsingItem`/`releaseUsingItem`, no override finish occurs and no item is consumed.

### Block/Item Use Semantics

This pass restores the generic item-use lifecycle for override items and the audited projectile/milk authority hooks for bow, crossbow, trident, and milk. It intentionally does not rewrite shield, bucket, throwable items, spawn eggs, or use-on-block `BlockItem` behavior because the audited original keeps ordinary item/block use in the real player packet path. The current Forge possession architecture still depends on that real player packet path for ordinary item/block use.

### Cooldown Parity

Original owner is the real player item cooldown manager. Forge keeps `Player#getCooldowns()` on the possessor or external item user. Cooldown is checked at start and completion. It is synchronized by vanilla player cooldown packets, does not live on the host, and does not persist as host state after release.

## Possessed Item/Block Authority

Audited original `2.0.0-beta.16` bytecode and readable `Requiem-1.20` sources for `BlockItemMixin`, `BowItemMixin`, `CrossbowItemMixin`, `TridentItemMixin`, `MilkBucketItemMixin`, `PlayerEntityMixin#getArrowType`, plus current Forge `PlayerMixin`, `PossessionManager`, `PossessedItemManager`, and inventory/food state.

### Original Injection Details

| Original mixin/class | Target class | Target method / injection point | Condition | Original behavior | Forge equivalent | Current status |
| --- | --- | --- | --- | --- | --- | --- |
| `BlockItemMixin` | `BlockItem` | `useOnBlock`, before fallback call to `BlockItem.use(world, player, hand)` | `context.getPlayer() != null` and possession item override returns non-`PASS` | Does not replace placement context. It only lets possession item overrides run when block placement falls through to normal item use. Placement position, face, collision, permission, sound, stat, and item consumption remain vanilla player-context behavior. | Existing `PossessedItemManager` covers item override use; no host-context block placement rewrite added. | `PARTIAL/NOT_NEEDED_ON_FORGE`: exact fallback-on-block override should be manually checked, but original does not justify a broad host placement context. |
| `BowItemMixin#giveSkeletonInfinity` | `BowItem` | `onStoppedUsing/releaseUsing`, modifies stored infinity boolean | Possessor has host; generated-arrow tag or host is skeleton | Generated arrow tag grants infinity once. Possessed skeletons get 80% chance not to consume arrows. | `PlayerMixin#getProjectile`, new Forge `BowItemMixin`. | `IMPLEMENTED/PARTIAL`: generated tag and 80% consume parity restored; custom skeleton arrow entity creation remains not fully ported. |
| `BowItemMixin#useSkeletonArrow` | `BowItem` | wraps `ArrowItem.createArrow(world, stack, shooter)` | host implements skeleton arrow factory | Replaces vanilla arrow entity with skeleton-created arrow entity, but vanilla shooter parameter is still the real player for rotation/owner flow. | Not fully equivalent; Forge still uses vanilla arrow class, with generated arrows pickup-protected. | `PARTIAL`: projectile class/effects may differ for special skeleton variants. |
| `CrossbowItemMixin#giveCrossbowInfinity` | `CrossbowItem` | `loadProjectiles/tryLoadProjectiles`, modifies stored creative flag | host is `CrossbowUser`; 50% random or client side | Treats load as creative, stores generated-arrow tag on crossbow, avoiding arrow consumption. | New Forge `CrossbowItemMixin`, using skeleton-like host tag because vanilla Forge skeleton classes do not implement the same Fabric/Yarn interface name. | `IMPLEMENTED/PARTIAL`: ammo/pickup parity restored for skeleton hosts; non-skeleton crossbow mobs need body-specific audit if added. |
| `CrossbowItemMixin#preventBoltPickup` | `CrossbowItem` | `shoot`, modifies args-only creative flag | server side, center projectile, generated-arrow tag present | Removes generated-arrow tag and marks bolt as creative-shot so it cannot be picked up. | New Forge `CrossbowItemMixin`. | `IMPLEMENTED`. |
| `TridentItemMixin#giveDrownedInfinity` | `TridentItem` | `onStoppedUsing/releaseUsing`, before `ThrownTrident/TridentEntity.setProperties/shootFromRotation` | host is drowned; 80% random; player not creative | Temporarily flips player creative/instabuild so vanilla trident throw does not remove the item. | New Forge `TridentItemMixin`. | `IMPLEMENTED`: uses temporary `Abilities.instabuild` around vanilla throw removal path. |
| `TridentItemMixin#revertCreativeMode` | `TridentItem` | `onStoppedUsing/releaseUsing`, before stat increment | temporary flag set | Reverts the temporary creative flag. | New Forge `TridentItemMixin`. | `IMPLEMENTED`. |
| `MilkBucketItemMixin` | `MilkBucketItem` | `finishUsing`, after vanilla status-effect clear | possessed host is skeleton-tagged | Vanilla milk still clears effects on the real player; possessed skeleton host additionally gets Resistance for 600 ticks. | New Forge `MilkBucketItemMixin`, after `finishUsingItem` return. | `IMPLEMENTED`: after-return hook preserves vanilla result and adds host Resistance. |
| bucket/shield mixins | none found for this pipeline in original | n/a | n/a | Original Requiem `2.0.0-beta.16` does not rewrite generic bucket raycast, bucket empty/pickup, shield active blocking, or block break authority for possession in this mixin set. | Current Forge uses vanilla player lifecycle plus possession movement/camera sync. | `NOT_NEEDED_ON_FORGE/PARTIAL`: document and manually test; do not invent host-executed bucket/shield semantics without evidence. |

### Authority Matrix

Original model separates controller and host conservatively: the real `Player` owns network input, inventory, active item state, cooldowns, stats, advancements, and most vanilla item execution. The possessed mob is a host used by item override predicates, movement/combat abilities, selected side effects, and generated-ammo rules. Original projectile weapon mixins modify vanilla player action rather than replacing it with `host.useItem`.

| Action | Input owner | Item owner | Logical actor | World interaction actor | Result owner |
| --- | --- | --- | --- | --- | --- |
| right click air | Player/controller | Player inventory, possibly host-restricted inventory state | Player, with possession item override predicate using host | Player vanilla `Item.use` unless override handles it | Player stack/cooldown/stat; host may receive healing/cure side effect |
| right click block | Player/controller | Player inventory | Player | Player `UseOnContext`; original does not replace position/reach/entity | Placed block/player stat/player stack, except fallback item override |
| right click entity | Player/controller or external player | User's stack | Player/user; target may be possessed host | Vanilla entity interaction, plus external cure after return | User stack/cooldown; possessor pending cure if valid |
| attack entity | Possession action packet / controller input | Host natural attack, not held item by default | Possessed host | `PossessionManager.attack` uses host reach/look and host damage source | Host aggro/damage attribution; possessor criteria |
| break block | Player/controller | Player tool/inventory | Player | Vanilla player block break unless remnant/vagrant rules block it | Player stats/durability |
| place block | Player/controller | Player stack | Player | Vanilla player placement context | World block/player item/stat |
| bucket pickup | Player/controller | Player bucket | Player | Vanilla player raycast/context | Player stack replacement |
| bucket empty | Player/controller | Player bucket | Player | Vanilla player raycast/context | World fluid/player stack |
| bow charge | Player/controller | Player bow/arrows or generated skeleton arrow | Player action modified by host predicate | Player active item state | Player active hand |
| bow release | Player/controller | Player bow/ammo | Player shooter; host grants ammo behavior | Vanilla bow projectile spawn/rotation from Player; generated pickup protected | Player stat/durability; arrow pickup may be creative-only |
| crossbow load | Player/controller | Player crossbow/ammo | Player loading modified by host predicate | Vanilla crossbow load | Crossbow NBT charged projectile; ammo may be non-consumed |
| crossbow fire | Player/controller | Player crossbow charged NBT | Player shooter | Vanilla crossbow projectile spawn/rotation | Player stat/durability; generated bolt pickup protected |
| trident charge | Player/controller | Player trident | Player | Vanilla active item state | Player active hand |
| trident throw | Player/controller | Player trident | Player shooter; drowned host grants non-consume chance | Vanilla trident spawn/rotation/riptide | Player stat/durability; item may remain 80% for drowned host |
| shield use | Player/controller | Player shield | Player | Vanilla player blocking | Player damage blocking/cooldown/durability |
| food | Player/controller | Player stack; host-backed food data during possession | Player, diet predicate uses host | Vanilla finish food use | Player food data currently swapped to host storage |
| healing override | Player/controller | Player stack | Player active item, host receives heal | Player active-item lifecycle | Host heal; player cooldown/stack |
| cure | Player/controller or external user | User stack | Player active item; host cure target | Self: player long use. External: entity interact helper | Possessor pending cure; user stack/cooldown |
| potion use | Player/controller | Player stack | Player | Vanilla player finish use unless specific override | Player effects/stack |
| milk | Player/controller | Player milk bucket | Player, with skeleton-host side effect | Vanilla player finish use | Player effects cleared/empty bucket; skeleton host gets Resistance |
| generic long-use item | Player/controller | Player stack | Player | Vanilla active item lifecycle | Player stack/effects unless override says otherwise |

### Release And Edge Semantics

Original long-use ownership is the real player. Bow, crossbow, trident, shield, food, cure, and healing all use player active item state. Forge now explicitly calls `player.stopUsingItem()` during possession release, and also stops host use state defensively, preventing stale charge/block/eat state after release, host death cleanup, logout-driven release, or dimension cleanup.

Projectile ownership remains mostly vanilla-player-owned in the audited original. Consequences: damage attribution, stats, advancements, PvP, and loyalty/riptide mechanics follow player vanilla behavior unless a specific mixin changes ammo/pickup/projectile class. This pass restores generated ammo/pickup and drowned trident non-consumption, but does not rewrite projectile owner to host.

### Manual Test Checklist - Item/Block Authority

| Case | Conditions | Action | Expected result |
| --- | --- | --- | --- |
| block placement | Possess item-using host, hold block. | Place block normally. | Vanilla placement succeeds with player context; no item override regression. |
| block placement with distant Player body | Move/camera possession so host and player body differ. | Place block at crosshair. | Placement follows current vanilla/Forge player context; document any reach mismatch for later architecture decision. |
| bucket pickup | Possess host, hold empty bucket. | Pick up water/lava. | Vanilla bucket pickup, player stack replaced. |
| bucket placement | Possess host, hold filled bucket. | Empty bucket. | Vanilla bucket empty, player stack replaced. |
| milk | Possess skeleton, give player a negative effect, drink milk. | Finish drinking. | Player effects clear, bucket returns, skeleton host gains Resistance 600 ticks. |
| bow full charge | Possess skeleton, use bow with/without arrows. | Fully charge and release. | Generated/no-consume ammo works; arrows from generated ammo are pickup-protected. |
| bow interrupted | Start bow draw. | Release before minimum charge or stop use. | No shot or vanilla weak-shot behavior; no stale active item. |
| crossbow loading | Possess skeleton, load crossbow. | Hold until charged. | Crossbow charges; generated ammo has 50% non-consume behavior. |
| crossbow firing | Charged crossbow. | Right click fire. | Vanilla firing; generated bolt pickup-protected. |
| multishot | Skeleton host with multishot crossbow. | Load and fire. | All vanilla multishot projectiles fire; generated center projectile pickup-protected. |
| trident throw | Possess drowned with trident. | Charge then release. | Vanilla trident throw; 80% chance item is not removed. |
| Loyalty trident | Possess drowned, throw Loyalty trident. | Wait for return. | Vanilla loyalty returns to player owner/inventory path. |
| Riptide | Possess drowned/player in rain or water with Riptide trident. | Release after charge. | Vanilla Riptide affects player as original did; no custom host riptide rewrite. |
| shield block | Possess host, hold shield. | Hold use while attacked. | Vanilla player shield state is active; host damage blocking is not rewritten in original and remains manual parity risk. |
| shield disable | Shield active, attacked by axe. | Observe cooldown. | Vanilla player shield disable/cooldown. |
| main/offhand | Put bow/crossbow/trident/shield/milk in either hand. | Use item. | Active hand remains player hand; stack affected in correct hand. |
| release possession while charging | Start bow/crossbow/trident/shield/food/cure use. | Release possession. | `stopUsingItem` clears active use; no stale charge/block/effect. |
| host death while charging | Start long-use/charge, kill host. | Wait. | Possession cleanup stops active use; no duplicate completion. |
| logout/reconnect | Start charge/use, disconnect and reconnect. | Observe state. | No stuck active item; existing suspend/restore possession flow remains authoritative. |
| projectile kill attribution | Possess skeleton/drowned and kill mob with bow/crossbow/trident. | Check aggro/credit. | Attribution follows vanilla player-owned projectile, matching audited original model. |

### Manual Test Checklist

| Case | Conditions | Action | Expected result |
| --- | --- | --- | --- |
| possessed zombie | Become remnant, possess zombie, give raw meat, damage host. | Hold right click with raw meat. | 32-tick eat animation; on completion host heals by food nutrition, item consumed, 150 tick cooldown. |
| possessed zombie villager | Possess zombie villager with Weakness and golden apple/tagged cure. | Hold right click until complete. | 48-tick use; item consumed after completion; cure starts; body restore occurs after Weakness duration. |
| possessed skeleton | Possess skeleton below full HP, hold bone. | Right click bone. | Instant consume/heal fallback and 60 tick cooldown; mark exact original bone-loss parity still partial. |
| ordinary item use | Possess a mob with no matching override item. | Right click ordinary non-food item. | Existing Forge/vanilla behavior is unchanged. |
| main hand | Put valid healing/cure item in main hand. | Hold use. | Active hand is main hand; host swings main hand on completion. |
| offhand | Put valid healing/cure item in offhand. | Hold use from offhand. | Active hand is offhand; offhand stack is consumed/finished. |
| eating | Possess villager/witch/piglin with matching diet food and host food data not full. | Eat allowed food. | Vanilla eating lifecycle runs; food data used is possessed-host-backed player food data. |
| interrupted eating | Start eating allowed diet/healing food. | Release before completion. | No healing override, no item consumption beyond vanilla interrupted semantics. |
| healing | Damage zombie/drowned/fox host and use configured healing item. | Hold until finish. | Host heals by food nutrition; configured cooldown starts only after finish. |
| healing at full HP | Full HP host with configured healing item. | Right click. | Use is blocked; item is not consumed and vanilla eating does not start. |
| self cure | Possess curable undead with Weakness and cure reagent. | Hold use until complete. | 48-tick use then existing pending cure starts. |
| external cure | Another player targets possessed undead with Weakness and cure reagent. | Right click entity. | Immediate pending cure start through shared helper; external item user cooldown/consumption applies. |
| cure without Weakness | Possessed curable undead without Weakness. | Try self and external cure. | No cure starts, item is not consumed. |
| cure with Weakness | Possessed curable undead with Weakness. | Complete self-use or external use. | Cure starts and completes after Weakness duration. |
| interrupted cure | Start self cure, release before 48 ticks. | Stop holding use. | No pending cure, no item consumption, no cooldown. |
| cooldown | Complete healing/cure, immediately repeat. | Right click same item during cooldown. | Start is blocked; repeated packets do not consume or reapply effect. |
| release possession during use | Start long healing/cure, stop possession before finish. | Wait past former finish time. | No override finish should apply to old host; no item consumption from override. |
| host death during use | Start long healing/cure, kill host before finish. | Wait past finish. | No override finish; possession cleanup remains handled by `PossessionManager`. |
| logout/reconnect during or after use | Start long use, disconnect; reconnect. | Observe possession/item state. | No duplicate consume/effect; active use is not client-only. After completed cure, possession state remains cleared/restored by existing manager. |

## Remnant Parity - Stage 5

Audited against readable `Requiem-1.20` sources and spot-checked with `javap -c -p` on `requiem-2.0.0-beta.16` class files. In original Requiem terminology, `REMNANT` is the demon body-capable type, while the free ghost gameplay state is `VAGRANT` / incorporeal. The Forge port stores both as `SoulState` in `PlayerSoul` capability; no second remnant store should be introduced.

### Original Remnant Classes And Mixins

| Original class/mixin | Target / method / injection | Original behavior | Side | Forge equivalent | Status |
| --- | --- | --- | --- | --- | --- |
| `RemnantComponentImpl` | CCA component state, sync, NBT, `become`, `setVagrant`, split/merge/respawn hooks | Stores remnant type plus ethereal/vagrant flag, fires type/state callbacks, syncs to client, delegates lifecycle to `RemnantState`. | server/client data | `PlayerSoul` capability with `SoulState`, possessed UUID, shell UUID, resurrection point, `SyncSoulDataPacket`. | `PARTIAL`: equivalent storage exists, event callback surface is not fully ported. |
| `DemonRemnantState` | remnant state implementation | Can regenerate human body, cure possessed body, applies attrition on death respawn, allows split only when forced/emancipated. | server | `SoulLifecycleManager`, cure/resurrection managers, attrition effect. | `PARTIAL`: core lifecycle exists; exact attrition/state callback parity incomplete. |
| `WandererRemnantState` | remnant state implementation | Starts as vagrant, cannot regenerate player body directly, can always split/dissociate, attrition may mend into current body/host. | server | `SoulState.VAGRANT`, possession release, rift/resurrection managers. | `PARTIAL`: free vagrant exists; attrition mend-to-body logic not fully ported. |
| `PlayerSplitter` | split/merge helper | Creates shell with copied player NBT, respawns soul with inventory loss, transfers soulbound items, anchors shell, merges inventory/body data back. | server | `PlayerShellEntity`, `SoulLifecycleManager`, `DroppedVesselTracker`. | `PARTIAL`: Stage 5 now stores/restores normal inventory in shell; full NBT/effects/impersonation/soulbound transfer remains partial. |
| `ServerPlayerEntityMixin` | `playerTick` HEAD; `onDeath` HEAD; `onDeath` before death-message gamerule | Cancels ticks while death is transient; first unchosen non-hardcore death suspends for choice or applies forced type; hardcore death revokes demon state. | server | `InitialSoulChoiceManager`, `SoulLifecycleManager`, Forge death/clone/respawn events. | `PARTIAL`: choice exists at login rather than original death-suspender screen; remnant death to vagrant exists. |
| `DamageTrackerMixin` | `DamageTracker.update` HEAD | Freezes damage tracker while death is transient. | server | No direct Forge equivalent. | `MISSING`: only relevant to original first-death suspension UI, not current server remnant lifecycle. |
| `AbstractBlockStateMixin` | `getCollisionShape` HEAD; `onEntityCollision` HEAD | No-clipping shape context passes through non-`soul_impermeable` blocks; incorporeal entities ignore block collision effects except portals. | server/client physics | `EntityMixin#collideBoundingBox` redirect, `BlockStateBaseMixin`, `VagrantRules`. | `PARTIAL`: implemented with Forge collision redirect and phase charge; exact shape-context parity is approximated. |
| `EntityShapeContextMixin` / `ShapeContextMixin` | `EntityShapeContext(Entity)` RETURN; interface default | Carries `MovementAlterer.isNoClipping()` into block shape queries. | server/client physics | `VagrantRules.shouldSuppressBlockCollision` + entity collision redirect. | `PARTIAL`: behavior implemented outside shape context. |
| `EntityMixin` | `Entity.fall` at first `fallDistance` read | Sets fall distance to 0 for incorporeal entities. | server/client physics | `VagrantEvents.onLivingFall`, `VagrantEvents` tick reset. | `IMPLEMENTED`. |
| `HungerManagerMixin` | `HungerManager.update` before first update invoke | If incorporeal, exhaustion is 0 and food level is 20. | server | Stage 5 `VagrantEvents.refreshSoulHunger`. | `IMPLEMENTED`. |
| `LivingEntityMixin` | `createAttributes` RETURN; `drop` before `dropInventory`; `fall` particle arg; `consumeItem`; `collides` RETURN; `pushAway`/`pushAwayFrom` HEAD | Adds soul defense attribute; fires drop callback/resurrection entity; fewer fall particles; incorporeal drops active consumed item if keepInventory false; vagrants do not collide or push. | mostly server | `ModEntityAttributes`, resurrection managers, `EntityMixin`, `LivingEntityMixin`, `VagrantEvents`. | `PARTIAL`: collision/push implemented; active consumed item drop and fall particle reduction not ported. |
| `PlayerEntityMixin` | `createAttributes`, `isSwimming`, `getMoveEffect`, `getAirSpeed`, `travel`, `getDimensions`, `getActiveEyeHeight` | Adds soul offense; swimming pose while flying/sprinting; no move effects; slow no-clip air speed; vertical fly-swim adjustment; smaller crouching soul hitbox/eye height. | server/client | `VagrantEvents`, `ClientVagrantState`, current player renderer. | `PARTIAL`: flight/noPhysics implemented and vagrant visual crawling is suppressed; exact swim pose, dimensions, eye height not ported. |
| `TargetPredicateMixin` | `TargetPredicate.test` before predicate field | Rejects incorporeal targets unless predicate explicitly includes them. | server AI | `TargetingConditionsMixin`, `LivingChangeTargetEvent`, target cleanup. | `IMPLEMENTED/PARTIAL`: ordinary free-vagrant targets are rejected at `TargetingConditions#test`; no include-incorporeal extension API was added because no current Forge caller needs it. |
| `NearestPlayersSensorMixin` | synthetic lambda RETURN | Brain nearest-player sensor ignores vagrant players. | server AI | `NearestLivingEntitySensorMixin`. | `IMPLEMENTED/PARTIAL`: 1.20.1 Forge uses `NearestLivingEntitySensor`; free vagrants are filtered before nearest living/visible memories are written. |
| `EntityPredicatesMixin` | vanilla non-creative/spectator predicate lambda RETURN | Treats vagrant players like excluded creative/spectator targets for that predicate. | server | `EntitySelectorMixin`. | `IMPLEMENTED`: wraps `EntitySelector.NO_CREATIVE_OR_SPECTATOR` to reject free vagrants. |
| `ServerPlayNetworkHandler1Mixin` | entity interaction packet inner handler `processInteract` HEAD | Calls `AllowUseEntityCallback`; cancels entity interaction when callback denies. | server | Stage 5 `PlayerInteractEvent.EntityInteract` and `EntityInteractSpecific` cancellation for restricted vagrant. | `IMPLEMENTED` for free vagrant restriction; callback API not ported. |
| `WeightedPressurePlateBlockMixin` | `calculateRedstoneOutput` after count cap | Removes vagrant players from weighted pressure plate count. | server | `WeightedPressurePlateBlockMixin#getSignalStrength`. | `IMPLEMENTED`. |
| `ClientPlayerEntityMixin` | `tickMovement`, `wouldCollideAt`, `tickNewAi` | Client-side no-clip vertical damping, block push-out suppression, fly sprint preservation, control alteration. | client | `ClientVagrantState`, current possession camera handling. | `PARTIAL`. |
| `ClientPlayerInteractionManagerMixin` | `interactEntity`, `interactEntityAtLocation` before packet send | Client refuses entity interaction when server callback would deny. | client | `ClientVagrantState` input guard partially allows entity use for server validation. | `PARTIAL`. |
| `GameRendererMixin` | target update, block outline, camera transform, render hand overlay | Updates custom target handler, hides block outline except soul-interactable blocks, applies camera callbacks, renders overlays when intangible. | client | `ClientPossessionTargetHint`, `HudOverlay`, no full camera shader stack. | `PARTIAL`. |
| `MinecraftClientMixin` | `setScreen`, `hasOutline` | Skips death screen for demons by auto-respawn; outlines shells/possessable mobs for no-clipping vagrant. | client | Initial choice screen, possession target hint/outline-ish UI. | `PARTIAL`. |
| `InGameHudMixin` | crosshair/status bar modifications | Hides health/armor/absorption/food/air for free vagrant, substitutes host health while possessing, custom crosshair. | client | `HudOverlay`. | `PARTIAL`: functional HUD exists, exact vanilla overlay surgery not identical. |
| `LivingEntityRendererMixin` / `PlayerRendererLayerMixin` | renderer feature iterator/render layer | Hides feature renderers for vagrants; renders incorporeal/death-transient players through shadow/end-portal layer. | client | `PlayerRendererMixin`, `LivingEntityRendererMixin`, `EntityRenderDispatcherMixin`. | `IMPLEMENTED/PARTIAL`: gray translucent free vagrant visual exists, vanilla feature/layers are suppressed, and shadows are hidden; Satin/shadow framebuffer style is not ported. |

### Remnant Parity Matrix

| Original behavior | Original implementation | Current Forge implementation | Status | Required action |
| --- | --- | --- | --- | --- |
| state storage | `RemnantComponentImpl`: type + ethereal flag in CCA NBT | `PlayerSoul` capability: `SoulState`, possession/shell/resurrection UUIDs | `IMPLEMENTED` | Keep using capability; no duplicate remnant store. |
| state synchronization | CCA sync writes raw remnant type + vagrant flag | `SyncSoulDataPacket` sends `SoulState` and related UUIDs | `IMPLEMENTED` | Verify reconnect/client render state manually. |
| entering Remnant | first-death choice/forced type calls `become(REMNANT)` | `InitialSoulChoiceManager`, vessels/Opus set `SoulState.REMNANT` | `PARTIAL` | Death-suspender choice UI remains different from original. |
| leaving Remnant | cure/vessel/merge/resurrection set mortal or non-vagrant | vessels, resurrection managers, shell reclaim | `PARTIAL` | Full shell NBT/effects parity still partial. |
| death | demon death respawns into vagrant; first unchosen death can suspend | `SoulLifecycleManager` cancels remnant death and creates shell/vagrant | `PARTIAL` | Stage 5 fixed inventory-in-shell; first-death transient death not ported. |
| resurrection | data-driven death/totem/ritual conversion to new body | `DeathResurrectionManager`, `TotemResurrectionManager`, `ResurrectionManager` | `PARTIAL` | Verify rule parity and interruption/completion effects manually. |
| damage | free vagrant ordinary damage suppressed by incorporeal flow; hardcore exception for host death attrition | `LivingAttackEvent` cancels all free-vagrant attacks | `IMPLEMENTED/PARTIAL` | Manual test all damage types; decide whether `/kill`/void needs explicit exception. |
| invulnerability | no single blanket flag; behavior via damage/target/collision hooks | event cancellation, no `isInvulnerableTo` mixin | `PARTIAL` | Add minimal mixin only if manual tests find unhooked damage. |
| interaction | entity-use callback may deny | Stage 5 cancels Forge entity/specific interaction for restricted vagrant | `IMPLEMENTED` | Keep rift/soul block exceptions separate. |
| block interaction | block use/break/item use blocked except soul-interactable systems | `VagrantEvents` cancels break/right-click/item; rift block checks `VagrantRules` | `IMPLEMENTED/PARTIAL` | Verify doors/buttons/levers/chests/rift. |
| entity interaction | `ServerPlayNetworkHandler1Mixin` cancels denied entity packets | Stage 5 `EntityInteract`/`EntityInteractSpecific` | `IMPLEMENTED` | Manual external cure/possession target tests. |
| attacking | vagrant cannot attack/collide | `AttackEntityEvent` cancels | `IMPLEMENTED` | Manual melee/projectile trigger tests. |
| collision | vagrant `collides=false`, push cancelled, no block collision when no-clipping | Entity/Living mixins + collision redirect | `PARTIAL` | Exact shape-context blocks/powder snow/fluids need manual tests. |
| movement | flight/no-clip altered movement, slow no-clip air speed, fly-swim behavior | server/client `VagrantEvents` and `ClientVagrantState` phase model | `PARTIAL` | Player dimensions/eye height/swim pose not exact. |
| flying/floating behavior | mayfly granted by state handlers/MovementAlterer | `VagrantEvents` grants mayfly, normalizes entry flying | `IMPLEMENTED/PARTIAL` | Manual jump/sprint/sneak/fly tests. |
| gravity | gravity suppressed only while phasing/noPhysics | `VagrantEvents` mirrors with `setNoGravity(player.noPhysics)` | `IMPLEMENTED` | Manual phase and fall tests. |
| fall damage | fallDistance reset, fall event suppressed | `LivingFallEvent`, per-tick reset | `IMPLEMENTED` | Manual high fall. |
| drowning | free vagrant damage suppressed and HUD air hidden in original | damage suppressed; HUD air hidden by overlay | `IMPLEMENTED/PARTIAL` | Manual underwater test. |
| fire/lava | damage suppressed by vagrant attack cancellation | same | `IMPLEMENTED/PARTIAL` | Manual fire/lava test. |
| hunger | hunger update sets exhaustion 0, food level 20 | Stage 5 refreshes food NBT to level 20/exhaustion 0 | `IMPLEMENTED` | Manual starvation test. |
| item use | free vagrant item use blocked | `RightClickItem` cancel | `IMPLEMENTED` | Manual main/offhand. |
| inventory | original split stores body inventory in shell/respawn soul loses normal inventory | Stage 5 shell stores/restores player inventory; soul inventory cleared | `PARTIAL` | Soulbound/modded inventory transfer not ported. |
| item pickup | free vagrant excluded like spectator/creative and pickup callback | `EntityItemPickupEvent` cancel | `IMPLEMENTED` | Manual pickup. |
| item dropping | free vagrant cannot toss | `ItemTossEvent` cancel | `IMPLEMENTED` | Manual drop key. |
| armor | shell carries equipment/body inventory; vagrant HUD hides armor | shell equipment + Stage 5 inventory NBT; HUD hides bars | `PARTIAL` | Exact armor feature/render hiding partial. |
| status effects | original full NBT copy and attrition state hooks | attrition effect, basic shell health/inventory; effects not fully copied through shell | `PARTIAL` | Add effect/body NBT parity only if manual lifecycle needs it. |
| mob targeting | target predicates and sensors ignore vagrants | `LivingChangeTargetEvent`, target cleanup | `PARTIAL` | Brain sensor memory exact parity not guaranteed. |
| visibility | vagrant rendered translucent, not vanilla invisible | Forge keeps player visible for vagrant; possession hides possessor | `PARTIAL` | Visual parity remains partial. |
| sleeping | original possession/sleep callbacks; vagrant physical interactions blocked | no explicit vagrant sleep hook beyond block/item restrictions | `PARTIAL` | Manual bed test. |
| riding | split preserves shell mount; vagrant interaction mostly blocked | possession/dismount flows exist; shell mount not fully copied | `PARTIAL` | Manual riding/split/logout. |
| teleport | original records anchors and teleports/merges vessel | `DroppedVesselTracker`, possession teleport sync, rift manager | `PARTIAL` | Cross-dimension anchor tests required. |
| dimension change | respawn/capability prepareRespawn handles state | `PlayerEvent.Clone`, dimension sync, possession cleanup | `PARTIAL` | Verify shell UUID/resurrection state across dimensions. |
| respawn | original `prepareRespawn` updates state, vagrant after demon death | Forge clone copies capability and forces vagrant after remnant/possession death | `IMPLEMENTED/PARTIAL` | Manual death/respawn with shell inventory. |
| logout | original dropped vessel tracker stores anchor to host/shell | Forge `DroppedVesselTracker`, suspend/resume possession | `PARTIAL` | Manual logout as vagrant and while possessing. |
| reconnect | CCA sync + tracker restore | login tries merge/resume and syncs soul data | `PARTIAL` | Manual reconnect target. |
| commands/state modification | original `/requiem` component mutation | Forge commands intentionally out of stage | `MISSING` | Do not address in Stage 5. |
| camera | client camera callbacks and possession camera | possession camera implemented; vagrant camera mostly vanilla | `PARTIAL` | Client parity later. |
| player rendering | shadow/end-portal layer or feature hiding | gray/hidden local vagrant mixins | `PARTIAL` | Satin/shader stack not ported. |
| first-person rendering | hand/overlay adjustments | basic render mixins/HUD only | `PARTIAL` | Later client parity. |
| HUD | hides free-vagrant body bars; host health while possessing | `HudOverlay` | `PARTIAL` | Exact vanilla status layout not identical. |
| particles/sounds | fall particle reduction, attrition/cure/split sounds/particles | many particles/sounds registered; some lifecycle FX | `PARTIAL` | Audit visual/audio later. |

### Stage 5 Server Changes

- `VagrantEvents` now cancels `PlayerInteractEvent.EntityInteract` and `EntityInteractSpecific` for restricted free vagrants, matching the original entity interaction packet hook.
- `VagrantEvents` now refreshes free-vagrant hunger to food level 20 with exhaustion 0 during server tick, matching `HungerManagerMixin`.
- `PlayerShellEntity` now persists a `RequiemBodyInventory` NBT list captured from the player and reloads it on shell reclaim.
- `SoulLifecycleManager` now clears the free soul inventory after shell creation instead of dropping the whole body inventory into the world.
- `DroppedVesselTracker` now clears player inventory after creating a disconnect vessel shell, preventing logout/reconnect inventory duplication when `DROP_VESSEL_ON_DISCONNECT` is enabled.
- `PlayerShellEntity` now clears its captured inventory after restore so a reclaimed shell cannot restore the same body inventory twice before removal completes.
- No new mixin was added in this pass; Forge events and existing shell/capability systems were sufficient.

### Manual Test Checklist - Remnant

| Conditions | Action | Expected result |
| --- | --- | --- |
| mortal/remnant choice available | choose remnant or force remnant | `SoulState.REMNANT`, no possession active, sync to client. |
| remnant with inventory/armor/offhand | die or split | player becomes `VAGRANT`; shell appears; free soul inventory is empty/restricted; shell stores body inventory. |
| vagrant near shell | reclaim/interact through supported path | player returns to `REMNANT`; inventory, armor, offhand restore from shell; shell removed. |
| free vagrant | walk, sprint, sneak, jump, fly toggle | mayfly works; no fall damage; phase behavior follows charge rules. |
| free vagrant | fall from height | no damage, fall distance reset. |
| free vagrant | stand in fire/lava/water/void edge cases | ordinary damage suppressed; verify whether void or `/kill` bypasses Forge events. |
| free vagrant | take melee/projectile/explosion/magic/starvation damage | ordinary damage does not kill or knock lifecycle out of vagrant. |
| hostile mobs nearby | wait and provoke targeting | mobs do not retain player as target; no nearest-player aggression. |
| free vagrant | break/place/use block/open chest/use door/button/lever | ordinary interactions fail; soul-interactable rift remains usable when valid. |
| free vagrant | right-click entity and attack entity | interaction and attack fail server-side. |
| free vagrant | pick up item/drop item/use item/offhand/craft/inventory | pickup/drop/use blocked; inventory should be empty/restricted. |
| remnant -> possession | possess valid soulless/allowed body | possession starts, `SoulState.VAGRANT` with possessed UUID, camera/control unchanged from prior stages. |
| possession -> release | release possession | player returns to free `VAGRANT`; active item cleaned up. |
| host death | let host die while possessing | player becomes free `VAGRANT`; attrition/cleanup remains coherent. |
| logout as vagrant | disconnect/reconnect | shell anchor restores/merges or teleports to anchor; soul state and restrictions reapply. |
| logout during possession | disconnect/reconnect | host suspend/resume path restores possession or safely clears to vagrant. |
| teleport/dimension change | use commands, Nether/End portal, rift | capability, shell UUID, possession UUID and restrictions persist correctly. |
| death/respawn | die as remnant and as vagrant | remnant death becomes vagrant; vagrant ordinary death should be suppressed unless an unhooked damage source is found. |
| resurrection | complete ritual/totem/death-resurrection rule | player exits free state or possesses result per rule; inventory/shell state stays coherent. |
| client parity enabled | third/first-person, HUD, camera, particles | visual state is acceptable but remains `PARTIAL` against original shader/render stack. |

## Stage 6 - Requiem Commands

Reference classes audited from `2.0.0-beta.16` bytecode and cross-read with `Requiem-1.20` source:
`RequiemCommand`, `RequiemSoulCommand`, `RequiemShellCommand`, `RequiemRemnantCommand`,
`RequiemPossessionCommand`, `RequiemEtherealCommand`, and `RemnantArgumentType`.

Original command root:

```text
/requiem
  vagrant query [target]
  vagrant set <ethereal:boolean> [target...]
  possession stop [target...]
  possession start <possessed:entity> [possessor:player]
  remnant query [target]
  remnant set <remnant_type:true|false|id> [target...]
  shell create [player]
  shell split [players...]
  shell merge <shell:entity> [player]
  shell identity set <shells:entities> <profile:game_profile>
  soul query <target:entity>
  soul remove <target:entities>
  soul restore <target:entities>
```

Forge command registration uses `RegisterCommandsEvent` and vanilla Brigadier. Fabric permissions are adapted to vanilla op level 2 via `CommandSourceStack#hasPermission(2)`.

| Command path | Arguments | Permission | Original action | Forge equivalent | Status |
| --- | --- | --- | --- | --- | --- |
| `/requiem` | none | Fabric permissions or level 2 | Root only; no root execution. | Root registered through `RegisterCommandsEvent`; level 2 required. | `IMPLEMENTED` |
| `/requiem soul query` | `target: entity` | `requiem.command.soul.query` | Report whether living entity has soul. | `EntitySoulManager.hasSoul`. | `IMPLEMENTED` |
| `/requiem soul remove` | `target: entities` | `requiem.command.soul.remove` | Remove soul unless target is not living or permanently soulless. | `EntitySoulManager.removeSoul`; rejects `PlayerShellEntity` and `SOULLESS` tag. | `IMPLEMENTED` |
| `/requiem soul restore` | `target: entities` | `requiem.command.soul.restore` | Restore soul unless target is not living or permanently soulless. | `EntitySoulManager.restoreSoul`. | `IMPLEMENTED` |
| `/requiem remnant query` | optional `target: player` | query self/other permissions | Report `RemnantType`. | Reports `MORTAL` for mortal, `REMNANT` for both embodied remnant and free vagrant, preserving original type-vs-vagrant distinction. | `IMPLEMENTED` |
| `/requiem remnant set` | `remnant_type`, optional `target: players` | set self/other permissions | `RemnantComponent.become(type)`. | `MORTAL` clears possession/shell/resurrection through existing managers; `REMNANT` uses `SoulManager.setState`. | `PARTIAL`: only `mortal` and `remnant` exist in Forge enum. |
| `/requiem vagrant query` | optional `target: player` | query self/other permissions | Report `RemnantComponent.isVagrant()`. | Reports free `SoulState.VAGRANT` with no possessed entity. | `IMPLEMENTED` |
| `/requiem vagrant set true` | optional `target: players` | set self/other permissions | `RemnantComponent.setVagrant(true)`. | `SoulLifecycleManager.splitPlayer`, preserving inventory capture to shell. | `IMPLEMENTED` |
| `/requiem vagrant set false` | optional `target: players` | set self/other permissions | `RemnantComponent.setVagrant(false)`. | Reclaims current captured shell through `SoulLifecycleManager.tryReclaimShell`. | `PARTIAL`: fails without a live captured shell; no fake body regeneration. |
| `/requiem possession start` | `possessed: entity`, optional `possessor: player` | start self/other permissions | Start possessing a `MobEntity`; requires incorporeal possessor. | `PossessionManager.tryPossess`; requires free `VAGRANT`; keeps Forge possession invariants. | `IMPLEMENTED` |
| `/requiem possession stop` | optional `target: players` | stop self/other permissions | Stop current possession. | `PossessionManager.release`. | `IMPLEMENTED` |
| `/requiem shell create` | optional `player` | root permission | Create shell from player at command source position. | Creates an identity-only `PlayerShellEntity` without captured inventory. | `PARTIAL`: intentionally does not copy inventory to avoid duplication. |
| `/requiem shell split` | optional `players` | root permission | Split remnant player into shell + vagrant. | `SoulLifecycleManager.splitPlayer`; Stage 5 inventory capture/clear preserved. | `IMPLEMENTED` |
| `/requiem shell merge` | `shell`, optional `player` | root permission | Merge player with shell. | `SoulLifecycleManager.tryReclaimShell`; requires captured inventory shell. | `IMPLEMENTED/PARTIAL`: command-created identity shells are not reclaimable. |
| `/requiem shell identity set` | `shells`, `profile` | root permission | Change shell display profile. | Updates shell owner UUID/name using existing shell synced owner data. | `PARTIAL`: renderer/profile parity is limited by current shell renderer. |

Stage 6 implementation notes:

- `RequiemCommands` is a single Forge registrar; no custom command framework was added.
- `RemnantArgumentType` is adapted as a `word` argument with suggestions for `true`, `false`, `mortal`, `remnant`, `requiemunofficial:mortal`, and `requiemunofficial:remnant` to avoid unregistered custom argument sync issues.
- `PlayerShellEntity` now tracks whether body inventory was actually captured. Reclaim requires this flag, so debug/identity shells cannot erase or duplicate player inventory.
- `SoulLifecycleManager.findShell` exposes the existing shell lookup for safe command transitions.
- Commands rely on `SoulManager`, `EntitySoulManager`, `SoulLifecycleManager`, `PossessionManager`, and `VagrantEvents` synchronization/side effects rather than direct field mutation.

Stage 6 runtime verification:

- `compileJava --rerun-tasks` passed after the command implementation and again after adding focused Stage 6 GameTests.
- The previous `runClient` timeout was not a confirmed hang or crash. `latest.log` showed normal client startup to the main menu, then `Minecraft: Stopping!`; no Java process remained afterward.
- The previous `runClient` did not start an integrated server. There were no `Server thread`, `Starting integrated server`, or `Preparing start region` entries, so in-game `/requiem` tab completion was not proven by that run.
- `debug.log` confirms Forge auto-subscribed `ua.meekeyn.requiemunofficial.common.command.RequiemCommands` to the FORGE bus.
- A server-side GameTest smoke check now verifies the real server dispatcher contains `/requiem` with `soul`, `remnant`, `vagrant`, `possession`, and `shell` children.
- A server-side GameTest guard check verifies an identity-only command-style `PlayerShellEntity` has no captured body inventory, is rejected by `SoulLifecycleManager.tryReclaimShell`, and does not overwrite player inventory.
- `runGameTestServer` starts the server, registers the command subscriber, and runs the new Stage 6 dispatcher/shell guard tests successfully. The full task still exits non-zero because pre-existing tests fail in the fake-player harness (`ServerGamePacketListenerImpl.connection` null) and a few older data assertions; those failures are not Stage 6 command registration failures.
- A quick-play `runClient --args="--quickPlaySingleplayer 'Новый мир (2)'"` launched the client but exited during client resource reload before integrated-server startup. It ended with a shutdown-only `TextureManager.close` `ConcurrentModificationException`; no `RegisterCommands`, Brigadier, duplicate literal, invalid argument type, command serialization, or Requiem command classloading failure appeared.
- `latest.log`/`debug.log` warnings observed during client startup are development/Forge/Realms/resource-load noise unless otherwise noted: missing dev refmap, Forge language jars without `mods.toml`, Forge version check, Realms auth, Netty reflective-access debug exceptions, and the quick-play shutdown-only texture manager exception.
- Manual integrated-world testing remains required for actual client tab completion and destructive command smoke tests with a real networked `ServerPlayer`.

### Manual Test Checklist - Commands

| Command | Initial state | Expected result |
| --- | --- | --- |
| `/requiem` | op player | Shows subcommands; no duplicate registration. |
| `/requiem soul query <entity>` | living mob | Reports soul status. |
| `/requiem soul remove <mob>` | normal mob | Mob becomes soulless/inert; no crash. |
| `/requiem soul restore <mob>` | soulless mob | Soul restored; mob AI restored when applicable. |
| `/requiem soul remove <shell>` | player shell | Fails as permanently soulless. |
| `/requiem remnant query [player]` | mortal/remnant/vagrant | Mortal reports mortal; remnant and vagrant report remnant type. |
| `/requiem remnant set remnant [player]` | mortal | Player becomes `REMNANT`, syncs to client. |
| `/requiem remnant set mortal [player]` | remnant/vagrant/possessing | Possession released, shell removed, resurrection point cleared, state becomes `MORTAL`. |
| `/requiem vagrant set true [player]` | remnant with full inventory | Shell captures inventory, player inventory clears, state becomes free `VAGRANT`. |
| `/requiem vagrant set false [player]` | vagrant with live shell | Shell reclaim restores inventory once, shell removed, state becomes `REMNANT`. |
| `/requiem vagrant set false [player]` | vagrant without shell | Command fails; no fake body/inventory state created. |
| `/requiem possession start <mob> [player]` | free vagrant + valid mob | Existing possession starts; possessed UUID syncs. |
| `/requiem possession start <entity>` | mortal player or invalid host | Command fails; no partial possession state. |
| `/requiem possession stop [player]` | possessing | Existing release path runs; player returns free `VAGRANT`. |
| `/requiem shell split [player]` | remnant full inventory | Stage 5 split path runs; no duplicate inventory. |
| `/requiem shell merge <shell> [player]` | vagrant + captured shell | Inventory restored exactly once; shell removed. |
| `/requiem shell create [player]` | any player | Identity shell spawned without captured inventory; cannot be used to reclaim inventory. |
| `/requiem shell identity set <shells> <profile>` | shell entity | Shell owner/name changes. |
| Any state-changing command | non-op source | Denied by permission level 2. |
| Any command | invalid argument | Brigadier command error, no server crash. |
| Commands after logout/reconnect | changed state | Capability, shell UUID, possession UUID, flight/noPhysics remain coherent. |

## Stage 7 - First-death Death Suspension + Initial Remnant Choice

Reference classes spot-checked from `requiem-2.0.0-beta.16` bytecode and readable `Requiem-1.20` source:
`ServerPlayerEntityMixin`, `DeathSuspender`, `RevivingDeathSuspender`, `RemnantChoiceDialogueAction`,
`RemnantComponentImpl`, `MutableRemnantState`, and `PlayerSplitter`.

Original behavior:

- `VERIFIED`: original `ServerPlayerEntityMixin#suspendDeath` runs from `ServerPlayerEntity.onDeath` HEAD, checks `requiem:adventure/the_choice`, skips completed choice, skips hardcore, reads `STARTING_SOUL_MODE`, suspends choose-mode death, and applies forced type without GUI.
- `VERIFIED`: original `RevivingDeathSuspender` sets health to 1, stores the `DamageSource`, marks life transient, waits 20 server ticks, opens `remnant_choice`, then `resumeDeath` clears transient state, sets health to 0, and calls death again with the saved source.
- `VERIFIED`: original `RemnantChoiceDialogueAction` applies `RemnantComponent.become(chosenType, true)` and resumes suspended death when life is transient.
- `PARTIAL`: original `PlayerSplitter` performs a full respawn/shell split with NBT, soulbound, mount, and event transfer. Forge Stage 5/6 shell lifecycle remains the active adaptation; Stage 7 did not rewrite it.

Forge adaptation:

- `FORGE_ADAPTATION`: `DeathSuspensionManager` intercepts first unchosen lethal `LivingDeathEvent` through `SoulLifecycleManager` before remnant death handling. It cancels the first death only in choose mode, keeps the player alive at health 1, and sends the existing `OpenSoulChoiceScreenPacket` after 20 ticks.
- `FORGE_ADAPTATION`: `InitialSoulChoiceManager.initialize` no longer opens choice UI at login/initialize. It only marks already non-mortal players as having made the initial choice. Origins sync code remains available but is not called automatically by login/tick/respawn.
- `FORGE_ADAPTATION`: existing `InitialSoulChoiceScreen`, `InitialSoulChoicePacket`, `OpenSoulChoiceScreenPacket`, `ModNetwork`, `PlayerSoul`, `SoulManager`, and `SoulLifecycleManager` are reused. No second soul-state system was added.

Transient state implementation:

- `FORGE_ADAPTATION`: live original `DamageSource` is stored in-memory in `DeathSuspensionManager.LIVE_DEATH_CAUSES` by player UUID.
- `FORGE_ADAPTATION`: persisted NBT under `Player.PERSISTED_NBT_TAG/RequiemDeathSuspension` stores only safe guards: `Suspended`, `Countdown`, `ChoiceOpened`, and `ResumeWithFallbackCause`.
- `PARTIAL`: original `DamageSourceSerialization` is not ported. After disconnect/reconnect, the exact source is not persisted; resume falls back to `player.damageSources().generic()` and is documented as a Forge safety adaptation.

Gamerule behavior:

- `PARTIAL`: existing Forge gamerule `requiemunofficial:startingRemnantType` is reused; values are current implementation integers `0=choose`, `1=force remnant`, `2=force vanilla/mortal`.
- `FORGE_ADAPTATION`: forced mortal/remnant applies choice during first death and does not show UI. Forced remnant is allowed to continue vanilla death handling for that same first death instead of being caught by the normal immediate remnant-death cancel path.

Hardcore behavior:

- `PARTIAL`: first-choice suspension is bypassed in hardcore, matching original first-death bypass.
- `FORGE_ADAPTATION`: remnant death in hardcore is forced back to `MORTAL` and vanilla death is allowed instead of creating a vagrant shell.

Advancement/marker behavior:

- `FORGE_ADAPTATION`: `resources/data/requiemunofficial/advancements/adventure/the_choice.json` is restored from the previously disabled Forge-adapted resource because `requiemunofficial:made_remnant_choice` is registered and loaded.
- `FORGE_ADAPTATION`: the reliable marker is still persistent capability-side NBT `RequiemInitialSoulChoiceMade`; `hasChosen` also treats the restored advancement as completed when present.
- `PARTIAL`: this is not exact original advancement-only parity because Forge fake-player and no-connection paths need the NBT marker.

Reconnect behavior:

- `FORGE_ADAPTATION`: logout removes the live `DamageSource` reference to avoid stale entity/source retention.
- `FORGE_ADAPTATION`: reconnect while suspended keeps the player alive, resets `ChoiceOpened=false`, and reopens choice after tick processing; if the player completes choice after reconnect, death resumes with generic fallback source.
- `PARTIAL`: exact original reconnect semantics cannot be claimed without porting CCA `DamageSourceSerialization`.

Automated tests:

- `VERIFIED`: new GameTests cover first lethal death entering suspension, repeated damage/death not creating a second suspension, 20-tick choice-open guard, Mortal choice clearing suspension, Remnant choice clearing suspension without direct `VAGRANT`, duplicate choice packet ignored, completed choice preventing future suspension, and forced remnant using vanilla death path without UI.
- `VERIFIED`: Stage 6 command-created shell guard test remains present and unchanged.
- `PARTIAL`: full `runGameTestServer` still exits non-zero because of pre-existing fake connection/data tests; after Stage 7 fixes, none of the new Stage 7 tests are listed among failing tests.

Manual tests deferred:

- `PARTIAL`: real networked choice screen open/close behavior, client death screen timing, actual vanilla respawn after Mortal choice, actual Remnant choice followed by clone/respawn to `VAGRANT`, reconnect mid-screen, and hardcore first death require manual integrated-world regression.
- `PARTIAL`: exact death message/source parity after reconnect uses generic fallback and needs manual acceptance.

Remaining parity gaps:

- `BLOCKED_BY_UNPORTED_SYSTEM`: original Blabber dialogue is not ported; Forge uses existing choice screen/network.
- `BLOCKED_BY_UNPORTED_SYSTEM`: original `DamageSourceSerialization` is not ported.
- `PARTIAL`: full `PlayerSplitter` NBT/soulbound/mount parity remains Stage 5 scope and is not rewritten here.

## Stage 8 - PlayerSplitter + Shell/Body + Soulbound/Sticky Lifecycle Parity

Reference classes audited from `requiem-2.0.0-beta.16` bytecode and readable `Requiem-1.20` source:
`PlayerSplitter`, `MutableRemnantState`, `RemnantComponentImpl`, `SoulbindingEnchantment`, and respawn/death lifecycle call sites around `ServerPlayerEntityMixin`.

Original `PlayerSplitter` flow:

- `VERIFIED`: original split creates a full body/shell from the old player state while vanilla respawn creates the soul-side player.
- `VERIFIED`: body-owned inventory remains in the shell/body snapshot; soulbound stacks return to the soul-side player.
- `VERIFIED`: soulbound stacks are removed from the body snapshot so the same stack cannot exist in both soul and shell.
- `VERIFIED`: original `MutableRemnantState.prepareRespawn` turns an embodied remnant into a vagrant after death, transfers global position, applies non-lossless death attrition, and carries sticky effects through the post-death respawn lifecycle.

Forge adaptation:

- `FORGE_ADAPTATION`: `SoulLifecycleManager`, `PlayerShellEntity`, `DroppedVesselTracker`, Forge capabilities, and vanilla `Inventory#save/load` slot IDs replace Fabric CCA and original respawn object copying.
- `FORGE_ADAPTATION`: split/death/logout now use the same body-transfer path: shell captures player body inventory/effects, player inventory is cleared, soulbound or keep-owned stacks are returned to the soul, and those returned stacks are removed from shell storage.
- `FORGE_ADAPTATION`: reclaim now merges captured body inventory back into the existing soul inventory instead of replacing the player inventory. This preserves soulbound/keep-owned stacks and restores only body-owned data from the shell.
- `FORGE_ADAPTATION`: command-created `/requiem shell create` shells remain identity-only and non-reclaimable because they do not contain captured body inventory.

Ownership table:

| Data | Owner after split/death | Reclaim behavior | Status |
| --- | --- | --- | --- |
| Normal main inventory/hotbar | Shell body snapshot | Restored to exact vanilla slot IDs where available | `VERIFIED` by Stage 8 GameTests |
| Soulbound main inventory | Soul player | Not restored from shell; removed from shell snapshot | `VERIFIED` |
| Keep-inventory stacks | Soul player | Not duplicated on reclaim | `VERIFIED` |
| Armor | Body shell unless soulbound | Restored from shell snapshot | `VERIFIED` |
| Offhand | Body shell unless soulbound | Soulbound offhand stays with soul and survives reclaim | `VERIFIED` |
| XP | Not fully ported into shell snapshot | Not fully restored from shell | `PARTIAL` |
| Health | Captured as shell health ratio | Restored on reclaim | `PARTIAL`: implemented, not exhaustively parity-tested |
| Hunger | Soul/player-side current Forge handling | Separate body hunger parity remains covered by older possession tests, still failing unrelated to Stage 8 | `PARTIAL` |
| Status effects | Soulbound effects stay with soul; body effects copy to shell | Body effects restore and soulbound effects survive | `VERIFIED` by Stage 8 GameTest |
| Sticky effects | Mapped to current soulbound/sticky effect set (`ATTRITION`, `EMANCIPATION`, `PENANCE`, `RECLAMATION`) | Preserved with soul across split/reclaim | `PARTIAL`: core behavior tested, full original callback parity not ported |
| Position/dimension | Shell spawned at player position; reclaim moves player to shell | Same-dimension reclaim tested through shell path | `PARTIAL`; cross-dimension manual regression deferred |
| Game mode | Not copied as body-owned data | No Stage 8 change | `PARTIAL` |
| Identity/profile | Shell owner UUID/name set from player | Command identity shells stay non-reclaimable | `VERIFIED` for command-shell guard |
| Persistent data | Existing Forge capabilities remain authoritative | No broad player NBT copy | `PARTIAL` |

Soulbound and sticky semantics:

- `VERIFIED`: Stage 8 uses a Forge-local `RequiemSoulbound` stack NBT marker as a minimal replacement for original `SoulboundStackCheckCallback`.
- `VERIFIED`: soulbound stacks are returned to the soul and removed from `PlayerShellEntity` captured NBT storage.
- `VERIFIED`: reclaim does not erase or duplicate soulbound/keep-owned stacks because shell inventory is merged rather than loaded over the soul inventory.
- `PARTIAL`: original enchantment/callback extensibility is not fully ported; only the confirmed local marker semantics are automated.
- `PARTIAL`: sticky effects are represented by the current Forge soulbound effect set. Full original callback/event parity is deferred.

Death, split, reclaim, and logout:

- `VERIFIED`: embodied remnant death uses existing `SoulLifecycleManager` and becomes `VAGRANT` with a captured shell; no second independent `REMNANT -> VAGRANT` path was added.
- `VERIFIED`: explicit split clears player body inventory after shell capture, returns soul-owned stacks, and keeps body-owned stacks in shell storage.
- `VERIFIED`: reclaim restores body-owned inventory/equipment/effects once, clears captured body inventory, discards shell, and returns state to `REMNANT`.
- `VERIFIED`: repeated reclaim of the discarded shell is rejected.
- `VERIFIED`: command-created identity shell cannot replace or erase real player inventory.
- `PARTIAL`: logout-created shell now uses the same finalize body-transfer path as split/death, preserving Stage 5 anti-duplication invariants. Real reconnect behavior is `DEFERRED_MANUAL_REGRESSION`.

Stage 5 anti-duplication protections preserved:

- `VERIFIED`: split order remains capture body snapshot -> clear player inventory -> return soulbound/keep-owned stacks -> remove returned stacks from shell snapshot.
- `VERIFIED`: reclaim restores only captured body-owned stacks and clears the captured shell inventory before shell discard.
- `VERIFIED`: keep-inventory path does not create a second copy in shell.
- `VERIFIED`: identity shells cannot be used as inventory replacement shells.

Fake-player GameTest guards:

- `FORGE_ADAPTATION`: explicit inventory broadcasts are skipped unless `ServerPlayer.connection` and its backing network connection are present.
- `FORGE_ADAPTATION`: reclaim/shell-destroy system messages are sent only to real networked players.
- `FORGE_ADAPTATION`: client-only vagrant visual effects are only applied through vanilla effect packets for real networked players.
- `FORGE_ADAPTATION`: fake-player restore teleport uses server-side `moveTo`; real players keep vanilla teleport packet path.
- `FORGE_ADAPTATION`: fake-player effect mutation for attrition/setup/restoration writes server-side effect state directly; real players keep vanilla `addEffect/removeEffect`.

Automated verification:

- `VERIFIED`: `compileJava --rerun-tasks` passed after the final Stage 8 fixes.
- `VERIFIED`: Stage 8 GameTests present and passing in the latest `runGameTestServer` run: `stage8SoulboundStackStaysWithSoulWithoutDuplication`, `stage8ArmorAndOffhandFollowSoulboundSemantics`, `stage8KeepInventoryLeavesStacksWithSoulAndNotShell`, `stage8SoulboundEffectsStayWithSoulAndBodyEffectsRestore`, plus the existing `commandCreatedShellCannotReclaimInventory` guard.
- `VERIFIED`: `deathHookCreatesShellForRemnantPlayer` no longer fails on fake-player packet paths.
- `PARTIAL`: full `runGameTestServer` still exits non-zero due to older unrelated failures: `possessedeaterstoresseparatehungerdata`, `vagrantcollisionignorestraversablebutnotimpermeableblocks`, `vagrantvisualsclearwhenstatechanges`, `filledvesselreleasestypedsoulandreturnsemptyvessel`, `possessedhealingrulesparse`, `possesseditemusefindscameracontrolledhost`, `soulvesseldataroundtrips`, `possessedwalkinghostusesvanillagravity`, `possessiondoesnotuseridingandallowssoullessvillagers`, and `possessedoffhanditemuseuseshostoverride`.
- `VERIFIED`: `runClient` reached client startup/resource load and was stopped by command timeout; no active client Java process remained.
- `VERIFIED`: latest `latest.log`/`debug.log` contain no Stage 8 `PlayerShell`, `SoulLifecycle`, `Vagrant`, or Requiem gameplay crash. Observed entries are development refmap/mixin/debug noise, Forge version check, and Realms auth noise.

Manual tests deferred:

- `DEFERRED_MANUAL_REGRESSION`: real multiplayer split/reclaim, logout/reconnect with `DROP_VESSEL_ON_DISCONNECT`, cross-dimension shell/reclaim, real client inventory UI sync, and manual first-death choice -> remnant death -> respawn/shell -> vagrant flow.
- `DEFERRED_MANUAL_REGRESSION`: full XP/game mode/persistent-player-NBT parity and original callback extensibility.

Remaining Stage 8 gaps:

- `PARTIAL`: XP and broad player NBT snapshot parity are not fully ported because Stage 8 only added ownership that was directly confirmed and locally testable.
- `PARTIAL`: sticky effect semantics are covered for the current Forge soulbound effect set, not the full original callback/event system.
- `BLOCKED_BY_UNPORTED_SYSTEM`: full original CCA/callback soulbound extensibility and exact Fabric respawn data plumbing are not present in Forge.

## Stage 9 - Resurrection Lifecycle Parity

Reference audit:

- `VERIFIED`: targeted audit used `requiem-2.0.0-beta.16`, including nested `requiem-core-2.0.0-beta.16.jar`.
- `VERIFIED`: audited original `ResurrectionDataLoader`, `MutableRemnantState`, `MobResurrectable`, resurrection call sites, death/drop callbacks, possessed-host death, `HOST_DEATH`, `POST_RESURRECTION`, consumable lookup, death/drop ordering, and resurrection datapack rules.
- `VERIFIED`: `ResurrectionDataLoader` uses one priority-sorted resurrection rule list. `getNextBody(player[, possessed], damage)` returns the first matching body after checking player, possessed entity, damage, rule requirements, and consumable availability.
- `VERIFIED`: `MobResurrectable` supplies the resurrectable body/possession target behavior used after a matching resurrection rule creates a new body.
- `VERIFIED`: `MutableRemnantState` coordinates remnant/vagrant state changes, respawn preparation, attrition/sticky-effect handling, and the lifecycle handoff used by resurrection paths.

Original ordering:

- `VERIFIED`: original death resurrection is evaluated from `LivingEntityDropCallback` before vanilla `dropInventory`.
- `VERIFIED`: original ordering is `death -> resurrection match -> pre-drop handling -> drop cancellation where applicable -> resurrection body spawn -> possession -> POST_RESURRECTION`.
- `VERIFIED`: Demon resurrection can cancel vanilla body inventory drop; Wanderer/non-demon branches do not collapse to one generic vanilla Totem path.
- `VERIFIED`: `HOST_DEATH` itself does not resurrect a host. It applies attrition or hardcore death behavior to the possessor; resurrection rule evaluation happens separately after that.
- `VERIFIED`: `POST_RESURRECTION` fires only after the new body is spawned and possession succeeds, for post-resurrection criterion/callback behavior.

Forge adaptation:

- `FORGE_ADAPTATION`: exact Fabric `LivingEntityDropCallback` is not ported literally. Forge uses existing managers plus Stage 8 lifecycle interception: `DeathResurrectionManager`, `TotemResurrectionManager`, `SoulLifecycleManager`, and `PossessionManager`.
- `FORGE_ADAPTATION`: `SoulLifecycleManager.onLivingDeath` gives `DeathResurrectionManager.tryHandleRemnantDeath` the first chance for `REMNANT` death. If it returns true, death is cancelled and the Stage 8 shell path is skipped.
- `VERIFIED`: if no resurrection rule matches, the normal Stage 8 fallback remains `REMNANT -> shell + VAGRANT`; this fallback was not replaced.
- `FORGE_ADAPTATION`: possessed host death uses existing `PossessionManager.onHostDeath`: host death criterion/attrition first, then resurrection matching, then generic possession release fallback.
- `FORGE_ADAPTATION`: no Fabric `ConsumableItemEvents.SEARCH` bus was added. Stage 9 checks available host/player hands in Forge code.
- `FORGE_ADAPTATION`: real network players keep teleport/network paths. GameTest fake players with null connections use server-side position fallback to avoid packet-only failures.

Integration details:

- `VERIFIED`: Stage 7 initial choice is not bypassed. Stage 9 runs after first-death choice/suspension handling allows remnant death processing.
- `VERIFIED`: Stage 8 shell creation is bypassed only when resurrection succeeds. No second shell is created on death resurrection.
- `VERIFIED`: normal death resurrection transitions the player to `VAGRANT`, spawns the resurrection body, moves/syncs the player to it, starts possession, and triggers the post-resurrection criterion.
- `VERIFIED`: possessed-host resurrection keeps the possessor in possession by moving control from the dying host to the newly spawned body.
- `VERIFIED`: duplicate same-tick death handling is guarded with persistent tick state so repeated death callbacks do not create a second body, consume a second item, or perform a second possession transition.
- `VERIFIED`: consumable/totem resurrection consumes the first matching item during successful match before final resurrection apply, matching the original timing confirmed in audit.
- `PARTIAL`: original player stat increment and consumption packet behavior are not fully represented as Fabric callbacks; current Forge behavior is the available server-side equivalent.

Ownership and gameplay surface:

| Area | Stage 9 status |
| --- | --- |
| SoulState transitions | `VERIFIED`: successful death resurrection sets `REMNANT -> VAGRANT` and starts possession; non-match keeps Stage 8 `REMNANT -> shell + VAGRANT`. |
| Possession transitions | `VERIFIED`: successful resurrection body possession triggers the existing possession start path and post-resurrection criterion. |
| Shell behavior | `VERIFIED`: no ordinary Stage 8 shell is created when resurrection succeeds; shell fallback remains for non-match. |
| Inventory ownership | `PARTIAL`: Stage 9 avoids duplicating/clearing soul inventory in focused coverage; exact real death/drop client sync remains manual. |
| Soulbound behavior | `PARTIAL`: current Stage 8 Forge soulbound ownership is preserved; full original callback breadth is not ported. |
| Armor/offhand behavior | `PARTIAL`: Stage 8 ownership remains active; exact death/drop UI sync and offhand runtime branches need manual regression. |
| XP behavior | `PARTIAL`: XP parity is not fully ported or verified by Stage 9. |
| Effects/attrition behavior | `VERIFIED`: HOST_DEATH attrition/helper ordering is covered; full original attrition breadth remains partial. |
| Consumable hand semantics | `VERIFIED/PARTIAL`: host/player main/offhand search is covered for available Forge hands; external provider callback parity is not ported. |
| Advancement/criterion behavior | `VERIFIED`: post-resurrection criterion is triggered after spawn and successful possession. |

Automated verification:

- `VERIFIED`: focused Stage 9 GameTests cover death resurrection beating shell creation, duplicate guard, soul-inventory preservation in the death-resurrection adaptation, possessed-host totem consumption/possession transfer, and direct host-death attrition helper behavior.
- `VERIFIED`: focused Stage 9 tests pass.
- `VERIFIED`: latest `runGameTestServer` introduced `0` new Stage 9 failures.
- `PARTIAL`: full `runGameTestServer` still reports the same `10` old unrelated failures from the Stage 8 baseline; they were not debugged as part of Stage 9.
- `VERIFIED`: `compileJava --rerun-tasks` passed with `BUILD SUCCESSFUL`.
- `VERIFIED`: `runClient` reached startup/resource load and remained running until timeout; this was not classified as a crash.
- `VERIFIED`: `run/logs/latest.log` and `run/logs/debug.log` show no Stage 9/Requiem resurrection, possession, or shell gameplay crash after the client run. Observed lines are known dev noise: refmap warning, Netty reflective-access debug noise, Realms auth, Jade/Mixin debug lines, and normal Forge dev output.

Status boundaries:

- `VERIFIED`: resurrection rule lifecycle integration; Stage 8 resurrection-vs-shell ordering; duplicate guard; consumable timing; HOST_DEATH ordering/helper; possessed-host resurrection transition in focused coverage; no new Stage 9 GameTest failures.
- `PARTIAL`: exact player respawn presentation; real death/drop client sync; XP; full NBT breadth; exact external consumable provider parity; full Fabric callback extensibility; real Totem/client animation; exact Wanderer branches not covered by datapack/runtime tests; multiplayer/reconnect/dimension behavior.
- `FORGE_ADAPTATION`: Forge resurrection managers instead of Fabric callbacks; no Fabric `ConsumableItemEvents.SEARCH`; fake-player network guards; Stage 8 lifecycle interception instead of literal Fabric drop callback.
- `MISSING`: full Fabric callback/provider extension surface for consumable search and resurrection events.
- `BLOCKED_BY_UNPORTED_SYSTEM`: exact CCA callback/event surfaces and any original systems not yet ported that feed extended resurrection predicates/providers.
- `DEFERRED_MANUAL_REGRESSION`: actual lethal resurrection, real respawn, actual inventory synchronization, main/offhand consumable in integrated play, possessed host real death, multiplayer, logout/reconnect, cross-dimension, shell interactions, death messages, kill attribution, animation, sound, and particles.

## Stage 10 - First Death Visible Gameplay Pass

Priority shift:

- `VERIFIED`: Stage 10 intentionally focused on player-visible first death behavior rather than deeper resurrection/compatibility internals.
- `VERIFIED`: resurrection was not expanded in this pass.

Original beta.16 audit:

- `VERIFIED`: original `ServerPlayerEntityMixin#suspendDeath` injects at `ServerPlayerEntity.onDeath` `HEAD`. If advancement `requiem:adventure/the_choice` is not done, the world is not hardcore, and `startingRemnantType` is not forced, death is suspended and vanilla death is cancelled.
- `VERIFIED`: original `RevivingDeathSuspender.suspendDeath` sets player health to `1`, sets player invulnerable, grants PAL `INVULNERABLE`, stores the lethal `DamageSource`, sets `lifeTransient=true`, syncs the component, and starts a `20` tick countdown.
- `VERIFIED`: original `ServerPlayerEntityMixin#stopTicking` cancels `playerTick` while `DeathSuspender.isLifeTransient()` is true. The suspended player does not continue normal server-side player ticking.
- `VERIFIED`: original `RevivingDeathSuspender.serverTick` starts Blabber dialogue `requiem:remnant_choice` when the countdown reaches zero.
- `VERIFIED`: original `requiem:remnant_choice` dialogue is an unskippable multi-step first-contact dialogue, not a simple two-button choice. It branches through introduction, explanation, interested/disinterested, final confirmation, then chooses `requiem:remnant` or `requiem:mortal`.
- `VERIFIED`: original `BlabberDialogueScreenMixin` suppresses dialogue rendering until `RequiemClient.worldFreezeFxRenderer().hasFinishedAnimation()` is true.
- `VERIFIED`: original `ZaWorldFx` starts when the synced death suspension component reports `lifeTransient=true`, plays `effect_time_stop` once, expands a radius at `4` units/tick to `100` over `25` ticks, contracts it over another `25` ticks, and reports the animation finished after `60` client ticks.
- `VERIFIED`: original `RemnantChoiceDialogueAction` applies the chosen remnant type with `makeChoice=true`; if the chosen type differs from the current type, it plays `EFFECT_BECOME_REMNANT` at volume `1.4`, pitch `0.1`, and sends the opus-use client packet; then it calls `DeathSuspender.resumeDeath()` if death is still suspended.
- `VERIFIED`: original `RevivingDeathSuspender.resumeDeath` clears invulnerability, sets health to `0`, clears `lifeTransient`, syncs the component, then calls vanilla `onDeath` with the stored lethal source or generic fallback.
- `VERIFIED`: after choosing Mortal, the player remains mortal and resumes vanilla death/respawn.
- `VERIFIED`: after choosing Remnant, the player is marked demon/remnant first, then the resumed vanilla death proceeds through remnant death lifecycle, leading into the existing remnant/vagrant/shell flow.

Forge adaptation implemented:

- `FORGE_ADAPTATION`: added explicit `DeathSuspensionFxPacket` because Forge capabilities here do not auto-sync the suspended state like original CCA.
- `FORGE_ADAPTATION`: `DeathSuspensionManager` now sends suspended FX state on suspension and reconnect, and clears it on resume.
- `FORGE_ADAPTATION`: added `ClientDeathSuspensionFx` as a no-Satin approximation of original `ZaWorldFx`: it plays `effect.time.stop`, keeps a 60-tick transition timer, draws a screen-space freeze wave/overlay, and exposes `hasFinishedAnimation()` to the choice screen.
- `FORGE_ADAPTATION`: added `SoulChoiceFxPacket` for post-choice color pulse feedback.
- `FORGE_ADAPTATION`: added `ModSounds.EFFECT_TIME_STOP` registry entry for the already-present `sounds.json` event.
- `FORGE_ADAPTATION`: replaced the two-button `InitialSoulChoiceScreen` presentation with the original first-contact dialogue flow and disabled visible choices until the freeze animation finishes.
- `FORGE_ADAPTATION`: choosing Remnant from suspended death now plays the original-style low-pitched `effect.become.remnant` sound and client pulse before death resumes.

Current first-death status:

| Surface | Status |
| --- | --- |
| Lethal damage interception | `VERIFIED` by Stage 7 tests and Stage 10 audit; unchanged except FX sync. |
| Suspended player server stability | `PARTIAL`: current Forge freezes health, damage, movement, item use, and invulnerability; original cancels full `playerTick`. Existing Forge behavior is intentionally retained to avoid broad lifecycle risk. |
| 20-tick delay before choice opens | `VERIFIED` by existing Stage 7 tests; unchanged. |
| 60-tick visual gate before choice is usable/visible | `PARTIAL`: implemented as Forge overlay/timer; requires manual visual verification. |
| Original multi-step choice dialogue | `PARTIAL`: state flow and translations match original JSON; UI is Forge custom screen, not Blabber. |
| Mortal choice continuation | `PARTIAL`: lifecycle path unchanged from Stage 7; manual real-client death screen/respawn check still required. |
| Remnant choice continuation | `PARTIAL`: lifecycle path unchanged from Stage 7-9; manual real-client remnant/vagrant/shell check still required. |
| Original Satin post shader parity | `MISSING/PARTIAL`: no Satin dependency was added; Forge uses a screen-space approximation instead of depth-aware world shader. |
| Original Blabber UI parity | `MISSING/PARTIAL`: Blabber is not ported; Forge screen reproduces the dialogue flow and gating only. |

Automated verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after Stage 10 changes.
- `VERIFIED`: `.\gradlew.bat runClient` reached client startup/resource loading and was stopped after timeout; no Stage 10 compile/runtime startup crash was observed.
- `VERIFIED`: latest `latest.log`/`debug.log` contain no Requiem first-death, mixin, render, or network crash from Stage 10 startup. Observed entries are known development refmap/mixin noise, Forge update timeout, missing vanilla goat horn sound warnings, shader sampler warning, and third-party config/version noise.

Manual verification required before promoting visual behavior:

- `DEFERRED_MANUAL_REGRESSION`: first lethal death in a fresh non-hardcore world should freeze the player at 1 health, play the time-stop sound/overlay, then reveal the multi-step first-contact dialogue.
- `DEFERRED_MANUAL_REGRESSION`: while suspended, nearby world activity should continue, but the player should not take more damage or move meaningfully.
- `DEFERRED_MANUAL_REGRESSION`: Mortal path should confirm through the dialogue, resume vanilla death, show/handle the normal respawn path, and leave the player mortal after respawn.
- `DEFERRED_MANUAL_REGRESSION`: Remnant path should confirm through the dialogue, play the remnant acquisition sound/pulse, resume death, and continue into the existing remnant/vagrant/shell gameplay.
- `DEFERRED_MANUAL_REGRESSION`: reconnect during suspended death should restart the freeze/choice presentation without losing the pending choice.

## Immediate next work

1. Manually verify the restored item override lifecycle in singleplayer for zombie, zombie villager, skeleton, and witch diet cases.
2. Manually verify item/block authority cases from the checklist: player-context block placement, bucket use, shield use, bow/crossbow/trident ammo behavior, milk side effect, and active-item cleanup on release.
3. Manually verify `replace_bone` with damaged wither skeleton possession on Easy/Normal/Hard thresholds and confirm possession transfers to the replacement skeleton.
4. Manually verify the Stage 5 Remnant checklist above, especially shell inventory restore, entity interaction denial, hunger/starvation, logout/reconnect, and dimension transitions.
5. Continue with the next full beta.16 -> Forge gap audit rather than assuming a preselected Stage 16.

## Stage 11 - Broad Gameplay/Client Parity Pass

Audited `PORTING_MIXIN_MAP.md`, readable `Requiem-1.20` sources, and local mapped vanilla bytecode for the beta.16 possession gameplay/client hooks. This pass intentionally reused existing Forge possession/soul lifecycle systems instead of adding parallel Fabric-style APIs.

Implemented:

- First-death dialogue choices now render as Blabber-like text choices instead of vanilla button backgrounds.
- First-person possessed shulker no longer renders the shulker head into the local camera.
- Witch possession parity hooks: witches prefer Weakness splash potions against undead targets without Weakness, and possessed witches regain a glass bottle after drinking.
- Mooshroom shearing now transfers possession to the spawned cow before vanilla adds it to the level.
- Pig lightning conversion now transfers possession to the spawned zombified piglin and triggers transformed-possessed criteria.
- Villager lightning conversion now transfers possession to the spawned witch and triggers transformed-possessed criteria.
- Possessed cat/ocelot server AI no longer clears sprint/pose each `customServerAiStep`, matching beta.16 cat-like sprint retention.
- Possessed-body bump detection now calls the existing Forge detection manager with `BUMP` after vanilla entity push returns.

Automated verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after the first-death/shulker/witch/mooshroom changes.
- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed again after pig, villager, cat-like, and bump-detection changes.
- `VERIFIED`: `.\gradlew.bat runClient` applied the new mixins and reached client resource loading without Requiem/mixin startup failure. The observed end-of-run `TextureManager.close` `ConcurrentModificationException` happened during forced shutdown while client resources were still loading and is treated as shutdown noise, not a gameplay hook failure.
- `VERIFIED`: latest log review after the mooshroom target fix showed no `InvalidMixin`, `InjectionError`, or Requiem gameplay crash. Warnings were known dev/runtime noise unless listed above.

Manual verification required:

- First lethal death: choices should appear as plain text options, not vanilla buttons, after the freeze gate.
- Possess a shulker in first person: the head should not block the camera; third-person shulker should still render normally.
- Possessed witch: drinking should leave a glass bottle; hostile witch AI should choose Weakness splash against eligible undead targets.
- Possessed mooshroom sheared with shears should remain possessed as the spawned cow.
- Possessed pig struck by lightning should remain possessed as the spawned zombified piglin.
- Possessed villager struck by lightning should remain possessed as the spawned witch.
- Possessed cat/ocelot should keep sprinting under player control instead of vanilla AI clearing sprint every tick.
- In normal/hard possession detection modes, bumping a hostile monster with a possessed body should provoke the expected single/crowd aggro.

Largest remaining gameplay/client gaps recommended for next broad pass:

1. Full possessed item override parity: `getMaxUseTime`/`finishUsing` for every original override, exact external cure duration, tooltips, and bone-loss component semantics.
2. Entity-specific possessed body behavior still partial: panda roll/sneeze edge cases, fox sleep/item-holding interactions, ravager roar/stun/shield behavior, pig carrot-on-stick boost, horse chest/inventory and full vanilla riding UX.
3. Client possession rendering parity: armor/features/eyes hiding, host/possessor name tag behavior under gamerule toggles, guardian beam visual verification, and night-vision/lightmap/fog exactness.
4. Remnant/vagrant exactness: brain sensors/target predicates, shape-context blocks like powder snow/doors/fluids, death messages/damage attribution, sleep/riding/logout/reconnect edge cases.
5. Movement authority parity: ridden/steerable body UX, skeleton-on-spider jump/dismount feel, block-use and crosshair interaction while camera-controlled. Basic controlled step-up is now implemented.

## Stage 12 - Entity-Specific Bodies + Movement Authority Pass

Reference scope:

- `VERIFIED`: continued from the existing map and Stage 11 notes instead of restarting a full audit.
- `VERIFIED`: compared current Forge systems with readable `Requiem-1.20` sources and local mapped 1.20.1 vanilla bytecode for the relevant beta.16 gameplay hooks.
- `FORGE_ADAPTATION`: current Forge possession remains camera-controlled through `PossessionManager` and packets, not literal vanilla passenger riding for every host.

Implemented:

- `PossessionManager.controlHost` temporarily raises controlled ordinary walking hosts to `maxUpStep >= 1.0F` around the `travel(...)` call, matching the original possessed riding-control step-up behavior without permanently changing mob step height. Direct `move(MoverType.SELF, ...)` is now reserved for special mobility profiles.
- Possessed horse jumps now use `AbstractHorse#getCustomJump` instead of generic mob jump height.
- `AbstractHorseMixin` ports the original undead-horse saddle rule: an undead possessed rider counts as saddled when riding an undead horse.
- `PandaMixin` ports/adapts the panda hooks for 1.20.1: possessed pandas do not stay lying on back, sneak maps to scared state, scared possessed pandas get taller dimensions, and eating animation only starts for held bamboo/cake food.
- `FoxMixin` ports the concrete fox control hooks that apply cleanly to 1.20.1: clear action state on first possessed tick and sync crouching to possessed sneak input.
- `RavagerMixin` ports the client-side ravager horizontal-collision guard so client prediction does not perform ravager griefing from possessed movement.

Already covered and intentionally not duplicated:

- Skeleton/spider mounting, target suppression, attack provocation, movement redirection, and sneak dismount already exist in `SkeletonSpiderRidingEvents` and `PossessionManager.controlledMovementEntity`.
- Pig lightning conversion transfer is already covered by `PigMixin`.
- Cat-like sprint retention is already covered by `CatLikeEntityMixin`.
- Frog, pufferfish, shulker, guardian/evoker-style possessed abilities remain in `PossessionManager`; this pass did not add parallel systems.

Automated verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after the Stage 12 changes.
- `VERIFIED`: `.\gradlew.bat runClient` reached client resource loading and stopped without `InvalidMixin`, `InjectionError`, or Requiem gameplay startup crash. The process ended by timeout/normal shutdown logging.
- `VERIFIED`: `.\gradlew.bat build` passed.

Remaining manual verification:

- Possessed panda: sneak should trigger scared/taller body behavior; non-bamboo/cake held items should not keep the eating animation; possession should clear lying-on-back.
- Possessed fox: sneak should crouch the fox and initial possession should cancel sleep/pounce/faceplant-style action states.
- Possessed ravager: client-side movement into breakable blocks should not visually/client-predict griefing; roar/stun/shield behavior still needs direct play testing.
- Possessed horse/donkey/mule/skeleton horse/zombie horse: jump height should match horse body stats; undead possessed body on undead horse should be controllable as saddled.
- General movement: possessed walking bodies should step up one-block ledges; verify skeleton/stray on spider movement, jump feel, and sneak dismount.

## Stage 14 - Remnant / Vagrant Gameplay Parity Pass

Reference scope:

- `VERIFIED`: continued from the current map and existing Forge `PlayerSoul`, shell, possession, resurrection, vagrant collision, hunger, interaction, and rendering systems; no full project audit was restarted.
- `VERIFIED`: compared beta.16 remnant hooks in readable `Requiem-1.20` sources and local mapped 1.20.1 Forge bytecode.
- `FORGE_ADAPTATION`: current Forge treats the restricted free-soul state as `SoulState.VAGRANT` with no possessed host. Embodied `REMNANT` players remain ordinary physical players, and possessed vagrants are handled through `PossessionManager`.

Missing or noticeably partial mechanics found:

- `MISSING`: vanilla `TargetingConditions#test` still allowed ordinary AI target predicates to consider free vagrant players until later Forge target-change cleanup ran.
- `MISSING`: vanilla `EntitySelector.NO_CREATIVE_OR_SPECTATOR` still treated free vagrant players as normal survival players.
- `MISSING`: 1.20.1 brain nearest-living memory collection still included free vagrant players; Forge `LivingChangeTargetEvent` does not cover all sensor-memory consumers.
- `ALREADY_COVERED`: free-vagrant fall damage/effects, incoming damage suppression, hunger/exhaustion reset, block/entity interaction denial, block breaking denial, item pickup/drop denial, entity push/collision suppression, weighted pressure plates, phase-through-block collision, game event step suppression, possession release state refresh, and target cleanup already existed in `VagrantEvents`, `VagrantRules`, and current mixins.

Implemented:

- Added `VagrantRules.shouldBeIgnoredByOrdinaryTargeting` and `shouldBeIgnoredByOrdinarySensors` as explicit central predicates for free-soul AI visibility.
- Added `TargetingConditionsMixin`, rejecting free vagrant targets at the start of vanilla `TargetingConditions#test`.
- Added `EntitySelectorMixin`, wrapping `EntitySelector.NO_CREATIVE_OR_SPECTATOR` so free vagrants behave like excluded creative/spectator entities for that vanilla predicate.
- Added `NearestLivingEntitySensorMixin`, filtering free vagrants before vanilla writes `NEAREST_LIVING_ENTITIES` and `NEAREST_VISIBLE_LIVING_ENTITIES` brain memories. This is the 1.20.1 Forge equivalent for the beta.16 `NearestPlayersSensorMixin` hook.
- Added focused GameTest coverage for `EntitySelector.NO_CREATIVE_OR_SPECTATOR` and `TargetingConditions` rejecting free vagrants.

Automated verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after adding the mixins.
- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed again after adding the targeted GameTest.
- `VERIFIED`: `.\gradlew.bat runClient` reached client startup/resource loading and exited cleanly with `BUILD SUCCESSFUL`; no `InvalidMixin`, `InjectionError`, or Requiem gameplay startup crash was observed.

Remaining substantial edge cases:

- `PARTIAL`: no original-style include-incorporeal target predicate extension API was ported. Current known Forge gameplay does not need ordinary mobs to opt back into targeting free souls, but special entities like Mortician-style soul hunters should be manually checked.
- `PARTIAL`: shape-context-specific blocks such as powder snow, doors/trapdoors, climbables, fluids, and portals still need manual comparison; this pass did not deepen fog/lightmap/render behavior.
- `PARTIAL`: death messages/damage attribution, sleeping/riding, logout/reconnect while free vagrant, and cross-dimension portal behavior remain manual parity risks.
- `PARTIAL`: the brain sensor hook filters nearest living memories broadly; direct nearest-player-only memories are not present as a separate 1.20.1 Forge class in the mapped jar.

Manual verification required:

- Free vagrant near zombies/skeletons/creepers/spiders should not be acquired as an attack target and should not remain in mob target memory after release from possession.
- Free vagrant near villagers/piglins/warden-like sensor users should not be treated as a normal nearby survival player.
- Free vagrant should still be able to move, phase through non-impermeable blocks, ignore ordinary damage/fall damage, and fail normal block/entity/item interactions as before.
- Possessed vagrant should continue to be controlled through the possessed host; these new free-soul filters should not break host targeting, host damage, or possessed-body combat.

## Stage 15 - Item Override Lifecycle Parity Pass

Reference scope:

- `VERIFIED`: this pass did not restart a full project audit; it continued from the existing item override notes, current Forge `PossessedItemManager`, beta.16 data, and readable original item override classes.
- `VERIFIED`: current Forge already had the important long-use/finish lifecycle hooks: transient `ItemStack#getUseDuration`, `finishUsingItem`, cure long use, healing `use_time`, diet finish filtering, cooldown-after-finish, and external cure.
- `FORGE_ADAPTATION`: the Forge port keeps the current server-authoritative possession and cure manager. It does not reintroduce Fabric registries or CCA components when a gameplay-equivalent Forge path exists.

Missing or noticeably partial mechanics found:

- `MISSING`: item override tooltip feedback was not wired on the Forge client. This included configured cure/diet/healing tooltips, invalid diet strikethrough feedback, legacy schema 0 cure-catalyst tooltip entries, and contextual possession item hints.
- `PARTIAL`: `replace_bone` consumed a bone and healed, but did not persist replaced-bone count or transform replaceable skeleton bodies after the original difficulty thresholds.
- `ALREADY_COVERED`: `getUseDuration`/long use, `finishUsingItem`, interruption behavior, self cure duration, healing cooldown timing, diet blocking, witch harmful-effect filtering, external cure, and host-backed food data were already present in the current pipeline.

Implemented:

- Added `ClientPossessedItemTooltips`, restoring client tooltip feedback while possessing a body.
- `PossessedItemManager` now parses tooltip components from healing/diet/cure JSON and returns priority-ordered tooltip matches with original-style short-circuit/fallback handling.
- Diet fallback tooltips now show strikethrough on nonmatching vanilla food for bodies with diet rules.
- Schema 0 cure catalyst pass-action entries now contribute tooltip feedback for Weakness and long Weakness potions without adding a generic old action registry.
- Restored contextual item hints for ammo generators, crossbow users, skeleton bows, skeleton milk, drowned tridents, and witch water bottles.
- `replace_bone` now stores replaced-bone count in entity persistent data, heals by 4 HP like original, consumes only on success, plays ambient sound for mobs, and transforms `#requiemunofficial:transformation/replaceable_skeletons` after original thresholds: Easy >8, Normal >4, Hard >2, never Peaceful.
- Wither skeleton replacement uses vanilla skeleton conversion, preserves possession through `PossessionManager.transferPossession`, may equip a wither skeleton skull with the original 1/5 chance, and triggers transformed-possessed criteria.

Automated verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after the Stage 15 changes.
- `VERIFIED`: `.\gradlew.bat runClient` reached client startup/resource loading and exited with `BUILD SUCCESSFUL`; no new Requiem mixin/resource startup failure was observed. The OpenAL stream-buffer warning happened during shutdown.

Remaining partial/missing:

- `PARTIAL`: full generic `OldPossessionItemOverride` action registry is still not ported. Current beta.16 data only needed schema 0 pass-action cure catalyst tooltip behavior plus explicit cure/healing/diet implementations.
- `PARTIAL`: `replace_bone` uses entity persistent NBT instead of a dedicated component/capability. Gameplay state persists with the entity, but this is not a public API equivalent to original `SkeletonBoneComponent`.
- `PARTIAL`: external cure remains immediate, matching original `useOnEntity`; the Forge pending cure completion still follows existing Weakness-duration cure flow rather than original CCA internals.

## Full Remaining Gap Audit after Stage 15

Scope: continued from the post-Stage-15 audit state after Remnant/Vagrant. This section does not start Stage 16 and does not implement fixes.

### Real remaining gaps

| System | Classification | Priority | Current Forge state | Missing or divergent beta.16 behavior |
| --- | --- | --- | --- | --- |
| Released souls / body records | `IMPLEMENTED` / `PARTIAL API` | `MEDIUM` | Stage 16 adds `BodyRecordData` world `SavedData`, persistent body record UUIDs on stripped entities, filled-vessel `BodyRecord` NBT, record-aware `ReleasedSoulEntity` body status/merge/teleport-away/invalidation, duplicate active-release prevention, and focused GameTests. Stage 17 reuses the same records for Mortician captured souls. | Forge now matches the beta.16 gameplay loop for normal soul-vessel body records and Mortician-captured normal bodies. Remaining divergence is API shape: no literal CCA `GlobalRecord`/`RecordType`/`EntityPointer` public API; descriptor parity needed for body and rift gameplay is implemented with focused Forge `SavedData` stores. |
| Mortician advanced behavior | `IMPLEMENTED` / `PARTIAL API` | `MEDIUM` | Stage 17 adds `BodyRecordData`-backed captured soul storage, death release through `ReleasedSoulEntity`, duplicate-release guards, invalid-record cleanup, persistent anger target/time, possessed-host retargeting to the possessor, remnant/vagrant-aware first trade preparation, `MoveBackToObeliskGoal`, and local nearest valid obelisk relink within original 20-block range before fade/despawn. Stage 18 adds descriptor UUID persistence for linked obelisks with legacy `homeDimension + homePos` fallback. Existing renderer, sounds, projection flag, base trades, soul steal/free-from-coil goals, spawn, and obelisk activation integration remain in place. | Remaining beta.16 divergence: not a literal `MerchantEntity`/`Angerable`/`GlobalRecord` implementation, no exact `RemnantTradeOffer` subclass/network serialization, no exact trade-side demon customer conversion callback, and soul-speed path preference/effects are not fully ported. Gameplay-important captured-soul lifecycle, anger persistence, descriptor return/relink, and remnant-aware offer surface are now covered. |
| Status effects sticky semantics | `IMPLEMENTED` / `PARTIAL API` | `MEDIUM` | Stage 19 adds Forge-local `StickyMobEffect`, `MobEffectInstance` freeze-duration handling, sticky reapplication with definitive-clear semantics, original-style Attrition max-health/damage/update behavior, Penance merge/cure/possession restrictions, Reclamation split/release cleanup, Emancipation remnant stick/release behavior, and shell/possession lifecycle integration. | Gameplay-important beta.16 sticky/freeze/lifecycle behavior is covered without porting the literal Quilt `StickyStatusEffect` API, CCA effect components, or unrelated visual/component paths. Remaining divergence is API and some broader beta effect lifecycle surfaces, especially exact Penance component warning visuals and original remnant-type abstractions not present in the Forge model. |
| Rift / obelisk record network | `IMPLEMENTED` / `PARTIAL API` | `MEDIUM` | Stage 18 adds `ObeliskRecordData` world `SavedData`, persistent descriptor UUIDs on head `RunestoneBlockEntity`, per-player persistent known rift UUIDs, witnessing on activated Rift use, descriptor-backed Rift menu destinations, server revalidation, selected-record teleport, stale/invalid filtering, same-dimension enforcement, safe arrival, and original distance-based Attrition length. Mortician projected links now persist descriptor UUIDs with legacy position fallback. | Gameplay-important beta.16 descriptor lifecycle is covered without porting the literal CCA `GlobalRecord` API or exact original record/network packet classes. Remaining divergences are UI/feedback/API shape: no original Rift witnessed toast, no generic `RecordPointer` API, no cross-dimension rift travel because original behavior rejects it, and Forge keeps local ritual/menu plumbing. |
| Opus / remnant vessels conversion flow | `PARTIAL` / `STRONGLY WRONG` for direct Opus conversion | `MEDIUM` | Forge `OpusDaemoniumItem` directly toggles Mortal/Remnant for XP. `RemnantVesselItem` also converts and restores bodies pragmatically. Opus is allowed on lecterns. | Original `OpusDemoniumItem#use` only awards use success/stat; conversion is carried by demon soul vessels and related UI/network effects. Original `DemonSoulVesselItem` handles lectern placement, remnant type transition, curing/restoring possessed bodies, dropping inventory when becoming incorporeal, and item exchange. Forge direct Opus conversion changes the item contract. Confirmed by `OpusDemoniumItem`, `DemonSoulVesselItem`, and Forge `OpusDaemoniumItem`/`RemnantVesselItem`. |
| Tachylite ordinary slab/stairs content | `MISSING` | `LOW` | Forge registers base, chiseled, chiseled pillar/slab/stairs, scraped, derelict, runestones, runic blocks. Active assets include ordinary slab/stairs models/blockstates, but recipes/loot are under `.disabled` and blocks/items are not registered. | beta.16 has ordinary `tachylite/tachylite_slab` and `tachylite/tachylite_stairs` recipes, blockstates, models, loot, and tags. This is content parity, not a major gameplay system. |
| Cured mob exact cure components | `PARTIAL` | `MEDIUM` | Forge has cured villager/piglin/piglin brute entity types, renderers, loot, and `TransformationManager` preserving data/equipment/effects/possession transfer for cure/decay/skeletonize paths. | Original uses `CurableEntityComponent`, `DelegatingCurableEntityComponent`, and `CurableZombifiedPiglinComponent`, with cured-state persistence, vanilla conversion initialization, nausea/world event, binding-curse equipment handling, original piglin type persistence, and cured villager default profession name override. Forge covers much of the replacement behavior but not the component API/exact cure metadata. |
| Possession snowflake edge cases | `PARTIAL` | `MEDIUM` | Core possession is implemented by `PossessionManager` with movement, inventory, item override, detection, abilities, rendering, and many vanilla/Alex's Mobs adaptations. | Remaining risks are exact action semantics, sleep/wake, block-use/projectile ownership, AI suppression, target/detection edge cases, riding, and entity-specific snowflake behavior. Do not classify core possession as missing. |
| Multiplayer/reconnect/dimension parity | `PARTIAL` | `MEDIUM` | Stage 8-10 lifecycle logic has fake-player guards and shell/death/resurrection paths. Sync packets exist for soul, possession, vagrant, name tags, choice, and visuals. | Real integrated/server multiplayer reconnect, logout-created shells/vessels, cross-dimension shell/reclaim, rift travel, real death/drop UI sync, late-join nametag/shader/HUD state, and death message attribution remain manual regression risks. |
| Networking/UI parity | `PARTIAL` | `MEDIUM` | Forge has `SyncSoulDataPacket`, possession input/action/fx/name-tag packets, vagrant input/sense packets, first-death choice packets, and Forge menu-button handling for rift actions. | Original network also has anchor damage pulse, body-cure particle packet, generic sub-data sync, opus-use floating item/pulse packet, ethereal animation packet, obelisk power update packet, rift witnessed toast, open crafting packet, indirect ability packet, and descriptor-based use-rift packet. Some are replaced by Forge systems, but not all original client feedback/sync semantics exist. Confirmed by `RequiemNetworking`, `ClientMessageHandler`, `ServerMessageHandling`, and Forge `ModNetwork`. |
| HUD/client rendering and shaders | `PARTIAL` | `MEDIUM` | Forge has possession/vagrant HUD suppression, possession hotbar/body health, target hint, first-death FX, vagrant phase/sense visuals, entity renderers, runestone renderer, post shaders, particles, and name-tag hooks. | Exact original HUD/render path is not fully claimed: original Blabber dialogue integration, Rift witnessed toast, body-cure packet particles, obelisk power network updates, Satin/depth-aware shader behavior, and some armor/features/eyes/body-type visuals remain manual parity risks. |
| Dialogue / first-death UX | `FORGE_ADAPTATION` | `LOW` | Forge implements first-death suspension, custom `InitialSoulChoiceScreen`, translations, visual gating, and choice packets. | Original uses Blabber dialogue resources/screens and a multi-step dialogue flow. Forge translations preserve much of the text, but the Blabber screen/system is not ported. This is intentional adaptation unless exact dialogue UX is required. |
| Compatibility modules | `PARTIAL` | `LOW`/`MEDIUM` | Forge has JEI info, Origins soul-type sync fallback, Alex's Mobs possession adaptations, and broad tag/data-driven behavior. | Original optional compat classes/mixins for Origins, Trinkets, Bewitchment, Bumblezone, BetterGraves, Additional Entity Attributes, Malum, Golems Galore, Eldritch Mobs, Snow Mercy, and Mod Menu are not broadly ported. Impact depends on installed dependencies; not core beta.16 parity unless those mods are in scope. |
| Advancement/criteria surface | `ALREADY COVERED` / `PARTIAL` | `LOW` | Active Forge advancement JSON mirrors the beta set under `requiemunofficial`, and criteria exist for remnant choice, possession, death after possession, possessed hit, transformation, and resurrection. | Original also registers `used_totem`; Forge has resurrection/totem managers but no matching custom criterion. No active beta advancement in this tree consumes it, so this is API/datapack parity rather than a visible advancement gap. |

### Already covered or Forge adaptations

- `ALREADY COVERED`: resurrection manager integration, death-resurrection before shell fallback, possessed-host resurrection transfer, post-resurrection criterion, core shell split/reclaim anti-duplication, soulbound inventory/effect ownership, sticky effect stick/freeze/reapply semantics, data-driven item diets/healing/cure, long-use item lifecycle, contextual item tooltips, slot locks, bump/attack detection, replace-bone gameplay, core Remnant/Vagrant interaction blocking/targeting/brain sensor/fall/noPhysics/hunger/shell behavior.
- `ALREADY COVERED`: gamerules for show possessor name tag, disable cure, starting remnant type, possession keep inventory, possession detection, and drop vessel on disconnect are registered and used. Commands for soul/remnant/vagrant/possession/shell query and mutation are present.
- `FORGE_ADAPTATION`: Forge capabilities/data managers replace CCA/Origins/PlayerAbilityLib; Forge network packets replace Fabric packet channels; Forge gamerules use integer modes instead of Fabric enum rules; Forge obelisk/rift identity uses focused `SavedData` descriptors plus block entity state instead of literal CCA `GlobalRecord`; Forge rituals are menu/button driven with stored soul energy; command permissions use vanilla permission level rather than Fabric permissions API; Forge initial-choice screen replaces Blabber dialogue.
- `NOT_NEEDED`: beta.16 `human_flesh` model is asset residue and is not registered in beta readable item/content registration. Fabric/Satin/Blabber APIs are not literal Forge dependencies; current Forge screens/shaders are pragmatic adaptations unless exact UI/shader parity is explicitly required.

### Largest remaining tasks

1. `MEDIUM`: exact Mortician trade/API internals. Only needed if literal beta API/network parity matters: exact `RemnantTradeOffer` serialization, exact exorcism conversion callback, and soul-speed path preference.
2. `MEDIUM`: Opus/remnant vessel contract. Remove or reclassify direct Opus conversion if beta item semantics matter; align conversion side effects with `DemonSoulVesselItem`. Depends on first-death choice, soul state, possession cure/body restore.
3. `MEDIUM`: cured mob exactness. Fill remaining component-level behavior: cured metadata, piglin original type persistence, villager display name, vanilla conversion event parity. Depends on `TransformationManager` and possession transfer.
4. `MEDIUM`: multiplayer/reconnect/dimension regression. Validate real clients for shell reclaim, possession, rifts, logout vessels, name tags, HUD/shaders, and death messages. Depends on network sync and world/dimension lifecycle.
5. `MEDIUM`: client/network feedback parity. Decide which original packets/visuals matter: body cure particles, Opus-use floating item/pulse, obelisk power sync, rift witnessed toast, anchor damage pulse, crafting/ability packets. Depends on current Forge screens, particles, shaders, and menu handling.
6. `MEDIUM`: generalize Stage 16/18 records if exact beta public record API semantics become necessary. Current `BodyRecordData` and `ObeliskRecordData` intentionally implement the gameplay identity layers first, not a full public `GlobalRecord` clone.
7. `LOW`/`MEDIUM`: optional compatibility modules. Port only if the target modpack includes the relevant dependencies. Depends on installed mods and replacement Forge APIs.
8. `LOW`: ordinary tachylite slab/stairs active registration/data. Existing assets are mostly present but disabled/unregistered. Depends on block/item registration, loot, recipes, tags, and lang key namespace cleanup.

## Recovery pass - possession/vagrant regressions

Scope covered after the interrupted 2026-09-26 GameTest rerun:

- `FIXED`: survival possession interaction now reaches `PossessionManager` before the broad survival Vagrant interaction cancel path in `VagrantEvents`.
- `FIXED`: `/requiem mortal` is registered and uses the existing mortal lifecycle cleanup path.
- `FIXED`: `/requiem vagrant set true` and `set false` no longer release an already possessed host just to confirm the vagrant flag.
- `FIXED`: possessed ordinary walking hosts keep vanilla gravity instead of applying `entity_mobility.json` custom gravity; special flight/swim/mobility profiles still use custom gravity.
- `FIXED`: gameplay debug HUD is hidden unless `-Drequiemunofficial.debugHud=true` is present.
- `FIXED`: vagrant wall-phasing collision, input, and visual FX now require sustained movement into a phaseable wall before `PHASE_REQUIRED_TICKS` grants collision suppression. Releasing the attempt resets charge.
- `FIXED/PARTIAL`: first-person possessed biped host arms now route through `PlayerRenderer.renderRightHand`/`renderLeftHand` injections and render the possessed host's humanoid model arm. The old yaw-dependent `RenderHandEvent` pose-stack offset is no longer used; `RenderHandEvent` only cancels free-vagrant hands. Held item screen-space stability still needs live manual verification.
- `FIXED/PARTIAL`: possessed host rotation is camera-authoritative (`camera host -> packet -> server host/player`), and old rotation interpolation fields are no longer force-overwritten. Needs real-client mouse/camera manual testing.
- `PARTIAL / NEEDS MANUAL`: zombie arm jitter should benefit from the rotation/interpolation authority fix, but must be verified manually with a zombie holding an item.
- `FIXED/PARTIAL`: wall-phasing particles now start only after the charge gate or while already inside a phaseable block. The visual was switched from vanilla `SOUL` to explicit small dust particles (`scale=0.35F`) so size is intentionally reduced versus the old large ghost-particle style. Manual visual parity is still recommended.
- `PARTIAL / NEEDS MANUAL`: `PlayerShellEntity` now drops captured body inventory through death loot once and clears it afterward, preventing duplication. The broader player death/body lifecycle intentionally still creates shells for legitimate remnant death paths and must be manually checked against the exact beta.16 scenario where the observed body should die and items should drop.
- `FIXED`: possessed host armor remains authoritative on the host, preserving rendering, attributes, durability, and zombie helmet sunlight protection. Hand slots keep the existing item-use transfer path.
- `FIXED/PARTIAL`: possessed Blaze attacks now use a server-side vanilla-like charge/delay/burst/cooldown state machine and the real Blaze charged flag through `BlazeAccessor`. Needs manual timing check for the three-shot burst.
- `FIXED`: Mortician duplicate trades are deduped by full trade identity after fixed offer population and before player-specific replacement.

Verification:

- `VERIFIED`: `.\gradlew.bat runGameTestServer` completed 137 tests after the walking-gravity fix. `possessedwalkinghostusesvanillagravity` no longer appears in the failure list.
- `VERIFIED`: the new focused regression tests for command/mortal, survival possession, vagrant set without release, shell captured inventory death/drop, armor on host, and Blaze charged state did not appear in the failure list.
- `VERIFIED`: remaining GameTest failures are the older baseline set: `possessedeaterstoresseparatehungerdata`, `vagrantcollisionignorestraversablebutnotimpermeableblocks`, `vagrantvisualsclearwhenstatechanges`, `possessedhealingrulesparse`, `filledvesselreleasestypedsoulandreturnsemptyvessel`, `possesseditemusefindscameracontrolledhost`, `soulvesseldataroundtrips`, `possessiondoesnotuseridingandallowssoullessvillagers`, and `possessedoffhanditemuseuseshostoverride`.
- `VERIFIED`: `.\gradlew.bat runClient` reached client startup/resource loading and exited with `BUILD SUCCESSFUL` on 2026-09-26. Fresh logs contain no crash, `InvalidMixin`, `InjectionError`, Blaze accessor failure, packet error, renderer error, or `RenderHandEvent` subscription failure. Observed matches were normal renderer atlas lines, one vanilla shader sampler warning, and Realms auth noise.

## Stage 16 - Body Records + Released Soul Lifecycle

Original beta.16 contract audited:

- Normal empty-vessel capture calls `EntityPositionClerk.get(target).getOrCreateRecord()`, stores the record UUID in the filled vessel soul fragment, then removes the target body's soul.
- `ReleasedSoulEntity` stores `owner_record`, resolves that record's `ENTITY_REF`, tracks `BODY_STATUS` as found/missing/isekai, flies toward loaded nearby bodies, merges on body collision, and uses special teleport/merge/expiry status effects.
- Merge and teleport-away do not directly carry a body reference through the item. They mark the global record with `RELEASED_SOUL`; `EntityRecordUpdateCallback` later gives the `SoulHolderComponent` soul back and invalidates the record when the body is present again.
- Missing owner records are invalidated on expiry. Unloaded/wrong-dimension/far bodies are treated as isekai so the soul can teleport/relink instead of deleting the body record.
- Aggregate soul captures deliberately store a random UUID/no linked body because there is no single body to restore.

Forge equivalent implemented:

- `common.record.BodyRecordData` is an overworld `SavedData` store keyed by record UUID. It persists entity UUID, dimension, entity type, last position, invalid flag, active released-soul flag, and pending released-soul flag.
- Soul-stripped living entities carry `RequiemBodyRecord` in persistent entity NBT. `EntitySoulManager.removeSoul` creates the record; living tick keeps the pointer fresh; death/removal invalidates the record without treating unloaded chunks as death.
- `SoulVesselItem` now writes `BodyRecord` into normal filled soul vessels and preserves legacy/aggregate vessels without a body record.
- `FilledSoulVesselItem` releases record-aware souls, prevents a second active release for the same record, and still supports direct body restore. If a vessel has a body record, direct restore requires the same recorded body, not just a matching entity type.
- `ReleasedSoulEntity` now saves/loads `OwnerRecord`, computes found/isekai/missing body status, flies toward recorded loaded bodies, merges by restoring the soulless body and invalidating the record, teleports away by setting pending relink, and invalidates truly missing records.

Focused tests added:

- `bodyRecordCreatedWhenSoulRemoved`
- `bodyRecordDataSerializesRecordState`
- `releasedSoulMergesWithRecordedBody`
- `releasedSoulInvalidatesMissingBodyRecord`
- `filledVesselPreventsDuplicateReleasedSoulForRecord`

Verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passes.
- `VERIFIED`: `.\gradlew.bat runClient` reaches client startup and exits `BUILD SUCCESSFUL`.
- `PARTIAL`: `.\gradlew.bat runGameTestServer` still exits non-zero, but Stage 16 tests pass. Remaining failures match the existing baseline set: `possessedeaterstoresseparatehungerdata`, `vagrantcollisionignorestraversablebutnotimpermeableblocks`, `vagrantvisualsclearwhenstatechanges`, `possessedhealingrulesparse`, `filledvesselreleasestypedsoulandreturnsemptyvessel`, `possesseditemusefindscameracontrolledhost`, `soulvesseldataroundtrips`, `possessedwalkinghostusesvanillagravity`, `possessiondoesnotuseridingandallowssoullessvillagers`, and `possessedoffhanditemuseuseshostoverride`.

Manual verification required:

- Possess a zombie/zombie villager/zombified piglin with Weakness: cure reagent should take 48 ticks, consume on completion only, start pending cure, and not consume if interrupted.

## Stage 17 - Mortician Behavior Parity

Original beta.16 contract audited:

- `MorticianEntity` extends `MerchantEntity`, implements `Angerable`, keeps a `captured_souls` UUID list, and releases each captured soul on death through `FilledSoulVesselItem.releaseSoul`.
- Soul steal creates/keeps the target body `GlobalRecord` before stripping the soul, so captured souls retain identity and can merge with the exact body later.
- Anger is persistent for 20-39 seconds, targets survival-like players even when incorporeal targeting predicates are needed, retargets possessed hosts to their possessor, and is not forgiven by players.
- Trades are fixed from the `TRADES` pool. The first offer is remnant-aware: normal customers pay gold/netherite for a sealed remnant vessel; demon/remnant customers see an empty-vessel/netherite variant; vagrant customers have the offer temporarily disabled. A successful exorcism trade converts the demon/remnant customer back to mortal.
- Linked projected Morticians point back to a rift obelisk record. If that link becomes invalid, they try to relink to the nearest valid rift obelisk within 20 blocks, otherwise fade/despawn. `MoveBackToObeliskGoal` pulls linked Morticians back when farther than 16 blocks from home.
- Projection visuals, spell particles/sounds, no far despawn, undead group/path preferences, soul-speed favoritism, custom interaction flow, and merchant screen cleanup are Mortician-specific hooks.

Forge equivalent implemented:

- `MorticianEntity` now owns a persistent `captured_souls` list of `BodyRecordData` UUIDs. `StealSoulGoal` uses `SoulVesselItem.tryStealSoulToRecord`, so normal stolen souls are body-record-backed instead of anonymous `SoulAmount`.
- Captured souls release through `ReleasedSoulEntity` on Mortician death. Release marks the body record `activeReleasedSoul`, removes the UUID from Mortician storage, skips missing/invalid records, and rejects duplicate release attempts.
- Anger target/time now save to NBT, attacking players start persistent anger, anger ticks down server-side, and possessed hosts are retargeted to their possessor when possible.
- The first trade is prepared per customer for remnant/vagrant state, preserving the existing fixed trade list while restoring the original remnant-aware offer surface.
- `MoveBackToObeliskGoal` was ported against the current `homeDimension + homePos` link. Projected Morticians now relink to the nearest valid local rift obelisk within 20 blocks before fading out.

Focused tests added:

- `morticianCapturedSoulStoresBodyRecordIdentity`
- `morticianDeathReleasesCapturedBodyRecordSoul`
- `morticianCapturedSoulReleasePreventsDuplicates`
- `morticianCapturedSoulReleaseDropsInvalidRecord`
- `morticianAngerPersistsAttackingPlayer`
- `morticianRelinksNearbyValidObelisk`

Verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passes after Stage 17.
- `PARTIAL`: `.\gradlew.bat runGameTestServer` ran 120 tests. Stage 17 tests did not appear in the failure list. The remaining 10 required failures match the existing baseline areas: `possessedeaterstoresseparatehungerdata`, `vagrantcollisionignorestraversablebutnotimpermeableblocks`, `vagrantvisualsclearwhenstatechanges`, `possessedhealingrulesparse`, `filledvesselreleasestypedsoulandreturnsemptyvessel`, `possesseditemusefindscameracontrolledhost`, `soulvesseldataroundtrips`, `possessedwalkinghostusesvanillagravity`, `possessiondoesnotuseridingandallowssoullessvillagers`, and `possessedoffhanditemuseuseshostoverride`.
- `VERIFIED`: `.\gradlew.bat runClient` reaches client startup/resource loading and exits `BUILD SUCCESSFUL`.

Remaining Stage 17 divergences:

- Forge still uses `WanderingTrader`/Forge entity plumbing instead of literal Fabric `MerchantEntity` + `Angerable` APIs.
- Stage 17 initially used `homeDimension + homePos`; Stage 18 replaces projected Mortician link identity with obelisk descriptor UUIDs while preserving legacy position fallback.
- Exact `RemnantTradeOffer` NBT/network representation and the beta exorcism trade-side conversion callback are not fully ported; Forge currently updates the visible first offer by customer state.
- Soul-speed path preference/effects and exact merchant screen cleanup are not claimed as exact parity.

## Stage 18 - Rift / Obelisk Descriptor Parity

Original beta.16 contract audited:

- Rift obelisks are descriptor records, not anonymous runtime points. The active rift has a stable record UUID, descriptor metadata, and a source block/entity reference.
- Players learn rifts by witnessing activated rifts. Their known rift list persists by record UUID and is filtered through the current descriptor state.
- The Rift UI lists known descriptors and travel targets a selected record. The server validates that selected descriptor again before teleporting.
- Teleport does not use cross-dimension destinations in this original path. Unloaded or stale descriptors are rejected or filtered instead of blindly using nearest runtime coordinates.
- Teleport applies distance-based Attrition: base 15 minutes plus a logarithmic distance penalty equivalent to `int(log(distance) / log(10^0.003) * 6)` ticks, with Java float/int truncation behavior preserved.
- Projected Morticians link back to an obelisk descriptor, with local position data as a practical fallback.

Forge equivalent implemented:

- `common.record.ObeliskRecordData` is an overworld `SavedData` store keyed by descriptor UUID. It persists dimension, head position, size, optional name, and invalid state.
- Head `RunestoneBlockEntity` stores `obelisk_record_id`, creates/updates descriptors when a valid powered rift structure is detected, and invalidates its descriptor when structure data is cleared.
- `PlayerSoul` persists `KnownRifts`; `PlayerRiftTracker` adds witnessed activated rifts and returns only currently valid known descriptors, pruning invalid records.
- `RiftMenu` serializes known descriptors to the client, `RiftScreen` exposes destination selection, and server-side button handling rebuilds the same filtered destination list before teleporting.
- `RiftTeleportManager` resolves selected record UUIDs, rejects self/cross-dimension/stale/unloaded invalid targets, finds a safe arrival position, consumes energy, teleports to the selected descriptor, and applies original distance Attrition length.
- `MorticianEntity` saves `linked_obelisk_record`, validates that descriptor when projected, updates legacy home dimension/position from the descriptor, and relinks nearby valid rifts within the original 20-block range when needed.

Focused tests added:

- `obeliskDescriptorCreationAndSerialization`
- `obeliskDescriptorDuplicatePrevention`
- `obeliskDescriptorInvalidation`
- `knownRiftDestinationsFilterInvalidRecords`
- `selectedRiftDestinationResolvesChosenObelisk`
- `riftAttritionLengthMatchesOriginalFormula`
- `morticianStoresDescriptorLinkWhenLinked`

Verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passes after the final filtered-destination server index fix.
- `VERIFIED`: Stage 18 GameTests pass in `.\gradlew.bat runGameTestServer`; remaining 10 failures are the known pre-Stage-18 baseline failures.
- `VERIFIED`: `.\gradlew.bat runClient` startup/shutdown smoke passed before the final documentation pass, with no new crash or mixin errors reported.

Remaining Stage 18 divergences:

- Forge implements the gameplay descriptor layer with focused `SavedData`, not the literal beta.16 CCA `GlobalRecord`/`RecordPointer` public API.
- Original Rift witnessed toast/client packet feedback is not ported; current Forge learning is server-side persistent state plus menu visibility.
- Cross-dimension rift travel remains intentionally rejected according to the checked original behavior.
- Forge Rift UI is the existing menu/screen adaptation, not a literal beta `RiftScreenHandler`/client screen clone.
- Use Weakness and long Weakness potions while possessing a curable undead: tooltip should show Cure Catalyst while vanilla potion behavior remains pass-through.
- Possess a witch: valid witch diet food should be edible, harmful effects from food should be reverted, and invalid vanilla food should be blocked with strikethrough diet tooltip.
- Possess damaged zombie/drowned/fox/supported Alex's Mobs bodies: configured healing items should use their JSON `use_time`, heal on finish, and apply cooldown after finish.
- Possess a damaged wither skeleton and use bones repeatedly: it should heal by 4 HP, increment replacement count, transform into a skeleton after the difficulty threshold, keep possession on the replacement, and potentially equip a wither skeleton skull.
- Check contextual tooltips while possessing skeleton/drowned/witch/crossbow-capable bodies: bow, crossbow, milk, trident, and water bottle hints should appear.

## Stage 19 - Sticky / Status Effect Semantics

Original beta.16 contract audited:

- `StickyStatusEffect` is an effect contract, not a separate ownership system. It exposes `shouldStick(entity)` and `shouldFreezeDuration(entity)`, with the static helper requiring both an active sticky effect and either remaining duration or frozen duration.
- Duration freeze happens in `StatusEffectInstance` ticking, independently from shell/effect ownership transfer.
- Attrition, Penance, and Emancipation are sticky only for the relevant soul states. Reclamation is not a generic soul-owned sticky effect; it freezes while applicable and is explicitly cleared during split/dissociation cleanup.
- Attrition stacks through `addAttrition`, preserves the longer duration, clamps its modifier level, reduces max health, clamps current health, and can use the special hardcore-death damage source when exceeding the maximum level.
- Penance behavior is lifecycle-driven: level II+ blocks player merge/reclaim and can split embodied remnants, level III+ blocks possession/release paths, and any Penance denies possessed-body cure where the original callback does so.
- Reclamation reduces Attrition on expiry and clears during dissociation/split rather than following ordinary sticky ownership.
- The original reapplicator restores sticky effects after ordinary removal, while effect code can mark a removal as definitive so it is not immediately re-added.

Forge equivalent implemented:

- `common.effect.StickyMobEffect` mirrors the original sticky/freeze contract and is used by existing Forge lifecycle code instead of adding a second event/ownership system.
- `MobEffectInstanceMixin` freezes duration by incrementing duration immediately before vanilla `tickDownDuration()` when `shouldFreezeDuration(entity)` is true.
- `StickyEffectReapplicationManager` re-adds sticky effects after ordinary removal and supports `definitivelyClear` for intentional clears such as Attrition reduction.
- Stage 8 effect ownership now delegates to `StickyMobEffect.shouldStick(effect, player)` for the soul-owned effect decision instead of treating all four rune effects as a static soul-owned set.
- Attrition now has original-style sticky/freeze semantics, max-health modifier behavior, health clamp on application, special attrition hardcore-death damage source, longer-duration preservation for Rift Attrition, possession-host freeze behavior, and host-death/release integration.
- Penance now uses the Forge `REMNANT`/`VAGRANT` model for sticky/lifecycle semantics: merge/reclaim denial at level II+, possessed-body cure denial, embodied remnant split behavior, and possession/release restrictions at level III+.
- Reclamation is no longer treated as generic sticky-owned. It freezes in the original vagrant case, reduces Attrition on expiry, and is cleared on split/release cleanup.
- Emancipation now sticks for remnant state, releases possession on tick when applicable, and does not incorrectly freeze duration outside the confirmed original rules.

Focused tests added:

- `stage19StickyRemovalReappliesForVagrantSoul`
- `stage19StickyDurationFreezesForVagrantSoul`
- `stage19ReclamationClearsOnSoulBodySplit`
- `stage19PenanceBlocksMergeAndCure`
- `stage19AttritionStacksAndKeepsLongerRiftDuration`
- `stage19AttritionMaxHealthModifierClampsHealth`

Verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passed after the Stage 19 changes.
- `VERIFIED`: Stage 19 GameTests pass in `.\gradlew.bat runGameTestServer`; the remaining 10 failures are the known pre-Stage-19 baseline failures.
- `VERIFIED`: `.\gradlew.bat runClient` reached client startup/resource loading and then shut down normally. `run/logs/latest.log` for the 2026-09-23 23:22 run contains no Stage 19 mixin errors, injection errors, effect registration errors, Requiem runtime exception, or crash. The only matched exception line is the usual Realms auth warning.

Remaining Stage 19 divergences:

- Forge exposes a local `StickyMobEffect` contract, not the literal Quilt `StickyStatusEffect` API.
- Forge still uses local capabilities/managers instead of beta.16 CCA components such as the exact Penance component data/sync surface.
- No unconfirmed damage path was added for beta.16 remnant-type abstractions that do not exist as separate Forge concepts.
- Reclamation's gameplay cleanup/reduction semantics are covered, but the implementation follows current Forge lifecycle hooks rather than copying the exact original callback bus.

Manual verification required:

- As a vagrant, remove Attrition/Penance/Emancipation through ordinary effect removal and confirm the effects reappear only when their sticky state still applies.
- Confirm Attrition duration does not tick down while vagrant or while held on a possessed host, but resumes when the freeze condition no longer applies.
- Confirm Reclamation clears during split/release cleanup and does not remain as a generic soul-owned sticky effect.
- Confirm Penance II blocks shell reclaim/merge and denies cure; confirm Penance III blocks possession/release paths expected by the current Forge model.
- Confirm long-distance Rift Attrition keeps its longer duration when a shorter Attrition application follows.

## Regression Pass 2 Continuation - Death, Possession, Equipment, Render, and Particles

Context:

- The pass resumed after a failed conversation compaction. It did not restart the broader regression sweep.
- `compileJava --rerun-tasks` had already passed twice before resume; it was run again after the Mortician fake-player guard and passed.
- The interrupted `runGameTestServer` result was recovered from `run/logs`: it completed 140 tests with 10 required failures. The extra failure over the expected 9-failure baseline was `morticianoffersdonotgrowacrossopenandload`, caused by the test using a synthetic `ServerPlayer` with no network connection.

Implementation notes:

- Camera/mouse possession rotation now treats the local `minecraft.player` as the mouse yaw/pitch authority, applies that rotation immediately to the local possessed camera host, and sends player yaw/pitch to the server for host/player synchronization. This avoids the previous host-to-player rotation copy that fought vanilla `LocalPlayer.turn(...)`.
- Normal Remnant death no longer creates a `PlayerShellEntity` or cancels vanilla death through `becomeVagrantOnDeath()`. Resurrection rules may still intercept death, while ordinary Remnant death proceeds through vanilla drops/XP/death and transitions soul state through clone/respawn lifecycle.
- Possession equipment now bridges the possessor inventory to the host's real vanilla equipment slots. `PossessionInventoryManager.syncPlayerEquipmentToHost` keeps host equipment visible to renderer, attributes, and mob logic such as zombie sunlight helmet checks. The bridge uses the player stack object for live host equipment rather than maintaining an independent authoritative copy.
- Mortician offers are canonicalized from `getOffers()` so inherited `WanderingTrader` initialization, persisted old offers, repeated GUI opens, and player-specific first-offer replacement do not append duplicates. `mobInteract` now returns successfully after preparing offers for synthetic `ServerPlayer` instances with `connection == null`, because vanilla merchant GUI packet sending cannot work for those test players.
- Possession jump now routes ordinary ground jumps through vanilla `LivingEntity.jumpFromGround()` via the invoker path; special/custom mobility remains outside that path where required.
- Possessed generic zombie attacks no longer restart vanilla swing with an extra `host.swing(...)` after `doHurtTarget()`, and the humanoid zombie cooldown path was adjusted.
- Local hidden possessed player render suppression now includes feature/layer rendering through `LivingEntityRendererMixin`, covering foreign render layers such as Citadel without hardcoded mod blacklists.
- Phasing visuals use the existing `ModParticles.GHOST` particle with original-style gaussian spread/movement; size is reduced in `SimpleGhostParticle` instead of replacing the particle type with dust.

Focused verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passes after the Mortician fake-player guard.
- `VERIFIED/PARTIAL`: `.\gradlew.bat runGameTestServer` completes 140 tests and returns to the expected 9 required baseline failures. The Mortician regression test no longer fails.
- `VERIFIED/PARTIAL`: `.\gradlew.bat runClient` starts the client through resource loading/main-menu startup and is stopped by the command timeout. Fresh `latest.log`/`debug.log` contain no crash, `InvalidMixin`, `InjectionError`, Requiem packet errors, renderer errors, or equipment synchronization errors. The only exception match is the usual Realms auth warning; the shader sampler warning is a vanilla/dev warning.

Current required GameTest baseline failures:

- `possessedeaterstoresseparatehungerdata`
- `vagrantcollisionignorestraversablebutnotimpermeableblocks`
- `vagrantvisualsclearwhenstatechanges`
- `filledvesselreleasestypedsoulandreturnsemptyvessel`
- `possessedhealingrulesparse`
- `possesseditemusefindscameracontrolledhost`
- `soulvesseldataroundtrips`
- `possessiondoesnotuseridingandallowssoullessvillagers`
- `possessedoffhanditemuseuseshostoverride`

Status by requested issue:

- Camera/mouse: `NEEDS MANUAL`. Automatic coverage is compile/log smoke only; actual mouse camera authority must be checked in a live client.
- Death body: `FIXED` for focused automated behavior: normal Remnant death no longer creates a shell or cancels vanilla death. Manual gameplay check still recommended.
- Death XP: `FIXED` for focused automated responsibility: XP is left to vanilla death lifecycle. Manual gameplay check still recommended.
- Armor/equipment: `FIXED` for focused automated behavior: possessed host equipment is real vanilla equipment and anatomy restrictions prevent unsupported armor transfer. Manual render/attribute/sunlight checks still recommended.
- Mortician: `FIXED` for automatic coverage: canonical offers stay at 8 across repeated interaction/load, including fake-player GameTest interaction.
- Jump: `FIXED` for focused automated zombie/vanilla jump behavior. Manual body-by-body movement feel still recommended.
- Zombie arm: `NEEDS MANUAL`. The duplicate swing restart root cause is removed, but visual jitter needs live observation.
- Citadel render layer: `NEEDS MANUAL`. Client starts with Citadel and mixins apply without error, but hidden-layer visual suppression requires live possessed-player observation.
- Phasing particles: `NEEDS MANUAL`. Particle type/path compiles and client starts; visual density/size needs live observation.

## Possession Architecture Rewrite Continuation - Vanilla Walking Authority

Context:

- This pass resumed from the interrupted 2026-09-27 `runGameTestServer`; it did not restart the possession rewrite.
- The interrupted run was recovered from `run/logs/latest.log`: 147 tests completed with 15 required failures.
- The extra failures over the expected 9-failure baseline were movement/controller regressions from routing normal walking through `travel(...)` while the soulless host was still `NoAI=true`.

Architecture now used:

- `INPUT -> HOST -> VANILLA TRAVEL/PHYSICS`: `PossessionInputPacket` stores possessor movement/rotation input, `PossessionManager.controlHost` copies that input into `xxa`/`yya`/`zza`, applies one host rotation authority, and ordinary walking hosts call `host.travel(...)`. Vanilla movement now owns gravity, friction, collision, step-up, falling, `fallDistance`, and jump lifecycle.
- `PLAYER INVENTORY -> HOST EQUIPMENT -> RENDER`: possessor equipment remains mirrored through `PossessionInventoryManager` using the existing last-synced guard; the host's real equipment remains the renderer/gameplay authority.
- `CAMERA ROTATION -> HOST ROTATION`: the possessor camera/input yaw and pitch are sent to the server and applied to the host/head/body rotation rather than feeding host rotation back into client old-rotation fields.

Implementation notes:

- Normal walking possession no longer uses the old competing runtime path of `travel(); if no displacement -> move(...)`.
- Normal walking possession no longer applies separate manual gravity or manual Y correction.
- Soulless mobs may still be made inert by `EntitySoulManager.removeSoul(...)`, but possession start/transfer now saves their original `NoAI` state and sets `NoAI=false` while controlled so `Mob.travel(...)` enters vanilla physics. Release restores the saved value.
- AI suppression during control is targeted to autonomous target/navigation state rather than disabling the whole entity simulation. This keeps vanilla movement lifecycle available while preventing the mob from pathing against player input.
- Special mobility profiles remain on the explicit movement path: phasing, forced flight, flight-capable profiles, custom fall-speed profiles, flopping land profiles, and water/lava special cases still bypass the ordinary walking `travel(...)` path.
- First-person possessed biped arms use `PlayerRendererMixin` injections into `renderRightHand` and `renderLeftHand` to render the possessed host humanoid model arm. The previous world-yaw-sensitive `RenderHandEvent` pose-stack translation/rotation hack is not used.
- `RenderHandEvent` is retained only to cancel free-vagrant hands; it does not transform possessed hand/item position.
- Vagrant wall phasing keeps the requested synchronized effective speed of `0.05625` on both client and server paths. The phase charge delay was not changed.
- Player-host death position capture is present through `PlayerSoulEvents.rememberPossessedBodyDeath(...)`; clone/respawn applies the stored possessed body death position back to the released Vagrant.
- The previous forced pushable/collidable inert soulless-body override remains removed; free-vagrant and soulless collision semantics remain covered by the existing baseline failures and need targeted beta.16 follow-up before changing globally.

Focused verification:

- `VERIFIED`: `.\gradlew.bat compileJava --rerun-tasks` passes after restoring possessed soulless mob `NoAI=false` during control.
- `VERIFIED/PARTIAL`: `.\gradlew.bat runGameTestServer` completes 147 tests and returns to the expected 9 required baseline failures.
- `VERIFIED/PARTIAL`: `.\gradlew.bat runClient` reached client startup/resource loading and was stopped by command timeout. Fresh `latest.log`/`debug.log` contain no crash for this run, no `InvalidMixin`, no `InjectionError`, no Requiem packet errors, and no renderer errors. Observed exception matches are the usual Netty reflective access noise; dependency metadata debug lines are normal dev-loader noise.

Current required GameTest baseline failures:

- `possessedeaterstoresseparatehungerdata`
- `vagrantcollisionignorestraversablebutnotimpermeableblocks`
- `vagrantvisualsclearwhenstatechanges`
- `filledvesselreleasestypedsoulandreturnsemptyvessel`
- `possessedhealingrulesparse`
- `possesseditemusefindscameracontrolledhost`
- `soulvesseldataroundtrips`
- `possessiondoesnotuseridingandallowssoullessvillagers`
- `possessedoffhanditemuseuseshostoverride`

Regression failures removed during this pass:

- `possessedhostmovesfrominputpacket`
- `possessedwalkinghostusesvanillagravity`
- `possessedwalkinghostfallswhenmovingoffledge`
- `possessedzombieusesvanillastepupandfallauthority`
- `possessedwalkinghostcannotflybyholdingjump`

Test note:

- `possessedwalkinghostusesvanillagravity` was updated to expect vanilla `travel(...)` gravity after drag (`-0.0784...`) instead of pre-drag raw gravity (`-0.08`). Runtime code was not changed for this expectation.

Manual verification still required:

- Zombie/Skeleton walking feel, jump stability, no levitation, automatic step-up, fall damage authority, camera height, right/left hand rendering, item-swap jitter, 360 degree held-item screen anchor stability, host self-rotation, phasing speed feel, player-host death release position, and soulless body standing/collision semantics against confirmed beta.16 behavior.
