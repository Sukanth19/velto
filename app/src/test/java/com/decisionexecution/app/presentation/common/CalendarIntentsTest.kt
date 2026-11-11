package com.decisionexecution.app.presentation.common

import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.service.allDayEndMillis
import com.decisionexecution.app.domain.service.allDayStartMillis
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Handing a task to the calendar app when no calendar is linked */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarIntentsTest {

    @Test
    fun opensANewAllDayEventFilledInFromTheTask() {
        val monday = LocalDate.of(2026, 11, 16)
        val intent = calendarInsertIntent(CalendarEvent("Study for test", "Chapters 4 to 6", monday, null))

        intent.action shouldBe Intent.ACTION_INSERT
        intent.data shouldBe Events.CONTENT_URI
        intent.getStringExtra(Events.TITLE) shouldBe "Study for test"
        intent.getStringExtra(Events.DESCRIPTION) shouldBe "Chapters 4 to 6"
        intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false) shouldBe true
        intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0) shouldBe allDayStartMillis(monday)
        intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0) shouldBe allDayEndMillis(monday)
    }
}
