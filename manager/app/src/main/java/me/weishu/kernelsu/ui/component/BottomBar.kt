package me.weishu.kernelsu.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.Backdrop
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalHandlePageChange
import me.weishu.kernelsu.ui.LocalPagerState
import me.weishu.kernelsu.ui.util.rootAvailable

@Composable
fun BottomBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isManager = Natives.isManager
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()

    if (!fullFeatured) return

    val pagerState = LocalPagerState.current
    val handlePageChange = LocalHandlePageChange.current

    val items = listOf(
        LiquidNavItem(stringResource(R.string.home), Icons.Rounded.Cottage),
        LiquidNavItem(stringResource(R.string.superuser), Icons.Rounded.Security),
        LiquidNavItem(stringResource(R.string.module), Icons.Rounded.Extension)
    )

    BottomTabs(
        modifier = modifier,
        tabs = items,
        pagerState = pagerState,
        onTabSelected = handlePageChange,
        backdrop = backdrop
    )
}
