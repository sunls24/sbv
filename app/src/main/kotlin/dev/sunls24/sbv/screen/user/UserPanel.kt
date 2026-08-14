package dev.sunls24.sbv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.R
import dev.sunls24.sbv.tv.component.TvAlertDialog
import dev.sunls24.sbv.tv.component.tvDialogButtonHeight
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl
import dev.sunls24.sbv.util.requestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserPanelDialog(
    modifier: Modifier = Modifier,
    isLogin: Boolean,
    username: String,
    face: String,
    level: Int,
    onHide: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonal: () -> Unit,
    onGoFollowingUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val dismiss = {
        if (visible) {
            visible = false
            scope.launch {
                delay(180)
                onHide()
            }
        }
    }

    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = SBVSpacing.lg,
                    end = SBVSpacing.lg,
                    bottom = SBVSpacing.lg,
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 3 },
                exit = fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 3 },
            ) {
                Surface(
                    modifier = modifier
                        .width(420.dp)
                        .fillMaxHeight(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    UserPanel(
                        modifier = Modifier.fillMaxWidth(),
                        isLogin = isLogin,
                        username = username,
                        face = face,
                        level = level,
                        onHide = dismiss,
                        onLogin = onLogin,
                        onLogout = onLogout,
                        onOpenSettings = onOpenSettings,
                        onOpenPersonal = onOpenPersonal,
                        onGoFollowingUp = onGoFollowingUp,
                    )
                }
            }
        }
    }
}

@Composable
fun UserPanel(
    modifier: Modifier = Modifier,
    isLogin: Boolean = true,
    username: String,
    face: String,
    level: Int,
    onHide: () -> Unit,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenPersonal: () -> Unit = {},
    onGoFollowingUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val logoutDismissFocusRequester = remember { FocusRequester() }
    var inIncognitoMode by remember { mutableStateOf(Prefs.incognitoMode) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    LaunchedEffect(showLogoutConfirmation) {
        if (showLogoutConfirmation) logoutDismissFocusRequester.requestFocus(scope)
    }

    Column(
        modifier = modifier
            .focusGroup()
            .padding(SBVSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.sm),
    ) {
        if (isLogin) {
            UserPanelHeader(
                username = username,
                face = face,
                level = level,
            )
        } else {
            Column(
                modifier = Modifier.padding(bottom = SBVSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SBVSpacing.xs),
            ) {
                Text(
                    text = "欢迎使用 SBV",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "登录后可以查看动态和个人内容",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isLogin) {
            UserPanelMenuItem(
                modifier = Modifier.focusRequester(focusRequester),
                title = "我的内容",
                icon = R.drawable.ic_symbol_person_filled,
                onClick = {
                    onOpenPersonal()
                    onHide()
                },
            )
            UserPanelMenuItem(
                title = "关注的 UP 主",
                icon = R.drawable.ic_symbol_list_alt_filled,
                onClick = {
                    onGoFollowingUp()
                    onHide()
                },
            )
            UserPanelMenuItem(
                title = "设置",
                icon = R.drawable.ic_symbol_settings_filled,
                onClick = {
                    onOpenSettings()
                    onHide()
                },
            )
            UserPanelSwitchItem(
                title = "隐身播放",
                supportingText = "不会向 Bilibili 上传播放进度",
                checked = inIncognitoMode,
                onCheckedChange = { checked ->
                    inIncognitoMode = checked
                    Prefs.incognitoMode = checked
                }
            )

            Spacer(modifier = Modifier.height(SBVSpacing.md))

            UserPanelMenuItem(
                title = "退出登录",
                icon = R.drawable.ic_symbol_logout_filled,
                onClick = { showLogoutConfirmation = true },
            )
        } else {
            UserPanelMenuItem(
                modifier = Modifier.focusRequester(focusRequester),
                title = "登录",
                icon = R.drawable.ic_symbol_account_circle_filled,
                onClick = {
                    onLogin()
                    onHide()
                },
            )
            UserPanelMenuItem(
                title = "设置",
                icon = R.drawable.ic_symbol_settings_filled,
                onClick = {
                    onOpenSettings()
                    onHide()
                },
            )
        }
    }

    if (showLogoutConfirmation) {
        TvAlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text(text = "退出登录？") },
            text = { Text(text = "退出后需要重新扫码登录，当前本地设置会保留。") },
            confirmButton = {
                Button(
                    modifier = Modifier.tvDialogButtonHeight(),
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                        onHide()
                    },
                ) {
                    Text(text = "退出登录")
                }
            },
            dismissButton = {
                OutlinedButton(
                    modifier = Modifier
                        .tvDialogButtonHeight()
                        .focusRequester(logoutDismissFocusRequester),
                    onClick = { showLogoutConfirmation = false },
                ) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun UserPanelHeader(
    username: String,
    face: String,
    level: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SBVSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            model = face.resizedImageUrl(ImageSize.Avatar),
            placeholder = painterResource(R.drawable.ic_symbol_account_circle_filled),
            error = painterResource(R.drawable.ic_symbol_account_circle_filled),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.xs),
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Lv.$level",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun UserPanelMenuItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = modifier,
        selected = false,
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        },
        headlineContent = { Text(text = title) },
        onClick = onClick,
    )
}

@Composable
private fun UserPanelSwitchItem(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        selected = false,
        leadingContent = {
            Icon(
                painter = painterResource(
                    if (checked) R.drawable.ic_symbol_visibility_off_filled
                    else R.drawable.ic_symbol_visibility_filled
                ),
                contentDescription = null,
            )
        },
        headlineContent = { Text(text = title) },
        supportingContent = {
            Text(text = supportingText)
        },
        trailingContent = {
            Switch(
                modifier = Modifier.focusable(false),
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserPanelPreview() {
    SBVTheme {
        UserPanelDialog(
            isLogin = true,
            username = "abcde",
            face = "",
            onHide = {},
            onLogin = {},
            onLogout = {},
            onOpenSettings = {},
            onOpenPersonal = {},
            onGoFollowingUp = {},
            level = 5,
        )
    }
}
