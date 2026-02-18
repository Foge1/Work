package com.loaderapp.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.ui.theme.SplashEnd
import com.loaderapp.ui.theme.SplashStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val iconScale = remember { Animatable(0.4f) }
    val iconAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }
    val dotsAlpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch {
            iconScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { delay(200); textAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { delay(200); textOffsetY.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
        launch { delay(680); dotsAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        // Пульс после появления
        launch {
            delay(1000)
            pulseScale.animateTo(1.06f, tween(300, easing = FastOutSlowInEasing))
            pulseScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
        delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashStart, SplashEnd))),
        contentAlignment = Alignment.Center
    ) {
        // Декоративные круги на фоне
        Box(modifier = Modifier.size(340.dp).align(Alignment.TopEnd).offset(x = 90.dp, y = (-70).dp).alpha(0.07f).clip(CircleShape).background(Color.White))
        Box(modifier = Modifier.size(220.dp).align(Alignment.BottomStart).offset(x = (-70).dp, y = 70.dp).alpha(0.05f).clip(CircleShape).background(Color.White))
        Box(modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = (-180).dp).alpha(0.04f).clip(CircleShape).background(Color.White))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Иконка
            Box(
                modifier = Modifier
                    .scale(iconScale.value * pulseScale.value)
                    .alpha(iconAlpha.value)
                    .size(104.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(text = "ГрузчикиApp", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.alpha(textAlpha.value).offset(y = textOffsetY.value.dp))
            Text(text = "Сервис поиска грузчиков", fontSize = 15.sp, color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).alpha(textAlpha.value).offset(y = textOffsetY.value.dp))

            Spacer(modifier = Modifier.height(52.dp))

            LoadingDots(modifier = Modifier.alpha(dotsAlpha.value))
        }

        Text(text = "9.4", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).alpha(textAlpha.value))
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dots")
    val a1 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse, StartOffset(0)), label = "d1")
    val a2 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse, StartOffset(183)), label = "d2")
    val a3 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse, StartOffset(366)), label = "d3")
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        listOf(a1, a2, a3).forEach { a -> Box(modifier = Modifier.size(8.dp).alpha(a).clip(CircleShape).background(Color.White)) }
    }
}
