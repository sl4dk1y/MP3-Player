package mp3player;

/**
 * Интерфейс наблюдателя событий воспроизведения.
 * Любой класс, который хочет следить за изменениями в MP3-плеере,
 * должен реализовать этот интерфейс.
 * 
 * Это реализация паттерна Observer — роль Observer (Наблюдатель)
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public interface MusicPlaybackObserver {
    
    /**
     * Вызывается, когда началось воспроизведение нового трека
     * 
     * @param songName название трека
     * @param artist исполнитель
     * @param duration длительность в секундах
     */
    void onPlaybackStarted(String songName, String artist, int duration);
    
    /**
     * Вызывается, когда воспроизведение остановлено (пользователем)
     */
    void onPlaybackStopped();
    
    /**
     * Вызывается, когда трек доиграл до конца естественным образом
     */
    void onPlaybackFinished();
    
    /**
     * Вызывается при изменении громкости
     * 
     * @param volume новое значение громкости (0-100)
     */
    void onVolumeChanged(int volume);
    
    /**
     * Вызывается при изменении позиции воспроизведения (прогресс)
     * 
     * @param progress позиция в секундах
     * @param duration общая длительность в секундах
     */
    void onProgressChanged(int progress, int duration);
}