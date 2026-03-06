package com.group04.scrapbookwidget;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.group04.scrapbookwidget.ui.PhotoDialogFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PhotoFlipTest {
    @Test
    public void photoFlipEffect() {
        FragmentScenario<PhotoDialogFragment> scenario = FragmentScenario.launchInContainer(PhotoDialogFragment.class);
        try {
            Thread.sleep(300_000); // Time-out for testing curl effect
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
