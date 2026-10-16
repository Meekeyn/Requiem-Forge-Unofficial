package ua.meekeyn.requiemunofficial.common.soul;

public enum SoulState {
    MORTAL,
    REMNANT,
    VAGRANT;

    public static SoulState byName(String name) {
        for (SoulState state : values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return MORTAL;
    }
}
