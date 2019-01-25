package main.java.music;

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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class MusicPlayer implements ActionListener {

    private JFrame frame = new JFrame("QQ音乐");
    private String LOCATION;
    //按钮
    private JButton input = new JButton("导入");
    private JLabel song = new JLabel("歌曲名字");
    private JButton play = new JButton("播放");
    private JButton stop = new JButton("停止");
    private JButton next = new JButton("下一首");
    private JButton previous = new JButton("上一首");
    private JButton delete = new JButton("删除");
    //面板
    private JPanel buttonPanel = new JPanel(); //按钮面板
    private JPanel listPanel = new JPanel(); //列表面板
    private JPanel sliderPanel = new JPanel();//进度条面板
    private JPanel textPanel = new JPanel();//歌词面板
    private String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
    private JComboBox<String> modeBox = new JComboBox<>(modeName);
    //进度条
    private JSlider slider = new JSlider();
    private JLabel leftLabel = new JLabel(Utils.secToTime(0));
    private JLabel rightLabel = new JLabel(Utils.secToTime(0));
    private List list = new List(10);
    //文字域
    private JTextArea textArea = new JTextArea(10, 20);

    private Player player; //播放器
    private Thread thread; //播放线程
    private Thread time; //时间线程
    private int index;//当前播放的音乐索引
    private String name = "";//当前播放的歌曲名称
    private String deleteName = ""; //需要删除的歌曲名字
    private int second; //歌词时间
    private Map<String, String> songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private boolean changed = false; //列表是否改变
    private java.util.List<String> saveList; //路径保存的列表
    private int totalTime; //当前歌曲总时间
    private ExecutorService serviceLRC;
    private Map<String, String> lrcMap;
    private volatile String lrcTime; //歌词时间字符串
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
        frame.setBounds(300, 400, 550, 590);
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
        buttonPanel.add(delete);
        frame.setLayout(new FlowLayout());
        frame.add(listPanel);
        frame.add(sliderPanel);
        frame.add(textPanel);
        frame.add(buttonPanel);
        textPanel.add(scrollPane);
        textArea.setFont(new Font(null, Font.PLAIN, 18));   // 设置字体
        textArea.setEditable(false);
        //歌曲列表的加载
        loadSong();
        //改变图标
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.png");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        frame.setIconImage(image.getImage());
        frame.setVisible(true);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(); //监听歌曲线程
        service.scheduleAtFixedRate(() -> {
            if (player != null && player.isComplete()) {
                choose2Play();
            }
            if (!deleteName.equals(name)) { //删除歌曲
                if (!deleteName.isEmpty()) {
                    if (Utils.deleteSong(deleteName)) {
                        System.out.println(deleteName + "删除成功！！");
                        deleteName = "";
                    }
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
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
                    index = list.getSelectedIndex();
                }
            }
        });
        //按钮监听
        next.addActionListener(this);
        input.addActionListener(this);
        previous.addActionListener(this);
        delete.addActionListener(this);
        play.addActionListener(this);
        stop.addActionListener(this);
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
                choose2Play();
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
            case "删除":
                deleteSong();
                break;
        }
    }

    private void deleteSong() {
        String item = list.getItem(index);
        deleteName = songPathMap.remove(item);
        list.remove(item);
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
        if (serviceLRC != null) {
            serviceLRC.shutdownNow();
        }
        player.close();
        song.setText("歌曲名字");
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
            String path = lrcPathMap.get(musicName);
            name = songPathMap.get(musicName);
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
                int second = player.getPosition() / 1000 + 1;
                String time = Utils.secToTime(second);
                if (time.equals(lrcTime) || this.second == second) {
                    return;
                }
                String lrc = lrcMap.remove(time);
                if (lrc != null) {
                    System.out.println(lrc);
                    textArea.append("   ");
                    textArea.append(lrc);
                    textArea.append("\n");
                    lrcTime = time;
                    this.second = second;
                    JScrollBar jscrollBar = scrollPane.getVerticalScrollBar();
                    if (jscrollBar != null) {
                        jscrollBar.setValue(jscrollBar.getMaximum());
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void choose2Play() {
        switch (modeBox.getModel().getSelectedItem().toString()) {
            case "顺序播放":
                next();
                break;
            case "单曲循环":
                playFile(list.getItem(index));
                break;
            case "随机播放":
                randomPlay();
                break;
        }
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