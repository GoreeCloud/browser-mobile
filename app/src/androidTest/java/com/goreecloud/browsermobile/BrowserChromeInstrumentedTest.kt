package com.goreecloud.browsermobile

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device, opt-in UI smoke tests. The normal CI workflow compiles these tests
 * but does not install or execute them on a handset.
 *
 * MainActivity initializes its normal filter-list engine on launch, which may
 * refresh the enabled HTTPS subscriptions. Use an explicitly authorized test device.
 */
@RunWith(AndroidJUnit4::class)
class BrowserChromeInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeAndNewTabRemainSeparateSurfaces() {
        composeRule.onNodeWithText("Where to next?").assertExists()

        composeRule.onNodeWithContentDescription("Home").performClick()
        composeRule.onNodeWithText("Your places").assertExists()

        composeRule.onNodeWithContentDescription("New tab").performClick()
        composeRule.onNodeWithText("Where to next?").assertExists()
    }

    @Test
    fun optionalPrivacyFiltersRemainDiscoverable() {
        composeRule.onNodeWithContentDescription("Home").performClick()
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithText("Privacy center").assertExists()

        composeRule.onNodeWithText("Manage filter lists").performScrollTo().performClick()
        composeRule.onNodeWithText("Hide filter lists").assertExists()

        composeRule.onNodeWithText("Hide filter lists").performScrollTo().performClick()
        composeRule.onNodeWithText("Manage filter lists").assertExists()
    }
}
