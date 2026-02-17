#!/bin/bash
echo "Checking for common syntax errors..."

# Проверка двойных закрывающих скобок
echo "=== Checking for double closing braces ==="
find app/src/main -name "*.kt" -exec grep -Hn "}\s*}\s*$" {} \; 2>/dev/null || echo "None found"

# Проверка лишних запятых перед закрывающей скобкой
echo -e "\n=== Checking for trailing commas before } ==="
find app/src/main -name "*.kt" -exec grep -Hn "},\s*$" {} \; 2>/dev/null | head -20 || echo "None found"

# Проверка незакрытых скобок (простая проверка)
echo -e "\n=== Checking bracket balance in UI files ==="
for file in app/src/main/java/com/loaderapp/ui/**/*.kt; do
    if [ -f "$file" ]; then
        open=$(grep -o "{" "$file" | wc -l)
        close=$(grep -o "}" "$file" | wc -l)
        if [ $open -ne $close ]; then
            echo "$file: { count=$open, } count=$close - IMBALANCE!"
        fi
    fi
done
