package main.java.music;

import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;

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
    private JButton lrcButton;
    private JComboBox<String> modeBox;
    //进度条
    private JSlider slider;
    private JLabel leftLabel;
    private JLabel rightLabel;
    private JScrollPane scrollPaneList;
    private DefaultListModel<String> list;
    private JList<String> jList;
    private String currentMusicName;
    private Player player; //播放器
    private Thread thread; //播放线程
    private int index = -1;//当前播放的音乐索引
    private Map<String, String> songPathMap; //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap; //歌曲名称和路径的键值对
    private java.util.List<String> saveList; //路径保存的列表
    private Map<Integer, String> lrcMap;
    private boolean needTurn = true;
    private JFrame lrcFrame;
    private Point lrcXY = new Point();
    private JLabel lrcLabel;
    private volatile boolean changeSong = true; //是否换歌
    private volatile boolean pause; //暂停歌曲

    private MusicPlayer() {
        init();//初始化
        listener();//监听
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    private void init() {
        loadSong(); //加载歌曲列表
        autoPlay(); //歌曲监听
        mainFrame();//主界面加载
    }

    private JFrame createLrcFrame() {
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.jpg");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        JFrame lrcFrame = new JFrame("歌词");
        lrcFrame.setIconImage(image.getImage());
        lrcFrame.setBounds(400, 900, 1000, 60);
        lrcLabel = new JLabel("透明窗口", JLabel.CENTER);
        lrcLabel.setForeground(new Color(31, 217, 224));
        lrcLabel.setFont(new Font("微软雅黑", Font.PLAIN, 38));
        lrcFrame.add(lrcLabel);
        lrcFrame.setUndecorated(true);
        lrcFrame.setBackground(new Color(0, 0, 0, 0));
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
     * 主面板
     */
    private void mainFrame() {
        frame = new JFrame("GFMusic");
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.jpg");
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


    /**
     * 列表面板
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
        jList.setSelectionBackground(new Color(64, 224, 208));
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
        song.setForeground(new Color(255, 76, 95));
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

    /**
     * 进度条面板
     * JButton 实现了普通的三态外加选中、禁用状态，有很多方法可以设置，不要自己去写鼠标监听器。
     *
     * setBorderPainted(boolean b)	//是否画边框，如果用自定义图片做按钮背景可以设为 false。
     *
     * setContentAreaFilled(boolean b)	//是否填充，如果你的自定义图片不是矩形或存在空白边距，可以设为 false 使按钮看起来透明。
     *
     * setFocusPainted(boolean b)	//是否绘制焦点（例如浅色虚线框或者加粗的边框表明按钮当前有焦点）。
     *
     * setMargin(Insets m)	//改变边距，如果 borderPainted 和 contentAreaFilled 都设成了 false，建议把边距都调为 0：new Insets(0, 0, 0, 0)。
     *
     * setIcon(Icon defaultIcon)	//注意了这是改的默认图标。三态中的默认，即鼠标未在其上的时候。
     *
     * setPressedIcon(Icon pressedIcon)	//按下时的图标。
     *
     * setRolloverIcon(Icon rolloverIcon)	//鼠标经过时的图标。
     *
     * setRolloverSelectedIcon(Icon rolloverSelectedIcon)	//鼠标经过时且被选中状态的图标。
     *
     * setSelectedIcon(Icon selectedIcon)	//选中时的图标。
     *
     * setDisabledIcon(Icon disabledIcon)	//禁用时显示的图标。例如可以换一张灰度图片。
     *
     * setDisabledSelectedIcon(Icon disabledSelectedIcon)	//禁用且被选中状态的图标。
     */
    private void setButton(JButton button, String pic, String press, String roll) {
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

    private void buttonBoxInit() {
        input = new JButton("导入");
        play = new JButton("播放");
        stop = new JButton("停止");
        next = new JButton("下一首");
        previous = new JButton("上一首");
        delete = new JButton("删除");
        deleteFile = new JButton("删除文件");
        lrcButton = new JButton("歌词:开");
        setButton(play, "default.png", "press.png", "put.png");
        setButton(previous, "default.png", "press.png", "put.png");
        setButton(next, "default.png", "press.png", "put.png");
        setButton(stop, "default.png", "press.png", "put.png");
        setButton(input, "default.png", "press.png", "put.png");
        setButton(delete, "default.png", "press.png", "put.png");
        setButton(deleteFile, "default.png", "press.png", "put.png");
        setButton(lrcButton, "default.png", "press.png", "put.png");
        String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
        modeBox = new JComboBox<>(modeName);
        modeBox.setBackground(new Color(255, 255, 255));
        modeBox.setPreferredSize(new Dimension(85, 25));
        modeBox.setOpaque(false);
    }

    private void loadSong() {
        list = new DefaultListModel<>();
        jList = new JList<>(list);
        songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
        lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
        if (getSize() == 0) {  //加载文件
            saveList = Utils.load();
            if (saveList != null) {
                for (String savePath : saveList) {
                    new Thread(() -> Utils.findAll(list, savePath, songPathMap, lrcPathMap))
                            .start();
                }
            }
        }
    }

    private void autoPlay() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(); //监听歌曲线程
        ScheduledExecutorService serviceLRC = Executors.newSingleThreadScheduledExecutor();//歌词和时间线程
        service.scheduleAtFixedRate(() -> {
            if (player != null && isComplete()) {
                choose2Play();
            }
        }, 1, 3, TimeUnit.SECONDS);
        serviceLRC.scheduleAtFixedRate(() -> {
            if (player == null || lrcMap == null || lrcMap.isEmpty() || pause) {
                return;
            }
            if (!isComplete()) {
                int second = getPosition() / 1000 + 1; //获得当前的时间
                String index = lrcMap.get(second);
                if (index != null) {
                    lrcLabel.setText(index);
                }
                int position = getPosition() / 1000;
                leftLabel.setText(Utils.secToTime(position));
                slider.setValue(position);
            }
        }, 1000, 500, TimeUnit.MILLISECONDS);
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
                    if (!changeSong) {
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

    /**删除*/
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
        if (thread == null || player == null) {
            return;
        }
        player.close();
        this.changeSong = false;
        play.setText("播放");
        leftLabel.setText(Utils.secToTime(0));
        rightLabel.setText(Utils.secToTime(0));
        song.setText("");
    }

    /**暂停*/
    private void pause() {
        if (thread == null) {
            return;
        }
        this.pause = true;
        play.setText("播放");
    }

    /**播放音乐*/
    private void playFile(String musicName) {
        stop();
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
            synchronized ("LOCK") {
                AudioDevice device = new JavaSoundAudioDevice();
                player = new Player(new FileInputStream(songPathMap.get(musicName)), device);
            }
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

    /**开启音乐线程*/
    private void play() {
        changeSong = true;
        pause = false;
        thread = new Thread(() -> {
            try {
                if (player != null) {
                    while (changeSong) {
                        if (pause) {
                            continue;
                        }
                        player.play();
                    }
                }
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        });
        thread.start();
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
        this.changeSong = false;
        if (index > 0) {
            index -= 1;
        } else {
            index = getSize() - 1;
        }
        playFile(getName());
    }
}
