package me.timeto.shared.vm.ui

import me.timeto.shared.*
import me.timeto.shared.db.IntervalDb
import kotlin.math.absoluteValue

class TimerDataUI(
    interval: IntervalDb,
    isPurple: Boolean,
) {

    val status: STATUS
    val timerText: String
    val timerColor: ColorRgba

    private val restartTimer = interval.note?.textFeatures()?.paused?.timer ?: interval.timer
    val restartText = restartTimer.toTimerHintNote(isShort = true)

    init {
        val now = time()
        val timeLeft = interval.id + interval.timer - now

        class TmpDTO(val color: ColorRgba, val timeLeft: Int, val status: STATUS)

        val pomodoro: Int = interval.getActivityDI().pomodoro_timer
        val tmpData: TmpDTO = when {
            timeLeft < -pomodoro -> TmpDTO(ColorRgba.red, -timeLeft - pomodoro, STATUS.OVERDUE)
            timeLeft <= 0 -> TmpDTO(ColorRgba.green, timeLeft + pomodoro, STATUS.BREAK)
            else -> TmpDTO(defColor, timeLeft, STATUS.PROCESS)
        }

        val timeForTitle = if (isPurple) (now - interval.id) else tmpData.timeLeft

        status = tmpData.status
        title = secondsToString(timeForTitle)
        color = if (isPurple) ColorRgba.purple else tmpData.color
    }

    fun restart() {
        launchExDefault {
            val lastInterval = IntervalDb.getLastOneOrNull()!!
            IntervalDb.addWithValidation(restartTimer, lastInterval.getActivityDI(), lastInterval.note)
        }
    }

    enum class STATUS {

        PROCESS, BREAK, OVERDUE;

        fun isProcess() = this == PROCESS
        fun isBreak() = this == BREAK
        fun isOverdue() = this == OVERDUE
    }
}

private fun secondsToString(seconds: Int): String {
    val hms = seconds.absoluteValue.toHms()
    val h = if (hms[0] > 0) "${hms[0]}:" else ""
    val m = hms[1].toString().padStart(2, '0') + ":"
    val s = hms[2].toString().padStart(2, '0')
    return "$h$m$s"
}
