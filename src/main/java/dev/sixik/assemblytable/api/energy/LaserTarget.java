package dev.sixik.assemblytable.api.energy;

/** This interface should be defined by any Tile which wants to receive power from BuildCraft lasers.
 * <p>
 * The respective Block MUST implement ILaserTargetBlock! */
public interface LaserTarget {

    /**
     * Возвращает количество энергии, которое сейчас требуется цели.
     * @return Требуемая энергия (в FE), или 0, если энергия не нужна.
     */
    int getRequiredLaserPower();

    /**
     * Передает энергию от лазера к цели.
     * @param energy Количество FE для передачи.
     * @return Излишек энергии (excess). Если цель приняла всё, вернет 0.
     */
    int receiveLaserPower(int energy);

    /**
     * Возвращает true, если BlockEntity больше не является валидной целью
     * (например, если блок сломали).
     */
    boolean isInvalidTarget();
}
