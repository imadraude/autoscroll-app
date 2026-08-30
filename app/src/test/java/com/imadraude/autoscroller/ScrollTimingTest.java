package com.imadraude.autoscroller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScrollTimingTest {

    @Test
    public void speedDurationsMatchExpectedProfile() {
        assertEquals(1200L, ScrollTiming.durationForSpeed(1));
        assertEquals(900L, ScrollTiming.durationForSpeed(2));
        assertEquals(650L, ScrollTiming.durationForSpeed(3));
        assertEquals(450L, ScrollTiming.durationForSpeed(4));
        assertEquals(300L, ScrollTiming.durationForSpeed(5));
    }

    @Test
    public void pausesMatchExpectedProfile() {
        assertEquals(650L, ScrollTiming.pauseForSpeed(1));
        assertEquals(420L, ScrollTiming.pauseForSpeed(2));
        assertEquals(250L, ScrollTiming.pauseForSpeed(3));
        assertEquals(140L, ScrollTiming.pauseForSpeed(4));
        assertEquals(70L, ScrollTiming.pauseForSpeed(5));
    }

    @Test
    public void invalidLevelsFallBackToDefault() {
        assertEquals(ScrollTiming.DEFAULT_LEVEL, ScrollTiming.normalize(0));
        assertEquals(ScrollTiming.DEFAULT_LEVEL, ScrollTiming.normalize(6));
        assertEquals(650L, ScrollTiming.durationForSpeed(-100));
        assertEquals(250L, ScrollTiming.pauseForSpeed(100));
    }

    @Test
    public void speedChangesStayInsideBounds() {
        assertEquals(1, ScrollTiming.slower(1));
        assertEquals(2, ScrollTiming.slower(3));
        assertEquals(4, ScrollTiming.faster(3));
        assertEquals(5, ScrollTiming.faster(5));
    }
}
