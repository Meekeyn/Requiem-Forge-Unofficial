# PHASING_COLLISION_JITTER_DIAGNOSIS

Дата: 2026-10-13
Проект: `I:\Requiem Forge Unofficial`
Рабочая директория анализа: `I:\Requiem Forge Unofficial\src\main`

## Статус проверки

Подтверждено статическим анализом актуальных исходников и успешной компиляцией `.\gradlew.bat compileJava` из корня проекта `I:\Requiem Forge Unofficial`.

Minecraft-клиент для live-воспроизведения не запускался, поэтому фактическая частота vanilla position correction packets не измерена логом. Вывод о коррекции основан на конкретном расхождении клиентского и серверного movement pipeline: клиент предсказывает движение через блоки с `noPhysics=true`, сервер принимает движение с `noPhysics=false` и filtered block collisions.

Временное диагностическое логирование не добавлялось: для текущего этапа достаточно доказуемого расхождения в коде, а пользователь запросил сначала отчёт. Файлы механики не изменялись.

## Краткий вывод

Причина вибрации при листьях и камне имеет общий технический корень, но разные триггеры по времени:

- Листья: на 20-м тике клиент включает `player.noPhysics=true` для leaf phase, а сервер оставляет `player.noPhysics=false`.
- Камень: на 60-м тике клиент включает `player.noPhysics=true` для full phase, а сервер оставляет `player.noPhysics=false`.

Это не старая гипотеза "оба включают `noPhysics` и ломают filtering". Актуальный серверный код уже исправлен в сторону filtered collision pipeline. Проблема теперь в том, что клиентская сторона осталась на старой модели `noPhysics`, поэтому клиентский `Entity.move()` обходит block collision filtering, а серверный `Entity.move()` использует redirect collision filtering. В результате клиент каждый тик предсказывает более свободное движение, чем разрешает сервер, и получает откат/коррекцию.

## Участвующие классы и методы

- `java/ua/meekeyn/requiemunofficial/client/ClientVagrantState.java`
  - `onClientTick`, строки 35-120.
  - `primeVagrantPhasePhysics`, строки 145-157.
  - `applyVagrantPhasePhysics`, строки 159-174.
  - `isPhaseAttemptInput`, строки 176-186.
  - `updateClientPhaseCharge`, строки 188-214.
  - `updateClientLeafPhaseCharge`, строки 216-241.
  - `applyManualVerticalPhaseMovement`, строки 251-266.
  - `applyEntryPhaseMovement`, строки 268-289.

- `java/ua/meekeyn/requiemunofficial/common/soul/VagrantEvents.java`
  - `onPlayerTick`, строки 143-160.
  - `setVagrantInput`, строки 182-185.
  - `updateVagrantState`, строки 188-227.
  - `primeVagrantPhasePhysics`, строки 285-292.
  - `applyVagrantPhasePhysics`, строки 294-310.
  - `updatePhaseCharge`, строки 312-340.
  - `updateLeafPhaseCharge`, строки 342-374.
  - `applyManualVerticalPhaseMovement`, строки 376-391.
  - `applyEntryPhaseMovement`, строки 393-414.

- `java/ua/meekeyn/requiemunofficial/common/soul/VagrantRules.java`
  - constants: `PHASE_REQUIRED_TICKS=60`, `LEAF_PHASE_REQUIRED_TICKS=20`, `LEAF_PHASE_GRACE_TICKS=20`, `PHASE_SPEED=0.05`, `PHASE_LOWERING=0.0`, строки 26-30.
  - `canPassThrough`, строки 73-78.
  - `shouldSuppressBlockCollision`, строки 94-98.
  - `shouldUsePhaseMovement`, строки 100-105.
  - `shouldSuppressOnlyLeafCollision`, строки 107-111.
  - `shouldSuppressCollisionWith`, строки 113-121.
  - block probing helpers, строки 123-160 и 219-297.

- `java/ua/meekeyn/requiemunofficial/mixin/EntityMixin.java`
  - redirect `Entity.collideBoundingBox -> Level.getBlockCollisions`, строки 93-108.
  - filtered shape collector, строки 110-133.

- `java/ua/meekeyn/requiemunofficial/common/network/VagrantInputPacket.java`
  - client input delivery to server, строки 23-31.

- `resources/data/requiemunofficial/tags/blocks/soul_impermeable.json`
  - impermeable blocks: portals, barrier, bedrock, command blocks, structure/jigsaw blocks.

- `resources/data/requiemunofficial/tags/blocks/wandering_soul_traversable.json`
  - traversable special blocks: soul fire base blocks and leaves.

## Полная последовательность движения

### Клиент

1. `ClientVagrantState.onClientTick` получает `TickEvent.ClientTickEvent`.
2. На `START` вызывает `primeVagrantPhasePhysics` (`ClientVagrantState.java:49-51`, `145-157`).
3. `isPhaseAttemptInput` считает попытку phase по `W/A/D`, либо `Space` вверх при взгляде вверх, либо `Shift` вниз при взгляде вниз; затем требует `VagrantRules.shouldChargePhaseEntry(player)` (`ClientVagrantState.java:176-186`).
4. `updateClientPhaseCharge` обновляет локальные счётчики:
   - full stone charge в `phaseChargeTicks`, до 60 (`ClientVagrantState.java:204-205`);
   - leaf charge в `leafPhaseChargeTicks`, до 20 (`ClientVagrantState.java:216-226`);
   - leaf grace в `leafPhaseGraceTicks`, 20 тиков (`ClientVagrantState.java:217-240`).
5. `sendVagrantInput` каждый START tick отправляет `VagrantInputPacket(jumping, shifting, phaseAttempt)` на сервер (`ClientVagrantState.java:243-249`).
6. `applyVagrantPhasePhysics` на клиенте делает критическое действие: `player.noPhysics = VagrantRules.shouldSuppressBlockCollision(player)` (`ClientVagrantState.java:159-164`).
7. После leaf charge или stone charge `shouldSuppressBlockCollision` становится true (`VagrantRules.java:94-98`), и клиентский player начинает двигаться с `noPhysics=true`.
8. С `noPhysics=true` vanilla `Entity.move()` не проходит через обычный block collision path, где стоит redirect `EntityMixin`. Следовательно, клиентский prediction не использует `requiemunofficial$vagrantBlockCollisions`.
9. Клиент дополнительно задаёт phase velocity:
   - вход в блок по look vector или horizontal look (`ClientVagrantState.java:268-289`);
   - вертикальное движение внутри блока по `Space`/`Shift` (`ClientVagrantState.java:251-266`);
   - clamp скорости до `PHASE_SPEED=0.05` (`ClientVagrantState.java:291-297`).

### Сервер

1. `VagrantInputPacket.handle` записывает input flags в `ServerPlayer` persistent data (`VagrantInputPacket.java:23-31`, `VagrantEvents.java:182-185`).
2. `VagrantEvents.onPlayerTick` игнорирует client side; на `START` вызывает `primeVagrantPhasePhysics`, на `END` вызывает `updateVagrantState` (`VagrantEvents.java:143-160`).
3. На `START` `updatePhaseCharge` пересчитывает server-side charge/grace (`VagrantEvents.java:285-290`, `312-374`).
4. Серверный `applyVagrantPhasePhysics` делает другое критическое действие: всегда ставит `player.noPhysics = false` (`VagrantEvents.java:294-300`).
5. Если phase movement разрешён (`shouldUsePhaseMovement`), сервер задаёт velocity/ограничивает скорость/вертикальное движение, но не включает `noPhysics` (`VagrantEvents.java:300-309`).
6. Поэтому серверный `Entity.move()` идёт через обычный collision pipeline.
7. Внутри `Entity.collideBoundingBox` redirect `EntityMixin.requiemunofficial$vagrantBlockCollisions` перехватывает `Level.getBlockCollisions` только когда `VagrantRules.shouldSuppressBlockCollision(player)` true (`EntityMixin.java:93-108`).
8. `requiemunofficial$phaseBlockCollisions` возвращает collision shapes только для блоков, которые не должны подавляться (`EntityMixin.java:110-133`).
9. Для full charge `shouldSuppressCollisionWith` пропускает все `canPassThrough` блоки и оставляет `soul_impermeable` твёрдыми (`VagrantRules.java:113-121`, `73-78`).
10. Для leaf-only active пропускаются только leaves, а stone/slab/stairs/bedrock остаются твёрдыми (`VagrantRules.java:107-121`).

## Что происходит на 20-м тике

На 20-м тике leaf charge:

- `LEAF_PHASE_REQUIRED_TICKS` равен 20 (`VagrantRules.java:27`).
- Клиент в `updateClientLeafPhaseCharge` доводит `leafPhaseChargeTicks` до 20 и вызывает `setLeafPhaseActive(true)` (`ClientVagrantState.java:216-226`).
- Сервер в `updateLeafPhaseCharge` делает то же по `LEAF_PHASE_CHARGE_TICKS` и `LEAF_PHASE_ACTIVE` (`VagrantEvents.java:342-359`).
- `VagrantRules.isLeafPhaseActive` становится true, включая также grace counter как active source (`VagrantRules.java:175-179`).
- Клиент затем ставит `player.noPhysics=true`, потому что `shouldSuppressBlockCollision` true при active leaf phase вне non-leaf block (`ClientVagrantState.java:159-164`, `VagrantRules.java:94-98`).
- Сервер в тот же логический режим ставит `player.noPhysics=false`, но использует filtered collision suppression для leaves (`VagrantEvents.java:294-310`, `EntityMixin.java:93-133`).

Именно на этом переходе начинается divergence: клиент проходит как no-clip entity, сервер проходит как обычная entity с selectively empty collision shapes.

## Что происходит на 60-м тике

На 60-м тике stone/full charge:

- `PHASE_REQUIRED_TICKS` равен 60 (`VagrantRules.java:26`).
- Клиент увеличивает `phaseChargeTicks` до 60 в `updateClientPhaseCharge` (`ClientVagrantState.java:204-205`, `211-213`).
- Сервер увеличивает `PHASE_CHARGE_TICKS` до 60 в `updatePhaseCharge` (`VagrantEvents.java:329-334`).
- `VagrantRules.isPhaseCharged` становится true (`VagrantRules.java:167-169`).
- Клиент включает `player.noPhysics=true` через `shouldSuppressBlockCollision` (`ClientVagrantState.java:159-164`, `VagrantRules.java:94-98`).
- Сервер сохраняет `player.noPhysics=false` и ожидает, что collision filtering в `EntityMixin` выполнит проход через stone и заблокирует impermeable (`VagrantEvents.java:294-310`, `EntityMixin.java:93-133`).

Отсюда камень начинает вибрировать через 60 тиков по той же client/server divergence причине, но с full-phase condition, а не leaf-only condition.

## Проверка `noPhysics` и реального collision pipeline

Подтверждённый факт: серверная сторона больше не включает `noPhysics` для свободного Vagrant phasing. В `VagrantEvents.applyVagrantPhasePhysics` строка 299 безусловно ставит `player.noPhysics = false`.

Подтверждённый факт: collision filtering написан как redirect внутри `Entity.collideBoundingBox`, а значит он работает только если vanilla доходит до `collideBoundingBox`. Это видно в `EntityMixin.java:93-108`.

Подтверждённый факт: клиентская сторона продолжает включать `noPhysics` через `ClientVagrantState.applyVagrantPhasePhysics` строку 163.

Следствие: клиентский movement bypasses тот самый collision filtering, который сервер требует для корректной семантики phasing. Поэтому hypothesis из аудита частично устарела: конфликт `noPhysics` и collision filtering реален, но сейчас он находится прежде всего на клиенте, а не на сервере.

## Листья

Подтверждённая логика:

- Первое прохождение после 20 тиков: `LEAF_PHASE_REQUIRED_TICKS=20`; серверные и клиентские счётчики это реализуют (`VagrantRules.java:27`, `VagrantEvents.java:342-359`, `ClientVagrantState.java:216-226`).
- Перемещение внутри сплошной листвы: если игрок inside leaves и leaf active, charge удерживается на 20, grace обновляется до 20 (`VagrantEvents.java:342-347`, `ClientVagrantState.java:217-220`).
- Выход из листьев: grace уменьшается при active leaf phase без текущей попытки/листьев (`VagrantEvents.java:360-369`, `ClientVagrantState.java:227-235`).
- Повторный вход в grace: `isLeafPhaseActive` true при `LEAF_PHASE_GRACE_TICK_DATA > 0` (`VagrantRules.java:175-179`).
- Повторный вход после grace: counters сбрасываются (`VagrantEvents.java:366-372`, `ClientVagrantState.java:232-240`).
- Соприкосновение листьев с камнем: leaf-only mode не подавляет stone collision (`VagrantRules.java:107-121`).
- Диагональ и соседние формы: текущий серверный filter собирает реальные shapes неподавленных блоков (`EntityMixin.java:110-133`), тесты проверяют stair/slab и impermeable corner cases (`CoreDataGameTests.java:1635-1672`, `1811-1838`).

Отдельная причина leaf jitter: включение `noPhysics=true` на клиенте уже после 20 ticks leaf activation, тогда как сервер оставляет `noPhysics=false` и пропускает только leaves. Если рядом stone/slab/stairs или non-leaf block, клиент может предсказать проникновение/свободное движение, которое сервер ограничит.

Grace по серверной логике не должна замедлять воздух: `shouldUsePhaseMovement` включает phase movement для leaf only только если `isLeafPhaseActive(player) && isInsideLeaves(player)` (`VagrantRules.java:100-105`). Это дополнительно закреплено тестом `vagrantLeafExitRestoresNormalMovementImmediately` (`CoreDataGameTests.java:1895-1910`). Но клиент из-за `noPhysics=true` при grace может всё ещё иметь неправильный collision state, потому что `shouldSuppressBlockCollision` true для active leaf phase вне non-leaf block (`VagrantRules.java:94-98`).

## Камень

Подтверждённая логика:

- Первое прохождение после 60 тиков: `PHASE_REQUIRED_TICKS=60`; `isPhaseCharged` проверяет `PHASE_CHARGE_TICKS >= 60` (`VagrantRules.java:26`, `167-169`).
- Удержание W после таймера: клиентский `isPhaseAttemptInput` удерживает phase attempt при movement key и `shouldChargePhaseEntry` (`ClientVagrantState.java:176-186`); сервер получает это через `VagrantInputPacket` (`VagrantInputPacket.java:23-31`).
- Несколько блоков подряд: когда player уже inside non-leaf phaseable block, серверный `updatePhaseCharge` сохраняет full charge, если он уже был charged (`VagrantEvents.java:318-327`).
- Остановка внутри стены: серверный `applyManualVerticalPhaseMovement` обнуляет Y, когда нет ровно одного вертикального input; horizontal velocity может быть ограничен `limitPhaseSpeed` (`VagrantEvents.java:376-391`, `450-455`). Без дополнительного input игрок может оставаться внутри phaseable block с filtered collisions.
- Движение назад: input packet для vagrant phase attempt не учитывает keyDown/backward напрямую; vanilla movement packet всё равно придёт, но phase entry push задаётся по look direction, не по фактическому backward vector (`ClientVagrantState.java:176-186`, `268-289`; `VagrantEvents.java:393-414`). Это потенциальный UX-риск, не доказанная причина jitter.
- Space/Shift: vertical phase entry поддержан через jump/upward look и shift/downward look (`ClientVagrantState.java:180-181`, `276-279`; `VagrantEvents.java:399-404`). Внутри блока Space/Shift задают Y velocity (`ClientVagrantState.java:251-266`; `VagrantEvents.java:376-391`).
- Impermeable: `soul_impermeable.json` содержит portals, barrier, bedrock и technical blocks. `canPassThrough` возвращает false для этих блоков, если они не air/leaves/wandering traversable (`VagrantRules.java:73-78`). Collision filter оставляет их shapes, то есть они должны блокировать движение до проникновения, а не только сбрасывать state после проникновения (`EntityMixin.java:122-130`).

Отдельная причина stone jitter: на 60-м тике клиент переходит в no-clip movement, сервер остаётся в collision-filtered movement. Для обычного stone оба в итоге "хотят" разрешить проход, но делают это разными физическими путями. Любая разница в swept AABB, step handling, vertical collision, pose/eye/packet timing или соседнем impermeable shape приводит к server correction.

## Shift

Shift раньше мог помогать не потому, что это правильный fix, а потому что он менял геометрию/вектор входа:

- `VagrantRules.isDescendingIntoPhaseableBlock` учитывает `player.isShiftKeyDown()` и probe вниз на `-0.2D` (`VagrantRules.java:219-225`).
- `isLookingIntoPhaseableBlock` и `isLookingIntoLeaves` сохраняют отрицательный Y probe только при Shift; без Shift отрицательный Y probe принудительно flatten до 0 (`VagrantRules.java:227-243`, `245-260`).
- `ClientVagrantState.isPhaseAttemptInput` разрешает vertical phase entry вниз только при Shift и взгляде вниз (`ClientVagrantState.java:180-181`).
- `applyEntryPhaseMovement` на клиенте и сервере при Shift+down look задаёт push по полному look vector, включая отрицательный Y (`ClientVagrantState.java:273-279`, `VagrantEvents.java:398-404`).
- Внутри блока Shift задаёт `-PHASE_SPEED` по Y (`ClientVagrantState.java:251-266`, `VagrantEvents.java:376-391`).
- `preventUncommandedPhaseDescent` не обнуляет отрицательный Y при Shift (`ClientVagrantState.java:299-307`, `VagrantEvents.java:458-468`).

Почему это могло "помогать": вертикальный компонент вниз мог быстрее перевести bounding box в состояние `insidePhaseableBlock`, удержать phase state или сместить AABB так, что серверная collision resolution меньше конфликтовала с клиентским no-clip prediction. Сейчас `PHASE_LOWERING=0.0` (`VagrantRules.java:30`), поэтому старый эффект принудительного опускания отсутствует, и оставшееся влияние Shift идёт только через input/probe/vertical velocity.

Не найдено доказательства, что crouching pose или eye height сами по себе являются основной причиной. В актуальном коде свободного Vagrant нет явного изменения pose/eye height. Shift важен как input-флаг для phase probing и Y velocity.

## Проверка beta.16

Оригинальная Requiem 2.0.0-beta.16/Fabric модель подавляла collision shape в `AbstractBlockStateMixin.getCollisionShape` при `ExtendedShapeContext.requiem_isNoClipping()` и не `SOUL_IMPERMEABLE` (`Requiem-1.20/src/main/java/ladysnake/requiem/mixin/common/remnant/AbstractBlockStateMixin.java:66-75`).

Порт Forge реализует похожую идею не через `ShapeContext`, а через redirect `Entity.collideBoundingBox -> Level.getBlockCollisions` (`EntityMixin.java:93-133`). Это требует, чтобы `Entity.move()` действительно дошёл до `collideBoundingBox`, то есть `noPhysics` не должен обходить collision pipeline на стороне, где нужна фильтрация.

## Подтверждённые факты

- Серверный Vagrant phasing держит `noPhysics=false` (`VagrantEvents.java:294-300`).
- Клиентский Vagrant phasing включает `noPhysics=true` при active leaf/full phase (`ClientVagrantState.java:159-164`, `VagrantRules.java:94-98`).
- Collision filtering находится в `EntityMixin` и применяется к block collision iteration, а не к произвольному no-clip movement (`EntityMixin.java:93-133`).
- Leaf charge равен 20 тикам, stone charge равен 60 тикам (`VagrantRules.java:26-28`).
- Leaf grace равен 20 тикам и считается active leaf phase (`VagrantRules.java:175-179`).
- `PHASE_LOWERING=0.0`, поэтому текущий код не должен принудительно опускать игрока (`VagrantRules.java:30`, `198-217`).
- Impermeable blocks должны оставаться collision-solid в filtered pipeline (`VagrantRules.java:73-78`, `113-121`; `soul_impermeable.json`).
- Существующие GameTest-сценарии явно закрепляют намерение "charged movement must use filtered collisions, not noPhysics" (`CoreDataGameTests.java:1792-1808`) и "leaf permission must not pass stone" (`CoreDataGameTests.java:1876-1893`).

## Гипотезы и недостающие runtime-данные

Нужны live logs только для подтверждения внешнего симптома:

- точный cadence server correction packets после 20/60 тиков;
- сравнение координат client before/after prediction и server accepted position;
- подтверждение, что на проблемном тикe клиентский `noPhysics=true`, серверный `noPhysics=false`;
- сравнение текущего block under/probe для листьев рядом с камнем и диагонального движения;
- отдельные записи для W, Space, Shift.

Минимальное временное логирование, если потребуется на следующем этапе:

- на клиенте в `ClientVagrantState.applyVagrantPhasePhysics`: tick, pos, delta, noPhysics, noGravity, charge/grace, insideLeaves/insideNonLeaf, shouldSuppressBlockCollision;
- на сервере в `VagrantEvents.applyVagrantPhasePhysics`: те же поля плюс `phaseMovement`;
- в `EntityMixin.requiemunofficial$vagrantBlockCollisions`: ограниченно логировать только первый N событий после charge, block state и suppress/keep result;
- отдельный клиентский hook на получение vanilla position correction packet, если потребуется точно доказать сетевой откат.

Лог должен быть gated system property/config flag и rate-limited.

## Минимальный способ исправления

Не возвращать forced Y lowering, smoothing, teleport или полное отключение столкновений.

Минимальный fix: привести клиентский `ClientVagrantState.applyVagrantPhasePhysics` к серверной модели:

- не ставить `player.noPhysics = VagrantRules.shouldSuppressBlockCollision(player)`;
- держать `player.noPhysics=false` для restricted free Vagrant phasing, как сервер;
- использовать тот же filtered collision pipeline через `EntityMixin` на клиенте;
- выставлять `noGravity` по той же семантике, что сервер: `phaseMovement`, а не `player.noPhysics`;
- сохранить entry/manual phase velocity и speed clamp.

Это делает client prediction и server authority одинаковыми: оба проходят через `Entity.move()` collision resolution и один и тот же redirect filter. Для leaf-only mode клиент перестанет no-clip через соседний stone/slab/stairs; для full mode impermeable blocks останутся твёрдыми на обеих сторонах.

## Возможные регрессии

- Клиентский prediction может стать чуть "жёстче" до/после threshold, потому что no-clip больше не будет скрывать реальные collision edge cases.
- Leaf grace в воздухе должен сохранить обычную скорость и gravity; после fix это нужно перепроверить, потому что сейчас клиентский `noPhysics` мог маскировать часть поведения.
- Вертикальный вход `Space`/`Shift` зависит от timing input packet и look direction; после client/server выравнивания надо проверить, что vertical entry не потерял responsiveness.
- Impermeable corner cases могут стать заметнее, потому что клиент начнёт честно блокироваться там же, где сервер.
- Если какие-то visual/fx ожидали `player.noPhysics=true` на клиенте, они должны быть переведены на explicit phase state, а не на physics flag.

## План проверки после исправления

1. Запустить `.\gradlew.bat compileJava`.
2. Запустить relevant GameTest suite, если доступен в текущей среде.
3. В live client проверить листья:
   - первый вход после 20 тиков;
   - движение внутри сплошной листвы;
   - выход из листьев;
   - повторный вход в течение 20 тиков grace;
   - повторный вход после grace;
   - листья рядом с камнем;
   - диагональное движение.
4. В live client проверить камень:
   - первый вход после 60 тиков;
   - удержание W после threshold;
   - несколько stone blocks подряд;
   - остановка внутри стены;
   - движение назад;
   - vertical Space/Shift;
   - bedrock, barrier, portals и остальные `soul_impermeable`.
5. С временным логированием подтвердить:
   - клиент и сервер имеют одинаковый `noPhysics=false` в phase mode;
   - `EntityMixin` вызывается на обеих сторонах;
   - correction packets исчезли или резко сократились именно на 20/60 tick transition.

