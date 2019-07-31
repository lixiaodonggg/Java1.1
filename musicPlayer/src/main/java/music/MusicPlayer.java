package main.java.music;

import javazoom.jl.decoder.JavaLayerException;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class MusicPlayer extends MusicFrame {

    private String currentMusicName; //当前播放的歌曲名
    private volatile Player player; //播放器
    private int index;//当前播放的音乐索引
    private Map<String, String> songPathMap; //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap; //歌曲名称和路径的键值对
    private java.util.List<String> saveList; //路径保存的列表
    private Map<Integer, String> lrcMap;
    private boolean needTurn;
    private volatile boolean playState; //为true则播放，false为结束
    private volatile boolean pause; //暂停歌曲
    private ExecutorService playThread;//播放线程

    public static void main(String[] args) {
        MusicPlayer player = new MusicPlayer();
        player.start();
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

    /**
     * 加载歌曲
     */
    private void loadSong() {
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

    /**
     * 播放初始化
     */
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
        }, 1000, 100, TimeUnit.MILLISECONDS); //每50秒执行一次
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
                        synchronized (this) {
                            this.pause = false;
                            notifyAll(); //继续
                        }
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

    /**
     * 从列表中移除歌曲
     */
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

    /**
     * 删除歌曲和歌词文件
     */
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

    /**
     * 随机播放
     */
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

    /**
     * 停止
     */
    private void stop() {
        this.playState = false;
        play.setText("播放");
        leftLabel.setText(Utils.secToTime(0));
        rightLabel.setText(Utils.secToTime(0));
        song.setText("歌曲");
        if (player != null) {
            player.close();
        }
    }


    /**
     * 暂停
     */
    private synchronized void pause() {
        if (playThread == null) {
            return;
        }
        this.pause = true;
        play.setText("播放");
    }

    /**
     * 播放音乐
     */
    private void playFile(String musicName) {
        try {
            synchronized (this) {
                stop();
                pause = false;
                notifyAll();
            }
            lrcLabel.setText(musicName);
            song.setText(musicName);
            JScrollBar jscrollBar = scrollPaneList.getVerticalScrollBar();
            if (jscrollBar != null && needTurn) {
                jscrollBar.setValue((index - 3) * 21);
            }
            jList.setSelectedIndex(index);
            if (lrcMap != null) {
                lrcMap.clear();
            }
            String path = lrcPathMap.get(musicName);
            if (path != null) {
                lrcMap = Utils.readLRC(path);
            }
            int totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            slider.setMinimum(0);
            slider.setMaximum(totalTime);
            rightLabel.setText(Utils.secToTime(totalTime));
            play.setText("暂停");
            player = new Player(new FileInputStream(songPathMap.get(musicName)));
            play();
            currentMusicName = musicName;
        } catch (JavaLayerException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 音乐播放线程
     */
    private void play() {
        playState = true;
        playThread.execute(() -> {
            try {
                while (playState) {
                    if (pause) {
                        synchronized (this) {
                            wait(); //暂停
                        }
                    }
                    player.play();
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

    /**
     * 下一曲
     */
    private void next() {
        if (index < getSize() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        playFile(getName());
    }

    /**
     * 上一曲
     */
    private void previous() {
        if (index > 0) {
            index -= 1;
        } else {
            index = getSize() - 1;
        }
        playFile(getName());
    }
}
