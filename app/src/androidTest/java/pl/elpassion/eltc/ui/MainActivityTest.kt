package pl.elpassion.eltc.ui

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.elpassion.android.commons.espresso.isDisplayed
import com.elpassion.android.commons.espresso.isNotDisplayed
import com.elpassion.android.commons.espresso.onId
import com.elpassion.android.commons.espresso.onText
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.elpassion.eltc.*

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @JvmField @Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    val states: Subject<AppState> = BehaviorSubject.createDefault(InitialState)

    init {
        DI.provideTeamCityModel = { mock { on { state }.thenReturn(states) } }
    }

    @Test
    fun Do_not_display_any_screen_before_state_change() {
        onId(R.id.loginScreen).isNotDisplayed()
        onId(R.id.loadingScreen).isNotDisplayed()
        onId(R.id.buildsScreen).isNotDisplayed()
    }

    @Test
    fun Display_login_screen() {
        states.onNext(LoginState())
        onId(R.id.loginScreen).isDisplayed()
    }

    @Test
    fun Display_loading_screen() {
        states.onNext(LoadingState)
        onId(R.id.loadingScreen).isDisplayed()
    }

    @Test
    fun Display_builds_screen_with_provided_data() {
        states.onNext(MainState(listOf(createBuild(667)), emptyList()))
        onText("#667").isDisplayed()
    }
}