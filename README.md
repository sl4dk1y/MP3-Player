# 🎵 MP3 Streaming Player

Курсовая работа по дисциплине «Разработка кроссплатформенных приложений». Десктопное приложение MP3-плеера с функциями стриминга и удаленного управления плейлистами через REST API.

## 🔧 Технологии

- **Java 17**
- **JavaFX** (медиа-движок)
- **Swing** (графический интерфейс)
- **Maven** (сборка)
- **Gson** (парсинг JSON)
- **JAudioTagger** (чтение метаданных MP3)
- **HttpServer** (встроенный сервер Java)

## 🚀 Как запустить

Проект состоит из двух частей: Сервера и Клиента.

### 1. Запуск сервера
Запустите класс `MusicServer.java` (пакет `server`).
Сервер запустится на `http://localhost:8080`.

### 2. Запуск клиента
Запустите класс `Main.java` (пакет `mp3player`).
Клиент подключится к серверу и откроет интерфейс.

## 📁 Структура проекта
```text
MP3-player/
├── src/
│   └── main/
│       └── java/
│           ├── mp3player/                 # 🎨 Модуль клиента (UI, плеер)
│           │   ├── client/                #  HTTP-клиент для общения с сервером
│           │   │   └── ServerClient.java
│           │   ├── DisplayScreen.java     #  Экран плеера
│           │   ├── Main.java              #  Точка входа
│           │   ├── MP3Player.java         #  Логика воспроизведения
│           │   ├── MP3PlayerUI.java       #  Графический интерфейс
│           │   ├── MusicPlaybackObserver.java
│           │   ├── PlaybackHistoryLogger.java
│           │   ├── Song.java              #  Модель песни
│           │   └── SystemTrayNotifier.java
│           │
│           └── server/                    #  Модуль сервера
│               ├── model/                 #  Модели данных
│               │   ├── Playlist.java
│               │   └── ServerSong.java
│               ├── storage/               #  Хранилище файлов и JSON
│               │   ├── PlaylistStorage.java
│               │   └── SongStorage.java
│               └── MusicServer.java       #  Основной класс сервера
│
├── pom.xml                                # ️ Настройки сборки Maven
└── README.md                              #  Описание проекта