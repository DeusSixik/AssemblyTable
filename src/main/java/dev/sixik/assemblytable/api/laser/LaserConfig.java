package dev.sixik.assemblytable.api.laser;

import dev.sixik.assemblytable.utils.Vec4i;

/**
 * Immutable configuration for a laser block.
 *
 * <p>Use {@link #builder()} to create a config for a laser variant. The same block entity
 * logic can then be reused by multiple blocks with different range, buffer, transfer speed,
 * and warmup behavior.</p>
 */
public record LaserConfig(
        int targetRange,
        int energyBuffer,
        int maxTransferPerTick,
        int maxReceivePerTick,
        Vec4i beamColor,
        Warmup warmup
) {

    /**
     * Creates a builder with sensible defaults for a basic laser.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether the laser should ramp up its transfer speed over time.
     */
    public boolean hasWarmup() {
        return warmup.enabled();
    }

    /**
     * Builder for {@link LaserConfig}.
     */
    public static final class Builder {
        private int targetRange = 6;
        private int energyBuffer = 1000;
        private int maxTransferPerTick = 1000;
        private int maxReceivePerTick = 5000;
        private Vec4i beamColor = new Vec4i(30, 100, 255, 200);
        private Warmup warmup = Warmup.disabled();

        /**
         * Sets how far the laser can search for valid targets.
         */
        public Builder targetRange(int targetRange) {
            this.targetRange = positive(targetRange, "targetRange");
            return this;
        }

        /**
         * Sets the internal energy buffer capacity in FE.
         */
        public Builder energyBuffer(int energyBuffer) {
            this.energyBuffer = positive(energyBuffer, "energyBuffer");
            return this;
        }

        /**
         * Sets the maximum amount of FE the laser can transfer per tick.
         */
        public Builder maxTransferPerTick(int maxTransferPerTick) {
            this.maxTransferPerTick = positive(maxTransferPerTick, "maxTransferPerTick");
            return this;
        }

        /**
         * Alias for {@link #maxTransferPerTick(int)} that makes the intent clearer
         * when warmup is enabled.
         */
        public Builder maxSpeedTransferPerTick(int maxSpeedTransferPerTick) {
            return maxTransferPerTick(maxSpeedTransferPerTick);
        }

        /**
         * Sets how much FE the laser can receive per tick from external sources.
         */
        public Builder maxReceivePerTick(int maxReceivePerTick) {
            this.maxReceivePerTick = positive(maxReceivePerTick, "maxReceivePerTick");
            return this;
        }

        /**
         * Sets the default beam color used when warmup is disabled.
         */
        public Builder beamColor(int red, int green, int blue, int alpha) {
            this.beamColor = new Vec4i(color(red), color(green), color(blue), color(alpha));
            return this;
        }

        /**
         * Sets the default beam color used when warmup is disabled.
         */
        public Builder beamColor(Vec4i beamColor) {
            if (beamColor == null) {
                throw new IllegalArgumentException("beamColor cannot be null");
            }
            this.beamColor = beamColor;
            return this;
        }

        /**
         * Disables warmup and keeps the classic instant-transfer behavior.
         */
        public Builder warmupDisabled() {
            this.warmup = Warmup.disabled();
            return this;
        }

        /**
         * Enables warmup and sets its duration in ticks.
         */
        public Builder warmup(int warmupTicks) {
            this.warmup = Warmup.enabled(warmupTicks);
            return this;
        }

        /**
         * Alias for {@link #warmup(int)}.
         */
        public Builder rampUpTicks(int rampUpTicks) {
            return warmup(rampUpTicks);
        }

        /**
         * Overrides the beam colors used for the low, medium, and maximum speed stages.
         */
        public Builder warmupColors(Vec4i lowSpeedColor, Vec4i midSpeedColor, Vec4i maxSpeedColor) {
            if (lowSpeedColor == null || midSpeedColor == null || maxSpeedColor == null) {
                throw new IllegalArgumentException("Warmup colors cannot be null");
            }

            boolean enabled = warmup.enabled();
            int ticks = warmup.ticks();
            this.warmup = new Warmup(enabled, ticks, lowSpeedColor, midSpeedColor, maxSpeedColor);
            return this;
        }

        /**
         * Builds an immutable laser configuration.
         */
        public LaserConfig build() {
            return new LaserConfig(
                    targetRange,
                    energyBuffer,
                    maxTransferPerTick,
                    maxReceivePerTick,
                    beamColor,
                    warmup
            );
        }

        private static int positive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }

        private static int color(int value) {
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("Color channel must be between 0 and 255");
            }
            return value;
        }
    }

    /**
     * Warmup settings for a laser.
     *
     * <p>When enabled, the laser does not immediately transfer at maximum speed.
     * Instead, it ramps up over the configured number of ticks and can expose
     * different colors for low, medium, and maximum speed stages.</p>
     */
    public record Warmup(boolean enabled, int ticks, Vec4i lowSpeedColor, Vec4i midSpeedColor, Vec4i maxSpeedColor) {

        /**
         * Creates a disabled warmup configuration with default stage colors.
         */
        public static Warmup disabled() {
            return new Warmup(false, 0, new Vec4i(255, 40, 20, 200), new Vec4i(255, 220, 30, 200), new Vec4i(30, 100, 255, 200));
        }

        /**
         * Creates an enabled warmup configuration with default stage colors.
         */
        public static Warmup enabled(int ticks) {
            if (ticks <= 0) {
                throw new IllegalArgumentException("warmupTicks must be positive");
            }
            return new Warmup(true, ticks, new Vec4i(255, 40, 20, 200), new Vec4i(255, 220, 30, 200), new Vec4i(30, 100, 255, 200));
        }
    }
}
