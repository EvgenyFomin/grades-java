# Знание особенностей работы ОС (в части ввода/вывода)

## 1. Введение

## 2. Загрузка ОС
- BIOS/UEFI → Bootloader → Kernel → Init (systemd) → userspace
- скрипт: `dmesg`, `systemd-analyze blame`

## 3. User mode и Kernel mode
- Rings 0–3, syscalls как мост
- `strace -c` для подсчёта syscall'ов
- написано: `03-User-mode-и-Kernel-mode/User-mode-и-Kernel-mode.md`

## 4. Система привилегий в Linux
- `rws`/`rwt`: setuid, setgid, sticky bit
- Права на директориях: чтение/запись/выполнение для каталогов, sticky bit (`/tmp`)
- Связь с kernel mode: проверку прав делает ядро
- скрипт: `ls -l`, `stat -c %a`, `chmod`, примеры `/usr/bin/passwd`

## 5. Концепция «Всё — файл»
- Регулярные файлы, директории, сокеты, пайпы, устройства, `/proc`, `/sys`
- скрипт: `ls -l /proc/self/fd/`, `stat` на разных типах
- написано: `05-Концепция-Всё-файл/Всё-файл.md`

## 6. Файловые дескрипторы и стандартные потоки
- stdin/stdout/stderr, перенаправление
- скрипт: `>`, `>>`, `2>`, `&>`, `|`, `exec`

## 7. Создание потоков в Java
- `Thread` / `Runnable`, `ExecutorService`, потоки vs процессы (на уровне ОС — LWP)
- Что происходит при создании потока под капотом: `pthread_create` → `clone()` syscall
- Пример Java-кода

## 8. Переключение контекста и планирование потоков
- Планировщик (CFS), вытеснение, прерывания таймера
- Что сохраняется при context switch: регистры, PC, стек, FPU, и режим процессора (user/kernel)
- mode switch (syscall) vs context switch (поток → поток)
- скрипт: `/proc/<pid>/sched`, `vmstat`, `pidstat`
- написано: `08-Переключение-контекста/Переключение-контекста.md`

## 9. Синхронный и асинхронный I/O на Java + syscall'ы под капотом
- **Синхронный**: `FileInputStream` → `read()` syscall
- **Non-blocking**: `SocketChannel` + `Selector` → `epoll`
- **Асинхронный**: `AsynchronousFileChannel` → `io_uring`
- Примеры Java-кода
- скрипт: `strace -e read,write,epoll_wait,io_uring_enter java ...`
- написано: `09-Синхронный-и-асинхронный-IO-на-Java/Синхронный-и-асинхронный-IO-на-Java.md`

## 10. Каналы (pipes) и FIFO
- Анонимные `|`, именованные `mkfifo`
- скрипт: `mkfifo`, параллельные чтение/запись
- написано: `10-Каналы-pipes-и-FIFO/Каналы-pipes-и-FIFO.md`

## 11. Виртуальные ФС: `/proc`, `/sys`, `/dev`
- скрипт: `/proc/<pid>/io`, `/proc/<pid>/fd`
- написано: `11-Виртуальные-ФС-proc-sys-dev/Виртуальные-ФС-proc-sys-dev.md`

## 12. Inode, hard/soft links
- `stat`, `ls -li`
- скрипт: создание hard/soft link, сравнение

## 13. Сервисы в Linux (systemd)
- `systemctl start/stop/enable/disable/status`
- Юниты: `.service`, `.target`, `.socket`
- Порядок запуска: `After=`, `Before=`, `Requires=`, `Wants=`, `Conflicts=`
- Граф зависимостей: `systemctl list-dependencies`, `systemd-analyze dot`
- Цели (targets): `multi-user.target`, `graphical.target` и т.д.
- скрипт: просмотр юнита, `systemd-analyze plot`
- написано: `13-Сервисы-в-Linux-systemd/Сервисы-в-Linux-systemd.md`

## 14. Заключение
