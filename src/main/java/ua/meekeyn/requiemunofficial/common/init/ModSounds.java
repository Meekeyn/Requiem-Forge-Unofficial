package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, RequiemUnofficial.MODID);

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    // Базова подія звуку для перевірки реєстрації
    public static final RegistryObject<SoundEvent> EFFECT_DISSOCIATE =
            SOUND_EVENTS.register("effect.dissociate",
                    () -> SoundEvent.createVariableRangeEvent(ModIds.id("effect.dissociate")));
    public static final RegistryObject<SoundEvent> EFFECT_BECOME_REMNANT =
            registerVariable("effect.become.remnant");
    public static final RegistryObject<SoundEvent> EFFECT_BECOME_MORTAL =
            registerVariable("effect.become.mortal");
    public static final RegistryObject<SoundEvent> EFFECT_TIME_STOP =
            registerVariable("effect.time.stop");

    public static final RegistryObject<SoundEvent> OBELISK_ACTIVATE =
            registerVariable("block.obelisk.activate");
    public static final RegistryObject<SoundEvent> OBELISK_AMBIENT =
            registerVariable("block.obelisk.ambient");
    public static final RegistryObject<SoundEvent> OBELISK_CHARGE =
            registerVariable("block.obelisk.charge");
    public static final RegistryObject<SoundEvent> OBELISK_DEACTIVATE =
            registerVariable("block.obelisk.deactivate");

    public static final RegistryObject<SoundEvent> BLOCK_RUNESTONE_CARVE =
            registerVariable("block.runestone.carve");
    public static final RegistryObject<SoundEvent> BLOCK_RUNESTONE_CLEAR =
            registerVariable("block.runestone.clear");

    public static final RegistryObject<SoundEvent> ITEM_EMPTY_VESSEL_USE =
            registerVariable("item.empty_vessel.use");
    public static final RegistryObject<SoundEvent> ITEM_FILLED_VESSEL_USE =
            registerVariable("item.filled_vessel.use");
    public static final RegistryObject<SoundEvent> ITEM_OPUS_USE =
            registerVariable("item.opus.use");

    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_AMBIENT =
            registerVariable("entity.mortician.ambient");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_CAST_SPELL =
            registerVariable("entity.mortician.cast_spell");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_DEATH =
            registerVariable("entity.mortician.death");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_HURT =
            registerVariable("entity.mortician.hurt");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_PREPARE_ATTACK =
            registerVariable("entity.mortician.prepare_attack");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_TRADE =
            registerVariable("entity.mortician.trade");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_YES =
            registerVariable("entity.mortician.yes");
    public static final RegistryObject<SoundEvent> ENTITY_MORTICIAN_NO =
            registerVariable("entity.mortician.no");

    private static RegistryObject<SoundEvent> registerVariable(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(ModIds.id(id)));
    }
}
