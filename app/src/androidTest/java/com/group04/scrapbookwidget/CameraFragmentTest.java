package com.group04.scrapbookwidget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.camera.core.Camera;
import androidx.camera.core.ImageCapture;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.group04.scrapbookwidget.ui.CameraFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CameraFragmentTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA);

    @Test
    public void testFlashStateChangesCorrectly() {
        FragmentScenario<CameraFragment> scenario = FragmentScenario.launchInContainer(CameraFragment.class, null, R.style.Theme_ScrapbookWidget);

        onView(withId(R.id.btnFlash)).perform(click());

        scenario.onFragment(fragment -> {
            assertEquals("FLASH IS NOT TURNED ON!", ImageCapture.FLASH_MODE_ON, fragment.getFlashMode());
        });

        onView(withId(R.id.btnFlash)).perform(click());

        scenario.onFragment(fragment -> {
            assertEquals("FLASH IS NOT TURNED OFF", ImageCapture.FLASH_MODE_OFF, fragment.getFlashMode());
        });
    }

    @Test
    public void testCameraHardwareZoomChanges() throws InterruptedException {
        FragmentScenario<CameraFragment> scenario = FragmentScenario.launchInContainer(CameraFragment.class, null, R.style.Theme_ScrapbookWidget);

        Thread.sleep(2000);

        scenario.onFragment(fragment -> {
            Camera camera = fragment.getCamera();
            assertNotNull("CAMERA IS NOT INITIALIZED SUCCESSFULLY", camera);

            float initialZoom = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();

            float maxZoom = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();

            if (maxZoom <= initialZoom) {
                System.out.println("ZOOM NOT SUPPORTED FOR THIS EMULATOR, SKIP TEST.");
                return;
            }

            float targetZoom = Math.min(initialZoom * 1.5f, maxZoom);
            camera.getCameraControl().setZoomRatio(targetZoom);

            try { Thread.sleep(500); } catch (InterruptedException e) {}

            float currentZoom = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
            assertTrue("ZOOM RATIO IS NOT CHANGED!", currentZoom > initialZoom);
        });
    }
}