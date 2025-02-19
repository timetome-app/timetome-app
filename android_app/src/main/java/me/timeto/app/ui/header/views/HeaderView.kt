package me.timeto.app.ui.header.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import me.timeto.app.MainActivity
import me.timeto.app.ZStack
import me.timeto.app.onePx

private const val animRatio = 50f
private val backgroundColor: Color = Color(26, 26, 26)
private val dividerColor: Color = Color(44, 44, 44)

@Composable
fun HeaderView(
    scrollState: ScrollableState?,
    content: @Composable () -> Unit,
) {

    val mainActivity = LocalContext.current as MainActivity

    val alphaValue = remember {

        derivedStateOf {

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

                else -> throw Exception("HeaderView.kt: invalid scrollState")
            }
        }
    }

    val alphaAnimate = animateFloatAsState(alphaValue.value)

    ZStack(
        modifier = Modifier
            .drawBehind {
                drawRect(color = backgroundColor.copy(alpha = alphaAnimate.value))
            }
            .padding(top = mainActivity.statusBarHeightDp),
    ) {

        content()

        ZStack(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(onePx)
                .fillMaxWidth()
                .drawBehind {
                    drawRect(color = dividerColor.copy(alpha = alphaAnimate.value))
                },
        )
    }
}
