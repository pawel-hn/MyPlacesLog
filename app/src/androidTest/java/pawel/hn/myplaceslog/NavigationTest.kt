package pawel.hn.myplaceslog

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class NavigationTest {

    private lateinit var navController: TestNavHostController
    private lateinit var placesScenario: FragmentScenario<PlacesFragment>
    private lateinit var addItemScenario: FragmentScenario<AddItemFragment>

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        placesScenario = launchFragmentInContainer(themeResId = R.style.Theme_MyPlacesLog)
        addItemScenario = launchFragmentInContainer(themeResId = R.style.Theme_MyPlacesLog)
    }


    @Test
    fun testNavigationToAddItemFragmentViaFloatButton() {
        placesScenario.onFragment { placesFragment ->
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(placesFragment.requireView(), navController)
        }

        onView(ViewMatchers.withId(R.id.button_add_item)).perform(click())
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.addItemFragment)
    }

    @Test
    fun testNavigateUpFromAddItemFragment() {
        addItemScenario.onFragment {  addItemFragment ->
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(addItemFragment.requireView(), navController)
        }

        onView(
            Matchers.allOf(
                withContentDescription("Navigate up"),
                isDisplayed()
            )
        ).perform(click())
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.placesFragment)
    }


}