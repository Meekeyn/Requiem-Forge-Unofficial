package ua.meekeyn.requiemunofficial.common.transformation;

/**
 * Реалізується міксинами тіл, чий ванільний таймер перетворення живе в customServerAiStep
 * (піглін, хоглін). Одержиме тіло не виконує serverAiStep, тож таймер тікається окремо.
 */
public interface PossessedConversionHost {

    void requiemunofficial$tickPossessedConversion();
}
