#!/bin/bash


replace=`echo $PATH | sed "s#java-8#java-17#"`
PATH=$replace

java -version

# Компилируем
#javac Loader.java

# Функция для запуска теста
run_test() {
    local gc_name=$1
    local gc_flags=$2

    echo "========================================="
    echo "Тестирование: $gc_name"
    echo "Флаги: $gc_flags"
    echo "========================================="

    # Запускаем с указанным GC
    java -cp target/classes \
          $gc_flags \
         -Xmx2g \
         -Xms2g \
         -XX:+PrintGCDetails \
         -Xlog:gc*:file=gc_${gc_name}.log \
         ru.protei.gc.Loader

    echo -e "\nРезультаты сохранены в gc_${gc_name}.log"
    echo "========================================="
    echo ""
}

# Тестируем разные GC
run_test "G1GC" "-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
run_test "ParallelGC" "-XX:+UseParallelGC"
run_test "ZGC" "-XX:+UseZGC"
#run_test "ShenandoahGC" "-XX:+UseShenandoahGC"
#run_test "SerialGC" "-XX:+UseSerialGC"

# Анализ результатов
echo ""
echo "=== Сводный анализ ==="
for gc in G1GC ParallelGC ZGC ShenandoahGC SerialGC; do
    if [ -f "gc_${gc}.log" ]; then
        echo ""
        echo "$gc:"
        grep -E "Pause|Real|Total" gc_${gc}.log | tail -5
    fi
done