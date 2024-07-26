package me.timeto.shared.vm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.timeto.shared.*
import me.timeto.shared.db.*
import me.timeto.shared.models.TaskUi
import me.timeto.shared.vm.ui.DayIntervalsUI
import me.timeto.shared.vm.ui.TimerDataUI

class HomeVm : __VM<HomeVm.State>() {

    data class State(
        val interval: IntervalDb,
        val isPurple: Boolean,
        val todayTasksUi: List<TaskUi>,
        val isTasksVisible: Boolean,
        val todayIntervalsUI: DayIntervalsUI?,
        val fdroidMessage: String?,
        val readmeMessage: String?,
        val whatsNewMessage: String?,
        val listsContainerSize: ListsContainerSize?,
        val idToUpdate: Long,
    ) {

        val timerData = TimerDataUI(interval, todayTasksUi.map { it.taskDb }, isPurple)

        val activity = interval.getActivityDI()

        // todo or use interval.getTriggers()
        val textFeatures = (interval.note ?: activity.name).textFeatures()

        val checklistDb: ChecklistDb? = textFeatures.checklists.firstOrNull()

        val triggers = textFeatures.triggers.filter {
            val clt = (it as? TextFeatures.Trigger.Checklist) ?: return@filter true
            val clDb = checklistDb ?: return@filter true
            return@filter clt.checklist.id != clDb.id
        }

        val goalsUI: List<GoalUI> = if (todayIntervalsUI == null)
            listOf()
        else DI.activitiesSorted
            .map { activity ->
                val activityName = activity.name.textFeatures().textNoFeatures
                activity.goals
                    .filter { it.period.isToday() }
                    .map { goal ->
                        var totalSeconds: Int = todayIntervalsUI.intervalsUI
                            .sumOf { if (it.activity?.id == activity.id) it.seconds else 0 }
                        val lastWithActivity = todayIntervalsUI.intervalsUI
                            .lastOrNull { it.activity != null }
                        if (lastWithActivity?.activity?.id == activity.id) {
                            val timeFinish = lastWithActivity.timeFinish()
                            val now = time()
                            if (now < timeFinish)
                                reportApi("MainActivity goal bad time $now $timeFinish")
                            totalSeconds += (now - timeFinish)
                        }

                        val timeDone = totalSeconds.limitMax(goal.seconds)
                        val timeLeft = goal.seconds - timeDone
                        val textRight = if (timeLeft > 0) timeLeft.toTimerHintNote(isShort = false) else "👍"
                        GoalUI(
                            textLeft = prepGoalTextLeft(
                                activityName = activityName,
                                secondsLeft = totalSeconds,
                            ),
                            textRight = textRight,
                            ratio = timeDone.toFloat() / goal.seconds.toFloat(),
                            bgColor = activity.colorRgba,
                        )
                    }
            }
            .flatten()

        val menuTime: String = UnixTime().getStringByComponents(UnixTime.StringComponent.hhmm24)
        val menuTasksNote = "${DI.tasks.count { it.isToday }}"

        val mainTasks: List<MainTask> = (
                if (KvDb.todayOnHomeScreenCached())
                    todayTasksUi.map { MainTask(it) }
                else
                    todayTasksUi
                        .mapNotNull { taskUi ->
                            val taskTf = taskUi.taskTf
                            if (taskTf.paused != null)
                                return@mapNotNull MainTask(taskUi)
                            if (taskTf.timeData?.type?.isEvent() == true)
                                return@mapNotNull MainTask(taskUi)
                            if (taskTf.isImportant)
                                return@mapNotNull MainTask(taskUi)
                            null
                        }
                                        )
            .sortedBy {
                it.taskUi.taskTf.timeData?.unixTime?.time ?: Int.MIN_VALUE
            }

        val listsSizes: ListsSizes = run {
            val lc = listsContainerSize ?: return@run ListsSizes(0f, 0f)
            //
            // No one
            if (checklistDb == null && mainTasks.isEmpty())
                return@run ListsSizes(0f, 0f)
            //
            // Only one
            if (checklistDb != null && mainTasks.isEmpty())
                return@run ListsSizes(checklist = lc.totalHeight, mainTasks = 0f)
            if (checklistDb == null && mainTasks.isNotEmpty())
                return@run ListsSizes(checklist = 0f, mainTasks = lc.totalHeight)
            //
            // Both
            checklistDb!!
            val halfHeight: Float = lc.totalHeight / 2
            val tasksCount: Int = mainTasks.size
            val tasksFullHeight: Float = tasksCount * lc.itemHeight
            // Tasks smaller the half
            if (tasksFullHeight < halfHeight)
                return@run ListsSizes(
                    checklist = lc.totalHeight - tasksFullHeight,
                    mainTasks = tasksFullHeight,
                )
            // Tasks bigger the half
            val checklistCount: Int = checklistDb.getItemsCached().size
            val checklistFullHeight: Float = checklistCount * lc.itemHeight
            if (checklistFullHeight < halfHeight)
                return@run ListsSizes(
                    checklist = checklistFullHeight,
                    mainTasks = lc.totalHeight - checklistFullHeight,
                )
            ListsSizes(checklist = halfHeight, mainTasks = halfHeight)
        }

        val batteryUi: BatteryUi = run {
            val level: Int? = batteryLevelOrNull
            val text = "${level ?: "--"}"
            when {
                isBatteryChargingOrNull == true ->
                    BatteryUi(text, if (level == 100) ColorRgba.green else ColorRgba.blue, true)

                batteryLevelOrNull in 0..20 ->
                    BatteryUi(text, ColorRgba.red, true)

                else -> BatteryUi(text, ColorRgba.homeFontSecondary, false)
            }
        }
    }

    override val state = MutableStateFlow(
        State(
            interval = DI.lastInterval,
            isPurple = false,
            todayTasksUi = listOf(),
            isTasksVisible = false,
            todayIntervalsUI = null, // todo init data
            fdroidMessage = null, // todo init data
            readmeMessage = null, // todo init data
            whatsNewMessage = null, // todo init data
            listsContainerSize = null,
            idToUpdate = 0,
        )
    )

    override fun onAppear() {
        val scope = scopeVM()
        IntervalDb.getLastOneOrNullFlow()
            .filterNotNull()
            .onEachExIn(scope) { interval ->
                state.update {
                    val isNewInterval = it.interval.id != interval.id
                    it.copy(
                        interval = interval,
                        isPurple = if (isNewInterval) false else it.isPurple,
                        isTasksVisible = if (isNewInterval) false else it.isTasksVisible,
                    )
                }
            }
        TaskDb
            .getAscFlow()
            .map { it.filter { task -> task.isToday } }
            .onEachExIn(scope) { tasks ->
                state.update { it.copy(todayTasksUi = tasks.map { it.toUi() }) }
            }
        if (deviceData.isFdroid)
            KvDb.KEY.IS_SENDING_REPORTS
                .getOrNullFlow()
                .onEachExIn(scope) { kvDb ->
                    state.update {
                        it.copy(fdroidMessage = if (kvDb == null) "Message for F-Droid users" else null)
                    }
                }
        KvDb.KEY.HOME_README_OPEN_TIME
            .getOrNullFlow()
            .onEachExIn(scope) { kvDb ->
                state.update {
                    it.copy(readmeMessage = if (kvDb == null) "How to use the app" else null)
                }
            }
        KvDb.KEY.WHATS_NEW_CHECK_UNIX_DAY
            .getOrNullFlow()
            .onEachExIn(scope) { kvDb ->
                val lastHistoryUnixDay = WhatsNewVm.prepHistoryItemsUi().first().unixDay
                val message: String? =
                    if ((kvDb == null) || (lastHistoryUnixDay > kvDb.value.toInt()))
                        "What's New"
                    else null
                state.update {
                    it.copy(whatsNewMessage = message)
                }
            }

        ////

        IntervalDb.anyChangeFlow()
            .onEachExIn(scope) {
                upTodayIntervalsUI()
            }
        scope.launch {
            while (true) {
                delayToNextMinute()
                upTodayIntervalsUI()
            }
        }

        ////

        scope.launch {
            while (true) {
                state.update {
                    it.copy(
                        interval = DI.lastInterval,
                        idToUpdate = it.idToUpdate + 1, // Force update
                    )
                }
                delay(1_000L)
            }
        }

        if (batteryLevelOrNull == null) {
            scope.launch {
                delay(2_000)
                if (batteryLevelOrNull == null)
                    reportApi("batteryLevelOrNull null")
            }
        }
    }

    fun upListsContainerSize(
        totalHeight: Float,
        itemHeight: Float,
    ) {
        val lc = ListsContainerSize(totalHeight, itemHeight)
        if (lc == state.value.listsContainerSize)
            return
        state.update { it.copy(listsContainerSize = lc) }
    }

    fun onReadmeOpen() {
        launchExDefault {
            KvDb.KEY.HOME_README_OPEN_TIME.upsertInt(time())
        }
    }

    fun toggleIsPurple() {
        state.update { it.copy(isPurple = !it.isPurple) }
    }

    fun toggleIsTasksVisible() {
        state.update { it.copy(isTasksVisible = !it.isTasksVisible) }
    }

    private suspend fun upTodayIntervalsUI() {
        val utcOffset = localUtcOffsetWithDayStart
        val todayDS = UnixTime(utcOffset = utcOffset).localDay
        val todayIntervalsUI = DayIntervalsUI
            .buildList(
                dayStart = todayDS,
                dayFinish = todayDS,
                utcOffset = utcOffset,
            )
            .first()
        state.update { it.copy(todayIntervalsUI = todayIntervalsUI) }
    }

    ///

    data class ListsContainerSize(
        val totalHeight: Float,
        val itemHeight: Float,
    )

    data class ListsSizes(
        val checklist: Float,
        val mainTasks: Float,
    )

    ///

    data class BatteryUi(
        val text: String,
        val colorRgba: ColorRgba,
        val isHighlighted: Boolean,
    )

    class GoalUI(
        val textLeft: String,
        val textRight: String,
        val ratio: Float,
        val bgColor: ColorRgba,
    )

    class MainTask(
        val taskUi: TaskUi,
    ) {

        val text = taskUi.taskTf.textUi()
        val timerContext = ActivityTimerSheetVM.TimerContext.Task(taskUi.taskDb)
        val timeUI: TimeUI? = taskUi.taskTf.timeData?.let { timeData ->
            val bgColor = when (timeData.status) {
                TextFeatures.TimeData.STATUS.IN -> ColorRgba.homeFg
                TextFeatures.TimeData.STATUS.SOON -> ColorRgba.blue
                TextFeatures.TimeData.STATUS.OVERDUE -> ColorRgba.red
            }

            val noteColor = when (timeData.status) {
                TextFeatures.TimeData.STATUS.IN -> ColorRgba.textSecondary
                TextFeatures.TimeData.STATUS.SOON -> ColorRgba.blue
                TextFeatures.TimeData.STATUS.OVERDUE -> ColorRgba.red
            }

            TimeUI(
                text = timeData.timeText(),
                textBgColor = bgColor,
                note = timeData.timeLeftText(),
                noteColor = noteColor,
            )
        }

        class TimeUI(
            val text: String,
            val textBgColor: ColorRgba,
            val note: String,
            val noteColor: ColorRgba,
        )
    }
}

private fun prepGoalTextLeft(
    activityName: String,
    secondsLeft: Int,
): String {
    if (secondsLeft == 0)
        return activityName
    val rem = secondsLeft % 60
    val secondsToUi = if (rem == 0) secondsLeft else (secondsLeft + (60 - rem))
    return "$activityName ${secondsToUi.toTimerHintNote(isShort = false)}"
}
