package me.timeto.shared

import me.timeto.shared.db.ActivityModel
import me.timeto.shared.db.ChecklistModel
import me.timeto.shared.db.ShortcutModel
import kotlin.math.absoluteValue

data class TextFeatures(
    val textNoFeatures: String,
    val checklists: List<ChecklistModel>,
    val shortcuts: List<ShortcutModel>,
    val fromRepeating: FromRepeating?,
    val fromEvent: FromEvent?,
    val activity: ActivityModel?,
    val timer: Int?,
    val paused: Paused?,
    val isImportant: Boolean,
) {

    val timeData: TimeData? = when {
        fromRepeating?.time != null -> TimeData(UnixTime(fromRepeating.time), TimeData.TYPE.REPEATING, this)
        fromEvent != null -> TimeData(fromEvent.unixTime, TimeData.TYPE.EVENT, this)
        else -> null
    }

    val triggers: List<Trigger> by lazy {
        checklists.map { Trigger.Checklist(it) } +
        shortcuts.map { Trigger.Shortcut(it) }
    }

    fun textUi(
        withActivityEmoji: Boolean = true,
        withPausedEmoji: Boolean = false,
        withTimer: Boolean = true,
        timerPrefix: String = "",
    ): String {
        val a = mutableListOf(textNoFeatures)
        if (paused != null && withPausedEmoji)
            a.add(0, "⏸️")
        if (activity != null && withActivityEmoji)
            a.add(activity.emoji)
        if (timer != null && withTimer)
            a.add(timerPrefix + timer.toTimerHintNote(isShort = false))
        return a.joinToString(" ")
    }

    fun textWithFeatures(): String {
        val strings = mutableListOf(textNoFeatures.trim())
        if (checklists.isNotEmpty())
            strings.add(checklists.joinToString(" ") { "#c${it.id}" })
        if (shortcuts.isNotEmpty())
            strings.add(shortcuts.joinToString(" ") { "#s${it.id}" })
        if (fromRepeating != null)
            strings.add("#r${fromRepeating.id}_${fromRepeating.day}_${fromRepeating.time ?: ""}")
        if (fromEvent != null)
            strings.add("#e${fromEvent.unixTime.time}")
        if (activity != null)
            strings.add("#a${activity.id}")
        if (timer != null)
            strings.add("#t$timer")
        if (paused != null)
            strings.add("#paused${paused.intervalId}_${paused.timer}")
        if (isImportant)
            strings.add(isImportantSubstring)
        return strings.joinToString(" ")
    }

    // Day to sync! May be different from the real one meaning "Day Start"
    // setting. "day" is used for sorting within "Today" tasks list.
    class FromRepeating(val id: Int, val day: Int, val time: Int?)

    class FromEvent(val unixTime: UnixTime)

    class Paused(val intervalId: Int, val timer: Int)

    sealed class Trigger(
        val id: String,
        val title: String,
        val emoji: String,
        val color: ColorRgba,
    ) {

        fun performUI() {
            val _when = when (this) {
                is Checklist -> launchExDefault { checklist.performUI() }
                is Shortcut -> launchExDefault { shortcut.performUI() }
            }
        }

        class Checklist(
            val checklist: ChecklistModel
        ) : Trigger("#c${checklist.id}", checklist.name, "✅", ColorRgba.green)

        class Shortcut(
            val shortcut: ShortcutModel
        ) : Trigger("#s${shortcut.id}", shortcut.name, "↗️", ColorRgba.red)
    }

    class TimeData(
        val unixTime: UnixTime,
        val type: TYPE,
        val _textFeatures: TextFeatures,
    ) {

        val secondsLeft: Int = unixTime.time - time()

        val status: STATUS = when {
            secondsLeft > 3_600 -> STATUS.IN
            secondsLeft > 0 -> STATUS.SOON
            else -> STATUS.OVERDUE
        }

        //////

        fun timeLeftText(): String = when (status) {
            STATUS.IN,
            STATUS.SOON -> secondsInToString(secondsLeft)
            STATUS.OVERDUE -> secondsOverdueToString(secondsLeft)
        }

        fun buildTimeDataUI(
            isHighlight: Boolean,
        ): TimeDataUI {
            val timeLeftText = timeLeftText()
            val textColor = when (status) {
                STATUS.IN -> ColorRgba.textSecondary
                STATUS.SOON -> ColorRgba.blue
                STATUS.OVERDUE -> ColorRgba.red
            }

            if (isHighlight) {

                val timeComponents =
                    if (unixTime.isToday())
                        listOf(UnixTime.StringComponent.hhmm24)
                    else
                        listOf(
                            UnixTime.StringComponent.dayOfMonth,
                            UnixTime.StringComponent.space,
                            UnixTime.StringComponent.month3,
                            UnixTime.StringComponent.comma,
                            UnixTime.StringComponent.space,
                            UnixTime.StringComponent.hhmm24,
                        )

                val backgroundColor = if (status.isOverdue()) ColorRgba.red else ColorRgba.blue

                return TimeDataUI.HighlightUI(
                    timeData = this,
                    title = unixTime.getStringByComponents(timeComponents),
                    backgroundColor = backgroundColor,
                    timeLeftText = timeLeftText,
                    timeLeftColor = textColor,
                )
            }

            val daytimeText = daytimeToString(unixTime.time - unixTime.localDayStartTime())
            return TimeDataUI.RegularUI(
                timeData = this,
                text = "$daytimeText  $timeLeftText",
                textColor = textColor,
            )
        }

        enum class TYPE {

            EVENT, REPEATING;

            fun isEvent() = this == EVENT
            fun isRepeating() = this == REPEATING
        }

        enum class STATUS {

            IN, SOON, OVERDUE;

            fun isIn() = this == IN
            fun isSoon() = this == SOON
            fun isOverdue() = this == OVERDUE
        }

        sealed class TimeDataUI(
            val _timeData: TimeData,
        ) {

            class HighlightUI(
                timeData: TimeData,
                val title: String,
                val backgroundColor: ColorRgba,
                val timeLeftText: String,
                val timeLeftColor: ColorRgba,
            ) : TimeDataUI(timeData)

            class RegularUI(
                timeData: TimeData,
                val text: String,
                val textColor: ColorRgba,
            ) : TimeDataUI(timeData)
        }
    }
}

fun String.textFeatures(): TextFeatures = parseLocal(this)

//////

private val checklistRegex = "#c(\\d{10})".toRegex()
private val shortcutRegex = "#s(\\d{10})".toRegex()
private val fromRepeatingRegex = "#r(\\d{10})_(\\d{5})_(\\d{10})?".toRegex()
private val fromEventRegex = "#e(\\d{10})".toRegex()
private val activityRegex = "#a(\\d{10})".toRegex()
private val timerRegex = "#t(\\d+)".toRegex()
private val pausedRegex = "#paused(\\d{10})_(\\d+)".toRegex()
private const val isImportantSubstring = "#important"

private fun parseLocal(initText: String): TextFeatures {

    var textNoFeatures = initText
    fun MatchResult.clean() {
        textNoFeatures = textNoFeatures.replace(this.value, "")
    }

    val checklists: List<ChecklistModel> = checklistRegex
        .findAll(textNoFeatures)
        .map { match ->
            val id = match.groupValues[1].toInt()
            val checklist = DI.getChecklistByIdOrNull(id) ?: return@map null
            match.clean()
            checklist
        }
        .filterNotNull()
        .toList()

    val shortcuts: List<ShortcutModel> = shortcutRegex
        .findAll(textNoFeatures)
        .map { match ->
            val id = match.groupValues[1].toInt()
            val shortcut = DI.getShortcutByIdOrNull(id) ?: return@map null
            match.clean()
            shortcut
        }
        .filterNotNull()
        .toList()

    val fromRepeating: TextFeatures.FromRepeating? = fromRepeatingRegex
        .find(textNoFeatures)?.let { match ->
            val id = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val time = match.groupValues[3].takeIf { it.isNotBlank() }?.toInt()
            match.clean()
            return@let TextFeatures.FromRepeating(id, day, time)
        }

    val fromEvent: TextFeatures.FromEvent? = fromEventRegex
        .find(textNoFeatures)?.let { match ->
            val time = match.groupValues[1].toInt()
            match.clean()
            return@let TextFeatures.FromEvent(UnixTime(time))
        }

    val activity: ActivityModel? = activityRegex
        .find(textNoFeatures)?.let { match ->
            val id = match.groupValues[1].toInt()
            val activity = DI.getActivityByIdOrNull(id) ?: return@let null
            match.clean()
            return@let activity
        }

    val timer: Int? = timerRegex
        .find(textNoFeatures)?.let { match ->
            val time = match.groupValues[1].toInt()
            match.clean()
            return@let time
        }

    val paused: TextFeatures.Paused? = pausedRegex
        .find(textNoFeatures)?.let { match ->
            val intervalId = match.groupValues[1].toInt()
            val intervalTimer = match.groupValues[2].toInt()
            match.clean()
            return@let TextFeatures.Paused(intervalId, intervalTimer)
        }

    val isImportant = isImportantSubstring in textNoFeatures
    if (isImportant)
        textNoFeatures = textNoFeatures.replace(isImportantSubstring, "")

    return TextFeatures(
        textNoFeatures = textNoFeatures.removeDuplicateSpaces().trim(),
        checklists = checklists,
        shortcuts = shortcuts,
        fromRepeating = fromRepeating,
        fromEvent = fromEvent,
        activity = activity,
        timer = timer,
        paused = paused,
        isImportant = isImportant,
    )
}

//////

private fun secondsInToString(seconds: Int): String {
    val (h, m) = seconds.toHms(roundToNextMinute = true)
    val d = h / 24
    return when {
        d >= 1 -> "In ${d.toStringEndingDays()}"
        h >= 10 -> "In ${h.toStringEndingHours()}"
        h > 0 -> "In ${h.toStringEndingHours()}${if (m == 0) "" else " $m min"}"
        else -> "In ${m.toStringEnding(true, "minute", "min")}"
    }
}

private fun secondsOverdueToString(seconds: Int): String {
    val (h, m) = seconds.absoluteValue.toHms()
    val d = h / 24
    return when {
        d >= 1 -> d.toStringEndingDays() + " overdue"
        h > 0 -> h.toStringEndingHours() + " overdue"
        m == 0 -> "Now! 🙀"
        else -> "$m min overdue"
    }
}

private fun Int.toStringEndingHours() = toStringEnding(true, "hour", "hours")
private fun Int.toStringEndingDays() = toStringEnding(true, "day", "days")
