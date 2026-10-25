package ua.meekeyn.requiemunofficial.common.gamerule;

/**
 * Значення правила {@code requiem:possessionKeepInventory} (оригінал beta.16).
 */
public enum PossessionKeepInventory {
    /** Ніколи не забирати інвентар із тіла (тіло лишається зі своїм вмістом). */
    NEVER,
    /** Забирати інвентар лише коли тіло померло. */
    LIVING,
    /** Завжди забирати інвентар. */
    ALWAYS;

    public boolean shouldTransfer(boolean hostAlive) {
        return switch (this) {
            case NEVER -> true;
            case LIVING -> !hostAlive;
            case ALWAYS -> false;
        };
    }
}
