package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ChatPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getAvatarUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 私信会话列表（嵌入「消息」页 pager 的「私信」tab）。
 */
@Composable
fun ChatListPage(chatListViewModel: ChatListViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val loggedIn = AccountUtil.isLoggedIn()
    LaunchedEffect(chatListViewModel) {
        chatListViewModel.send(ChatListUiIntent.Refresh)
    }
    val uiState by chatListViewModel.uiState.collectAsState()

    if (uiState.sessions.isEmpty()) {
        if (!loggedIn) {
            EmptyState(
                title = stringResource(id = R.string.title_empty_login),
                subtitle = stringResource(id = R.string.chat_empty_login_subtitle),
                illustration = EmptyIllustration.Astronaut,
                actions = {
                    Button(
                        onClick = { navigator.navigate(LoginPageDestination) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.button_login))
                    }
                },
            )
        } else {
            EmptyState(
                title = stringResource(id = R.string.chat_empty_title),
                subtitle = stringResource(id = R.string.chat_empty_subtitle),
                // Hide 插画为斜置构图，空会话用正置的空纸箱
                illustration = EmptyIllustration.Box,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(uiState.sessions, key = { "${it.uid}-${it.peerUid}" }) { session ->
                ChatSessionItem(session = session) {
                    navigator.navigate(
                        ChatPageDestination(
                            peerUid = session.peerUid,
                            peerName = session.peerName,
                            peerPortrait = session.peerPortrait,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSessionItem(session: ImSession, onClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            imageUri = getAvatarUrl(session.peerPortrait),
            contentDescription = session.peerName,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ExtendedTheme.colors.chip),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.peerName.ifEmpty { "用户${session.peerUid}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = ExtendedTheme.colors.text,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatChatTime(session.lastMsgTime),
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
            Text(
                text = session.lastMsgPreview,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ExtendedTheme.colors.textSecondary,
            )
        }
        if (session.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ExtendedTheme.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (session.unreadCount > 99) "99+" else session.unreadCount.toString(),
                    fontSize = 11.sp,
                    color = ExtendedTheme.colors.onPrimary,
                )
            }
        }
    }
}

/** 会话时间显示：当日显示时分，跨日显示日期 */
internal fun formatChatTime(timestampSeconds: Long): String {
    if (timestampSeconds <= 0L) return ""
    val date = Date(timestampSeconds * 1000)
    val todayStart = java.util.Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val pattern = if (date.time >= todayStart) "HH:mm" else "yyyy-MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
