package com.imadraude.autoscroller;

final class ScrollTiming {

    static final int MIN_LEVEL = 1;
    static final int MAX_LEVEL = 5;
    static final int DEFAULT_LEVEL = 3;

    private ScrollTiming() {
    }

    static int slower(int level) {
        return Math.max(MIN_LEVEL, normalize(level) - 1);
    }

    static int faster(int level) {
        return Math.min(MAX_LEVEL, normalize(level) + 1);
    }

    static long durationForSpeed(int level) {
        switch (normalize(level)) {
            case 1:
                return 1200L;
            case 2:
                return 900L;
            case 3:
                return 650L;
            case 4:
                return 450L;
            case 5:
                return 300L;
            default:
                throw new AssertionError("Unexpected normalized speed level");
        }
    }

    static long pauseForSpeed(int level) {
        switch (normalize(level)) {
            case 1:
                return 650L;
            case 2:
                return 420L;
            case 3:
                return 250L;
            case 4:
                return 140L;
            case 5:
                return 70L;
            default:
                throw new AssertionError("Unexpected normalized speed level");
        }
    }

    static int normalize(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            return DEFAULT_LEVEL;
        }
        return level;
    }
}
