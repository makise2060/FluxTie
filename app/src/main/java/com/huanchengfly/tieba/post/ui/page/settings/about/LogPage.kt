package com.huanchengfly.tieba.post.ui.page.settings.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LogEntry(
    val level: Char,
    val tag: String,
    val message: String,
    val time: String,
)

private val LEVEL_COLORS = mapOf(
    'E' to Color(0xFFE53935),
    'W' to Color(0xFFFB8C00),
    'I' to Color(0xFF43A047),
    'D' to Color(0xFF1E88E5),
)

private val LEVEL_FILTERS = listOf("全部" to null, "错误" to 'E', "警告" to 'W')

@Destination
@Composable
fun LogPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var entries by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var levelFilter by remember { mutableIntStateOf(0) }
    val pid = android.os.Process.myPid()

    suspend fun refresh() {
        loading = true
        entries = withContext(Dispatchers.IO) {
            runCatching {
                val process = Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-v", "time", "--pid=$pid"))
                process.inputStream.bufferedReader().readLines().mapNotNull { line ->
                    // 格式：MM-DD HH:MM:SS.mmm LEVEL/TAG( pid): message
                    val match = Regex("^\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d+)\\s+([VDIWEF])/([^(:]*)\\(\\s*\\d+\\):\\s?(.*)$").find(line)
                        ?: return@mapNotNull null
                    val (time, level, tag, msg) = match.destructured
                    LogEntry(level = level.first(), tag = tag.trim(), message = msg, time = time)
                }
            }.getOrDefault(emptyList())
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_app_logs),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            clipboardManager.setText(
                                AnnotatedString(
                                    entries.joinToString("\n") { "${it.time} ${it.level}/${it.tag}: ${it.message}" }
                                )
                            )
                            context.toastShort(R.string.toast_copied)
                        }
                    }) {
                        Icon(Icons.Rounded.TextSnippet, contentDescription = stringResource(id = R.string.button_copy))
                    }
                    IconButton(onClick = {
                        val text = entries.joinToString("\n") { "${it.time} ${it.level}/${it.tag}: ${it.message}" }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(id = R.string.button_share))
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()
                            }
                            refresh()
                        }
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(id = R.string.button_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 级别过滤
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LEVEL_FILTERS.forEachIndexed { index, (label, level) ->
                    val selected = levelFilter == index
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) ExtendedTheme.colors.onPrimary else ExtendedTheme.colors.text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.card
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                if (loading) {
                    Text(
                        text = "…",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            val filterLevel: Char? = LEVEL_FILTERS.getOrNull(levelFilter)?.second
            val filtered = if (filterLevel == null) entries else entries.filter { it.level == filterLevel }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.time + it.tag + it.message.hashCode() }) { entry ->
                    val levelColor = LEVEL_COLORS[entry.level] ?: ExtendedTheme.colors.textSecondary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "${entry.time}  ${entry.level}/${entry.tag}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = levelColor,
                            fontWeight = FontWeight.Medium
                        )
                        if (entry.message.isNotBlank()) {
                            Text(
                                text = entry.message,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ExtendedTheme.colors.text,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                if (!loading && filtered.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.summary_logs_empty),
                            fontSize = 13.sp,
                            color = ExtendedTheme.colors.textSecondary,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}
