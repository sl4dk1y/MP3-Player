package mp3player;

import mp3player.MP3PlayerUI;
import javax.swing.SwingUtilities;

/**
 * Главный класс приложения.
 * Точка входа в MP3-плеер.
 * 
 * @author Студент группы 23ВВИ1 Галкин Александр Павлович
 */
public class Main {
    
    public static void main(String[] args) {
        // Запускаем GUI в потоке обработки событий Swing
        // Это гарантирует потокобезопасность интерфейса
        SwingUtilities.invokeLater(() -> {
            MP3PlayerUI ui = new MP3PlayerUI();
            ui.setVisible(true);
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("🛑 Завершение работы...");
        }));    
    }
}