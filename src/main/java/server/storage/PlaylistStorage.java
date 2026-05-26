package server.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import server.model.Playlist;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище плейлистов
 */
public class PlaylistStorage {
    
    private static final String PLAYLISTS_DIR = "server_data/playlists/";
    private static final String INDEX_FILE = "server_data/playlists_index.json";
    
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();
    private final Gson gson;
    
    public PlaylistStorage() {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        gson = builder.create();
        
        try {
            Files.createDirectories(Path.of(PLAYLISTS_DIR));
        } catch (IOException e) {
            System.err.println("[PLAYLIST STORAGE] Ошибка создания директории: " + e.getMessage());
        }
        
        loadPlaylists();
    }
    
    private void loadPlaylists() {
        try {
            Path indexPath = Path.of(INDEX_FILE);
            if (Files.exists(indexPath)) {
                String json = Files.readString(indexPath);
                Playlist[] playlistArray = gson.fromJson(json, Playlist[].class);
                if (playlistArray != null) {
                    for (Playlist playlist : playlistArray) {
                        playlists.put(playlist.getId(), playlist);
                    }
                    System.out.println("[PLAYLIST STORAGE] Загружено " + playlists.size() + " плейлистов");
                }
            }
        } catch (IOException e) {
            System.err.println("[PLAYLIST STORAGE] Ошибка загрузки: " + e.getMessage());
        }
    }
    
    private void saveIndex() {
        try {
            Path indexPath = Path.of(INDEX_FILE);
            Files.createDirectories(indexPath.getParent());
            Playlist[] array = playlists.values().toArray(new Playlist[0]);
            Files.writeString(indexPath, gson.toJson(array));
        } catch (IOException e) {
            System.err.println("[PLAYLIST STORAGE] Ошибка сохранения: " + e.getMessage());
        }
    }
    
    // CRUD операции
    
    public Playlist createPlaylist(Playlist playlist) {
        playlists.put(playlist.getId(), playlist);
        saveIndex();
        System.out.println("[PLAYLIST STORAGE] Создан плейлист: " + playlist.getName());
        return playlist;
    }
    
    public Playlist getPlaylist(String id) {
        return playlists.get(id);
    }
    
    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }
    
    public Playlist updatePlaylist(String id, Playlist updates) {
        Playlist existing = playlists.get(id);
        if (existing != null) {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            existing.setPublic(updates.isPublic());
            existing.setUpdatedAt(LocalDateTime.now());
            saveIndex();
            return existing;
        }
        return null;
    }
    
    public boolean deletePlaylist(String id) {
        Playlist removed = playlists.remove(id);
        if (removed != null) {
            saveIndex();
            System.out.println("[PLAYLIST STORAGE] Удалён плейлист: " + removed.getName());
            return true;
        }
        return false;
    }
    
    public boolean addSongToPlaylist(String playlistId, String songId) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null) {
            playlist.addSong(songId);
            saveIndex();
            return true;
        }
        return false;
    }
    
    public boolean removeSongFromPlaylist(String playlistId, String songId) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null) {
            playlist.removeSong(songId);
            saveIndex();
            return true;
        }
        return false;
    }
    
    // Адаптер для LocalDateTime
    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}