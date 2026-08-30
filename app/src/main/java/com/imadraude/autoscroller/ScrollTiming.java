package com.imadraude.autoscroller;

final class ScrollTiming {

    static final int MIN_LEVEL = 1;
    static final int MAX_LEVEL = 5;
    static final int DEFAULT_SWIPE_SPEED_LEVEL = 3;
    static final int DEFAULT_FREQUENCY_LEVEL = 3;

    private ScrollTiming() {
    }

    static int slowerSwipe(int level) {
        return Math.max(MIN_LEVEL, normalizeSwipeSpeed(level) - 1);
    }

    static int fasterSwipe(int level) {
        return Math.min(MAX_LEVEL, normalizeSwipeSpeed(level) + 1);
    }

    static int lessFrequent(int level) {
        return Math.max(MIN_LEVEL, normalizeFrequency(level) - 1);
    }

    static int moreFrequent(int level) {
        return Math.min(MAX_LEVEL, normalizeFrequency(level) + 1);
    }

    static long swipeDurationForSpeed(int level) {
        switch (normalizeSwipeSpeed(level)) {
            case 1:
                return 320L;
            case 2:
                return 250L;
            case 3:
                return 190L;
            case 4:
                return 140L;
            case 5:
                return 100L;
            default:
                throw new AssertionError("Unexpected normalized swipe speed level");
        }
    }

    static long periodForFrequency(int level) {
        switch (normalizeFrequency(level)) {
            case 1:
                return 3000L;
            case 2:
                return 2000L;
            case 3:
                return 1200L;
            case 4:
                return 750L;
            case 5:
                return 450L;
            default:
                throw new AssertionError("Unexpected normalized frequency level");
        }
    }

    static long delayAfterGesture(int frequencyLevel, long gestureDuration) {
        long period = periodForFrequency(frequencyLevel);
        return Math.max(80L, period - Math.max(0L, gestureDuration));
    }

    static int normalizeSwipeSpeed(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            return DEFAULT_SWIPE_SPEED_LEVEL;
        }
        return level;
    }

    static int normalizeFrequency(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            return DEFAULT_FREQUENCY_LEVEL;
        }
        return level;
    }
}
