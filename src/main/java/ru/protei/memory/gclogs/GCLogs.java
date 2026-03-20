package ru.protei.memory.gclogs;

/**
 * Логи:
 * -Xlog:what:where:how
 *
 * Часть      Описание                             Пример
 * what       Что логировать (теги и уровень)      gc*, safepoint=debug
 * where      Куда выводить                        stdout, stderr, file=gc.log
 * how        Как форматировать                    time, uptime, level, tags
 *
 * Тег            Что логирует                    Аналог в Java 8
 * gc             Всё про GC                      -XX:+PrintGCDetails
 * gc+age         Распределение по возрастам      -XX:+PrintTenuringDistribution
 * gc+heap        Информация о куче               -XX:+PrintHeapAtGC
 * gc+phases      Фазы GC                         нет аналога
 * gc+ergo        Эвристики GC                    -XX:+PrintAdaptiveSizePolicy
 * safepoint      STW-паузы                       -XX:+PrintGCApplicationStoppedTime
 * jit            JIT-компиляция                  -XX:+PrintCompilation
 * class          Загрузка классов                -XX:+TraceClassLoading
 * thread         Потоки                          нет аналога
 * os             Системные вызовы                нет аналога
 *
 *
 * Синтаксис:
 * # Один тег
 * -Xlog:gc
 *
 * # Комбинация тегов (+)
 * -Xlog:gc+heap+phases
 *
 * # Исключение (-)
 * -Xlog:gc-ergo  # Всё про GC, кроме эвристик
 *
 * # Wildcard (*)
 * -Xlog:gc*      # Всё, что начинается с gc (gc, gc+heap, gc+age, ...)
 *
 * Уровни логирования:
 * off          Выключить
 * error        Только ошибки
 * warning      Ошибки + предупреждения
 * info         По умолчанию — основная информация
 * debug        Детальная отладка
 * trace        Всё подряд (очень много)
 *
 * # Примеры с уровнями
 * -Xlog:gc=info           # GC с уровнем info
 * -Xlog:gc+heap=debug     # Детально про heap
 * -Xlog:gc*=trace         # ВСЁ про GC (очень много!)
 *
 * Варианты вывода:
 * # В консоль (по умолчанию)
 * -Xlog:gc:stdout
 * -Xlog:gc:stderr
 *
 * # В файл
 * -Xlog:gc:file=gc.log
 *
 * # В файл с ротацией (храним 5 файлов до 10МБ)
 * -Xlog:gc:file=gc.log:filecount=5,filesize=10m
 *
 * # Не выводить (только сбор)
 * -Xlog:gc::
 */
public class GCLogs {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Начало демонстрации GC логов...");

        for (int i = 0; i < 1000; i++) {
            // Создаем мусор
            byte[] garbage = new byte[10_000];
            Thread.sleep(10);

            if (i % 100 == 0) {
                System.out.println("Итерация: " + i);
                System.gc(); // Явный вызов GC для демонстрации
            }
        }
    }
}
