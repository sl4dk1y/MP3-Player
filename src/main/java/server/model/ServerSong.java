package server.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель трека на сервере
 */
public class ServerSong {
    private String id;
    private String title;
    private String artist;
    private String album;
    private int duration;
    private String filePath;
    private String mimeType;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private int playCount;

    public ServerSong() {
        this.id = UUID.randomUUID().toString();
        this.uploadedAt = LocalDateTime.now();
        this.playCount = 0;
        this.mimeType = "audio/mpeg";
    }

    // Геттеры и Сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public int getPlayCount() { return playCount; }
    public void incrementPlayCount() { this.playCount++; }
}