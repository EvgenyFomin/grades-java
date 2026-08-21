# 9. Синхронный и асинхронный I/O на Java + syscall'ы под капотом

## Проблема: I/O — главное узкое место

Большинство приложений ограничены не CPU, а ожиданием ввода/вывода: чтением с диска, приёмом данных из сети, записью в лог. Пока поток ждёт завершения операции I/O, ядро **снимает его с ядра** (см. п. 8 — context switch), а когда данные приходят — возвращает обратно. Чем больше таких «сниманий», тем больше накладных расходов.

Отсюда главный вопрос выбора: **как ждать I/O, чтобы не простаивать зря?** Ответ дают разные модели ввода/вывода — от простого блокирующего `read()` до асинхронного `io_uring`. Под капотом каждой модели лежат свои системные вызовы, и их видно в `strace`.

## Модели I/O в Linux

Все четыре модели — это разные способы ответить на вопрос «кто и когда ждёт данные»:

| Модель | Кто ждёт | Syscall'ы | Когда использовать |
| --- | --- | --- | --- |
| Блокирующий (синхронный) | поток целиком | `read()`, `write()` | простые последовательные операции |
| Неблокирующий | поток «поллит» готовность | `fcntl(O_NONBLOCK)`, `poll()` | редкие, короткие операции |
| Мультиплексирование | поток ждёт события на многих fd | `select()`, `poll()`, `epoll_wait()` | много соединений, event loop |
| Асинхронный | никто: ядро само завершит операцию | `io_uring_enter()`, `aio_read()` | высокая нагрузка, файловый/сетевой I/O |

Ключевая идея: чем «умнее» модель, тем меньше потоков простаивает и тем меньше context switch'ов. За это платят сложностью кода и числом syscall'ов.

## 1. Блокирующий (синхронный) I/O: `FileInputStream` → `read()`

Классика — обычное чтение файла. Поток вызывает операцию и **замирает**, пока данные не появятся.

```java
import java.io.*;

try (FileInputStream in = new FileInputStream("/tmp/data.bin")) {
    byte[] buf = new byte[4096];
    int n;
    while ((n = in.read(buf)) != -1) {
        // данные в buf — поток ждал read() столько, сколько нужно
    }
}
```

Под капотом:

```
Java-код:  in.read(buf)
   │
   ▼
JVM (native): FileInputStream.readBytes(...)
   │
   ▼
libc: read(fd, buf, 4096)          ← системный вызов
   │
   ▼
Ядро Linux: read() syscall
   │          ├─ файл уже в page cache → данные возвращаются сразу
   │          └─ данных нет → поток уходит в сон (state S или D),
   │             ядро будит его, когда данные готовы (см. п. 8)
   ▼
Поток продолжает выполнение
```

Важные следствия:

- Пока поток ждёт `read()`, он **не потребляет CPU** — ядро снимает его с ядра и ставит другой поток. Для одного потока это нормально.
- Но если таких потоков сотни (например, «поток на каждое соединение»), система тратит время на context switch'и, а не на работу.
- **`read()` всегда возвращает данные синхронно** — либо всё, либо ничего; поток не продолжит работу, пока syscall не завершится.

Посмотреть на практике:

```bash
# видим блокирующий read(), который ждёт ввода с терминала
strace -e trace=read,write cat /dev/stdin <<< "hello" 2>&1 | head

# сколько времени поток провёл в ожидании I/O
cat /proc/<pid>/io        # rchar/wchar — байты, syscr/syscw — число read/write syscall'ов
```

## 2. Non-blocking I/O: `SocketChannel` + `Selector` → `epoll`

Поток-на-соединение не масштабируется. Решение — **мультиплексирование**: один поток следит сразу за тысячами соединений и реагирует только на те, где есть данные. В Java это NIO (`java.nio`): `SocketChannel` в неблокирующем режиме + `Selector`.

```java
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

Selector selector = Selector.open();
SocketChannel ch = SocketChannel.open();
ch.configureBlocking(false);                    // O_NONBLOCK на уровне ядра
ch.register(selector, SelectionKey.OP_READ);    // «сообщи, когда можно читать»

while (true) {
    selector.select(1000);                      // блокируется до появления события
    for (SelectionKey key : selector.selectedKeys()) {
        if (key.isReadable()) {
            SocketChannel ready = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate(4096);
            ready.read(buf);                    // не блокирует: данные точно готовы
            System.out.println(new String(buf.array()));
        }
    }
    selector.selectedKeys().clear();
}
```

Под капотом:

```
Java-код:  selector.select(timeout)
   │
   ▼
JVM (native): 1_1_SelectorImpl → poll0(...)
   │
   ▼
libc: epoll_wait(epfd, events, max, timeout)    ← системный вызов
   │
   ▼
Ядро Linux: следит за всеми fd, добавленными через epoll_ctl()
   │          ├─ есть событие (данные пришли) → возвращает список готовых fd
   │          └─ нет событий → поток спит, но НЕ на отдельном соединении,
   │             а «на всех сразу» (один context switch на все fd)
   ▼
Java-код обрабатывает только готовые каналы
```

Разница с блокирующим режимом:

- **`epoll` — это один поток на десятки тысяч соединений** вместо «поток на соединение». Один context switch вместо тысяч.
- Неблокирующий `read()` на готовом канале почти никогда не блокирует: ядро уже подтвердило, что данные есть.
- Это модель event loop: **один поток, цикл `select()`/`epoll_wait()`, обработка готовых событий**. На ней построены Netty, Reactor, Jetty.

## 3. Асинхронный I/O: `AsynchronousFileChannel` → `io_uring`

Асинхронный I/O — следующий шаг. Здесь поток **не ждёт вообще**: операция запускается, и когда ядро её завершит, оно вызывает callback. В Java это `AsynchronousFileChannel` и `AsynchronousSocketChannel`.

```java
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

AsynchronousFileChannel file =
    AsynchronousFileChannel.open(Path.of("/tmp/big.bin"), StandardOpenOption.READ);

ByteBuffer buf = ByteBuffer.allocate(65536);
Future<Integer> future = file.read(buf, 0);    // НЕ блокирует поток

// пока файл читается, можно делать что-то ещё
System.out.println("Читаю файл, а поток свободен...");

int n = future.get();                          // ждём завершения (если нужно)
System.out.println("Прочитано байт: " + n);
```

То же через callback:

```java
file.read(buf, 0, null, new CompletionHandler<Integer, Void>() {
    @Override
    public void completed(Integer result, Void attachment) {
        System.out.println("Прочитано байт: " + result);
    }
    @Override
    public void failed(Throwable exc, Void attachment) {
        exc.printStackTrace();
    }
});
// main-поток продолжает работать, не дожидаясь чтения
```

Под капотом — **`io_uring`**, современный асинхронный механизм ядра Linux:

```
Java-код:  file.read(buf, pos, null, handler)
   │
   ▼
JVM (native): делает io_uring_setup(), затем submit
   │
   ▼
libc: io_uring_enter(...)            ← системный вызов
   │
   ▼
Ядро Linux: io_uring — это две кольцевые структуры в памяти процесса:
   │          • SQ (Submission Queue): поток кладёт запрос «прочитай этот файл»
   │          • CQ (Completion Queue): ядро кладёт результат, когда операция готова
   │          Поток НЕ блокируется и продолжает работу
   ▼
Ядро кладёт результат в CQ → JVM вызывает ваш callback
```

Ключевое отличие от `epoll`:

- `epoll` — это **уведомление о готовности**: «данные уже пришли, читай сам». Чтение всё равно делает ваш поток (снимается с ядра ещё на сам `read()`).
- `io_uring` — это **полное делегирование**: ядро само выполняет чтение и сообщает о завершении. Поток вообще не участвует в ожидании.

Результат — меньше syscall'ов и context switch'ов на единицу данных, что важно для дискового I/O и высокой нагрузки. Обратная сторона: сложнее код и зависимость от версии ядра (`io_uring` доступен с ядра 5.1+; JVM использует его для `AsynchronousFileChannel`, но не для всех провайдеров).

## Сравнение моделей

| Критерий | Блокирующий | Non-blocking + epoll | Асинхронный (io_uring) |
| --- | --- | --- | --- |
| Блокирует ли поток | да, на время операции | почти нет | нет вообще |
| Потоков на N соединений | N | 1 | 1 |
| Кто выполняет чтение | поток (в ядре) | поток (в ядре) | ядро (в фоне) |
| Syscall'ы | `read()`/`write()` | `epoll_ctl` + `epoll_wait` + `read()` | `io_uring_enter` |
| Context switch на 1 операцию | 2+ (заснуть и проснуться) | ~1–2 (один на все fd) | минимум |
| Сложность кода | низкая | средняя | высокая |
| Когда выбирать | простые случаи, один поток | сеть, много соединений | высокий дисковый/сетевой I/O |

Правило: не усложняйте без необходимости. Для простого чтения файла в фоне — блокирующий поток в пуле. Для сетевого сервера с тысячами клиентов — NIO/`Selector`. Для задач с тяжёлым файловым I/O — `AsynchronousFileChannel`.

## Практика

```bash
# 1. Блокирующий read(): поток заснул на ожидании ввода
strace -e trace=read,write,epoll_wait,io_uring_enter cat

# 2. Посмотреть syscall'ы вашей Java-программы
strace -e trace=read,write,epoll_wait,io_uring_enter java -cp . IoDemo

# 3. Мультиплексирование: сколько соединений обслуживает один epoll
cat /proc/$(pgrep -n java)/fd | grep -c epoll
ls /proc/$(pgrep -n java)/fdinfo | grep -c .   # общее число fd (связь с п. 6)

# 4. Нагрузка на I/O процесса: сколько байт и сколько syscall'ов
cat /proc/$(pgrep -n java)/io    # rchar, wchar, syscr, syscw

# 5. Состояния потоков при блокирующем I/O
ps -o pid,state,comm -e | grep java
#   S — ждёт данных (обычный sleep), D — ждёт диска (непрерываемый сон)
```

Типичная картина: `strace` на `cat` показывает один `read()` без возврата, пока вы не нажмёте Enter; у приложения с `Selector` в выводе доминирует `epoll_wait` — это и есть признак event loop.

## Связь со следующими разделами

- Модели I/O — это способы не платить за **context switch** и mode switch, о которых шла речь в п. 3 и 8: асинхронный I/O «убирает» поток из ожидания.
- `FileInputStream`, сокеты, пайпы — всё это файловые дескрипторы из п. 6, над которыми эти модели работают.
- Пайпы и FIFO (следующий раздел) — ещё один вид «файла», для которого работают те же `read()`/`write()` и те же модели.

Далее — раздел «Каналы (pipes) и FIFO».
