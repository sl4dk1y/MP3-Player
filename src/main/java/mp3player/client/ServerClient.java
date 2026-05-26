package mp3player.client;

import com.google.gson.Gson;
import mp3player.Song;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Клиент для общения с MusicServer
 */
public class ServerClient {
    
    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient httpClient;
    private final Gson gson;
    
    public ServerClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Получить список всех треков с сервера
     */
    public CompletableFuture<List<RemoteSongInfo>> fetchSongs() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/songs"))
            .header("Accept", "application/json")
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    System.err.println("[CLIENT] Ошибка: " + response.statusCode());
                    return Collections.emptyList();
                }
                RemoteSongInfo[] songs = gson.fromJson(response.body(), RemoteSongInfo[].class);
                return Arrays.asList(songs != null ? songs : new RemoteSongInfo[0]);
            });
    }
    
    /**
     * Поиск треков по запросу
     */
    public CompletableFuture<List<RemoteSongInfo>> searchSongs(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BASE_URL + "/songs/search?q=" + encodedQuery;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) return Collections.emptyList();
                    RemoteSongInfo[] songs = gson.fromJson(response.body(), RemoteSongInfo[].class);
                    return Arrays.asList(songs != null ? songs : new RemoteSongInfo[0]);
                });
        } catch (Exception e) {
            System.err.println("[CLIENT] Ошибка кодирования: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
    
    /**
     * Получить URL для стриминга трека
     */
    public String getStreamUrl(String songId) {
        return BASE_URL + "/songs/stream/" + songId;
    }
    
    /**
     * Скачать трек локально
     */
    public CompletableFuture<Path> downloadSong(String songId, Path destinationDir) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/songs/stream/" + songId))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(
            destinationDir.resolve(songId + ".mp3")
        )).thenApply(response -> {
            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        });
    }
    
    /**
     * Загрузить трек на сервер
     */
    public CompletableFuture<String> uploadSong(RemoteSongInfo song, Path audioFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] fileBytes = Files.readAllBytes(audioFile);
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("title", song.getTitle());
                payload.put("artist", song.getArtist());
                payload.put("album", song.getAlbum());
                payload.put("duration", song.getDuration());
                payload.put("fileName", audioFile.getFileName().toString());
                payload.put("fileContent", base64Content);
                
                String json = gson.toJson(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/songs/upload"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 201) {
                    Map<String, String> result = gson.fromJson(response.body(), Map.class);
                    return result.get("id");
                } else {
                    throw new RuntimeException("Upload failed: " + response.statusCode());
                }
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Удалить трек с сервера
     */
    public CompletableFuture<Boolean> deleteSong(String songId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/songs/delete?id=" + songId))
            .DELETE()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200);
    }
    
    /**
     * Проверить доступность сервера
     */
    public CompletableFuture<Boolean> isServerAvailable() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/health"))
            .timeout(Duration.ofSeconds(2))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .exceptionally(e -> false);
    }
    
    // === Методы для работы с плейлистами ===
    
    /**
     * Получить список всех плейлистов
     */
    public CompletableFuture<List<RemotePlaylistInfo>> fetchPlaylists() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/playlists"))
            .header("Accept", "application/json")
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    return Collections.emptyList();
                }
                RemotePlaylistInfo[] playlists = gson.fromJson(response.body(), RemotePlaylistInfo[].class);
                return Arrays.asList(playlists != null ? playlists : new RemotePlaylistInfo[0]);
            });
    }
    
    /**
     * Создать новый плейлист
     */
    public CompletableFuture<RemotePlaylistInfo> createPlaylist(RemotePlaylistInfo playlist) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = gson.toJson(playlist);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/playlists"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 201) {
                    return gson.fromJson(response.body(), RemotePlaylistInfo.class);
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Обновить плейлист
     */
    public CompletableFuture<Boolean> updatePlaylist(String playlistId, RemotePlaylistInfo playlist) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = gson.toJson(playlist);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/playlists/" + playlistId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                return response.statusCode() == 200;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Удалить плейлист
     */
    public CompletableFuture<Boolean> deletePlaylist(String playlistId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/playlists/" + playlistId))
            .DELETE()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200);
    }
    
    /**
     * Скачать плейлист (JSON)
     */
    public CompletableFuture<Path> downloadPlaylist(String playlistId, Path destination) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/playlists/download?id=" + playlistId))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(destination))
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return response.body();
                }
                return null;
            });
    }
    
    /**
     * Добавить трек в плейлист
     */
    public CompletableFuture<Boolean> addSongToPlaylist(String playlistId, String songId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> payload = Collections.singletonMap("songId", songId);
                String json = gson.toJson(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/playlists/" + playlistId + "/songs"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                return response.statusCode() == 200;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Получить треки из плейлиста
     */
    public CompletableFuture<List<RemoteSongInfo>> getPlaylistSongs(String playlistId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/playlists/" + playlistId + "/songs"))
            .header("Accept", "application/json")
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    return Collections.emptyList();
                }
                RemoteSongInfo[] songs = gson.fromJson(response.body(), RemoteSongInfo[].class);
                return Arrays.asList(songs != null ? songs : new RemoteSongInfo[0]);
            });
    }
    
    /**
     * Информация о треке с сервера
     */
    public static class RemoteSongInfo {
        private String id;
        private String title;
        private String artist;
        private String album;
        private int duration;
        private long fileSize;
        private String uploadedAt;
        private int playCount;
        
        public Song toLocalSong() {
            return new Song(title, artist, duration, "stream:" + id);
        }
        
        // Геттеры
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public int getDuration() { return duration; }
        public long getFileSize() { return fileSize; }
        public String getUploadedAt() { return uploadedAt; }
        public int getPlayCount() { return playCount; }
        
        // Сеттеры
        public void setTitle(String title) { this.title = title; }
        public void setArtist(String artist) { this.artist = artist; }
        public void setAlbum(String album) { this.album = album; }
        public void setDuration(int duration) { this.duration = duration; }
        
        @Override
        public String toString() {
            int minutes = duration / 60;
            int seconds = duration % 60;
            String formattedDuration = String.format("%02d:%02d", minutes, seconds);
            
            String cleanTitle = cleanFileName(title);
            String cleanArtist = cleanFileName(artist);
            
            if ("Неизвестный исполнитель".equals(cleanArtist) || cleanArtist.isEmpty()) {
                return cleanTitle + " [" + formattedDuration + "]";
            }
            
            return cleanArtist + " - " + cleanTitle + " [" + formattedDuration + "]";
        }
        
        private String cleanFileName(String name) {
            if (name == null || name.isEmpty()) return "";
            String cleaned = name.replace("_", " ");
            cleaned = cleaned.replaceAll("_?\\d{6,}$", "");
            cleaned = cleaned.trim().replaceAll("\\s+", " ");
            return cleaned;
        }
    }
    
    /**
     * Информация о плейлисте с сервера
     */
    public static class RemotePlaylistInfo {
        private String id;
        private String name;
        private String description;
        private String createdBy;
        private String createdAt;
        private List<String> songIds;
        
        public RemotePlaylistInfo() {
            this.songIds = new ArrayList<>();
        }
        
        public RemotePlaylistInfo(String name, String description) {
            this.name = name;
            this.description = description;
            this.songIds = new ArrayList<>();
        }
        
        // Геттеры и сеттеры
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public List<String> getSongIds() { return songIds; }
        public void setSongIds(List<String> songIds) { this.songIds = songIds; }
        
        @Override
        public String toString() {
            int songCount = songIds != null ? songIds.size() : 0;
            return "📁 " + name + " (" + songCount + " треков)";
        }
    }
}