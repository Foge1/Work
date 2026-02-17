#!/bin/bash

echo "==================================="
echo "Проверка проекта LoaderApp"
echo "==================================="
echo ""

# Проверка наличия gradlew
if [ -f "./gradlew" ]; then
    echo "✅ Gradle wrapper найден"
else
    echo "❌ Gradle wrapper не найден"
    exit 1
fi

# Проверка прав на выполнение
if [ -x "./gradlew" ]; then
    echo "✅ Права на выполнение установлены"
else
    echo "⚠️  Установка прав на выполнение..."
    chmod +x ./gradlew
fi

# Проверка структуры проекта
echo ""
echo "Проверка структуры проекта:"
if [ -d "./app/src/main/java/com/loaderapp" ]; then
    echo "✅ Исходный код найден"
else
    echo "❌ Исходный код не найден"
    exit 1
fi

if [ -f "./app/build.gradle" ]; then
    echo "✅ build.gradle найден"
else
    echo "❌ build.gradle не найден"
    exit 1
fi

if [ -f "./settings.gradle" ]; then
    echo "✅ settings.gradle найден"
else
    echo "❌ settings.gradle не найден"
    exit 1
fi

# Подсчет файлов
echo ""
echo "Статистика проекта:"
kt_files=$(find ./app/src -name "*.kt" | wc -l)
xml_files=$(find ./app/src/main/res -name "*.xml" | wc -l)
echo "📄 Kotlin файлов: $kt_files"
echo "📄 XML ресурсов: $xml_files"

echo ""
echo "==================================="
echo "✅ Проект готов к сборке!"
echo "==================================="
echo ""
echo "Для сборки выполните:"
echo "./gradlew assembleDebug"
echo ""
echo "APK будет в:"
echo "app/build/outputs/apk/debug/app-debug.apk"
