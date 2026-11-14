package ua.meekeyn.requiemunofficial.common.gamerule;

/**
 * Значення правила {@code requiem:possessionKeepInventory} (оригінал beta.16).
 */
public enum PossessionKeepInventory {
    /**
     * Душа ніколи не зберігає інвентар тіла: при виході речі лишаються в живому тілі,
     * а при смерті тіла — випадають на місці смерті.
     */
    NEVER,
    /**
     * Душа зберігає інвентар, лише покидаючи живе тіло; якщо тіло померло з душею
     * всередині, речі випадають на місці смерті.
     */
    LIVING,
    /** Душа завжди зберігає інвентар (і при виході, і при смерті тіла) — нічого не випадає. */
    ALWAYS;

    /**
     * @return {@code true}, якщо речі треба забрати в душі (передати живому тілу або
     * скинути на місці смерті тіла)
     */

    public boolean shouldTransfer(boolean hostAlive) {
        return switch (this) {
            case NEVER -> true;
            case LIVING -> !hostAlive;
            case ALWAYS -> false;
        };
    }
}
