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

    JFrame frame = new JFrame("音乐");
    String Location = "";
    //按钮
    JButton input = new JButton("导入");
    JLabel song = new JLabel("曲名");
    JButton play = new JButton("播放");
    JButton stop = new JButton("停止");
    JButton next = new JButton("下一首");
    JButton previous = new JButton("上一首");
    //面板
    JPanel buttonPanel = new JPanel(); //按钮面板
    JPanel listPanel = new JPanel(); //列表面板
    JPanel sliderPanel = new JPanel();//进度条面板
    JPanel textPanel = new JPanel();//歌词面板
    String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
    JComboBox modeBox = new JComboBox(modeName);
    //进度条
    JSlider slider = new JSlider();
    JLabel leftLabel = new JLabel(Utils.secToTime(0));
    JLabel rightLabel = new JLabel(Utils.secToTime(0));
    List list = new List(10);  //列表
    //文字域
    JTextArea textArea = new JTextArea(10, 20);
    Player player; //播放
    Thread thread; //播放线程
    Thread time; //时间线程
    int index;//当前播放的音乐索引
    int nextIndex;//下一首音乐索引
    String musicName; //当前播放的乐曲名称
    String nextMusicName;//下一曲

    Map<String, String> songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    Map<String, String> lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    boolean changed = false; //列表是否改变
    private java.util.List<String> saveList; //路径保存的列表
    private int totalTime; //当前歌曲总时间
    int start = 0; //进度条初始值
    int end = 100;//进度条最大值
    ExecutorService serviceLRC;
    Map<String, String> lrcMap;
    volatile String lrcshow;
    JScrollPane scrollPane = new JScrollPane(
            textArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    );

    public MusicPlayer() {
        init();//窗口
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
        if (list.getItemCount() == 0) {  //加载文件
            saveList = Utils.load();
            if (saveList != null) {
                for (String s : saveList) {
                    Utils.findAll(list, s, songPathMap, lrcPathMap);
                }
                randomPlay();
            }
        }
        //存储线程开启
        ExecutorService service = Executors.newSingleThreadScheduledExecutor();
        ((ScheduledExecutorService) service)
                .scheduleAtFixedRate(this::saveSong, 10, 10, TimeUnit.SECONDS);

        frame.setVisible(true);
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
        if (cmd.equals("停止")) {
            stop();
        }
        if (cmd.equals("播放")) {
            if (thread == null) {
                return;
            }
            thread.resume();
            time.resume();
            play.setText("暂停");
        } else if (cmd.equals("暂停")) {
            pause();
        } else if (cmd.equals("下一首")) {
            if (thread == null || list.getItemCount() == 0) {
                return;
            }
            next();
        } else if (cmd.equals("上一首")) {
            if (thread == null || list.getItemCount() == 0) {
                return;
            }
            previous();
        } else if (cmd.equals("导入")) {
            Location = Utils.open();
            if (Location == null) {
                return;
            }
            if (!saveList.contains(Location)) {
                saveList.add(Location);
            }
            Utils.findAll(list, Location, songPathMap, lrcPathMap);
            changed = true;
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
        if (thread == null && player == null) {
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
            serviceLRC.shutdown();
        }
        if (lrcMap != null) {
            lrcMap.clear();
        }
        try {
            player = new Player(new FileInputStream(songPathMap.get(musicName)));
            totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            this.musicName = musicName;
            String path = lrcPathMap.get(Utils.getLrcName(musicName));
            if (path != null) {
                lrcMap = Utils.readLRC(path);
                textArea.append(Utils.getHeader(lrcMap));
            }
        } catch (JavaLayerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
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
        start = 0;
        end = totalTime;
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

        ((ScheduledExecutorService) serviceLRC)
                .scheduleAtFixedRate((Runnable) () -> {
                    if (!player.isComplete()) {
                        String time1 = Utils.secToTime(player.getPosition() / 1000 + 2);
                        if (time1.equals(lrcshow)) {
                            return;
                        }
                        String lrc = lrcMap.get(time1);
                        if (lrc != null) {
                            System.out.println(lrc);
                            textArea.append("  ");
                            textArea.append(lrc);
                            textArea.append("\n");
                            lrcshow = time1;
                            JScrollBar jscrollBar = scrollPane.getVerticalScrollBar();
                            if (jscrollBar != null)
                                jscrollBar.setValue(jscrollBar.getMaximum());
                        }
                    }
                }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void next() {
        if (index < list.getItemCount() - 1) {
            thread.stop();
            time.stop();
            index += 1;
            playFile(list.getItem(index));
        }
    }

    public void previous() {
        if (index > 0) {
            thread.stop();
            time.stop();
            index -= 1;
            playFile(list.getItem(index));
        }
    }
/*    JPanel leftPanel = new JPanel();
    JPanel centerPanel = new JPanel();
    JPanel rightPanel = new JPanel();
    JPanel listPanel = new JPanel();
    JPanel sliderPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel titlePanel = new JPanel();*/

    /*public void newFrame() {

        frame.add(leftPanel);
        frame.add(centerPanel);
        frame.add(rightPanel);
        frame.setLayout(null);
        frame.setBounds(100, 50, 1100, 700);

*//*        leftPanel.setBounds(100, 50, 250, 700);
        leftLabel.setLayout(null);*//*

        centerPanel.setBounds(250, 50, 650, 700);
        centerPanel.setLayout(null);
        centerPanel.add(listPanel);
        centerPanel.add(sliderPanel);
        centerPanel.add(buttonPanel);

        rightPanel.setBounds(650, 50, 1100, 700);
        rightPanel.add(titlePanel);
        rightPanel.add(lrcPanel);
        rightPanel.setLayout(null);

        listPanel.setBounds(250, 50, 650, 500);
        listPanel.setLayout(null);
        sliderPanel.setBounds(250, 500, 650, 700);
        sliderPanel.setLayout(null);
        buttonPanel.setBounds(250, 600, 650, 700);
        buttonPanel.setLayout(null);
        titlePanel.setBounds(650, 50, 1100, 150);
        lrcPanel.setBounds(650, 150, 1100, 700);
        list.setBounds(250, 50, 650, 500);
        listPanel.add(list);
        sliderPanel.add(leftLabel);
        sliderPanel.add(slider);
        sliderPanel.add(rightLabel);
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(next);
        buttonPanel.add(stop);
        buttonPanel.add(input);
        buttonPanel.add(modeBox);
        lrcPanel.add(scrollPane);
        textArea.setBounds(650, 150, 1100, 700);
        frame.setVisible(true);

    }*/


}