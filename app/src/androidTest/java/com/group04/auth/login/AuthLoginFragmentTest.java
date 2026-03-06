package com.group04.auth.login;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.ui.auth.login.AuthLoginFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthLoginFragmentTest {

    @Test
    public void testLoginScreenDisplayed() throws InterruptedException {
        // Launch the fragment with the application theme using the 3-argument version to avoid ambiguity
        FragmentScenario.launchInContainer(AuthLoginFragment.class, null, R.style.Theme_ScrapbookWidget);

        // Verify that the title from view_auth_header is displayed
        onView(withText(R.string.login_welcome)).check(matches(isDisplayed()));

        // Verify that the subtitle from view_auth_header is displayed
        onView(withText(R.string.login_subtitle)).check(matches(isDisplayed()));

        // Verify the login button is displayed
        onView(withText(R.string.login)).check(matches(isDisplayed()));

        // Keep the screen open for a long time (e.g., 10 minutes) for manual inspection.
        // You can stop the test manually in Android Studio when you're done.
        Thread.sleep(600000);
    }
}
