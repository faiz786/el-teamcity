package pl.elpassion.eltc

import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import org.hamcrest.core.IsNot.not

fun ViewInteraction.isDisplayedEffectively(): ViewInteraction =
        check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

fun ViewInteraction.isClickable(): ViewInteraction = check(matches(ViewMatchers.isClickable()))

fun ViewInteraction.isNotClickable(): ViewInteraction = check(matches(not(ViewMatchers.isClickable())))