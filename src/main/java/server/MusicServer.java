package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.model.Playlist;
import server.model.ServerSong;
import server.storage.PlaylistStorage;
import server.storage.SongStorage;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Основной класс сервера MP3-плеера
 */
public class MusicServer {
    
    private static final int PORT = 8080;
    private static final String API = "/api";
    
    private HttpServer server;
    private SongStorage songStorage;
    private PlaylistStorage playlistStorage;
    private final Gson gson;

    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter());
        this.gson = builder.create();
    }

    public MusicServer() throws IOException {
        songStorage = new SongStorage();
        playlistStorage = new PlaylistStorage();  // ← Инициализация хранилища плейлистов
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        setupRoutes();
    }

    private void setupRoutes() {
        // === Song endpoints ===
        server.createContext(API + "/songs", new SongsHandler());
        server.createContext(API + "/songs/search", new SearchHandler());
        server.createContext(API + "/songs/stream", new StreamHandler());
        server.createContext(API + "/songs/upload", new UploadHandler());
        server.createContext(API + "/songs/delete", new DeleteSongHandler());
        
        // === Playlist endpoints ===
        server.createContext(API + "/playlists", new PlaylistsHandler());  // ← Добавлено!
        server.createContext(API + "/playlists/download", new DownloadPlaylistHandler());
        
        // === System endpoints ===
        server.createContext("/health", exchange -> {
            sendJson(exchange, 200, Map.of("status", "OK", "port", PORT));
        });
        
        // Root page
        server.createContext("/", exchange -> {
            String html = """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8">
                    <title>🎵 Music Streaming Server</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                               max-width: 900px; margin: 40px auto; padding: 20px; background: #f5f5f5; }
                        .container { background: white; padding: 30px; border-radius: 12px; 
                                     box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
                        h2 { color: #34495e; margin-top: 30px; }
                        code { background: #ecf0f1; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
                        .endpoint { background: #f8f9fa; padding: 15px; margin: 10px 0; 
                                   border-left: 4px solid #3498db; border-radius: 0 4px 4px 0; }
                        .method { display: inline-block; padding: 3px 8px; border-radius: 3px; 
                                 font-weight: bold; font-size: 0.9em; margin-right: 10px; }
                        .get { background: #61affe; color: white; }
                        .post { background: #49cc90; color: white; }
                        .delete { background: #f93e3e; color: white; }
                        .put { background: #50e3c2; color: white; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🎵 Music Streaming Server</h1>
                        <p><strong>Статус:</strong> <span style="color:green">● Работает</span></p>
                        <p><strong>Порт:</strong> %d</p>
                        
                        <div style="background: #e8f4f8; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <h3>📊 Статистика:</h3>
                            <p>Треков на сервере: %d</p>
                            <p>Плейлистов: %d</p>
                        </div>
                        
                        <h2>📡 API Endpoints:</h2>
                        
                        <h3>🎵 Управление треками:</h3>
                        
                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <code>/api/songs</code><br>
                            <small>Получить список всех треков</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <code>/api/songs/search?q=запрос</code><br>
                            <small>Поиск треков</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <code>/api/songs/stream/{id}</code><br>
                            <small>Стриминг аудиофайла</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method post">POST</span>
                            <code>/api/songs/upload</code><br>
                            <small>Загрузить новый трек</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method delete">DELETE</span>
                            <code>/api/songs/delete?id={id}</code><br>
                            <small>Удалить трек</small>
                        </div>
                        
                        <h3>📁 Управление плейлистами:</h3>
                        
                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <code>/api/playlists</code><br>
                            <small>Получить список всех плейлистов</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method post">POST</span>
                            <code>/api/playlists</code><br>
                            <small>Создать новый плейлист</small><br>
                            <strong>Тело:</strong> <code>{"name":"My Playlist","description":"..."}</code>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method put">PUT</span>
                            <code>/api/playlists/{id}</code><br>
                            <small>Обновить плейлист</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method delete">DELETE</span>
                            <code>/api/playlists/{id}</code><br>
                            <small>Удалить плейлист</small>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <code>/api/playlists/download?id={id}</code><br>
                            <small>Скачать плейлист (JSON)</small>
                        </div>
                        
                        <hr>
                        <p style="color:#7f8c8d;font-size:0.9em">
                            MP3 Streaming Server v2.0 • Курсовая работа • Галкин А.П., гр. 23ВВИ1
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(PORT, songStorage.getAllSongs().size(), playlistStorage.getAllPlaylists().size());

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
    }

    public void start() {
        server.start();
        System.out.println("🎵 Music Server запущен на http://localhost:" + PORT);
        System.out.println("📂 Данные хранятся в папке ./server_data");
        System.out.println("🔗 API документация: http://localhost:" + PORT + "/");
        System.out.println("📊 Треков: " + songStorage.getAllSongs().size());
        System.out.println("📁 Плейлистов: " + playlistStorage.getAllPlaylists().size());
    }

    public void stop() {
        server.stop(0);
        System.out.println("🛑 Music Server остановлен");
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // === Handlers ===

    class SongsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                List<ServerSong> songs = songStorage.getAllSongs();
                List<Map<String, Object>> result = songs.stream().map(song -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", song.getId());
                    map.put("title", song.getTitle());
                    map.put("artist", song.getArtist());
                    map.put("album", song.getAlbum());
                    map.put("duration", song.getDuration());
                    map.put("fileSize", song.getFileSize());
                    map.put("uploadedAt", song.getUploadedAt().toString());
                    map.put("playCount", song.getPlayCount());
                    return map;
                }).collect(Collectors.toList());
                sendJson(exchange, 200, result);
            } else {
                sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            }
        }
    }

    class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String searchQuery = "";
            if (query != null && query.startsWith("q=")) {
                searchQuery = query.substring(2);
            }
            
            List<ServerSong> results = songStorage.search(searchQuery);
            List<Map<String, Object>> result = results.stream().map(song -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", song.getId());
                map.put("title", song.getTitle());
                map.put("artist", song.getArtist());
                map.put("album", song.getAlbum());
                map.put("duration", song.getDuration());
                map.put("fileSize", song.getFileSize());
                map.put("uploadedAt", song.getUploadedAt().toString());
                map.put("playCount", song.getPlayCount());
                return map;
            }).collect(Collectors.toList());
            
            sendJson(exchange, 200, result);
        }
    }

    class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String id = path.replace(API + "/songs/stream/", "").replaceAll("/", "");
            
            if (id.isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "Song ID required"));
                return;
            }
            
            InputStream audioStream = songStorage.getSongStream(id);
            if (audioStream == null) {
                sendJson(exchange, 404, Map.of("error", "Song not found", "id", id));
                return;
            }
            
            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                handleRangeRequest(exchange, id, rangeHeader);
            } else {
                exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, 0);
                
                try (OutputStream out = exchange.getResponseBody()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = audioStream.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
            
            audioStream.close();
            ServerSong song = songStorage.getSongById(id);
            if (song != null) song.incrementPlayCount();
        }
        
        private void handleRangeRequest(HttpExchange exchange, String songId, String rangeHeader) throws IOException {
            ServerSong song = songStorage.getSongById(songId);
            if (song == null) {
                sendJson(exchange, 404, Map.of("error", "Song not found"));
                return;
            }
            
            String range = rangeHeader.replace("bytes=", "");
            long start = 0;
            long end = song.getFileSize() - 1;
            
            String[] parts = range.split("-");
            if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
            
            if (start > end || start >= song.getFileSize()) {
                exchange.getResponseHeaders().set("Content-Range", "bytes */" + song.getFileSize());
                sendJson(exchange, 416, Map.of("error", "Range not satisfiable"));
                return;
            }
            
            long contentLength = end - start + 1;
            
            try (RandomAccessFile raf = new RandomAccessFile(song.getFilePath(), "r")) {
                raf.seek(start);
                
                exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
                exchange.getResponseHeaders().set("Content-Range", 
                    "bytes " + start + "-" + end + "/" + song.getFileSize());
                exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(206, contentLength);
                
                try (OutputStream out = exchange.getResponseBody()) {
                    byte[] buffer = new byte[8192];
                    long remaining = contentLength;
                    int read;
                    
                    while (remaining > 0 && (read = raf.read(buffer, 0, 
                            (int) Math.min(buffer.length, remaining))) != -1) {
                        out.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            }
        }
    }

    class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }
                
                byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = gson.fromJson(body, Map.class);
                
                if (data == null || !data.containsKey("fileContent")) {
                    sendJson(exchange, 400, Map.of("error", "Missing required field: fileContent"));
                    return;
                }
                
                ServerSong song = new ServerSong();
                song.setTitle((String) data.getOrDefault("title", "Unknown Title"));
                song.setArtist((String) data.getOrDefault("artist", "Unknown Artist"));
                song.setAlbum((String) data.getOrDefault("album", "Unknown Album"));
                
                Object durationObj = data.get("duration");
                int duration = 180;
                if (durationObj instanceof Number) {
                    duration = ((Number) durationObj).intValue();
                } else if (durationObj instanceof String) {
                    try {
                        duration = Integer.parseInt((String) durationObj);
                    } catch (NumberFormatException e) {
                        duration = 180;
                    }
                }
                song.setDuration(Math.max(0, duration));
                
                String base64Content = (String) data.get("fileContent");
                if (base64Content == null || base64Content.isEmpty()) {
                    sendJson(exchange, 400, Map.of("error", "Empty file content"));
                    return;
                }
                
                byte[] fileBytes = Base64.getDecoder().decode(base64Content);
                System.out.println("[SERVER] 📥 Загрузка: " + song.getTitle() + 
                                 " | Размер: " + fileBytes.length + " байт");
                
                ServerSong saved = songStorage.addSong(new ByteArrayInputStream(fileBytes), song);
                
                if (saved != null) {
                    System.out.println("[SERVER] ✅ Сохранён: " + saved.getId());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", saved.getId());
                    response.put("message", "Uploaded successfully");
                    response.put("title", saved.getTitle());
                    response.put("artist", saved.getArtist());
                    response.put("duration", saved.getDuration());
                    
                    sendJson(exchange, 201, response);
                } else {
                    sendJson(exchange, 500, Map.of("error", "Failed to save file"));
                }
                
            } catch (IllegalArgumentException e) {
                System.err.println("[SERVER] ❌ Ошибка Base64: " + e.getMessage());
                sendJson(exchange, 400, Map.of("error", "Invalid base64 encoding"));
            } catch (Exception e) {
                System.err.println("[SERVER] ❌ Ошибка upload: " + e.getMessage());
                e.printStackTrace();
                sendJson(exchange, 500, Map.of("error", "Upload failed: " + e.getMessage()));
            }
        }
    }

    class DeleteSongHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"DELETE".equals(exchange.getRequestMethod()) && !"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String id = "";
            if (query != null && query.startsWith("id=")) {
                id = query.substring(3);
            }

            if (id.isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "Song ID required"));
                return;
            }

            System.out.println("[SERVER] 🗑 Удаление трека: " + id);
            
            if (songStorage.deleteSong(id)) {
                System.out.println("[SERVER] ✅ Трек удалён");
                sendJson(exchange, 200, Map.of("message", "Song deleted", "id", id));
            } else {
                System.out.println("[SERVER] ❌ Трек не найден");
                sendJson(exchange, 404, Map.of("error", "Song not found"));
            }
        }
    }

    // === Playlist Handler ===
    class PlaylistsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            System.out.println("[SERVER] 📁 Playlist request: " + method + " " + path);

            // GET /api/playlists - получить все плейлисты
            if ("GET".equals(method) && path.equals(API + "/playlists")) {
                List<Playlist> playlists = playlistStorage.getAllPlaylists();
                System.out.println("[SERVER] Возвращаю " + playlists.size() + " плейлистов");
                sendJson(exchange, 200, playlists);
                return;
            }

            // === ДОБАВЛЕНО: GET /api/playlists/{id}/songs - получить треки плейлиста ===
            if ("GET".equals(method) && path.contains("/songs")) {
                System.out.println("[SERVER] Получение треков из плейлиста: " + path);

                try {
                    // Извлекаем ID плейлиста из пути
                    String[] parts = path.replace(API + "/playlists/", "").split("/");
                    String playlistId = parts[0];

                    System.out.println("[SERVER] Запрашиваю треки плейлиста " + playlistId);

                    // Получаем плейлист
                    Playlist playlist = playlistStorage.getPlaylist(playlistId);

                    if (playlist == null) {
                        System.out.println("[SERVER] ❌ Плейлист не найден");
                        sendJson(exchange, 404, Map.of("error", "Playlist not found"));
                        return;
                    }

                    // Получаем информацию о треках
                    List<ServerSong> songs = new ArrayList<>();
                    for (String songId : playlist.getSongIds()) {
                        ServerSong song = songStorage.getSongById(songId);
                        if (song != null) {
                            songs.add(song);
                        }
                    }

                    System.out.println("[SERVER] Возвращаю " + songs.size() + " треков из плейлиста");

                    // Конвертируем в Map для JSON
                    List<Map<String, Object>> result = songs.stream().map(song -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", song.getId());
                        map.put("title", song.getTitle());
                        map.put("artist", song.getArtist());
                        map.put("album", song.getAlbum());
                        map.put("duration", song.getDuration());
                        map.put("fileSize", song.getFileSize());
                        map.put("uploadedAt", song.getUploadedAt().toString());
                        map.put("playCount", song.getPlayCount());
                        return map;
                    }).collect(Collectors.toList());

                    sendJson(exchange, 200, result);
                } catch (Exception e) {
                    System.err.println("[SERVER] ❌ Ошибка получения треков: " + e.getMessage());
                    e.printStackTrace();
                    sendJson(exchange, 500, Map.of("error", "Failed to get playlist songs: " + e.getMessage()));
                }
                return;
            }
            // ============================================================================

            // POST /api/playlists - создать плейлист
            if ("POST".equals(method) && path.equals(API + "/playlists")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("[SERVER] Создание плейлиста: " + body);

                try {
                    Playlist playlist = gson.fromJson(body, Playlist.class);
                    Playlist created = playlistStorage.createPlaylist(playlist);
                    System.out.println("[SERVER] ✅ Плейлист создан: " + created.getId());
                    sendJson(exchange, 201, created);
                } catch (Exception e) {
                    System.err.println("[SERVER] ❌ Ошибка создания плейлиста: " + e.getMessage());
                    e.printStackTrace();
                    sendJson(exchange, 500, Map.of("error", "Failed to create playlist: " + e.getMessage()));
                }
                return;
            }

            // POST /api/playlists/{id}/songs - добавить трек в плейлист
            if ("POST".equals(method) && path.contains("/songs")) {
                System.out.println("[SERVER] Добавление трека в плейлист: " + path);

                try {
                    // Извлекаем ID плейлиста из пути
                    String[] parts = path.replace(API + "/playlists/", "").split("/");
                    String playlistId = parts[0];

                    // Читаем тело запроса
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    System.out.println("[SERVER] Тело запроса: " + body);

                    // Парсим JSON
                    Map<String, String> data = gson.fromJson(body, Map.class);
                    String songId = data.get("songId");

                    System.out.println("[SERVER] Добавляю трек " + songId + " в плейлист " + playlistId);

                    // Добавляем трек
                    boolean success = playlistStorage.addSongToPlaylist(playlistId, songId);

                    if (success) {
                        System.out.println("[SERVER] ✅ Трек добавлен в плейлист");
                        sendJson(exchange, 200, Map.of("message", "Song added to playlist"));
                    } else {
                        System.out.println("[SERVER] ❌ Плейлист не найден");
                        sendJson(exchange, 404, Map.of("error", "Playlist not found"));
                    }
                } catch (Exception e) {
                    System.err.println("[SERVER] ❌ Ошибка добавления трека: " + e.getMessage());
                    e.printStackTrace();
                    sendJson(exchange, 500, Map.of("error", "Failed to add song: " + e.getMessage()));
                }
                return;
            }

            // PUT /api/playlists/{id} - обновить плейлист
            if ("PUT".equals(method) && path.startsWith(API + "/playlists/")) {
                String id = path.replace(API + "/playlists/", "").split("/")[0];
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Playlist updates = gson.fromJson(body, Playlist.class);
                Playlist updated = playlistStorage.updatePlaylist(id, updates);
                if (updated != null) {
                    sendJson(exchange, 200, updated);
                } else {
                    sendJson(exchange, 404, Map.of("error", "Playlist not found"));
                }
                return;
            }

            // DELETE /api/playlists/{id} - удалить плейлист
            if ("DELETE".equals(method) && path.startsWith(API + "/playlists/")) {
                String id = path.replace(API + "/playlists/", "").split("\\?")[0];
                if (playlistStorage.deletePlaylist(id)) {
                    sendJson(exchange, 200, Map.of("message", "Playlist deleted"));
                } else {
                    sendJson(exchange, 404, Map.of("error", "Playlist not found"));
                }
                return;
            }

            sendJson(exchange, 404, Map.of("error", "Not found"));
        }
    }

    class DownloadPlaylistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String playlistId = "";
            if (query != null && query.startsWith("id=")) {
                playlistId = query.substring(3);
            }
            
            Playlist playlist = playlistStorage.getPlaylist(playlistId);
            if (playlist == null) {
                sendJson(exchange, 404, Map.of("error", "Playlist not found"));
                return;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("playlist", playlist);
            
            List<Map<String, Object>> songs = new ArrayList<>();
            for (String songId : playlist.getSongIds()) {
                var song = songStorage.getSongById(songId);
                if (song != null) {
                    Map<String, Object> songMap = new HashMap<>();
                    songMap.put("id", song.getId());
                    songMap.put("title", song.getTitle());
                    songMap.put("artist", song.getArtist());
                    songMap.put("duration", song.getDuration());
                    songs.add(songMap);
                }
            }
            response.put("songs", songs);
            
            String json = gson.toJson(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Content-Disposition", 
                "attachment; filename=\"" + playlist.getName() + ".json\"");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().close();
        }
    }
    
    /**
     * Адаптер для сериализации LocalDateTime
     */
    static class LocalDateTimeAdapter implements 
        com.google.gson.JsonSerializer<java.time.LocalDateTime>, 
        com.google.gson.JsonDeserializer<java.time.LocalDateTime> {

        @Override
        public com.google.gson.JsonElement serialize(java.time.LocalDateTime src, 
                                                      java.lang.reflect.Type typeOfSrc, 
                                                      com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public java.time.LocalDateTime deserialize(com.google.gson.JsonElement json, 
                                                     java.lang.reflect.Type typeOfT, 
                                                     com.google.gson.JsonDeserializationContext context) 
                throws com.google.gson.JsonParseException {
            return java.time.LocalDateTime.parse(json.getAsString());
        }
    }    

    public static void main(String[] args) {
        try {
            MusicServer server = new MusicServer();
            server.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Получен сигнал завершения...");
                server.stop();
            }));
            
            System.out.println("✅ Сервер готов. Нажмите Ctrl+C для остановки.");
            
        } catch (IOException e) {
            System.err.println("❌ Ошибка запуска сервера: " + e.getMessage());
            if (e.getMessage().contains("Address already in use")) {
                System.err.println("💡 Порт 8080 занят. Завершите другой процесс или измените PORT в коде.");
            }
            System.exit(1);
        }
    }
}