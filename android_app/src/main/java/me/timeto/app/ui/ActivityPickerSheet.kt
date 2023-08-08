package me.timeto.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.timeto.app.c
import me.timeto.app.rememberVM
import me.timeto.shared.db.ActivityModel
import me.timeto.shared.vm.ActivityPickerSheetVM

@Composable
fun ActivityPickerSheet(
    layer: WrapperView.Layer,
    onPick: (ActivityModel) -> Unit,
) {

    val (_, state) = rememberVM { ActivityPickerSheetVM() }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(c.sheetBg)
    ) {

        val scrollState = rememberScrollState()

        Sheet.HeaderViewOld(
            onCancel = { layer.close() },
            title = state.headerTitle,
            doneText = null,
            isDoneEnabled = false,
            scrollState = scrollState,
            cancelText = "Back",
        ) {}

        Column(
            modifier = Modifier
                .verticalScroll(state = scrollState)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {

            Row(Modifier.height(20.dp)) { }

            val activitiesUI = state.activitiesUI
            activitiesUI.forEach { activityUI ->
                val isFirst = state.activitiesUI.first() == activityUI
                MyListView__ItemView(
                    isFirst = isFirst,
                    isLast = state.activitiesUI.last() == activityUI,
                    withTopDivider = !isFirst,
                ) {
                    MyListView__ItemView__ButtonView(
                        text = activityUI.text,
                    ) {
                        onPick(activityUI.activity)
                        layer.close()
                    }
                }
            }
        }
    }
}
