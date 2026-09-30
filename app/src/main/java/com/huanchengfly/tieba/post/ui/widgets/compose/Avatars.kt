package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.DisplayRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@Composable
fun AvatarIcon(
    icon: ImageVector,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarIcon(
    @DrawableRes
    resId: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = ImageVector.vectorResource(id = resId),
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarPlaceholder(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Avatar(
        data = data,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        shape = shape,
    )
}

@Composable
fun Avatar(
    data: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
            modifier = Modifier.fillMaxSize(0.55f)
        )
        AsyncImage(
            request = DisplayRequest(LocalContext.current, data) {
                crossfade(durationMillis = 200)
            },
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
fun Avatar(
    data: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
            modifier = Modifier.fillMaxSize(0.55f)
        )
        AsyncImage(
            request = DisplayRequest(LocalContext.current, newResourceUri(data)) {
                crossfade(durationMillis = 200)
            },
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}