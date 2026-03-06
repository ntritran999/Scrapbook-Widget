package com.group04.auth.register;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.ui.auth.register.AuthRegisterFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthRegisterFragmentTest {

    @Test
    public void testRegisterScreenDisplayed() throws InterruptedException {
        // Launch the fragment with the application theme
        FragmentScenario.launchInContainer(AuthRegisterFragment.class, null, R.style.Theme_ScrapbookWidget);

        // Verify that the title from view_auth_header is displayed
        onView(withText(R.string.register_welcome)).check(matches(isDisplayed()));

        // Verify that the subtitle from view_auth_header is displayed
        onView(withText(R.string.register_subtitle)).check(matches(isDisplayed()));

        // Verify the register button is displayed
        onView(withText(R.string.register)).check(matches(isDisplayed()));

        // Verify the login link button is displayed
        onView(withText(R.string.already_have_account)).check(matches(isDisplayed()));

        // Keep the screen open for manual inspection
        Thread.sleep(600000); 
    }
}
