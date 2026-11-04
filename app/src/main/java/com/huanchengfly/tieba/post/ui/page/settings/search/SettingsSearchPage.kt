package com.huanchengfly.tieba.post.ui.page.settings.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 设置搜索：中文/拼音/首字母多词检索，结果点击跳转目标设置页并按锚点分组定位。
 */
@Destination
@Composable
fun SettingsSearchPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val index = remember { SettingsSearchIndex.build(context) }
    val results = remember(query.text) { SettingsSearchIndex.search(query.text, index) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    MyScaffold(
        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            Toolbar(
                navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
                title = {
                    SettingsSearchField(
                        value = query,
                        onValueChange = { query = it },
                        focusRequester = focusRequester,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    if (query.text.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.btn_close),
                            modifier = Modifier
                                .size(22.dp)
                                .debounceClickable(onClick = { query = TextFieldValue("") })
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        if (results.isEmpty()) {
            EmptyState(
                title = stringResource(
                    id = if (query.text.isBlank()) R.string.tip_settings_search_hint else R.string.tip_settings_search_no_result
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = results,
                    key = { "${it.entry.destination.name}_${it.entry.anchor}_${it.title}" }
                ) { item ->
                    SettingsSearchResultRow(
                        title = item.title,
                        summary = item.summary.ifBlank { stringResource(id = R.string.tip_settings_search_in_page, destinationLabel(item.entry.destination)) },
                        onClick = { item.entry.destination.navigate(navigator, item.entry.anchor) }
                    )
                }
            }
        }
    }
}

private fun destinationLabel(destination: SettingsDestination): String = when (destination) {
    SettingsDestination.SETTINGS -> "设置"
    SettingsDestination.CUSTOM -> "外观设置"
    SettingsDestination.HABIT -> "使用习惯"
    SettingsDestination.MEME -> "表情包管理"
    SettingsDestination.BLOCK -> "屏蔽设置"
    SettingsDestination.OKSIGN -> "一键签到"
    SettingsDestination.ABOUT -> "关于"
    SettingsDestination.ACCOUNT -> "账号管理"
}

@Composable
private fun SettingsSearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.subtitle1.copy(
            color = ExtendedTheme.colors.onTopBar
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(100))
                    .background(ExtendedTheme.colors.onTopBar.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.onTopBarSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.tip_settings_search_hint),
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.onTopBarSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        },
        modifier = modifier.focusRequester(focusRequester)
    )
}

@Composable
private fun SettingsSearchResultRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = stringResource(id = R.string.tip_settings_search_go),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsSearchResultRowPreview() {
    SettingsSearchResultRow(title = "主题模式", summary = "深色 / 浅色 / 自动", onClick = {})
}
