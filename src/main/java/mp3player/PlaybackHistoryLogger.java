package mp3player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Логгер — записывает все события воспроизведения в историю.
 * Реализует паттерн Observer в роли ConcreteObserver.
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class PlaybackHistoryLogger implements MusicPlaybackObserver {
    
    private final List<String> history = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public void onPlaybackStarted(String songName, String artist, int duration) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String entry = String.format("[%s] ▶ НАЧАЛО: %s - %s (длит: %d сек)", 
            timestamp, artist, songName, duration);
        history.add(entry);
        
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                      ЛОГГЕР                          ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-60s ║\n", truncate(entry, 60));
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    @Override
    public void onPlaybackStopped() {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String entry = String.format("[%s] ■ ОСТАНОВКА: Пользователь остановил воспроизведение", timestamp);
        history.add(entry);
        System.out.println("[ЛОГГЕР] " + entry);
    }
    
    @Override
    public void onPlaybackFinished() {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String entry = String.format("[%s] ⏹ ЗАВЕРШЕНИЕ: Трек доиграл до конца", timestamp);
        history.add(entry);
        System.out.println("[ЛОГГЕР] " + entry);
    }
    
    @Override
    public void onVolumeChanged(int volume) {
        // Логгеру не интересна громкость — метод пустой
        // Но мы обязаны его реализовать из-за интерфейса
    }
    
    @Override
    public void onProgressChanged(int progress, int duration) {
        // Логгеру не интересен текущий прогресс
    }
    
    /**
     * Вывести всю историю на экран
     */
    public void printHistory() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                 ИСТОРИЯ ПРОСЛУШИВАНИЙ                 ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        
        if (history.isEmpty()) {
            System.out.println("║ История пуста                                         ║");
        } else {
            for (int i = 0; i < history.size() && i < 10; i++) {
                System.out.printf("║ %-60s ║\n", truncate(history.get(i), 60));
            }
            if (history.size() > 10) {
                System.out.println("║ ... и ещё " + (history.size() - 10) + " записей                                ║");
            }
        }
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Получить количество записей в истории
     */
    public int getHistorySize() {
        return history.size();
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}