# 13. Сервисы в Linux (systemd)

## Проблема: как ОС запускает и поддерживает фоновые программы

В Linux сотни фоновых программ должны стартовать при загрузке, в правильном порядке, с правильными зависимостями, перезапускаться при падении и уметь управляться администратором. Раньше это делал хаос из SysV-скриптов (`/etc/init.d/*.sh`), которые запускались по номерам (`S01`, `S02`...) и никак не знали друг о друге.

Современный ответ — **systemd**: первый процесс (`PID 1`), который берёт на себя запуск всей системы и управление сервисами. В этой теме systemd рассматривается не как «служба ввода/вывода», а как **диспетчер жизненного цикла процессов** — связующее звено между ядром (п. 2, п. 8) и пользовательскими программами, чьи дескрипторы и I/O мы разбирали в п. 6–12.

```
┌───────────────┐  стартует как PID 1   ┌──────────────────────────────┐
│     ядро      │ ────────────────────► │  systemd (PID 1)             │
│  (см. п. 2)   │                       │   │  читает юниты            │
└───────────────┘                       │   ▼                          │
                                        │  /usr/lib/systemd/system/    │
                                        │  /etc/systemd/system/        │
                                        │   │  запускает процессы      │
                                        │   ▼                          │
                                        │  nginx, sshd, mysql...       │
                                        └──────────────────────────────┘
```

Ключевая идея: **всё управление — через юниты** (unit'ы). Юнит — это файл с описанием «что запускать, когда и при каких условиях». Управление юнитами — команда `systemctl`.

## 1. `systemctl`: основная команда управления

`systemctl` — интерфейс к systemd. Он говорит PID 1 «запусти юнит», «останови», «покажи статус» и т.д. Основные действия:

| Команда | Что делает | Связь с темой |
| --- | --- | --- |
| `systemctl start <unit>` | запустить юнит сейчас | создаёт процесс через `fork`/`exec` |
| `systemctl stop <unit>` | остановить | посылает SIGTERM/SIGKILL |
| `systemctl restart <unit>` | stop + start | |
| `systemctl enable <unit>` | включить автозапуск при загрузке | добавляет симлинк в `/etc/systemd/system/*.wants/` |
| `systemctl disable <unit>` | отключить автозапуск | убирает симлинк |
| `systemctl status <unit>` | статус + последние логи | связь с журналом |
| `systemctl reload <unit>` | перечитать конфиг без остановки | посылает SIGHUP |
| `systemctl is-active <unit>` | активен ли | скриптовые проверки |

Пример:

```bash
systemctl status sshd
● ssh.service - OpenSSH server daemon
     Loaded: loaded (/usr/lib/systemd/system/ssh.service; enabled; preset: enabled)
     Active: active (running) since ...
   Main PID: 1234 (sshd)
      Tasks: 1 (limit: 9191)
        CPU: 12ms
     Memory: 4.5M
```

Из вывода видно три ключевые строки:

- **Loaded** — где лежит юнит и включён ли автозапуск (`enabled`/`disabled`).
- **Active** — текущее состояние (`active (running)`, `inactive (dead)`, `failed`...).
- **Main PID** — PID главного процесса сервиса. Его можно проверить через `/proc/<pid>/fd` и `/proc/<pid>/io` из п. 11 — сервис это обычный процесс с обычными дескрипторами.

Разница `start` vs `enable`: `start` запускает **сейчас**, `enable` делает так, чтобы сервис запускался **при загрузке**. Часто нужны оба.

## 2. Юниты: `.service`, `.target`, `.socket`

Каждый юнит — текстовый файл (INI-формат: секции `[Unit]`, `[Service]`, `[Install]` и т.д.). Разные суффиксы — разные типы объектов:

| Тип юнита | Что описывает | Пример файла |
| --- | --- | --- |
| **`.service`** | один процесс/сервис | `nginx.service`, `sshd.service` |
| **`.target`** | группа юнитов (состояние загрузки) | `multi-user.target`, `graphical.target` |
| **`.socket`** | слушающий сокет (socket activation) | `sshd.socket`, `dbus.socket` |
| `.timer` | запуск по расписанию (замена cron) | `fstrim.timer` |
| `.mount` | точка монтирования | `home.mount` |
| `.path` | реакция на появление файла | `sshd.path` |

Где лежат юниты:

- `/usr/lib/systemd/system/` — юниты из **пакетов** (не редактируем).
- `/etc/systemd/system/` — локальные юниты и оверрайды администратора (приоритет выше).
- `/run/systemd/system/` — динамические юниты, созданные на лету.

Пример `nginx.service`:

```ini
[Unit]
Description=The nginx HTTP and reverse proxy server
After=network.target            # запускать после сети

[Service]
Type=forking
PIDFile=/run/nginx.pid
ExecStart=/usr/sbin/nginx
ExecReload=/usr/sbin/nginx -s reload
Restart=on-failure

[Install]
WantedBy=multi-user.target      # включается в multi-user.target
```

`[Unit]` — метаданные и **зависимости** (раздел 3), `[Service]` — как именно запускать процесс, `[Install]` — в какой target включается юнит (раздел 4).

### Socket activation

`.socket` — особый трюк: сервис **не запущен**, но systemd уже слушает порт/сокет от его имени. Когда приходит соединение, systemd мгновенно поднимает `.service`. Это экономит память и ускоряет загрузку. Логика та же, что в п. 10/п. 9: «файл» (в данном случае сокет) существует, даже когда за ним никто не стоит.

```bash
systemctl list-sockets          # кто слушает какие сокеты
sshd.socket                     # слушает :22, а sshd.service стартует по запросу
```

## 3. Порядок запуска: зависимости

SysV-скрипты запускались по номеру `S01`–`S99`. systemd решает порядок **декларативно** — юнит сам объявляет, с кем он связан. Директивы в `[Unit]`:

| Директива | Смысл | Тип связи |
| --- | --- | --- |
| `After=` / `Before=` | порядок старта (что раньше) | только упорядочивание |
| `Requires=` | если зависимость не запустилась — юнит тоже не запустится | жёсткая |
| `Wants=` | «желательно»: если зависимость доступна — запустится, но её сбой не мешает | мягкая |
| `Conflicts=` | не могут работать одновременно (запуск одного останавливает другой) | взаимоисключение |

Правило чтения: `After=network.target` означает «я хочу стартовать **после** network.target». Это про **порядок**, а не про «обязан» — обязательность задают `Requires=`/`Wants=`.

```ini
[Unit]
Description=My app
After=network.target postgresql.service   # стартовать после сети и БД
Wants=postgresql.service                  # но без жёсткой привязки
Conflicts=dev-only.service                # не вместе с dev-режимом
```

Граф зависимостей можно посмотреть:

```bash
systemctl list-dependencies sshd                # дерево зависимостей
systemd-analyze dot sshd | dot -Tsvg > dep.svg  # визуальный граф
```

## 4. Targets: состояния загрузки как «точки сбора»

`.target` — это **группа** юнитов, объединённых по признаку «на каком этапе системы». Вместо уровней запуска (`runlevel 3`, `runlevel 5` из SysV) systemd вводит цели:

```
local-fs.target → sysinit.target → basic.target → multi-user.target → graphical.target
```

Это цепочка загрузки: каждый следующий запускается после предыдущего. Что означает каждый из них — в таблице ниже.

### Все стандартные targets: основные и редкие

Нужно ли запоминать все? Нет. При создании сервисов вы **на практике используете один-два target**, остальные просто знать «что это» — они выстроены в цепочку и не требуют запоминания по отдельности.

**Обязательные к знанию (используются при создании сервисов):**

| Target | Роль | Где нужен |
| --- | --- | --- |
| `multi-user.target` | текстовый режим: сеть + все сервисы | `WantedBy=` почти всех сервисов |
| `graphical.target` | `multi-user` + графический вход | `WantedBy=` для display manager (GDM, SDDM, LightDM) |
| `default.target` | цель по умолчанию при загрузке (симлинк на multi-user или graphical) | `systemctl set-default` |

Это всё, что реально нужно для написания юнитов: сервис пишет `WantedBy=multi-user.target`, GUI-сервис — `WantedBy=graphical.target`.

**Полезно понимать (цепочка загрузки, ссылки на них встречаются в юнитах):**

| Target | Роль |
| --- | --- |
| `local-fs.target` | монтирование локальных ФС |
| `sysinit.target` | ранняя инициализация системы (после ядра) |
| `basic.target` | базовые сервисы: udev, systemd-journald, sockets, tmpfiles |
| `network.target` | сеть готова (обычно упоминается в `After=`, см. выше) |
| `shutdown.target` / `reboot.target` / `poweroff.target` | финальные цели: остановка, перезагрузка, выключение |

**Редкие (восстановление, спецслучаи):**

| Target | Роль |
| --- | --- |
| `rescue.target` | single-user режим для починки (mount всех ФС, минимум сервисов) |
| `emergency.target` | голый shell до init, ничего не смонтировано кроме `/` |
| `ctrl-alt-del.target` | реакция на Ctrl+Alt+Del |
| `sleep.target` / `suspend.target` / `hibernate.target` | управление энергосбережением |
| `timers.target` | запуск всех `.timer` юнитов |
| `sockets.target` | запуск всех `.socket` юнитов |

Как быстро увидеть все targets системы:

```bash
systemctl list-units --type=target --all   # все цели, включая неактивные
systemctl list-dependencies multi-user.target   # кто входит в target
ls /usr/lib/systemd/system/*.target            # файлы самих целей
```

Правило запоминания: **цепочка из трёх — `basic` → `multi-user` → `graphical`**, и один рабочий `default` на неё. Всё остальное — «фон», который встречается в чужих юнитах, а не в ваших `WantedBy=`.

Каждый сервис «хочет» попасть в какой-то target через `WantedBy=` в секции `[Install]`:

```ini
[Install]
WantedBy=multi-user.target
```

Когда systemd переходит в `multi-user.target`, он запускает все юниты, которые в него «хотят» (`systemctl enable` создаёт для этого симлинки в `/etc/systemd/system/multi-user.target.wants/`).

Смена цели — как `systemctl isolate graphical.target`, а текущую цель показывает `systemctl get-default`.

### `After=` vs `WantedBy=`: в чём разница

Это два самых путаемых параметра, потому что оба упоминают другие юниты. Но они отвечают на **разные вопросы** и живут в **разных секциях**:

| | `After=` | `WantedBy=` |
| --- | --- | --- |
| Секция | `[Unit]` | `[Install]` |
| Вопрос | **Когда** запускать? (порядок) | **Нужно ли** запускать вообще? (включение) |
| Работает при | `systemctl start`, переходе в target | только `systemctl enable` |
| Кого упоминает | того, кто должен стартовать **раньше** (сеть, БД) | target, который **подтянет** юнит при загрузке |
| Побочный эффект | **не** запускает упомянутое, только упорядочивает | создаёт симлинк в `<target>.wants/` |

Ключевая мысль: `After=` **не включает** другой юнит — он лишь говорит «если мы оба стартуем, то я позже». А `WantedBy=` **не задаёт порядок** — он лишь говорит «при входе в этот target запусти и меня тоже».

```ini
[Unit]
Description=My app
After=network.target postgresql.service   # порядок: не раньше сети и БД

[Service]
ExecStart=/opt/app/server

[Install]
WantedBy=multi-user.target                # включить в загрузку через этот target
```

**Насколько часто они должны быть разными?** Практически всегда. `After=` ссылается на то, что нужно приложению **для работы** (сеть, БД, файловая система) — это могут быть десятки разных юнитов. `WantedBy=` ссылается ровно на один target — «точку сбора» загрузки (`multi-user.target`, реже `graphical.target`). Совпадение строк (`After=multi-user.target` и `WantedBy=multi-user.target`) возможно, но бессмысленно и встречается почти никогда: упорядочивать юнит относительно «группы всех сервисов» незачем, а включать его должен именно target. 

Короткое правило: **`After=` — «запусти меня после X» (порядок), `WantedBy=` — «включи меня в загрузку» (включение)**. Они независимы: можно иметь `WantedBy=` без `After=` (порядок не важен), и наоборот.

## 5. Пример: что происходит при старте сервиса

1. `systemctl start nginx` отправляет запрос PID 1.
2. systemd читает `nginx.service`, смотрит `After=`/`Wants=` и выстраивает очередь.
3. systemd выполняет `ExecStart` через `fork`+`exec` (п. 7 — как создаются процессы/потоки).
4. Для сервиса создаётся cgroup, PID записывается как `Main PID`.
5. При падении процесса срабатывает `Restart=on-failure` — systemd перезапускает.
6. Вывод сервиса (stdout/stderr, п. 6) перехватывается **journald** — журналом systemd.

```bash
systemctl status nginx | grep Main        # главный PID
journalctl -u nginx -f                    # живые логи сервиса
systemctl show nginx -p MainPID           # узнать PID программно
```

Это же видно в `strace` (связь с п. 3): `fork()`/`execve()` — как systemd порождает процессы, а сам `systemctl` говорит с PID 1 через сокет (Unix-сокет из п. 10).

## 6. Анализ загрузки

`systemd-analyze` показывает, сколько времени тратится на старт компонентов:

```bash
systemd-analyze                 # общее время загрузки
systemd-analyze blame           # кто сколько стартовал (по убыванию)
systemd-analyze critical-chain  # критический путь: самый долгий маршрут
systemd-analyze plot > boot.svg # визуальная временная диаграмма
```

`critical-chain` — главный инструмент оптимизации: показывает цепочку юнитов, которая **определяет** время загрузки (пока её не запустили, всё остальное ждёт).

## Практика

```bash
# 1. Базовое управление (примеры на реальном сервисе — sshd или nginx)
systemctl status sshd
systemctl is-active sshd
systemctl restart sshd
systemctl enable sshd            # автозапуск при загрузке
systemctl disable sshd           # отключить автозапуск

# 2. Просмотр юнита — где лежит и что внутри
systemctl cat sshd               # показать содержимое юнита
systemctl show sshd -p MainPID -p FragmentPath   # PID и путь к файлу

# 3. Зависимости и порядок
systemctl list-dependencies sshd
systemd-analyze critical-chain sshd
systemd-analyze dot sshd | dot -Tsvg > dep.svg   # если установлен graphviz

# 4. Targets
systemctl get-default            # текущая цель (например, graphical.target)
systemctl list-units --type=target   # все цели

# 5. Сокеты
systemctl list-sockets | head
ss -tlnp | head                  # кто реально слушает порты

# 6. Связь с I/O: сервис — это процесс
pgrep -a sshd
cat /proc/$(pgrep -x sshd | head -1)/io    # I/O-счётчики из п. 11
ls -l /proc/$(pgrep -x sshd | head -1)/fd  # дескрипторы из п. 6

# 7. Время загрузки
systemd-analyze
systemd-analyze blame | head
systemd-analyze plot > boot.svg
```

## Связь с предыдущими и следующими разделами

- `systemctl` → PID 1 через Unix-сокет, а сами сервисы — процессы с обычными дескрипторами из п. 6 и I/O-счётчиками из п. 11.
- Старт процесса — `fork`/`exec` (п. 7); на уровне ядра это те же `clone()`/`execve()`, что и для потоков.
- Логи сервисов — это перехваченные stdout/stderr (п. 6), которые journald складывает в `/var/log/journal/`.
- `Restart=`, планирование задач `systemd` через cgroups — мостик к управлению ресурсами и планировщику из п. 8.

Далее — заключение курса.
