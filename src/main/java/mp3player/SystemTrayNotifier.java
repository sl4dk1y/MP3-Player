package mp3player;

/**
 * Системный трей — показывает уведомления (имитация).
 * В реальном приложении здесь был бы код для работы с java.awt.SystemTray.
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class SystemTrayNotifier implements MusicPlaybackObserver {
    
    private int lastVolume = 70;
    
    @Override
    public void onPlaybackStarted(String songName, String artist, int duration) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                 СИСТЕМНЫЙ ТРЕЙ                        ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.printf("║ 🔔 УВЕДОМЛЕНИЕ: Начинается %s - %s\n", artist, songName);
        System.out.println("║    Длительность: " + duration + " секунд                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }
    
    @Override
    public void onPlaybackStopped() {
        System.out.println("[ТРЕЙ] 🔔 Уведомление: Воспроизведение остановлено");
    }
    
    @Override
    public void onPlaybackFinished() {
        System.out.println("[ТРЕЙ] 🔔 Уведомление: Трек завершён");
    }
    
    @Override
    public void onVolumeChanged(int volume) {
        if (Math.abs(volume - lastVolume) >= 10) {
            System.out.println("[ТРЕЙ] 🔔 Уведомление: Громкость изменена на " + volume + "%");
            lastVolume = volume;
        }
    }
    
    @Override
    public void onProgressChanged(int progress, int duration) {
        // Трею не нужен прогресс каждую секунду — тишина
        // Отображаем только на 50% трека для примера
        if (duration > 0 && progress == duration / 2 && progress > 0) {
            System.out.println("[ТРЕЙ] 🔔 Половина трека пройдена!");
        }
    }
}