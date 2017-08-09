package com.putao.ptx.camera

import android.graphics.Point
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SdkSuppress
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test, which will execute on an Android device.

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 18)
class ExampleInstrumentedTest {
    //    @Test
//    @Throws(Exception::class)
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getTargetContext()
//
//        assertEquals("com.putao.test", appContext.packageName)
//    }
    val TAG = "Example__Test "
    val uiDevice: UiDevice by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    @Test
    fun testAnr() {
        var i = 0
        val ptPort = Point(1400, 1024)
        val ptLand = Point(1915, 790)
        val camera = uiDevice.findObject(UiSelector().resourceId("com.putao.ptx.camera:id/controlMenu"))
        val btnNo = uiDevice.findObject(UiSelector().resourceId("com.putao.ptx.accountcenter:id/cropcircle_no"))
        val exists = { camera.exists() }
        val tm = System.currentTimeMillis()
        exists()
        val tmDiff = System.currentTimeMillis() - tm
        val waste = "waste time:$tmDiff"
        while (i < 10_000_000) {
//            val tm = System.currentTimeMillis()
//            exists()
//            val tmDiff = System.currentTimeMillis() - tm
//            val waste = "waste time:$tmDiff"
            if (exists()) {
                val bNaturalOri = uiDevice.isNaturalOrientation
                if (bNaturalOri) {
                    clickSleep(ptPort, times = 200, exist = exists)
                    print("displayHeight:${uiDevice.displayHeight}")
                } else {
                    clickSleep(ptLand, times = 200, exist = exists)
                }
            }
            if (btnNo.exists()) {
                btnNo.click()
            }
            println("testAnr")
            Thread.sleep(200)
            i++
            Log.d(TAG, "testAnr time:$i")/*InstrumentationRegistry.getTargetContext()*/
        }
        println("finish testAnr!!!")

    }

    inline private fun clickSleep(pt: Point, times: Int = 10, sleep: Long = 30, exist: () -> Boolean) {
        repeat(times) {
            uiDevice.click(pt.x, pt.y)
            Thread.sleep(sleep)
            if (it > 10 && !exist()) return
        }
    }
}
