package me.weishu.kernelsu.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination.invoke
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KeyEventBlocker
import me.weishu.kernelsu.ui.component.LiquidButton
import me.weishu.kernelsu.ui.component.ModernSectionTitle
import me.weishu.kernelsu.ui.component.TopBarBackground
import me.weishu.kernelsu.ui.component.rememberConfirmDialog
import me.weishu.kernelsu.ui.util.FlashResult
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.flashModule
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.installBoot
import me.weishu.kernelsu.ui.util.reboot
import me.weishu.kernelsu.ui.util.restoreBoot
import me.weishu.kernelsu.ui.util.uninstallPermanently
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Save
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @author weishu
 * @date 2023/1/1.
 */

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

// Lets you flash modules sequentially when mutiple zipUris are selected
fun flashModulesSequentially(
    uris: List<Uri>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    for (uri in uris) {
        flashModule(uri, onStdout, onStderr).apply {
            if (code != 0) {
                return FlashResult(code, err, showReboot)
            }
        }
    }
    return FlashResult(0, "", true)
}

@Composable
@Destination<RootGraph>
fun FlashScreen(
    navigator: DestinationsNavigator,
    flashIt: FlashIt
) {
    var text by rememberSaveable { mutableStateOf("") }
    var tempText: String
    val logContent = rememberSaveable { StringBuilder() }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var flashing by rememberSaveable {
        mutableStateOf(FlashingStatus.FLASHING)
    }

    LaunchedEffect(Unit) {
        if (text.isNotEmpty()) {
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            flashIt(flashIt, onStdout = {
                tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    text = tempText.substring(6)
                } else {
                    text += tempText
                }
                logContent.append(it).append("\n")
            }, onStderr = {
                logContent.append(it).append("\n")
            }).apply {
                if (code != 0) {
                    text += "Error code: $code.\n $err Please save and check the log.\n"
                }
                if (showReboot) {
                    text += "\n\n\n"
                    showFloatAction = true
                }
                flashing = if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED
            }
        }
    }
    val backdrop = rememberLayerBackdrop()

    Scaffold(
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }

        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .scrollEndHaptic()
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateStartPadding(layoutDirection),
                    )
                    .verticalScroll(scrollState),
            ) {
                LaunchedEffect(text) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                ModernSectionTitle(
                    title = stringResource(
                        when (flashing) {
                            FlashingStatus.FLASHING -> R.string.flashing
                            FlashingStatus.SUCCESS -> R.string.flash_success
                            FlashingStatus.FAILED -> R.string.flash_failed
                        }
                    ),
                    modifier = Modifier
                        .displayCutoutPadding()
                        .padding(top = innerPadding.calculateTopPadding() + 80.dp)
                )
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = text,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(
                    Modifier.height(
                        12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
            Row(
                Modifier
                    .displayCutoutPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidButton(
                    onClick = dropUnlessResumed { navigator.popBackStack() },
                    modifier = Modifier.size(40.dp),
                    backdrop = backdrop
                ) {
                    Icon(
                        MiuixIcons.Useful.Back,
                        contentDescription = null,
                        tint = colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    LiquidButton(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            scope.launch {
                                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                                val date = format.format(Date())
                                val file = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "KernelSU_install_log_${date}.log"
                                )
                                file.writeText(logContent.toString())
                                Toast.makeText(context, "Log saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        backdrop = backdrop
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Save,
                            contentDescription = stringResource(id = R.string.save_log),
                            tint = colorScheme.onBackground
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
            ) {
                if (showFloatAction) {
                    val reboot = stringResource(id = R.string.reboot)
                    LiquidButton(
                        modifier = Modifier
                            .padding(
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() +
                                        WindowInsets.captionBar.asPaddingValues()
                                            .calculateBottomPadding() + 20.dp,
                                end = 20.dp
                            )
                            .padding(bottom = innerPadding.calculateBottomPadding() + 20.dp, end = 20.dp),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    reboot()
                                }
                            }
                        },
                        backdrop = backdrop,
                        surfaceColor = colorScheme.primaryVariant.copy(0.75f),
                        content = {
                            Icon(
                                Icons.Rounded.Refresh,
                                reboot,
                                Modifier.size(40.dp),
                                tint = Color.White
                            )
                        },
                    )
                }
            }
            TopBarBackground(backdrop)
        }
    }
}

@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashBoot(val boot: Uri? = null, val lkm: LkmSelection, val ota: Boolean, val partition: String? = null) :
        FlashIt()

    data class FlashModules(val uris: List<Uri>) : FlashIt()

    data object FlashRestore : FlashIt()

    data object FlashUninstall : FlashIt()
}

fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return when (flashIt) {
        is FlashIt.FlashBoot -> installBoot(
            flashIt.boot,
            flashIt.lkm,
            flashIt.ota,
            flashIt.partition,
            onStdout,
            onStderr
        )

        is FlashIt.FlashModules -> {
            flashModulesSequentially(flashIt.uris, onStdout, onStderr)
        }

        FlashIt.FlashRestore -> restoreBoot(onStdout, onStderr)

        FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
    }
}
