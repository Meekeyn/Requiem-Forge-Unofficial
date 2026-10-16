package ua.meekeyn.requiemunofficial.common.ritual;

import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.Arrays;
import java.util.Optional;

public enum RitualType {
    RESURRECTION("resurrection", 5, "rift", 1, 100),
    DECAY_ZOMBIE("decay_zombie", 3, "rift", 1, 60),
    DECAY_SKELETON("decay_skeleton", 3, "rift", 1, 60);

    private final ResourceLocation id;
    private final int energyCost;
    private final String requiredRune;
    private final int requiredRuneLevel;
    private final int durationTicks;

    RitualType(String path, int energyCost, String requiredRune, int requiredRuneLevel, int durationTicks) {
        this.id = ModIds.id(path);
        this.energyCost = energyCost;
        this.requiredRune = requiredRune;
        this.requiredRuneLevel = requiredRuneLevel;
        this.durationTicks = durationTicks;
    }

    public ResourceLocation id() {
        return id;
    }

    public int energyCost() {
        return energyCost;
    }

    public String requiredRune() {
        return requiredRune;
    }

    public int requiredRuneLevel() {
        return requiredRuneLevel;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public static Optional<RitualType> fromId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.toString().equals(id))
                .findFirst();
    }
}
