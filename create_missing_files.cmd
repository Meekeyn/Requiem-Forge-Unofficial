@echo off
chcp 65001 >nul
setlocal

rem Створення відсутніх тек та порожніх файлів без перезапису існуючих

call :touch "src\main\java\ua\meekeyn\requiemunofficial\RequiemUnofficial.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModBlocks.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModBlockEntities.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModItems.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModEntities.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModParticles.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModSounds.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModMenuTypes.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModEffects.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\init\ModStructures.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\block\TachyliteBlock.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\block\RunestoneBlock.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\block\entity\DummyBlockEntity.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\block\entity\RunestoneBlockEntity.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\entity\ObeliskSoulEntity.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\entity\ReleasedSoulEntity.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\entity\MorticianEntity.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\entity\PlayerShellEntity.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\item\DebugItem.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\item\SoulVesselItem.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\item\IchorVesselItem.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\menu\RiftMenu.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\network\ModNetwork.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\particle\RequiemEntityParticleOptions.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\particle\WispTrailParticleOptions.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\particle\RequiemSimpleParticleType.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\particle\SimpleGhostParticle.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\sound\ModSoundDefinitions.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\util\ModIds.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\effect\AttritionMobEffect.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\effect\EmancipationMobEffect.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\effect\PenanceMobEffect.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\common\effect\ReclamationMobEffect.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\ClientSetup.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\particle\ObeliskSoulParticle.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\particle\SoulTrailParticle.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\particle\SimpleGhostParticle.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\render\entity\ObeliskSoulRenderer.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\render\entity\ReleasedSoulRenderer.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\render\entity\MorticianRenderer.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\render\entity\PlayerShellRenderer.java"

call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\screen\RiftScreen.java"
call :touch "src\main\java\ua\meekeyn\requiemunofficial\client\screen\DummyScreen.java"

echo.
echo Done.
exit /b 0

:touch
set "file=%~1"
for %%I in ("%file%") do (
    if not exist "%%~dpI" mkdir "%%~dpI" >nul 2>nul
)
if not exist "%file%" (
    type nul > "%file%"
    echo CREATED: %file%
) else (
    echo EXISTS:  %file%
)
exit /b 0