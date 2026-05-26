package mp3player;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MP3-плеер с поддержкой локальных файлов и стриминга
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class MusicPlayer {
    
    private final List<MusicPlaybackObserver> observers = new CopyOnWriteArrayList<>();
    
    private Song currentSong;
    private int currentVolume = 70;
    private int currentProgress = 0;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    
    private MediaPlayer mediaPlayer;
    private Thread progressThread;
    private volatile boolean stopProgress = false;
    private static boolean javaFXInitialized = false;
    
    public MusicPlayer() {
        initJavaFX();
    }
    
    private void initJavaFX() {
        if (!javaFXInitialized) {
            try {
                new JFXPanel();
                javaFXInitialized = true;
                System.out.println("[PLAYER] JavaFX инициализирован");
            } catch (Exception e) {
                System.err.println("[PLAYER] Ошибка JavaFX: " + e.getMessage());
            }
        }
    }
    
    public void addObserver(MusicPlaybackObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            System.out.println("[PLAYER] Наблюдатель подписан. Всего: " + observers.size());
        }
    }
    
    public void removeObserver(MusicPlaybackObserver observer) {
        observers.remove(observer);
        System.out.println("[PLAYER] Наблюдатель отписан. Осталось: " + observers.size());
    }
    
    private void notifyPlaybackStarted() {
        for (MusicPlaybackObserver obs : observers) {
            obs.onPlaybackStarted(
                currentSong.getTitle(), 
                currentSong.getArtist(), 
                currentSong.getDuration()
            );
        }
    }
    
    private void notifyPlaybackStopped() {
        for (MusicPlaybackObserver obs : observers) {
            obs.onPlaybackStopped();
        }
    }
    
    private void notifyPlaybackFinished() {
        for (MusicPlaybackObserver obs : observers) {
            obs.onPlaybackFinished();
        }
    }
    
    private void notifyVolumeChanged() {
        for (MusicPlaybackObserver obs : observers) {
            obs.onVolumeChanged(currentVolume);
        }
    }
    
    private void notifyProgressChanged() {
        for (MusicPlaybackObserver obs : observers) {
            obs.onProgressChanged(currentProgress, currentSong.getDuration());
        }
    }
    
    private void disposeMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                // Игнорируем
            }
            mediaPlayer = null;
        }
    }
    
    public void play(Song song) {
        if (song == null) {
            System.err.println("[PLAYER] Ошибка: песня null");
            return;
        }
        
        if (isPlaying) {
            stop();
        }
        
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        this.currentSong = song;
        this.currentProgress = 0;
        this.isPlaying = true;
        this.isPaused = false;
        
        System.out.println("[PLAYER] ▶ " + song.getArtist() + " - " + song.getTitle());
        
        notifyPlaybackStarted();
        notifyVolumeChanged();
        notifyProgressChanged();
        
        String filePath = song.getFilePath();
        if (filePath != null && (filePath.startsWith("http://") || filePath.startsWith("https://"))) {
            startStreamingPlayback(filePath);
        } else {
            startLocalPlayback(filePath);
        }
        
        startProgressUpdater();
    }
    
    private void startLocalPlayback(String filePath) {
        if (filePath == null || filePath.isEmpty() || filePath.startsWith("dummy_")) {
            System.err.println("[PLAYER] Файл не найден: " + filePath);
            isPlaying = false;
            notifyPlaybackStopped();
            return;
        }
        
        Platform.runLater(() -> {
            try {
                disposeMediaPlayer();
                
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println("[PLAYER] Файл не существует: " + filePath);
                    isPlaying = false;
                    notifyPlaybackStopped();
                    return;
                }
                
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(currentVolume / 100.0);
                
                mediaPlayer.setOnEndOfMedia(() -> {
                    Platform.runLater(() -> {
                        if (isPlaying) {
                            isPlaying = false;
                            System.out.println("[PLAYER] Трек закончился");
                            notifyPlaybackFinished();
                        }
                    });
                });
                
                mediaPlayer.setOnReady(() -> {
                    System.out.println("[PLAYER] Трек готов");
                    mediaPlayer.play();
                });
                
                mediaPlayer.setOnError(() -> {
                    System.err.println("[PLAYER] Ошибка: " + mediaPlayer.getError());
                    isPlaying = false;
                });
                
                if (mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                    mediaPlayer.play();
                }
                
            } catch (Exception e) {
                System.err.println("[PLAYER] Ошибка: " + e.getMessage());
                e.printStackTrace();
                isPlaying = false;
            }
        });
    }
    
    private void startStreamingPlayback(String streamUrl) {
        Platform.runLater(() -> {
            try {
                disposeMediaPlayer();
                
                System.out.println("[PLAYER] 🌐 Стриминг с: " + streamUrl);
                
                Media media = new Media(streamUrl);
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(currentVolume / 100.0);
                
                mediaPlayer.setOnEndOfMedia(() -> {
                    Platform.runLater(() -> {
                        if (isPlaying) {
                            isPlaying = false;
                            System.out.println("[PLAYER] Стрим закончился");
                            notifyPlaybackFinished();
                        }
                    });
                });
                
                mediaPlayer.setOnReady(() -> {
                    System.out.println("[PLAYER] Стрим готов");
                    mediaPlayer.play();
                });
                
                mediaPlayer.setOnError(() -> {
                    System.err.println("[PLAYER] Ошибка стриминга: " + mediaPlayer.getError());
                    isPlaying = false;
                });
                
                if (mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                    mediaPlayer.play();
                }
                
            } catch (Exception e) {
                System.err.println("[PLAYER] Ошибка стриминга: " + e.getMessage());
                e.printStackTrace();
                isPlaying = false;
            }
        });
    }
    
    private void startProgressUpdater() {
        stopProgress = false;
        
        progressThread = new Thread(() -> {
            try {
                while (!stopProgress && isPlaying) {
                    Thread.sleep(300);
                    if (isPlaying && !isPaused && mediaPlayer != null) {
                        Platform.runLater(() -> {
                            try {
                                Duration currentTime = mediaPlayer.getCurrentTime();
                                if (currentTime != null) {
                                    double seconds = currentTime.toSeconds();
                                    if (!Double.isNaN(seconds) && seconds > 0) {
                                        currentProgress = (int) seconds;
                                        notifyProgressChanged();
                                    }
                                }
                            } catch (Exception e) {
                                // Игнорируем
                            }
                        });
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        progressThread.setDaemon(true);
        progressThread.start();
    }
    
    public void pause() {
        if (!isPlaying || isPaused) return;
        isPaused = true;
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try { mediaPlayer.pause(); } catch (Exception e) {}
            }
        });
        System.out.println("[PLAYER] ⏸ Пауза");
    }
    
    public void resume() {
        if (!isPlaying || !isPaused) return;
        isPaused = false;
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try { mediaPlayer.play(); } catch (Exception e) {}
            }
        });
        System.out.println("[PLAYER] ▶ Возобновлено");
    }
    
    public void stop() {
        if (!isPlaying) return;
        
        stopProgress = true;
        isPlaying = false;
        isPaused = false;
        
        Platform.runLater(() -> disposeMediaPlayer());
        
        if (progressThread != null) {
            progressThread.interrupt();
            progressThread = null;
        }
        
        System.out.println("[PLAYER] ■ Остановлено");
        notifyPlaybackStopped();
    }
    
    public void setVolume(int volume) {
        final int newVolume = Math.max(0, Math.min(100, volume));
        this.currentVolume = newVolume;
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setVolume(newVolume / 100.0);
                    System.out.println("[PLAYER] Громкость: " + newVolume + "%");
                } catch (Exception e) {}
            }
        });
        notifyVolumeChanged();
    }
    
    public void setProgress(int seconds) {
        if (currentSong == null) return;
        final int newProgress = Math.max(0, Math.min(seconds, currentSong.getDuration()));
        this.currentProgress = newProgress;
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.seek(Duration.seconds(newProgress));
                } catch (Exception e) {}
            }
        });
        notifyProgressChanged();
    }
    
    public void shutdown() {
        stop();
        Platform.runLater(() -> disposeMediaPlayer());
    }
    
    public Song getCurrentSong() { return currentSong; }
    public int getCurrentVolume() { return currentVolume; }
    public int getCurrentProgress() { return currentProgress; }
    public boolean isPlaying() { return isPlaying; }
    public boolean isPaused() { return isPaused; }
}