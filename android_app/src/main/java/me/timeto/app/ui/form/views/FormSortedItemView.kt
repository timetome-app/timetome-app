package me.timeto.app.ui.form.views

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.timeto.app.HStack
import me.timeto.app.H_PADDING
import me.timeto.app.R
import me.timeto.app.ZStack
import me.timeto.app.c
import me.timeto.app.mics.Haptic
import me.timeto.app.ui.DividerBg
import me.timeto.app.ui.form.Form__itemMinHeight
import kotlin.math.absoluteValue

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LazyItemScope.FormSortedItemView(
    title: String,
    isFirst: Boolean,
    itemIdx: Int,
    sortedState: FormSortedState,
    sortedMovingIdx: MutableState<Int?>,
    onMove: (Int, Int) -> Unit,
    onFinish: () -> Unit,
    onClick: () -> Unit,
) {

    DisposableEffect(Unit) {
        onDispose {
            sortedState.idxToYMap.remove(itemIdx)
        }
    }

    ZStack(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .background(c.bg),
    ) {

        HStack(
            modifier = Modifier
                .clickable {
                    onClick()
                }
                .sizeIn(minHeight = Form__itemMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                title,
                modifier = Modifier
                    .padding(start = H_PADDING)
                    .weight(1f),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            HStack(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val y: Int = coordinates.positionInWindow().y.toInt()
                        val height: Int = coordinates.size.height
                        if (sortedMovingIdx.value == null) {
                            sortedState.idxToYMap[itemIdx] = (y + (height / 2))
                        }
                    }
                    .height(Form__itemMinHeight)
                    .motionEventSpy { event ->
                        if (sortedMovingIdx.value == null) {
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                sortedMovingIdx.value = itemIdx
                                hapticFeedback()
                            }
                            return@motionEventSpy
                        }
                        if (event.action == MotionEvent.ACTION_UP) {
                            sortedMovingIdx.value = null
                            onFinish()
                        } else if (event.action == MotionEvent.ACTION_MOVE) {
                            val newIdx: Int? = sortedState.idxToYMap
                                .map { it.key to (it.value - event.y).absoluteValue }
                                .minByOrNull { it.second }
                                ?.first
                            if ((newIdx != null) && (newIdx != itemIdx)) {
                                onMove(itemIdx, newIdx)
                                hapticFeedback()
                            }
                        } else {
                            // Other action like many taps
                            sortedMovingIdx.value = null
                            onFinish()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.sf_line_3_horizontal_medium_light),
                    contentDescription = "Sort",
                    tint = c.textSecondary,
                    modifier = Modifier
                        .padding(end = H_PADDING)
                        .padding(start = H_PADDING) // To tap area
                        .size(20.dp)
                )
            }
        }

        if (!isFirst) {
            DividerBg(Modifier.padding(start = H_PADDING), true)
        }
    }
}

///

private fun hapticFeedback() {
    Haptic.shot()
}
