package dev.sixik.assemblytable.api.energy;

/**
 * Implemented by block entities that can receive power from AssemblyTable lasers.
 *
 * <p>Lasers search for nearby targets implementing this interface and transfer FE
 * while the target reports that it still needs energy.</p>
 */
public interface LaserTarget {

    /**
     * Returns how much energy this target can currently accept.
     *
     * @return requested energy in FE, or {@code 0} if no laser power is needed right now
     */
    int getRequiredLaserPower();

    /**
     * Tries to receive energy from a laser.
     *
     * @param energy amount of FE offered by the laser
     * @return leftover FE that could not be accepted; return {@code 0} when all energy was consumed
     */
    int receiveLaserPower(int energy);

    /**
     * Returns whether this target should be ignored by lasers.
     *
     * @return {@code true} when the target is no longer valid, for example after removal
     */
    boolean isInvalidTarget();
}
