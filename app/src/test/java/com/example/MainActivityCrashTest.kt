package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityCrashTest {
    @Test
    fun testActivityLaunches() {
        Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }
}
