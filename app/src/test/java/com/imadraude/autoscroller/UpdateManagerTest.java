package com.imadraude.autoscroller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UpdateManagerTest {

    @Test
    public void parsesReleaseBuildTag() {
        assertEquals(31, UpdateManager.parseBuildNumber("build-31"));
        assertEquals(1001, UpdateManager.parseBuildNumber("build-1001"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnexpectedReleaseTag() {
        UpdateManager.parseBuildNumber("v1.0");
    }
}
