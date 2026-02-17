package com.loaderapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0
)

@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        // Тонкая линия-разделитель сверху
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    AppBottomBarItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemSelected(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 52.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "pillWidth"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(200),
        label = "iconColor"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = tween(180),
        label = "labelColor"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill-индикатор + иконка
        Box(contentAlignment = Alignment.Center) {
            // Фоновая pill-капсула
            Box(
                modifier = Modifier
                    .size(width = pillWidth, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else Color.Transparent
                    )
            )
            // Иконка с бейджем
            BadgedBox(
                badge = {
                    if (item.badgeCount > 0) {
                        Badge {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}",
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                modifier = Modifier.scale(iconScale)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Подпись — всегда видна, но цвет меняется
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            maxLines = 1
        )
    }
}
