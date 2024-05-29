package me.timeto.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.timeto.app.*
import me.timeto.app.R

private val enterAnimation = fadeIn(spring(stiffness = Spring.StiffnessMedium))
private val exitAnimation = fadeOut(spring(stiffness = Spring.StiffnessMedium))

object Fs {

    fun show(
        content: @Composable (WrapperView.Layer) -> Unit,
    ) {

        WrapperView.Layer(
            enterAnimation = enterAnimation,
            exitAnimation = exitAnimation,
            alignment = Alignment.BottomCenter,
            onClose = {},
            content = { layer ->
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {}
                ) {
                    content(layer)
                }
            }
        ).show()
    }
}

@Composable
fun Fs__CloseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ZStack(
        modifier = modifier
            .size(31.dp)
            .clip(roundedShape)
            .background(c.fg)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.sf_xmark_small_medium),
            contentDescription = "Close",
            tint = c.formButtonRightNoteText,
            modifier = Modifier
                .size(11.dp),
        )
    }
}

@Composable
fun Fs__Header(
    scrollState: ScrollableState?,
    content: @Composable () -> Unit,
) {

    val alphaValue = remember {
        derivedStateOf {
            val animRatio = 50f
            when (scrollState) {
                null -> 0f
                is LazyListState -> {
                    val offset = scrollState.firstVisibleItemScrollOffset
                    when {
                        scrollState.firstVisibleItemIndex > 0 -> 1f
                        offset == 0 -> 0f
                        offset > animRatio -> 1f
                        else -> offset / animRatio
                    }
                }
                is ScrollState -> {
                    val offset = scrollState.value
                    when {
                        offset == 0 -> 0f
                        offset > animRatio -> 1f
                        else -> offset / animRatio
                    }
                }
                else -> throw Exception("todo FS.kt")
            }
        }
    }

    val alphaAnimate = animateFloatAsState(alphaValue.value)

    ZStack(
        modifier = Modifier
            .padding(top = statusBarHeight),
    ) {

        content()

        ZStack(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(onePx)
                .padding(horizontal = H_PADDING)
                .fillMaxWidth()
                .drawBehind {
                    drawRect(color = c.dividerBg.copy(alpha = alphaAnimate.value))
                },
        )
    }
}

@Composable
fun Fs__HeaderClose(
    title: String,
    scrollState: ScrollableState?,
    onClose: () -> Unit,
) {

    Fs__Header(
        scrollState = scrollState,
    ) {

        HStack(
            modifier = Modifier
                .padding(top = 28.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            HeaderTitle(
                title = title,
            )

            Fs__CloseButton(
                modifier = Modifier
                    .padding(end = H_PADDING),
            ) {
                onClose()
            }
        }
    }
}

@Composable
fun Fs__HeaderAction(
    title: String,
    actionText: String,
    scrollState: ScrollableState?,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {

    Fs__Header(
        scrollState = scrollState,
    ) {

        VStack {

            Text(
                text = "Cancel",
                modifier = Modifier
                    .padding(start = H_PADDING_HALF, top = 12.dp)
                    .clip(roundedShape)
                    .clickable { onCancel() }
                    .padding(horizontal = H_PADDING_HALF),
                color = c.textSecondary,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
            )

            HStack(
                modifier = Modifier
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                HeaderTitle(
                    title = title,
                )

                Text(
                    text = actionText,
                    modifier = Modifier
                        .padding(end = H_PADDING)
                        .clip(roundedShape)
                        .background(c.blue)
                        .clickable {
                            onDone()
                        }
                        .padding(horizontal = 10.dp)
                        .padding(top = 3.dp + onePx, bottom = 3.dp),
                    color = c.white,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RowScope.HeaderTitle(
    title: String,
) {
    Text(
        text = title,
        modifier = Modifier
            .padding(start = H_PADDING)
            .weight(1f),
        fontSize = 26.sp, // Golden ratio to lists text
        fontWeight = FontWeight.Medium,
        color = c.text,
    )
}