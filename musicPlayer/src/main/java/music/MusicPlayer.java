package main.java.music;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.JavaSoundAudioDevice;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class MusicPlayer implements ActionListener {

    private JFrame frame;
    private JButton input;
    private JLabel song;
    private JButton play;
    private JButton stop;
    private JButton next;
    private JButton previous;
    private JButton delete;
    private JButton deleteFile;
    private JButton lrcButton; //歌词开关按钮
    private JComboBox<String> modeBox; //模式选项
    private JSlider slider;    //进度条
    private JLabel leftLabel;
    private JLabel rightLabel;
    private JScrollPane scrollPaneList;
    private DefaultListModel<String> list;
    private JList<String> jList;
    private String currentMusicName;
    private volatile Player player; //播放器
    private volatile int index;//当前播放的音乐索引
    private Map<String, String> songPathMap; //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap; //歌曲名称和路径的键值对
    private java.util.List<String> saveList; //路径保存的列表
    private Map<Integer, String> lrcMap;
    private boolean needTurn;
    private JFrame lrcFrame;
    private Point lrcXY;
    private JLabel lrcLabel;
    private volatile boolean playState; //为true则播放，false为结束
    private volatile boolean pause; //暂停歌曲
    private ExecutorService playThread;//播放线程

    public static void main(String[] args) {
        MusicPlayer player = new MusicPlayer();
        player.start();
        for (int i = 0; i < 100; i++) {
            player.randomPlay();
        }
    }

    private void start() {
        init();//初始化
        listener();//监听
    }

    private void init() {
        loadSong(); //加载歌曲列表
        playInit(); //播放线程初始化
        mainFrame();//主界面加载
    }

    /**加载歌曲*/
    private void loadSong() {
        list = new DefaultListModel<>();
        jList = new JList<>(list);
        songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
        lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
        if (getSize() == 0) {  //加载文件
            saveList = Utils.load();
            if (saveList != null) {
                for (String savePath : saveList) {
                    Utils.findAll(list, savePath, songPathMap, lrcPathMap);
                }
            }
        }
    }

    /**播放初始化*/
    private void playInit() {
        index = -1; //当前播放索引初始化为-1
        playState = true;//歌曲播放状态
        needTurn = true;//列表显示标记
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(); //监听歌曲线程
        ScheduledExecutorService serviceLRC = Executors.newSingleThreadScheduledExecutor();//歌词和时间线程
        playThread = Executors.newSingleThreadExecutor();//歌曲播放线程
        service.scheduleAtFixedRate(() -> {
            if (player != null && isComplete()) {
                choose2Play();
            }
        }, 1, 2, TimeUnit.SECONDS);
        serviceLRC.scheduleAtFixedRate(() -> {
            if (player == null || lrcMap == null || pause) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }
            if (!isComplete()) {
                int second = getPosition() / 1000; //获得当前的时间
                String index = lrcMap.get(second);
                if (index != null) {
                    lrcLabel.setText(index);
                }
                int position = getPosition() / 1000;
                leftLabel.setText(Utils.secToTime(position));
                slider.setValue(position);
            }
        }, 1000, 50, TimeUnit.MILLISECONDS); //每50秒执行一次
    }

    /**
     * 主面板
     */
    private void mainFrame() {
        frame = new JFrame("GFMusic");
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.png");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        frame.setIconImage(image.getImage());
        frame.setBounds(700, 300, 340, 360);
        frame.add(listPanel(), BorderLayout.NORTH); //歌曲列表面板
        frame.add(sliderPanel(), BorderLayout.CENTER);//滑动条面板
        frame.add(controlPanel(), BorderLayout.SOUTH);//按钮面板
        frame.setResizable(false);
        lrcFrame = createLrcFrame(); //歌词面板
        frame.setVisible(true);
    }

    /**歌词面板*/
    private JFrame createLrcFrame() {
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.jpg");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        JFrame lrcFrame = new JFrame("歌词");
        lrcFrame.setIconImage(image.getImage());
        lrcFrame.setBounds(400, 900, 1000, 60);
        lrcLabel = new JLabel("歌词", JLabel.CENTER);
        lrcLabel.setForeground(new Color(51, 51, 51));
        lrcLabel.setFont(new Font("微软雅黑", Font.PLAIN, 38));
        lrcFrame.add(lrcLabel);
        lrcFrame.setUndecorated(true);
        lrcFrame.setBackground(new Color(0, 0, 0, 0));
        lrcXY = new Point();
        lrcFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                lrcFrame.setVisible(false);
                lrcButton.setText("歌词:开");
            }
        });
        lrcFrame.setVisible(false);
        lrcFrame.setAlwaysOnTop(!lrcFrame.isAlwaysOnTop());
        lrcFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Color color = JColorChooser.showDialog(frame, "选择颜色", new Color(31, 217, 224));
                    lrcLabel.setForeground(color);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                lrcXY.x = e.getX();
                lrcXY.y = e.getY();
            }
        });
        lrcFrame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = lrcFrame.getLocation();
                // 设置窗口的位置
                // 窗口当前的位置 + 鼠标当前在窗口的位置 - 鼠标按下的时候在窗口的位置
                lrcFrame.setLocation(p.x + e.getX() - lrcXY.x, p.y + e.getY() - lrcXY.y);
            }
        });
        return lrcFrame;
    }

    /**
     * 歌曲列表面板
     */
    private JPanel listPanel() {
        scrollPaneList = new JScrollPane(jList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel listPanel = new JPanel();
        listPanel.add(scrollPaneList, BorderLayout.SOUTH);
        jList.setFixedCellWidth(350);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        jList.setCellRenderer(renderer);
        jList.setSelectionBackground(new Color(255, 128, 128));
        jList.setFont(new Font("微软雅黑", Font.BOLD, 14));
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setValueIsAdjusting(true);
        return listPanel;
    }

    /**
     * 滑动条面板
     */
    private JPanel sliderPanel() {
        JPanel lrcPanel = new JPanel(); //歌词面板
        JPanel namePanel = new JPanel();//歌曲名字面板
        song = new JLabel("歌曲");
        namePanel.add(song);
        song.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        song.setForeground(new Color(255, 128, 128));
        lrcPanel.setLayout(new BorderLayout());
        slider = new JSlider();
        leftLabel = new JLabel(Utils.secToTime(0));
        rightLabel = new JLabel(Utils.secToTime(0));
        slider.setUI(new MySliderUI(slider));
        slider.setValue(0);
        JPanel sliderPanel = new JPanel();//进度条面板
        sliderPanel.add(leftLabel, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(rightLabel, BorderLayout.EAST);
        lrcPanel.add(namePanel, BorderLayout.NORTH);
        lrcPanel.add(sliderPanel, BorderLayout.CENTER);
        return lrcPanel;
    }

    /**按钮样式*/
    private void setButton(JButton button) {
        String pic = "default.png";
        String press = "press.png";
        String roll = "put.png";
        setPic(button, pic, press, roll);
        button.setPreferredSize(new Dimension(85, 25));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setHorizontalTextPosition(SwingConstants.CENTER);
    }

    private void setPic(JButton button, String pic, String pressPic, String rollPic) {
        URL resource = MusicPlayer.class.getClassLoader().getResource(pic);
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        URL pressPicResource = MusicPlayer.class.getClassLoader().getResource(pressPic);
        assert pressPicResource != null;
        ImageIcon pressPicImage = new ImageIcon(pressPicResource);
        URL rollPicResource = MusicPlayer.class.getClassLoader().getResource(rollPic);
        assert rollPicResource != null;
        ImageIcon rollPicImage = new ImageIcon(rollPicResource);
        button.setIcon(image);
        button.setPressedIcon(pressPicImage);
        button.setRolloverIcon(rollPicImage);
    }

    /**按钮面板*/
    private JPanel controlPanel() {
        buttonBoxInit();
        JPanel controlPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 3));
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(next);
        buttonPanel.add(stop);
        buttonPanel.add(input);
        buttonPanel.add(modeBox);
        buttonPanel.add(delete);
        buttonPanel.add(deleteFile);
        buttonPanel.add(lrcButton);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        return controlPanel;
    }

    /**按钮面板*/
    private void buttonBoxInit() {
        input = new JButton("导入");
        play = new JButton("播放");
        stop = new JButton("停止");
        next = new JButton("下一首");
        previous = new JButton("上一首");
        delete = new JButton("删除");
        deleteFile = new JButton("删除文件");
        lrcButton = new JButton("歌词:开");
        setButton(play);
        setButton(previous);
        setButton(next);
        setButton(stop);
        setButton(input);
        setButton(delete);
        setButton(deleteFile);
        setButton(lrcButton);
        String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
        modeBox = new JComboBox<>(modeName);
        modeBox.setBackground(new Color(255, 255, 255));
        modeBox.setPreferredSize(new Dimension(85, 25));
        modeBox.setOpaque(false);
    }


    private int getPosition() {
        return player.getPosition();
    }

    private boolean isComplete() {
        return player.isComplete();
    }

    private void listener() {
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedIndex = jList.getSelectedIndex();
                    if (index == selectedIndex) {
                        return;
                    }
                    needTurn = false;
                    playFile(jList.getSelectedValue());
                    needTurn = true;
                    index = selectedIndex;
                    jList.setSelectedIndex(index);
                }

            }
        });
        next.addActionListener(this);
        input.addActionListener(this);
        previous.addActionListener(this);
        delete.addActionListener(this);
        deleteFile.addActionListener(this);
        play.addActionListener(this);
        stop.addActionListener(this);
        lrcButton.addActionListener(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void saveSong() {
        //保存数据
        if (jList.getVisibleRowCount() > 0) {
            Utils.save(saveList);
        }
    }

    private int getSize() {
        return jList.getModel().getSize();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
            case "停止":
                stop();
                return;
            case "播放":
                if (player == null) {
                    index = jList.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    playFile(getName());

                } else {
                    if (!playState) {
                        index = jList.getSelectedIndex();
                        if (index == -1) {
                            return;
                        }
                        playFile(getName());
                    } else {
                        this.pause = false;
                    }
                }
                play.setText("暂停");
                break;
            case "暂停":
                pause();
                break;
            case "下一首":
                if (getSize() == 0) {
                    return;
                }
                choose2Play();
                break;
            case "上一首":
                if (getSize() == 0) {
                    return;
                }
                previous();
                break;
            case "导入":
                String LOCATION = Utils.open();
                if (LOCATION == null || saveList == null) {
                    return;
                }
                if (!saveList.contains(LOCATION)) {
                    saveList.add(LOCATION);
                }
                Utils.findAll(list, LOCATION, songPathMap, lrcPathMap);
                saveSong();
                break;
            case "删除":
                deleteSong();
                break;
            case "删除文件":
                deleteFile();
                break;
            case "歌词:开":
                lrcFrame.setVisible(true);
                lrcButton.setText("歌词:关");
                break;
            case "歌词:关":
                lrcFrame.setVisible(false);
                lrcButton.setText("歌词:开");
                break;
        }
    }

    /**从列表中移除歌曲*/
    private void deleteSong() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) jList.getModel();
        int index = jList.getSelectedIndex();
        if (index == -1) {
            return;
        }
        String name = listModel.get(jList.getSelectedIndex());
        if (name == null) {
            return;
        }
        listModel.remove(index);
        if (index == listModel.size()) {
            jList.setSelectedIndex(index - 1);
        } else {
            jList.setSelectedIndex(index);
        }
        songPathMap.remove(name);//歌曲
        lrcPathMap.remove(name);//歌词
    }

    /**删除歌曲和歌词文件*/
    private void deleteFile() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) jList.getModel();
        int index = jList.getSelectedIndex();
        if (index == -1) {
            return;
        }
        String name = listModel.get(index);
        if (name == null || name.equals(getName())) {
            return;
        }
        listModel.remove(jList.getSelectedIndex());
        String delete = songPathMap.remove(name);//歌曲
        if (delete != null) {
            Utils.deleteSong(delete);
        }
        delete = lrcPathMap.remove(name);//歌词
        if (delete != null) {
            Utils.deleteSong(delete);
        }
    }

    /**随机播放*/
    private void randomPlay() {
        if (getSize() == 0) {
            return;
        }
        int index = (int) (Math.random() * getSize());
        if (this.index == index) {
            return;
        }
        this.index = index;
        playFile(getName());
        song.setText(getName());
    }

    /**停止*/
    private void stop() {
        if (playThread == null || player == null) {
            return;
        }
        player.close();
        this.playState = false;
        play.setText("播放");
        leftLabel.setText(Utils.secToTime(0));
        rightLabel.setText(Utils.secToTime(0));
        song.setText("歌曲");
    }

    /**暂停*/
    private void pause() {
        if (playThread == null) {
            return;
        }
        this.pause = true;
        play.setText("播放");
    }

    /**播放音乐*/
    private void playFile(String musicName) {

        if (lrcMap != null) {
            lrcMap.clear();
        }
        lrcLabel.setText(musicName);
        JScrollBar jscrollBar = scrollPaneList.getVerticalScrollBar();
        if (jscrollBar != null && needTurn) {
            jscrollBar.setValue((index - 3) * 21);
        }
        jList.setSelectedIndex(index);
        String path = lrcPathMap.get(musicName);
        if (path != null) {
            lrcMap = Utils.readLRC(path);
        }
        currentMusicName = musicName;
        try {
            stop();
            AudioDevice device = new JavaSoundAudioDevice();
            player = new Player(new FileInputStream(songPathMap.get(musicName)), device);
            int totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            slider.setMinimum(0);
            slider.setMaximum(totalTime);
            rightLabel.setText(Utils.secToTime(totalTime));
            play();
            play.setText("暂停");
            song.setText(musicName);
        } catch (JavaLayerException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**音乐播放线程*/
    private void play() {
        playState = true;
        pause = false;
        playThread.execute(() -> {
            try {
                if (player != null) {
                    while (playState) {
                        if (pause) {
                            Thread.sleep(100); //解决CUP占用过高
                            continue;
                        }
                        player.play();
                    }
                }
            } catch (JavaLayerException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void choose2Play() {
        switch (modeBox.getModel().getSelectedItem().toString()) {
            case "顺序播放":
                next();
                break;
            case "单曲循环":
                playFile(currentMusicName);
                break;
            case "随机播放":
                randomPlay();
                break;
        }
    }

    private String getName() {
        return jList.getModel().getElementAt(index);
    }

    /**下一曲*/
    private void next() {
        if (index < getSize() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        playFile(getName());
    }

    /**上一曲*/
    private void previous() {
        if (index > 0) {
            index -= 1;
        } else {
            index = getSize() - 1;
        }
        playFile(getName());
    }
}
