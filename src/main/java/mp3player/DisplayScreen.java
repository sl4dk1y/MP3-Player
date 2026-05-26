package mp3player;

/**
 * Экранный дисплей — показывает информацию о текущем треке.
 * Реализует паттерн Observer в роли ConcreteObserver.
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class DisplayScreen implements MusicPlaybackObserver {
    
    private String currentSongName = "—";
    private String currentArtist = "—";
    private int currentDuration = 0;
    private int currentProgress = 0;
    
    @Override
    public void onPlaybackStarted(String songName, String artist, int duration) {
        this.currentSongName = songName;
        this.currentArtist = artist;
        this.currentDuration = duration;
        this.currentProgress = 0;
        
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                   ДИСПЛЕЙ ПЛЕЕРА                     ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.printf("║ Сейчас играет: %-36s ║\n", truncate(artist + " - " + songName, 36));
        System.out.printf("║ Длительность: %-37s ║\n", formatTime(duration));
        System.out.printf("║ Прогресс:     %-37s ║\n", "0:00 / " + formatTime(duration));
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    @Override
    public void onPlaybackStopped() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                   ДИСПЛЕЙ ПЛЕЕРА                     ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ Состояние: Воспроизведение ОСТАНОВЛЕНО               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    @Override
    public void onPlaybackFinished() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                   ДИСПЛЕЙ ПЛЕЕРА                     ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ Состояние: Трек ЗАВЕРШЁН                             ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    @Override
    public void onVolumeChanged(int volume) {
        System.out.println("[ДИСПЛЕЙ] 🔈 Громкость: " + volume + "%");
        drawVolumeBar(volume);
    }
    
    @Override
    public void onProgressChanged(int progress, int duration) {
        this.currentProgress = progress;
        System.out.print("\r[ДИСПЛЕЙ] Прогресс: " + formatTime(progress) + " / " + formatTime(duration) + " ");
        drawProgressBar(progress, duration);
    }
    
    /**
     * Рисует текстовый индикатор громкости
     */
    private void drawVolumeBar(int volume) {
        int barLength = 20;
        int filled = (volume * barLength) / 100;
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        
        System.out.println("[ДИСПЛЕЙ] " + bar.toString() + " " + volume + "%");
    }
    
    /**
     * Рисует текстовый прогресс-бар
     */
    private void drawProgressBar(int progress, int duration) {
        if (duration <= 0) return;
        
        int barLength = 30;
        int filled = (progress * barLength) / duration;
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "=" : i == filled ? ">" : " ");
        }
        bar.append("]");
        
        System.out.print(bar.toString());
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}