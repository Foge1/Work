package com.loaderapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Единые токены радиусов — используем везде вместо хардкода
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // чипы, бейджи
    small      = RoundedCornerShape(10.dp),  // кнопки, маленькие карточки
    medium     = RoundedCornerShape(14.dp),  // стандартные карточки
    large      = RoundedCornerShape(18.dp),  // большие карточки, bottom sheet
    extraLarge = RoundedCornerShape(28.dp)   // диалоги, аватары
)

// Алиасы для удобства
val ShapeCard    get() = AppShapes.medium
val ShapeButton  get() = AppShapes.small
val ShapeChip    get() = AppShapes.extraSmall
val ShapeDialog  get() = AppShapes.extraLarge
val ShapeAvatar  get() = AppShapes.extraLarge
