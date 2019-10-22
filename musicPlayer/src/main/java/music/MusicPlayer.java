package main.java.music;

import javazoom.jl.decoder.JavaLayerException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class MusicPlayer implements ActionListener {
    public Map<String, String> getSongPathMap() {
        return songPathMap;
    }

    public Map<String, String> getLrcPathMap() {
        return lrcPathMap;
    }

    public List<String> getSaveList() {
        return saveList;
    }

    private String currentMusicName; //当前播放的歌曲名
    private volatile Player player; //播放器
    private int index;//当前播放的音乐索引
    private Map<String, String> songPathMap; //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap; //歌曲名称和路径的键值对
    private java.util.List<String> saveList; //路径保存的列表
    private Map<Integer, String> lrcMap;

    public boolean getPlayState() {
        return playState;
    }


    private boolean needTurn;
    private volatile boolean playState; //为true则播放，false为结束
    private volatile boolean pause; //暂停歌曲

    public int getIndex() {  //当前播放的索引
        return index;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setNeedTurn(boolean needTurn) {
        this.needTurn = needTurn;
    }

    private ExecutorService playThread;//播放线程
    private MusicFrame frame;

    public MusicPlayer() {
        this.frame = new MusicFrame();
        init();//初始化
        listener();
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    private void init() {
        loadSong(); //加载歌曲列表
        playInit(); //播放线程初始化
        frame.mainFrame(); //先加载歌曲，后显示界面
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
                    Utils.findAll(savePath, songPathMap, lrcPathMap);
                }
                for (String name : songPathMap.keySet()) {
                    frame.list.addElement(name);
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
            if (player == null || pause || lrcMap == null) {
                return;
            }
            if (!isComplete()) {
                int second = getPosition() / 1000; //获得当前的时间
                String index = lrcMap.get(second);
                if (index != null) {
                    frame.lrcLabel.setText(index);
                }
                int position = getPosition() / 1000;
                frame.leftLabel.setText(Utils.secToTime(position));
                frame.slider.setValue(position);
            }
        }, 1000, 50, TimeUnit.MILLISECONDS); //每50秒执行一次
    }

    private int getPosition() {
        return player.getPosition();
    }

    private boolean isComplete() {
        return player.isComplete();
    }


    public void saveSong() {
        //保存数据
        if (frame.jList.getVisibleRowCount() > 0) {
            Utils.save(saveList);
        }
    }

    public int getSize() {
        return frame.jList.getModel().getSize();
    }


    /**
     * 从列表中移除歌曲
     */
    public void deleteSong() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) frame.jList.getModel();
        int index = frame.jList.getSelectedIndex();
        if (index == -1) {
            return;
        }
        String name = listModel.get(frame.jList.getSelectedIndex());
        if (name == null) {
            return;
        }
        listModel.remove(index);
        if (index == listModel.size()) {
            frame.jList.setSelectedIndex(index - 1);
        } else {
            frame.jList.setSelectedIndex(index);
        }
        songPathMap.remove(name);//歌曲
        lrcPathMap.remove(name);//歌词
    }

    /**
     * 删除歌曲和歌词文件
     */
    public void deleteFile() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) frame.jList.getModel();
        int index = frame.jList.getSelectedIndex();
        if (index == -1) {
            return;
        }
        String name = listModel.get(index);
        if (name == null || name.equals(getName())) {
            return;
        }
        listModel.remove(frame.jList.getSelectedIndex());
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
        frame.song.setText(getName());
    }

    /**
     * 停止
     */
    public void stop() {
        this.playState = false;
        frame.play.setText("播放");
        frame.leftLabel.setText(Utils.secToTime(0));
        frame.rightLabel.setText(Utils.secToTime(0));
        frame.song.setText("歌曲");
        if (player != null) {
            player.close();
        }
    }


    /**
     * 暂停
     */
    public synchronized void pause() {
        if (playThread == null) {
            return;
        }
        this.pause = true;
        frame.play.setText("播放");
    }

    /**
     * 播放音乐
     */
    public void playFile(String musicName) {
        try {
            synchronized (this) {
                stop();
                pause = false;
                notifyAll();
            }
            frame.lrcLabel.setText(musicName);
            frame.song.setText(musicName);
            JScrollBar jscrollBar = frame.scrollPaneList.getVerticalScrollBar();
            if (jscrollBar != null && needTurn) {
                jscrollBar.setValue((index - 3) * 21);
            }
            frame.jList.setSelectedIndex(index);
            if (lrcMap != null) {
                lrcMap.clear();
            }
            String path = lrcPathMap.get(musicName);
            if (path != null) {
                lrcMap = Utils.readLRC(path);
            } else {
                lrcMap = new HashMap<>();
            }
            int totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            frame.slider.setMinimum(0);
            frame.slider.setMaximum(totalTime);
            frame.rightLabel.setText(Utils.secToTime(totalTime));
            frame.play.setText("暂停");
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

    public void choose2Play() {
        switch (frame.modeBox.getModel().getSelectedItem().toString()) {
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

    public String getName() {
        return frame.jList.getModel().getElementAt(index);
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
    public void previous() {
        if (index > 0) {
            index -= 1;
        } else {
            index = getSize() - 1;
        }
        playFile(getName());
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
            case "停止":
                stop();
                return;
            case "播放":
                if (getPlayer() == null) {
                    setIndex(frame.jList.getSelectedIndex());
                    if (getIndex() == -1) {
                        return;
                    }
                    playFile(getName());

                } else {
                    if (!getPlayState()) {
                        setIndex(frame.jList.getSelectedIndex());
                        if (getIndex() == -1) {
                            return;
                        }
                        playFile(getName());
                    } else {
                        synchronized (this) {
                            setPause(false);
                            notifyAll(); //继续
                        }
                    }
                }
                frame.play.setText("暂停");
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
                if (LOCATION == null || getSaveList() == null) {
                    return;
                }
                if (!getSaveList().contains(LOCATION)) {
                    getSaveList().add(LOCATION);
                }
                Utils.findAll(LOCATION, getSongPathMap(), getLrcPathMap());
                for (String name : songPathMap.keySet()) {
                    frame.list.addElement(name);
                }
                saveSong();
                break;
            case "删除":
                deleteSong();
                break;
            case "删除文件":
                deleteFile();
                break;
            case "歌词:开":
                frame.lrcFrame.setVisible(true);
                frame.lrcButton.setText("歌词:关");
                break;
            case "歌词:关":
                frame.lrcFrame.setVisible(false);
                frame.lrcButton.setText("歌词:开");
                break;
        }
    }


    private void listener() {
        frame.jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedIndex = frame.jList.getSelectedIndex();
                    if (getIndex() == selectedIndex) {
                        return;
                    }
                    setNeedTurn(false);
                    playFile(frame.jList.getSelectedValue());
                    setNeedTurn(true);
                    setIndex(selectedIndex);
                    frame.jList.setSelectedIndex(getIndex());
                }

            }
        });
        frame.next.addActionListener(this);
        frame.input.addActionListener(this);
        frame.previous.addActionListener(this);
        frame.delete.addActionListener(this);
        frame.deleteFile.addActionListener(this);
        frame.play.addActionListener(this);
        frame.stop.addActionListener(this);
        frame.lrcButton.addActionListener(this);
        frame.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
