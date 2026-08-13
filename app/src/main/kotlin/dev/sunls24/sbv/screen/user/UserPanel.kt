package dev.sunls24.sbv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.R
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.requestFocus

@Composable
fun UserPanelDialog(
    modifier: Modifier = Modifier,
    isLogin: Boolean,
    username: String,
    face: String,
    level: Int,
    currentExp: Int,
    nextLevelExp: Int,
    onHide: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonal: () -> Unit,
    onGoFollowingUp: () -> Unit,
) {
    Dialog(
        onDismissRequest = onHide,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = modifier.widthIn(min = 560.dp, max = 720.dp),
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
                currentExp = currentExp,
                nextLevelExp = nextLevelExp,
                onHide = onHide,
                onLogin = onLogin,
                onLogout = onLogout,
                onOpenSettings = onOpenSettings,
                onOpenPersonal = onOpenPersonal,
                onGoFollowingUp = onGoFollowingUp,
            )
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
    currentExp: Int,
    nextLevelExp: Int,
    onHide: () -> Unit,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenPersonal: () -> Unit = {},
    onGoFollowingUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var inIncognitoMode by remember { mutableStateOf(Prefs.incognitoMode) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
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
                currentExp = currentExp,
                nextLevelExp = nextLevelExp,
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
                title = "个人内容",
                icon = R.drawable.ic_symbol_person_filled,
                onClick = {
                    onOpenPersonal()
                    onHide()
                },
            )
            UserPanelMenuItem(
                title = "正在关注",
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
            UserPanelMenuItem(
                title = if (inIncognitoMode) "隐身已开启" else "隐身已关闭",
                icon = if (inIncognitoMode) {
                    R.drawable.ic_symbol_visibility_off_filled
                } else {
                    R.drawable.ic_symbol_visibility_filled
                },
                onClick = {
                    inIncognitoMode = !inIncognitoMode
                    Prefs.incognitoMode = inIncognitoMode
                },
            )
            UserPanelMenuItem(
                title = "退出登录",
                icon = R.drawable.ic_symbol_logout_filled,
                onClick = {
                    onLogout()
                    onHide()
                },
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
}

@Composable
private fun UserPanelHeader(
    username: String,
    face: String,
    level: Int,
    currentExp: Int,
    nextLevelExp: Int,
) {
    val progress = currentExp.toFloat() / nextLevelExp.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SBVSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            model = face,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SBVSpacing.sm),
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Lv.$level",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
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
            currentExp = 100,
            nextLevelExp = 200,
        )
    }
}
