package ua.meekeyn.requiemunofficial.common.remnant;

import net.minecraft.server.level.ServerPlayer;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerRiftTracker {
    private PlayerRiftTracker() {
    }

    public static boolean addKnownRift(ServerPlayer player, ObeliskRecordData.ObeliskRecord record) {
        return ModCapabilities.soul(player)
                .map(soul -> soul.addKnownRift(record.recordId()))
                .orElse(false);
    }

    public static Set<ObeliskRecordData.ObeliskDescriptor> fetchKnownObelisks(ServerPlayer player) {
        ObeliskRecordData data = ObeliskRecordData.get(player.getServer());
        Set<ObeliskRecordData.ObeliskDescriptor> descriptors = new LinkedHashSet<>();
        ModCapabilities.soul(player).ifPresent(soul -> {
            for (UUID recordId : soul.getKnownRifts()) {
                if (!data.validateLoadedDestination(player.getServer(), recordId)) {
                    soul.removeKnownRift(recordId);
                    continue;
                }
                data.getRecord(recordId).ifPresent(record -> descriptors.add(record.descriptor()));
            }
        });
        return descriptors;
    }
}
