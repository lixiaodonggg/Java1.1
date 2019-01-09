package music;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class MusicPlayer implements ActionListener {

    private JFrame frame = new JFrame("音乐");
    private String LOCATION;
    //按钮
    private JButton input = new JButton("导入");
    private JLabel song = new JLabel("曲名");
    private JButton play = new JButton("播放");
    private JButton stop = new JButton("停止");
    private JButton next = new JButton("下一首");
    private JButton previous = new JButton("上一首");
    //面板
    private JPanel buttonPanel = new JPanel(); //按钮面板
    private JPanel listPanel = new JPanel(); //列表面板
    private JPanel sliderPanel = new JPanel();//进度条面板
    private JPanel textPanel = new JPanel();//歌词面板
    private String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
    private JComboBox modeBox = new JComboBox(modeName);
    //进度条
    private JSlider slider = new JSlider();
    private JLabel leftLabel = new JLabel(Utils.secToTime(0));
    private JLabel rightLabel = new JLabel(Utils.secToTime(0));
    private List list = new List(10);  //列表
    //文字域
    private JTextArea textArea = new JTextArea(10, 20);
    private Player player; //播放
    private Thread thread; //播放线程
    private Thread time; //时间线程
    private int index;//当前播放的音乐索引
    private int nextIndex;//下一首音乐索引
    private String musicName; //当前播放的乐曲名称
    private String nextMusicName;//下一曲

    Map<String, String> songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private boolean changed = false; //列表是否改变
    private java.util.List<String> saveList; //路径保存的列表
    private int totalTime; //当前歌曲总时间
    private ExecutorService serviceLRC;
    private Map<String, String> lrcMap;
    private volatile String lrcshow;
    private JScrollPane scrollPane =
            new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    public MusicPlayer() {
        init();//初始化
        listener();//监听
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    /**
     * 窗口初始化
     */
    public void init() {

        //面板初始化
        frame.setBounds(300, 400, 550, 550);
        listPanel.add(list);
        sliderPanel.add(leftLabel, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(rightLabel, BorderLayout.EAST);
        sliderPanel.add(song, BorderLayout.SOUTH);
        textArea.setForeground(Color.BLUE);
        buttonPanel.setLayout(new GridLayout(2, 2));
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(next);
        buttonPanel.add(stop);
        buttonPanel.add(input);
        buttonPanel.add(modeBox);
        frame.setLayout(new FlowLayout());
        frame.add(listPanel);
        frame.add(sliderPanel);
        frame.add(textPanel);
        frame.add(buttonPanel);
        textPanel.add(scrollPane);
        play.addActionListener(this);
        stop.addActionListener(this);
        textArea.setFont(new Font(null, Font.PLAIN, 18));   // 设置字体
        loadSong();
        frame.setVisible(true);
    }

    private void loadSong() {
        if (list.getItemCount() == 0) {  //加载文件
            saveList = Utils.load();
            if (saveList != null) {
                for (String savePath : saveList) {
                    new Thread(() -> Utils.findAll(list, savePath, songPathMap, lrcPathMap))
                            .start();
                }
            }
        }
    }

    public void listener() {
        list.addMouseListener(new MouseAdapter() {  //列表
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    playFile(list.getSelectedItem());
                }
            }
        });
        //按钮监听
        next.addActionListener(this);
        input.addActionListener(this);
        previous.addActionListener(this);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void saveSong() {
        //保存数据
        if (list.getItemCount() > 0 && changed) {
            Utils.save(saveList);
            changed = false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
            case "停止":
                stop();
                return;
            case "播放":
                if (thread == null) {
                    return;
                }
                thread.resume();
                time.resume();
                play.setText("暂停");
                break;
            case "暂停":
                pause();
                break;
            case "下一首":
                if (thread == null || list.getItemCount() == 0) {
                    return;
                }
                next();
                break;
            case "上一首":
                if (thread == null || list.getItemCount() == 0) {
                    return;
                }
                previous();
                break;
            case "导入":
                LOCATION = Utils.open();
                if (LOCATION == null) {
                    return;
                }
                if (!saveList.contains(LOCATION)) {
                    saveList.add(LOCATION);
                }
                Utils.findAll(list, LOCATION, songPathMap, lrcPathMap);
                changed = true;
                saveSong();
                break;
        }
    }

    public void randomPlay() {
        if (list.getItemCount() == 0) {
            return;
        }
        index = (int) (Math.random() * list.getItemCount());
        playFile(list.getItem(index));
        song.setText(list.getItem(index));
    }

    private void stop() {
        if (thread == null || player == null) {
            return;
        }
        thread.stop();
        time.stop();
        serviceLRC.shutdownNow();
        player.close();
        song.setText("选曲");
        play.setText("播放");
    }

    private void pause() {
        if (thread == null || time == null) {
            return;
        }
        thread.suspend();
        time.suspend();
        play.setText("播放");

    }

    public void playFile(String musicName) {
        if (player != null) {
            player.close();
            textArea.setText("");
        }
        if (lrcMap != null) {
            lrcMap.clear();
        }
        try {
            player = new Player(new FileInputStream(songPathMap.get(musicName)));
            totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            this.musicName = musicName;
            String path = lrcPathMap.get(Utils.replaceSuffix(musicName, ".lrc"));
            if (path != null) {
                lrcMap = Utils.readLRC(path);
                textArea.append(Utils.getHeader(lrcMap));
            }
        } catch (JavaLayerException | FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            play();
            play.setText("暂停");
            song.setText(musicName);
        }

    }

    private void play() {

        thread = new Thread(() -> {
            try {
                if (player != null) {
                    player.play();
                }
            } catch (JavaLayerException e) {
                e.printStackTrace();
            } finally {
                next();
            }
        });
        thread.start();
        //进度条初始值
        int start = 0;
        //进度条最大值
        int end = totalTime;
        slider.setMinimum(start);
        slider.setMaximum(end);

        rightLabel.setText(Utils.secToTime(totalTime));
        time = new Thread(() -> {
            while (!player.isComplete()) {
                leftLabel.setText(Utils.secToTime(player.getPosition() / 1000));
                slider.setValue(player.getPosition() / 1000);
            }
        });
        time.start();
        if (lrcMap == null) {
            return;
        }
        serviceLRC = Executors.newSingleThreadScheduledExecutor();
        ((ScheduledExecutorService) serviceLRC).scheduleAtFixedRate(() -> {
            if (!player.isComplete()) {
                String time1 = Utils.secToTime(player.getPosition() / 1000 + 1);
                if (time1.equals(lrcshow)) {
                    return;
                }
                String lrc = lrcMap.remove(time1);
                if (lrc != null) {
                    System.out.println(lrc);
                    textArea.append("   ");
                    textArea.append(lrc);
                    textArea.append("\n");
                    lrcshow = time1;
                    JScrollBar jscrollBar = scrollPane.getVerticalScrollBar();
                    if (jscrollBar != null) {
                        jscrollBar.setValue(jscrollBar.getMaximum());
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

    }

    public void next() {
        thread.stop();
        time.stop();
        if (index < list.getItemCount() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        playFile(list.getItem(index));
    }

    public void previous() {
        thread.stop();
        time.stop();
        if (index > 0) {
            index -= 1;
        } else {
            index = list.getItemCount() - 1;
        }
        playFile(list.getItem(index));
    }
}