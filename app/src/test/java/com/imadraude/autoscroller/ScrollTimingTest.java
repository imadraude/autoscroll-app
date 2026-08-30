package com.imadraude.autoscroller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScrollTimingTest {

    @Test
    public void swipeDurationsMatchExpectedProfile() {
        assertEquals(320L, ScrollTiming.swipeDurationForSpeed(1));
        assertEquals(250L, ScrollTiming.swipeDurationForSpeed(2));
        assertEquals(190L, ScrollTiming.swipeDurationForSpeed(3));
        assertEquals(140L, ScrollTiming.swipeDurationForSpeed(4));
        assertEquals(100L, ScrollTiming.swipeDurationForSpeed(5));
    }

    @Test
    public void frequencyPeriodsMatchExpectedProfile() {
        assertEquals(3000L, ScrollTiming.periodForFrequency(1));
        assertEquals(2000L, ScrollTiming.periodForFrequency(2));
        assertEquals(1200L, ScrollTiming.periodForFrequency(3));
        assertEquals(750L, ScrollTiming.periodForFrequency(4));
        assertEquals(450L, ScrollTiming.periodForFrequency(5));
    }

    @Test
    public void delayAccountsForSwipeDuration() {
        assertEquals(1010L, ScrollTiming.delayAfterGesture(3, 190L));
        assertEquals(80L, ScrollTiming.delayAfterGesture(5, 1000L));
    }

    @Test
    public void invalidLevelsFallBackToDefaults() {
        assertEquals(ScrollTiming.DEFAULT_SWIPE_SPEED_LEVEL, ScrollTiming.normalizeSwipeSpeed(0));
        assertEquals(ScrollTiming.DEFAULT_FREQUENCY_LEVEL, ScrollTiming.normalizeFrequency(6));
        assertEquals(190L, ScrollTiming.swipeDurationForSpeed(-100));
        assertEquals(1200L, ScrollTiming.periodForFrequency(100));
    }

    @Test
    public void controlsStayInsideBounds() {
        assertEquals(1, ScrollTiming.slowerSwipe(1));
        assertEquals(2, ScrollTiming.slowerSwipe(3));
        assertEquals(4, ScrollTiming.fasterSwipe(3));
        assertEquals(5, ScrollTiming.fasterSwipe(5));
        assertEquals(1, ScrollTiming.lessFrequent(1));
        assertEquals(2, ScrollTiming.lessFrequent(3));
        assertEquals(4, ScrollTiming.moreFrequent(3));
        assertEquals(5, ScrollTiming.moreFrequent(5));
    }
}
