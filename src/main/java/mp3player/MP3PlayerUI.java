package mp3player;

import mp3player.client.ServerClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Графический интерфейс MP3-плеера.
 */
public class MP3PlayerUI extends JFrame {
    
    private MusicPlayer musicPlayer;
    private List<Song> playlist;  // Локальный плейлист (песни)
    private DefaultListModel<String> playlistModel;
    
    private JList<String> playlistList;
    private JLabel nowPlayingLabel;
    private JLabel progressLabel;
    private JSlider volumeSlider;
    private JProgressBar progressBar;
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton addSongButton;
    private JButton removeSongButton;
    private JTextField searchField;
    private JButton searchButton;
    private JCheckBox shuffleCheckBox;
    private JCheckBox repeatCheckBox;
    
    private DisplayScreen displayScreen;
    private PlaybackHistoryLogger historyLogger;
    private SystemTrayNotifier trayNotifier;
    
    // === Серверная часть ===
    private ServerClient serverClient;
    private JList<String> remotePlaylistList;
    private DefaultListModel<String> remotePlaylistModel;
    private List<ServerClient.RemoteSongInfo> remotePlaylist;
    private JLabel serverStatusLabel;
    private JButton connectButton;
    private JTextField serverSearchField;
    private JButton serverSearchButton;
    private JButton downloadButton;
    private JButton uploadButton;
    private boolean serverConnected = false;
    
    // === Плейлисты на сервере ===
    private JList<ServerClient.RemotePlaylistInfo> playlistsList;
    private DefaultListModel<ServerClient.RemotePlaylistInfo> playlistsModel;
    private List<ServerClient.RemotePlaylistInfo> userPlaylists;
    private JButton createPlaylistButton;
    private JButton editPlaylistButton;
    private JButton deletePlaylistButton;
    private JButton downloadPlaylistButton;
    private JButton addSongToPlaylistButton;
    
    private int currentSongIndex = -1;
    private Timer progressTimer;
    
    public MP3PlayerUI() {
        initComponents();
        initMusicPlayer();
        initPlaylist();
        setupEventHandlers();
        setupProgressTimer();
        initServerPanel();
        initPlaylistsPanel();
    }
    
    private void initComponents() {
        setTitle("MP3 Плеер - Курсовая работа");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Верхняя панель
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new TitledBorder("Сейчас играет"));

        nowPlayingLabel = new JLabel("Нет трека", SwingConstants.CENTER);
        nowPlayingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nowPlayingLabel.setForeground(new Color(0, 100, 0));
        topPanel.add(nowPlayingLabel, BorderLayout.CENTER);

        progressLabel = new JLabel("0:00 / 0:00", SwingConstants.CENTER);
        topPanel.add(progressLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Центральная панель (плейлист)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new TitledBorder("Локальный плейлист"));

        playlistModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistModel);
        playlistList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(playlistList);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel playlistControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addSongButton = new JButton("➕ Добавить трек");
        removeSongButton = new JButton("❌ Удалить");
        playlistControlPanel.add(addSongButton);
        playlistControlPanel.add(removeSongButton);
        centerPanel.add(playlistControlPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Нижняя панель (3 строки)
        JPanel bottomPanel = new JPanel(new GridLayout(3, 1, 5, 10));
        bottomPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // === Строка 1: Поиск и громкость ===
        JPanel topControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));

        // Поиск
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("🔍 Поиск:"));
        searchField = new JTextField(15);  // Уменьшил с 20 до 15
        searchField.setPreferredSize(new Dimension(150, 25));
        searchButton = new JButton("Найти");
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Громкость (горизонтальная, но компактная)
        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        volumePanel.add(new JLabel("🔊"));
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 70);
        volumeSlider.setPreferredSize(new Dimension(120, 25));  // Компактный размер
        volumePanel.add(volumeSlider);

        topControlsPanel.add(searchPanel);
        topControlsPanel.add(volumePanel);

        bottomPanel.add(topControlsPanel);

        // === Строка 2: Прогресс бар (на всю ширину) ===
        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setToolTipText("Кликните для перемотки");
        progressBar.setPreferredSize(new Dimension(0, 25));  // Фиксированная высота
        progressPanel.add(progressBar, BorderLayout.CENTER);

        progressLabel = new JLabel("0:00 / 0:00", SwingConstants.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        progressPanel.add(progressLabel, BorderLayout.EAST);

        bottomPanel.add(progressPanel);

        // === Строка 3: Кнопки управления (2 ряда) ===
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Ряд 1: Основные кнопки
        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        prevButton = new JButton("⏮ Prev");
        playButton = new JButton("▶ Play");
        pauseButton = new JButton("⏸ Pause");
        stopButton = new JButton("■ Stop");
        nextButton = new JButton("⏭ Next");

        mainButtonsPanel.add(prevButton);
        mainButtonsPanel.add(playButton);
        mainButtonsPanel.add(pauseButton);
        mainButtonsPanel.add(stopButton);
        mainButtonsPanel.add(nextButton);

        // Ряд 2: Чекбоксы
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 2));
        shuffleCheckBox = new JCheckBox("🔀 Перемешать");
        repeatCheckBox = new JCheckBox("🔁 Повтор");

        optionsPanel.add(shuffleCheckBox);
        optionsPanel.add(repeatCheckBox);

        buttonsPanel.add(mainButtonsPanel);
        buttonsPanel.add(optionsPanel);

        bottomPanel.add(buttonsPanel);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.add(new JLabel("Готов к работе"));
        add(statusBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }
    
    private void initMusicPlayer() {
        musicPlayer = new MusicPlayer();

        displayScreen = new DisplayScreen();
        historyLogger = new PlaybackHistoryLogger();
        trayNotifier = new SystemTrayNotifier();

        musicPlayer.addObserver(displayScreen);
        musicPlayer.addObserver(historyLogger);
        musicPlayer.addObserver(trayNotifier);

        // Инициализация клиента для сервера
        serverClient = new ServerClient();
        remotePlaylist = new ArrayList<>();
        userPlaylists = new ArrayList<>();

        // GUI-наблюдатель для обновления интерфейса
        musicPlayer.addObserver(new MusicPlaybackObserver() {
            @Override
            public void onPlaybackStarted(String songName, String artist, int duration) {
                SwingUtilities.invokeLater(() -> {
                    if (progressBar != null && nowPlayingLabel != null) {
                        // Используем songName как есть (он уже содержит красивое название)
                        nowPlayingLabel.setText(songName);
                        progressBar.setMaximum(duration);
                        progressBar.setValue(0);
                    }
                });
            }

            @Override
            public void onPlaybackStopped() {
                SwingUtilities.invokeLater(() -> {
                    if (nowPlayingLabel != null) {
                        nowPlayingLabel.setText("Воспроизведение остановлено");
                    }
                });
            }

            @Override
            public void onPlaybackFinished() {
                SwingUtilities.invokeLater(() -> {
                    if (nowPlayingLabel != null) {
                        nowPlayingLabel.setText("Трек завершён");
                    }
                    playNext();
                });
            }

            @Override
            public void onVolumeChanged(int volume) {
                SwingUtilities.invokeLater(() -> {
                    if (volumeSlider != null) {
                        volumeSlider.setValue(volume);
                    }
                });
            }

            @Override
            public void onProgressChanged(int progress, int duration) {
                SwingUtilities.invokeLater(() -> {
                    if (progressBar != null && progressLabel != null && duration > 0) {
                        progressBar.setValue(progress);
                        progressLabel.setText(formatTime(progress) + " / " + formatTime(duration));
                    }
                });
            }
        });
    }
    
    private void initPlaylist() {
        playlist = new ArrayList<>();
    }
    
    private void addSongToPlaylist(Song song) {
        playlist.add(song);
        playlistModel.addElement(song.toString());
    }
    
    private void setupEventHandlers() {
        playButton.addActionListener(e -> {
            if (currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
                musicPlayer.play(playlist.get(currentSongIndex));
            } else if (!playlist.isEmpty()) {
                currentSongIndex = 0;
                musicPlayer.play(playlist.get(0));
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Плейлист пуст. Добавьте треки с помощью кнопки 'Добавить трек'",
                    "Нет треков", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        pauseButton.addActionListener(e -> {
            if (musicPlayer.isPlaying() && !musicPlayer.isPaused()) {
                musicPlayer.pause();
            } else if (musicPlayer.isPlaying() && musicPlayer.isPaused()) {
                musicPlayer.resume();
            }
        });
        
        stopButton.addActionListener(e -> musicPlayer.stop());
        prevButton.addActionListener(e -> playPrev());
        nextButton.addActionListener(e -> playNext());
        
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                musicPlayer.setVolume(volumeSlider.getValue());
            }
        });
        
        playlistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = playlistList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    currentSongIndex = selectedIndex;
                }
            }
        });
        
        addSongButton.addActionListener(e -> addNewSongDialog());
        removeSongButton.addActionListener(e -> removeSelectedSong());
        searchButton.addActionListener(e -> searchInPlaylist());
        searchField.addActionListener(e -> searchInPlaylist());
        
        setupProgressBarHandlers();
    }
    
    private void initServerPanel() {
        JPanel serverPanel = new JPanel(new BorderLayout(10, 10));
        serverPanel.setBorder(BorderFactory.createTitledBorder("🌐 Стриминговый сервер"));
        serverPanel.setPreferredSize(new Dimension(320, 0));
        
        // Статус подключения
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        serverStatusLabel = new JLabel("🔴 Не подключен");
        serverStatusLabel.setForeground(Color.RED);
        
        connectButton = new JButton("🔄 Подключиться");
        connectButton.addActionListener(e -> toggleServerConnection());
        
        statusPanel.add(connectButton);
        statusPanel.add(serverStatusLabel);
        serverPanel.add(statusPanel, BorderLayout.NORTH);
        
        // Поиск на сервере
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        serverSearchField = new JTextField(10);
        serverSearchButton = new JButton("🔍");
        serverSearchButton.setToolTipText("Поиск на сервере");
        serverSearchButton.addActionListener(e -> searchOnServer());
        serverSearchField.addActionListener(e -> searchOnServer());
        
        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(serverSearchField);
        searchPanel.add(serverSearchButton);
        serverPanel.add(searchPanel, BorderLayout.CENTER);
        
        // Список треков на сервере
        remotePlaylistModel = new DefaultListModel<>();
        remotePlaylistList = new JList<>(remotePlaylistModel);
        remotePlaylistList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        remotePlaylistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        remotePlaylistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    playSelectedRemoteTrack();
                }
            }
        });
        JScrollPane remoteScroll = new JScrollPane(remotePlaylistList);
        remoteScroll.setBorder(BorderFactory.createTitledBorder("Треки на сервере"));
        serverPanel.add(remoteScroll, BorderLayout.CENTER);
        
        // Кнопки действий (2 ряда)
        JPanel actionsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        downloadButton = new JButton("⬇ Скачать");
        uploadButton = new JButton("⬆ Загрузить");
        
        downloadButton.addActionListener(e -> downloadSelectedTrack());
        uploadButton.addActionListener(e -> uploadLocalTrack());
        
        topButtons.add(downloadButton);
        topButtons.add(uploadButton);
        
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton removeRemoteButton = new JButton("🗑 Удалить");
        removeRemoteButton.setForeground(Color.RED);
        removeRemoteButton.addActionListener(e -> deleteSelectedRemoteTrack());
        
        bottomButtons.add(removeRemoteButton);
        
        actionsPanel.add(topButtons);
        actionsPanel.add(bottomButtons);
        serverPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        add(serverPanel, BorderLayout.EAST);
    }
    
    private void initPlaylistsPanel() {
        JPanel playlistsPanel = new JPanel(new BorderLayout(10, 10));
        playlistsPanel.setBorder(BorderFactory.createTitledBorder("📁 Мои плейлисты"));
        playlistsPanel.setPreferredSize(new Dimension(250, 0));
        
        // Кнопки управления
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        
        createPlaylistButton = new JButton("➕ Создать");
        editPlaylistButton = new JButton("✏️ Редактировать");
        deletePlaylistButton = new JButton("🗑 Удалить");
        downloadPlaylistButton = new JButton("⬇ Скачать");
        addSongToPlaylistButton = new JButton("➕ Добавить трек");
        
        createPlaylistButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        editPlaylistButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deletePlaylistButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        downloadPlaylistButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        addSongToPlaylistButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        createPlaylistButton.addActionListener(e -> createNewPlaylist());
        editPlaylistButton.addActionListener(e -> editSelectedPlaylist());
        deletePlaylistButton.addActionListener(e -> deleteSelectedPlaylist());
        downloadPlaylistButton.addActionListener(e -> downloadSelectedPlaylist());
        addSongToPlaylistButton.addActionListener(e -> addSongToSelectedPlaylist());
        
        buttonsPanel.add(createPlaylistButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonsPanel.add(editPlaylistButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonsPanel.add(deletePlaylistButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonsPanel.add(downloadPlaylistButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonsPanel.add(addSongToPlaylistButton);
        
        playlistsPanel.add(buttonsPanel, BorderLayout.NORTH);
        
        // Список плейлистов
        playlistsModel = new DefaultListModel<>();
        playlistsList = new JList<>(playlistsModel);
        playlistsList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        playlistsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedPlaylist();
                }
            }
        });
        JScrollPane playlistsScroll = new JScrollPane(playlistsList);
        playlistsPanel.add(playlistsScroll, BorderLayout.CENTER);
        
        add(playlistsPanel, BorderLayout.WEST);
    }
    
    // === Методы работы с сервером ===
    
    private void toggleServerConnection() {
        if (serverConnected) {
            serverConnected = false;
            serverStatusLabel.setText("🔴 Не подключен");
            serverStatusLabel.setForeground(Color.RED);
            connectButton.setText("🔄 Подключиться");
            remotePlaylistModel.clear();
            remotePlaylist.clear();
            playlistsModel.clear();
            userPlaylists.clear();
            return;
        }
        
        connectButton.setEnabled(false);
        connectButton.setText("⏳ Подключение...");
        
        serverClient.isServerAvailable()
            .thenAccept(available -> SwingUtilities.invokeLater(() -> {
                if (available) {
                    serverConnected = true;
                    serverStatusLabel.setText("🟢 Подключен");
                    serverStatusLabel.setForeground(new Color(0, 150, 0));
                    connectButton.setText("✖ Отключиться");
                    loadRemoteTracks();
                    loadUserPlaylists();
                } else {
                    serverStatusLabel.setText("❌ Ошибка");
                    serverStatusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(this, 
                        "Сервер не отвечает. Запустите MusicServer.java",
                        "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
                }
                connectButton.setEnabled(true);
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("❌ " + ex.getMessage());
                    connectButton.setEnabled(true);
                    connectButton.setText("🔄 Подключиться");
                });
                return null;
            });
    }
    
    private void loadRemoteTracks() {
        if (!serverConnected) return;
        
        serverClient.fetchSongs()
            .thenAccept(songs -> SwingUtilities.invokeLater(() -> {
                remotePlaylistModel.clear();
                remotePlaylist.clear();
                
                for (ServerClient.RemoteSongInfo song : songs) {
                    remotePlaylist.add(song);
                    remotePlaylistModel.addElement("🌐 " + song.toString());
                }
                
                if (songs.isEmpty()) {
                    remotePlaylistModel.addElement("   (сервер пуст)");
                }
            }));
    }
    
    private void loadUserPlaylists() {
        if (!serverConnected) return;

        serverClient.fetchPlaylists()
            .thenAccept(playlists -> SwingUtilities.invokeLater(() -> {
                playlistsModel.clear();
                userPlaylists.clear();

                for (ServerClient.RemotePlaylistInfo playlist : playlists) {
                    userPlaylists.add(playlist);
                    playlistsModel.addElement(playlist);
                }

                // Если плейлистов нет, просто оставляем список пустым
                // ИЛИ добавляем заглушку:
                if (playlists.isEmpty()) {
                    // playlistsModel.addElement(new ServerClient.RemotePlaylistInfo("(нет плейлистов)", ""));
                }
            }))
            .exceptionally(ex -> {
                System.err.println("[UI] Ошибка загрузки плейлистов: " + ex.getMessage());
                return null;
            });
    }
    
    private void searchOnServer() {
        if (!serverConnected) {
            JOptionPane.showMessageDialog(this, "Сначала подключитесь к серверу");
            return;
        }
        
        String query = serverSearchField.getText().trim();
        if (query.isEmpty()) {
            loadRemoteTracks();
            return;
        }
        
        serverClient.searchSongs(query)
            .thenAccept(results -> SwingUtilities.invokeLater(() -> {
                remotePlaylistModel.clear();
                remotePlaylist.clear();
                
                for (ServerClient.RemoteSongInfo song : results) {
                    remotePlaylist.add(song);
                    remotePlaylistModel.addElement("🔍 " + song.toString());
                }
                
                if (results.isEmpty()) {
                    remotePlaylistModel.addElement("   Ничего не найдено");
                }
            }));
    }
    
    private void downloadSelectedTrack() {
        int index = remotePlaylistList.getSelectedIndex();
        if (index < 0 || index >= remotePlaylist.size()) {
            JOptionPane.showMessageDialog(this, "Выберите трек для скачивания");
            return;
        }
        
        ServerClient.RemoteSongInfo remote = remotePlaylist.get(index);
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить трек");
        chooser.setSelectedFile(new java.io.File(remote.getTitle() + ".mp3"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path destination = chooser.getSelectedFile().toPath();
            
            serverClient.downloadSong(remote.getId(), destination.getParent())
                .thenAccept(downloaded -> SwingUtilities.invokeLater(() -> {
                    if (downloaded != null) {
                        Song local = new Song(
                            remote.getTitle(), 
                            remote.getArtist(), 
                            remote.getDuration(), 
                            downloaded.toString()
                        );
                        addSongToPlaylist(local);
                        
                        JOptionPane.showMessageDialog(this, 
                            "✅ Трек скачан: " + downloaded.getFileName(),
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка скачивания", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка: " + ex.getMessage(), 
                            "Ошибка", JOptionPane.ERROR_MESSAGE)
                    );
                    return null;
                });
        }
    }
    
    private void uploadLocalTrack() {
        if (!serverConnected) {
            JOptionPane.showMessageDialog(this, "Сначала подключитесь к серверу");
            return;
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 файлы", "mp3"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            
            ServerClient.RemoteSongInfo song = new ServerClient.RemoteSongInfo();
            song.setTitle(file.getFileName().toString().replace(".mp3", ""));
            song.setArtist("Неизвестный исполнитель");
            
            int realDuration = 180;
            try {
                org.jaudiotagger.audio.AudioFile audioFile = 
                    org.jaudiotagger.audio.AudioFileIO.read(file.toFile());
                if (audioFile.getAudioHeader() != null) {
                    realDuration = audioFile.getAudioHeader().getTrackLength();
                }
            } catch (Exception e) {
                System.out.println("[UI] Не удалось прочитать длительность: " + e.getMessage());
            }
            song.setDuration(realDuration);
            
            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField titleField = new JTextField(song.getTitle());
            JTextField artistField = new JTextField(song.getArtist());
            panel.add(new JLabel("Название:")); panel.add(titleField);
            panel.add(new JLabel("Исполнитель:")); panel.add(artistField);
            panel.add(new JLabel("Файл:")); panel.add(new JLabel(file.getFileName().toString()));
            
            if (JOptionPane.showConfirmDialog(this, panel, "Загрузка на сервер", 
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                
                song.setTitle(titleField.getText().trim());
                song.setArtist(artistField.getText().trim());
                
                JOptionPane optionPane = new JOptionPane("⏳ Загрузка...", 
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new String[]{}, null);
                JDialog dialog = optionPane.createDialog("Загрузка");
                dialog.setModal(false);
                dialog.setVisible(true);
                
                serverClient.uploadSong(song, file)
                    .thenAccept(songId -> SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        if (songId != null) {
                            JOptionPane.showMessageDialog(this, 
                                "✅ Трек загружен! ID: " + songId.substring(0, 8) + "...",
                                "Успех", JOptionPane.INFORMATION_MESSAGE);
                            loadRemoteTracks();
                        } else {
                            JOptionPane.showMessageDialog(this, 
                                "❌ Ошибка загрузки", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    }))
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(this, 
                                "❌ Ошибка: " + ex.getMessage(), 
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                        });
                        return null;
                    });
            }
        }
    }
    
    private void deleteSelectedRemoteTrack() {
        int index = remotePlaylistList.getSelectedIndex();
        if (index < 0 || index >= remotePlaylist.size()) {
            JOptionPane.showMessageDialog(this, "Выберите трек для удаления");
            return;
        }

        ServerClient.RemoteSongInfo remote = remotePlaylist.get(index);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Удалить трек с сервера?\n" + remote.toString(),
            "Подтверждение удаления", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            serverClient.deleteSong(remote.getId())
                .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, 
                            "✅ Трек удалён с сервера", 
                            "Успех", 
                            JOptionPane.INFORMATION_MESSAGE);
                        loadRemoteTracks();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка удаления трека", 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка: " + ex.getMessage(), 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE)
                    );
                    return null;
                });
        }
    }
    
    // === Методы работы с плейлистами ===
    
    private void createNewPlaylist() {
        if (!serverConnected) {
            JOptionPane.showMessageDialog(this, "Сначала подключитесь к серверу");
            return;
        }
        
        String name = JOptionPane.showInputDialog(this, "Название плейлиста:", "Создание плейлиста", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            String description = JOptionPane.showInputDialog(this, "Описание (необязательно):", "Создание плейлиста", JOptionPane.PLAIN_MESSAGE);
            
            ServerClient.RemotePlaylistInfo newPlaylist = new ServerClient.RemotePlaylistInfo(name, description != null ? description : "");
            
            serverClient.createPlaylist(newPlaylist)
                .thenAccept(created -> SwingUtilities.invokeLater(() -> {
                    if (created != null) {
                        userPlaylists.add(created);
                        playlistsModel.addElement(created);
                        JOptionPane.showMessageDialog(this, 
                            "✅ Плейлист создан: " + created.getName(), 
                            "Успех", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка создания плейлиста", 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(this, 
                        "❌ Ошибка: " + ex.getMessage(), 
                        "Ошибка", 
                        JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        }
    }
    
    private void editSelectedPlaylist() {
        int index = playlistsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Выберите плейлист для редактирования");
            return;
        }
        
        ServerClient.RemotePlaylistInfo playlist = userPlaylists.get(index);
        
        String newName = JOptionPane.showInputDialog(this, "Новое название:", playlist.getName(), JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.trim().isEmpty()) {
            String newDescription = JOptionPane.showInputDialog(this, "Описание:", playlist.getDescription(), JOptionPane.PLAIN_MESSAGE);
            
            ServerClient.RemotePlaylistInfo updated = new ServerClient.RemotePlaylistInfo(newName, newDescription != null ? newDescription : "");
            updated.setId(playlist.getId());
            
            serverClient.updatePlaylist(playlist.getId(), updated)
                .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                    if (success) {
                        playlist.setName(newName);
                        playlist.setDescription(newDescription != null ? newDescription : "");
                        playlistsModel.set(index, playlist);
                        JOptionPane.showMessageDialog(this, 
                            "✅ Плейлист обновлён!", 
                            "Успех", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка обновления плейлиста", 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(this, 
                        "❌ Ошибка: " + ex.getMessage(), 
                        "Ошибка", 
                        JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        }
    }
    
    private void deleteSelectedPlaylist() {
        int index = playlistsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Выберите плейлист для удаления");
            return;
        }
        
        ServerClient.RemotePlaylistInfo playlist = userPlaylists.get(index);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Удалить плейлист \"" + playlist.getName() + "\"?",
            "Подтверждение удаления", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            serverClient.deletePlaylist(playlist.getId())
                .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                    if (success) {
                        userPlaylists.remove(index);
                        playlistsModel.remove(index);
                        JOptionPane.showMessageDialog(this, 
                            "✅ Плейлист удалён", 
                            "Успех", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка удаления плейлиста", 
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(this, 
                        "❌ Ошибка: " + ex.getMessage(), 
                        "Ошибка", 
                        JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        }
    }
    
    private void downloadSelectedPlaylist() {
        int index = playlistsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Выберите плейлист для скачивания");
            return;
        }
        
        ServerClient.RemotePlaylistInfo playlist = userPlaylists.get(index);
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить плейлист");
        chooser.setSelectedFile(new java.io.File(playlist.getName() + ".json"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path destination = chooser.getSelectedFile().toPath();
            
            serverClient.downloadPlaylist(playlist.getId(), destination)
                .thenAccept(downloaded -> SwingUtilities.invokeLater(() -> {
                    if (downloaded != null) {
                        JOptionPane.showMessageDialog(this, 
                            "✅ Плейлист сохранён: " + downloaded.getFileName(),
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "❌ Ошибка скачивания плейлиста", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(this, 
                        "❌ Ошибка: " + ex.getMessage(), 
                        "Ошибка", 
                        JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        }
    }
    
    private void addSongToSelectedPlaylist() {
        int playlistIndex = playlistsList.getSelectedIndex();
        if (playlistIndex < 0) {
            JOptionPane.showMessageDialog(this, "Выберите плейлист");
            return;
        }

        int songIndex = remotePlaylistList.getSelectedIndex();
        if (songIndex < 0) {
            JOptionPane.showMessageDialog(this, "Выберите трек для добавления");
            return;
        }

        ServerClient.RemotePlaylistInfo playlist = userPlaylists.get(playlistIndex);
        ServerClient.RemoteSongInfo song = remotePlaylist.get(songIndex);

        serverClient.addSongToPlaylist(playlist.getId(), song.getId())
            .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(this, 
                        "✅ Трек добавлен в плейлист \"" + playlist.getName() + "\"",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);

                    // === ДОБАВЛЕНО: Обновляем список плейлистов ===
                    loadUserPlaylists();
                    // ===============================================
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "❌ Ошибка добавления трека", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }))
            .exceptionally(ex -> {
                JOptionPane.showMessageDialog(this, 
                    "❌ Ошибка: " + ex.getMessage(), 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
                return null;
            });
    }
    
    private void openSelectedPlaylist() {
        int index = playlistsList.getSelectedIndex();
        if (index < 0) return;

        ServerClient.RemotePlaylistInfo selectedPlaylist = userPlaylists.get(index);

        serverClient.getPlaylistSongs(selectedPlaylist.getId())
            .thenAccept(songs -> SwingUtilities.invokeLater(() -> {
                playlistModel.clear();
                playlist.clear();
                currentSongIndex = -1;

                for (ServerClient.RemoteSongInfo remoteSong : songs) {
                    // ИСПРАВЛЕНИЕ: Используем красивое название из toString()
                    String beautifulName = remoteSong.toString(); // "Artist - Title [MM:SS]"

                    // Создаём Song с красивым названием
                    Song localSong = new Song(
                        beautifulName,  // <-- Красивое название
                        remoteSong.getArtist(),
                        remoteSong.getDuration(),
                        serverClient.getStreamUrl(remoteSong.getId())
                    );

                    playlist.add(localSong);
                    playlistModel.addElement("📁 " + remoteSong.toString());
                }

                if (songs.isEmpty()) {
                    playlistModel.addElement("   (плейлист пуст)");
                }

                JOptionPane.showMessageDialog(this, 
                    "Открыт плейлист: " + selectedPlaylist.getName() + "\nТреков: " + songs.size(),
                    "Плейлист", JOptionPane.INFORMATION_MESSAGE);
            }))
            .exceptionally(ex -> {
                JOptionPane.showMessageDialog(this, 
                    "❌ Ошибка загрузки плейлиста: " + ex.getMessage(), 
                    "Ошибка", 
                    JOptionPane.ERROR_MESSAGE);
                return null;
            });
    }
    
    private void playSelectedRemoteTrack() {
        int index = remotePlaylistList.getSelectedIndex();
        if (index < 0 || index >= remotePlaylist.size()) return;
        
        ServerClient.RemoteSongInfo remote = remotePlaylist.get(index);
        playRemoteTrack(remote);
    }
    
    private void playRemoteTrack(ServerClient.RemoteSongInfo remote) {
        // Создаём локальную модель для плеера с URL стриминга
        Song local = new Song(
            remote.getTitle(), 
            remote.getArtist(), 
            remote.getDuration(), 
            serverClient.getStreamUrl(remote.getId())
        );
        
        // Воспроизводим через MusicPlayer
        musicPlayer.play(local);
        
        // Обновляем UI
        currentSongIndex = -1;
        nowPlayingLabel.setText(remote.toString());
        
        System.out.println("[UI] ▶ Стриминг: " + remote.toString());
        System.out.println("[UI] URL: " + serverClient.getStreamUrl(remote.getId()));
    }
    
    private void setupProgressBarHandlers() {
        if (progressBar == null) return;  // <-- Защита от NPE

        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                seekToMousePosition(evt.getX());
            }
        });

        progressBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent evt) {
                seekToMousePosition(evt.getX());
            }
        });
    }
    
    private void seekToMousePosition(int mouseX) {
        if (musicPlayer.getCurrentSong() == null) return;
        
        int width = progressBar.getWidth();
        if (width <= 0) return;
        
        double percent = (double) mouseX / width;
        int duration = musicPlayer.getCurrentSong().getDuration();
        int newProgress = (int) (percent * duration);
        
        if (newProgress < 0) newProgress = 0;
        if (newProgress > duration) newProgress = duration;
        
        musicPlayer.setProgress(newProgress);
    }
    
    private void playNext() {
        if (playlist.isEmpty()) return;
        
        if (shuffleCheckBox.isSelected()) {
            int newIndex = (int) (Math.random() * playlist.size());
            if (newIndex == currentSongIndex && playlist.size() > 1) {
                newIndex = (newIndex + 1) % playlist.size();
            }
            currentSongIndex = newIndex;
        } else {
            currentSongIndex++;
            if (currentSongIndex >= playlist.size()) {
                if (repeatCheckBox.isSelected()) {
                    currentSongIndex = 0;
                } else {
                    currentSongIndex = playlist.size() - 1;
                    musicPlayer.stop();
                    return;
                }
            }
        }
        
        musicPlayer.play(playlist.get(currentSongIndex));
        playlistList.setSelectedIndex(currentSongIndex);
        playlistList.ensureIndexIsVisible(currentSongIndex);
    }
    
    private void playPrev() {
        if (playlist.isEmpty()) return;
        
        currentSongIndex--;
        if (currentSongIndex < 0) {
            if (repeatCheckBox.isSelected()) {
                currentSongIndex = playlist.size() - 1;
            } else {
                currentSongIndex = 0;
            }
        }
        
        musicPlayer.play(playlist.get(currentSongIndex));
        playlistList.setSelectedIndex(currentSongIndex);
        playlistList.ensureIndexIsVisible(currentSongIndex);
    }
    
    private void addNewSongDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите MP3 файл");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "MP3 файлы (*.mp3)", "mp3"));
        
        int fileResult = fileChooser.showOpenDialog(this);
        
        if (fileResult == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            String fileName = selectedFile.getName();
            
            String autoTitle = fileName.replace(".mp3", "").replace(".MP3", "");
            String autoArtist = "Неизвестный исполнитель";
            int autoDuration = 180;
            
            try {
                org.jaudiotagger.audio.AudioFile audioFile = 
                    org.jaudiotagger.audio.AudioFileIO.read(selectedFile);
                
                if (audioFile.getAudioHeader() != null) {
                    autoDuration = audioFile.getAudioHeader().getTrackLength();
                    System.out.println("[UI] Длительность: " + autoDuration + " сек");
                }
                
                org.jaudiotagger.tag.Tag tag = audioFile.getTag();
                if (tag != null) {
                    String titleTag = tag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE);
                    if (titleTag != null && !titleTag.isEmpty()) {
                        autoTitle = titleTag;
                    }
                    
                    String artistTag = tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST);
                    if (artistTag != null && !artistTag.isEmpty()) {
                        autoArtist = artistTag;
                    }
                    
                    System.out.println("[UI] Теги: " + autoArtist + " - " + autoTitle);
                }
            } catch (Exception e) {
                System.err.println("[UI] Ошибка чтения тегов: " + e.getMessage());
            }
            
            JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
            JTextField titleField = new JTextField(autoTitle, 20);
            JTextField artistField = new JTextField(autoArtist, 20);
            JTextField durationField = new JTextField(String.valueOf(autoDuration), 10);
            
            panel.add(new JLabel("Название:"));
            panel.add(titleField);
            panel.add(new JLabel("Исполнитель:"));
            panel.add(artistField);
            panel.add(new JLabel("Длительность (сек):"));
            panel.add(durationField);
            panel.add(new JLabel("Файл:"));
            panel.add(new JLabel(selectedFile.getName()));
            panel.add(new JLabel("(авто-определено)"));
            panel.add(new JLabel(""));
            
            int result = JOptionPane.showConfirmDialog(this, panel, 
                "Информация о треке", JOptionPane.OK_CANCEL_OPTION);
            
            if (result == JOptionPane.OK_OPTION) {
                try {
                    String title = titleField.getText().trim();
                    String artist = artistField.getText().trim();
                    int duration = Integer.parseInt(durationField.getText().trim());
                    
                    if (title.isEmpty()) title = autoTitle;
                    if (artist.isEmpty()) artist = autoArtist;
                    if (duration <= 0) duration = 180;
                    
                    Song newSong = new Song(title, artist, duration, filePath);
                    addSongToPlaylist(newSong);
                    
                    JOptionPane.showMessageDialog(this, 
                        "Трек добавлен!\n" +
                        "Исполнитель: " + artist + "\n" +
                        "Название: " + title + "\n" +
                        "Длительность: " + formatTime(duration),
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, 
                        "Ошибка: длительность должна быть целым числом", 
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void removeSelectedSong() {
        int selectedIndex = playlistList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < playlist.size()) {
            playlist.remove(selectedIndex);
            playlistModel.remove(selectedIndex);
            
            if (currentSongIndex == selectedIndex) {
                musicPlayer.stop();
                currentSongIndex = -1;
            } else if (currentSongIndex > selectedIndex) {
                currentSongIndex--;
            }
        }
    }
    
    private void searchInPlaylist() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            refreshPlaylistDisplay();
            return;
        }
        
        playlistModel.clear();
        for (Song song : playlist) {
            if (song.getTitle().toLowerCase().contains(query) || 
                song.getArtist().toLowerCase().contains(query)) {
                playlistModel.addElement(song.toString());
            }
        }
        
        if (playlistModel.isEmpty()) {
            playlistModel.addElement("--- НИЧЕГО НЕ НАЙДЕНО ---");
        }
    }
    
    private void refreshPlaylistDisplay() {
        playlistModel.clear();
        for (Song song : playlist) {
            playlistModel.addElement(song.toString());
        }
    }
    
    private void setupProgressTimer() {
        progressTimer = new Timer(100, e -> {
            if (musicPlayer.isPlaying() && !musicPlayer.isPaused()) {
                Song current = musicPlayer.getCurrentSong();
                if (current != null && progressBar != null && progressLabel != null) {
                    int progress = musicPlayer.getCurrentProgress();
                    int duration = current.getDuration();
                    if (duration > 0) {
                        progressBar.setValue(progress);
                        progressLabel.setText(formatTime(progress) + " / " + formatTime(duration));
                    }
                }
            }
        });
        progressTimer.start();
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private void shutdown() {
        if (progressTimer != null) progressTimer.stop();
        if (musicPlayer != null) musicPlayer.shutdown();
        dispose();
        System.exit(0);
    }
}