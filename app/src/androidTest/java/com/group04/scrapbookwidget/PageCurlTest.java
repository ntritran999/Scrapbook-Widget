package com.group04.scrapbookwidget;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.group04.scrapbookwidget.ui.PageFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PageCurlTest {
    @Test
    public void testCurlEffect() {
        FragmentScenario<PageFragment> scenario = FragmentScenario.launchInContainer(PageFragment.class);
        try {
            Thread.sleep(300_000); // Time-out for testing curl effect
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
