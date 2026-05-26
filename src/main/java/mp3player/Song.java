package mp3player;

import java.io.File;
import java.util.Objects;

/**
 * Модель данных для музыкального трека.
 * Хранит информацию об одной песне.
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class Song {
    
    private String title;      // Название трека
    private String artist;     // Исполнитель
    private int duration;      // Длительность в секундах
    private String filePath;   // Путь к файлу на диске
    private String album;      // Альбом (опционально)
    private int year;          // Год выпуска
    
    /**
     * Конструктор для создания трека
     * 
     * @param title название трека
     * @param artist исполнитель
     * @param duration длительность в секундах
     * @param filePath путь к MP3-файлу
     */
    public Song(String title, String artist, int duration, String filePath) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.filePath = filePath;
        this.album = "Неизвестный альбом";
        this.year = 0;
    }
    
    /**
     * Альтернативный конструктор с полной информацией
     */
    public Song(String title, String artist, int duration, String filePath, String album, int year) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.filePath = filePath;
        this.album = album;
        this.year = year;
    }
    
    // ============= Геттеры и сеттеры =============
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    /**
     * Форматирует длительность в формат MM:SS
     * 
     * @return строка вида "03:45"
     */
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    @Override
    public String toString() {
        return artist + " - " + title + " [" + getFormattedDuration() + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Song song = (Song) obj;
        return Objects.equals(filePath, song.filePath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }
}