package mp3player;

public class MP3Player {
    public static void main(String[] args) {
        // Запускаем графический интерфейс
        java.awt.EventQueue.invokeLater(() -> {
            MP3PlayerUI ui = new MP3PlayerUI();
            ui.setVisible(true);
        });
    }
}