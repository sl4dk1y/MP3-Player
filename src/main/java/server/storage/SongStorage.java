package server.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import server.model.ServerSong;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Хранилище треков (файловая система + JSON индекс)
 */
public class SongStorage {
    private static final String STORAGE_DIR = "server_data/songs/";
    private static final String INDEX_FILE = "server_data/index.json";

    private final Map<String, ServerSong> index = new HashMap<>();
    private final Gson gson;

    public SongStorage() {
        // Настраиваем Gson для работы с датами
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        gson = builder.create();

        // Создаем папку если нет
        try { Files.createDirectories(Path.of(STORAGE_DIR)); } 
        catch (IOException e) { e.printStackTrace(); }

        loadIndex();
    }

    private void loadIndex() {
        try {
            Path path = Path.of(INDEX_FILE);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                ServerSong[] songs = gson.fromJson(json, ServerSong[].class);
                if (songs != null) {
                    for (ServerSong s : songs) index.put(s.getId(), s);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки индекса: " + e.getMessage());
        }
    }

    private void saveIndex() {
        try {
            Path path = Path.of(INDEX_FILE);
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(index.values().toArray()));
        } catch (IOException e) {
            System.err.println("Ошибка сохранения индекса: " + e.getMessage());
        }
    }

    // === Основные методы ===

    public ServerSong addSong(InputStream fileInput, ServerSong metadata) {
        try {
            // Сохраняем файл
            Path filePath = Path.of(STORAGE_DIR, metadata.getId() + ".mp3");
            Files.copy(fileInput, filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Обновляем метаданные
            metadata.setFilePath(filePath.toString());
            metadata.setFileSize(Files.size(filePath));
            
            // Добавляем в индекс
            index.put(metadata.getId(), metadata);
            saveIndex();
            return metadata;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getSongStream(String id) {
        ServerSong song = index.get(id);
        if (song != null) {
            try {
                return new FileInputStream(song.getFilePath());
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public List<ServerSong> getAllSongs() {
        return new ArrayList<>(index.values());
    }

    public ServerSong getSongById(String id) {
        return index.get(id);
    }

    public List<ServerSong> search(String query) {
        String q = query.toLowerCase();
        return index.values().stream()
            .filter(s -> s.getTitle().toLowerCase().contains(q) || 
                         s.getArtist().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }
    /**
     * Удалить трек по ID (и файл, и запись в индексе)
     */
    public boolean deleteSong(String id) {
        ServerSong song = index.remove(id);
        if (song != null) {
            try {
                // Удаляем файл с диска
                File file = new File(song.getFilePath());
                if (file.exists()) {
                    file.delete();
                    System.out.println("[STORAGE] Файл удалён: " + song.getFilePath());
                }
                // Обновляем JSON индекс
                saveIndex();
                System.out.println("[STORAGE] Трек удалён из индекса: " + song.getTitle());
                return true;
            } catch (Exception e) {
                System.err.println("[STORAGE] Ошибка удаления файла: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    // Адаптер даты для Gson
    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
    
    
}