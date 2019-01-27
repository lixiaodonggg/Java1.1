package main.java.music;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static sun.security.pkcs11.wrapper.Constants.NEWLINE;

public class MusicPlayer implements ActionListener {

    private JFrame frame = new JFrame("GFMusic");
    private JButton input = new JButton("导入");
    private JLabel song = new JLabel("");
    private JButton play = new JButton("播放");
    private JButton stop = new JButton("停止");
    private JButton next = new JButton("下一首");
    private JButton previous = new JButton("上一首");
    private JButton delete = new JButton("删除");
    private JButton deleteFile = new JButton("删除文件");
    //面板
    private JPanel buttonPanel = new JPanel(); //按钮面板
    private JPanel sliderPanel = new JPanel();//进度条面板
    private JPanel textPanel = new JPanel();
    private String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
    private JComboBox<String> modeBox = new JComboBox<>(modeName);
    //进度条
    private JSlider slider = new JSlider();
    private JLabel leftLabel = new JLabel(Utils.secToTime(0));
    private JLabel rightLabel = new JLabel(Utils.secToTime(0));
    private JScrollPane scrollPaneList;
    private DefaultListModel<String> list = new DefaultListModel<>();
    //文字域
    private JList<String> jList = new JList<>(list);
    private JTextArea textArea = new JTextArea(10,27);

    private Player player; //播放器

    private Thread thread; //播放线程
    private Thread time; //时间线程
    private int index;//当前播放的音乐索引
    private int second; //歌词时间
    private Map<String, String> songPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private Map<String, String> lrcPathMap = new HashMap<>(); //歌曲名称和路径的键值对
    private boolean changed = false; //列表是否改变
    private java.util.List<String> saveList; //路径保存的列表
    private int totalTime; //当前歌曲总时间
    private ExecutorService serviceLRC = Executors.newSingleThreadScheduledExecutor();
    private Map<Integer, String> lrcMap;
    private JScrollPane scrollPane =
            new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private String lastLrc;
    private boolean needturn = true;

    public MusicPlayer() {
        init();//初始化
        listener();//监听
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    private void init() {
        loadSong();
        autoPlay();
        mainFrame();
    }

    /**
     * 主面板
     */
    private void mainFrame() {
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.jpg");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        frame.setIconImage(image.getImage());
        frame.setBounds(400, 200, 400, 610);
        frame.add(listPanel(), BorderLayout.NORTH);
        frame.add(lrcPanel(), BorderLayout.CENTER);
        frame.add(controlPanel(), BorderLayout.SOUTH);
        frame.setResizable(false);
        frame.setVisible(true);
    }


    /**
     * 列表面板
     */
    private JPanel listPanel() {
     scrollPaneList =
                new JScrollPane(jList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel listPanel = new JPanel();
        listPanel.add(scrollPaneList, BorderLayout.SOUTH);
        jList.setFixedCellWidth(380);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        jList.setCellRenderer(renderer);
        jList.setSelectionBackground(Color.CYAN);
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setValueIsAdjusting(true);
        return listPanel;
    }

    /**
     * 歌词面板
     */
    private JPanel lrcPanel() {
        JPanel lrcPanel = new JPanel();
        JPanel namePanel = new JPanel();
        namePanel.add(song);
        song.setFont(new Font(null, Font.PLAIN, 20));
        song.setForeground(Color.RED);
        textArea.setForeground(Color.BLUE);
        textPanel.add(scrollPane);
        textPanel.setOpaque(false);
        textArea.setFont(new Font(null, Font.PLAIN, 18));   // 设置字体
        textArea.setEditable(false);
        textArea.setSelectedTextColor(Color.RED);
        lrcPanel.setLayout(new BorderLayout());
        sliderPanel.add(leftLabel, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(rightLabel, BorderLayout.EAST);
        lrcPanel.add(namePanel, BorderLayout.NORTH);
        lrcPanel.add(textPanel, BorderLayout.CENTER);
        lrcPanel.add(sliderPanel, BorderLayout.SOUTH);
        return lrcPanel;
    }

    /**
     * 进度条面板
     */
    private JPanel controlPanel() {
        JPanel controlPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 2));
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(next);
        buttonPanel.add(stop);
        buttonPanel.add(input);
        buttonPanel.add(modeBox);
        buttonPanel.add(delete);//deleteFile
        buttonPanel.add(deleteFile);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        return controlPanel;
    }

    private void loadSong() {
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
        service.scheduleAtFixedRate(() -> {
            if (player != null && player.isComplete()) {
                choose2Play();
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void listener() {
        jList.addMouseListener(new MouseAdapter() {  //列表
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    stop();
                    int selectedIndex = jList.getSelectedIndex();
                    if (index==selectedIndex){
                        return;
                    }
                    needturn = false;
                    playFile(jList.getSelectedValue());
                    needturn = true;
                    index = selectedIndex;
                    jList.setSelectedIndex(index);
                }

            }
        });
        //按钮监听
        next.addActionListener(this);
        input.addActionListener(this);
        previous.addActionListener(this);
        delete.addActionListener(this);
        deleteFile.addActionListener(this);
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
        if (jList.getVisibleRowCount() > 0 && changed) {
            Utils.save(saveList);
            changed = false;
        }
    }

    private int getSize(){
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
                if (thread == null || getSize() == 0) {
                    return;
                }
                choose2Play();
                break;
            case "上一首":
                if (thread == null ||  getSize()  == 0) {
                    return;
                }
                previous();
                break;
            case "导入":
                String LOCATION = Utils.open();
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
            case "删除文件":
                deleteFile();
                break;
        }
    }

    private void deleteSong() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) jList.getModel();
        int index = jList.getSelectedIndex();
        String name = ((DefaultListModel<String>) jList.getModel()).get(jList.getSelectedIndex());
        listModel.remove(index);
        jList.setSelectedIndex(index);
        songPathMap.remove(name); //歌曲
        lrcPathMap.remove(name);//歌词
    }

    private void deleteFile() {
        DefaultListModel<String> listModel = (DefaultListModel<String>) jList.getModel();
        String name = ((DefaultListModel<String>) jList.getModel()).get(jList.getSelectedIndex());
        if (name.equals(getName())){
            return;
        }
        listModel.remove(jList.getSelectedIndex());
        String delete = songPathMap.remove(name); //歌曲
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
        if (this.index==index){
            return;
        }
        this.index=index;
        playFile(getName());
        song.setText(getName());
    }

    private void stop() {
        if (thread == null || player == null) {
            return;
        }
        thread.stop();
        time.stop();
        player.close();
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

    private void playFile(String musicName) {
        textArea.setText("");
        if (player != null) {
            player.close();
        }
        if (lrcMap != null) {
            lrcMap.clear();
        }
        JScrollBar jscrollBar = scrollPaneList.getVerticalScrollBar();
        if (jscrollBar != null&&needturn) {
            jscrollBar.setValue((index-3)*20);
        }
        try {
            player = new Player(new FileInputStream(songPathMap.get(musicName)));
            totalTime = Utils.getMp3Time(songPathMap.get(musicName));
            String path = lrcPathMap.get(musicName);
            jList.setSelectedIndex(index);
            if (path != null) {
                lrcMap = Utils.readLRC(path);
                textArea.append(Utils.getHeader(lrcMap));
            }
        } catch (JavaLayerException | FileNotFoundException  e) {
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
        this.second = 0;
        lastLrc = "";
        ((ScheduledExecutorService) serviceLRC).scheduleAtFixedRate(() -> {
            if (!player.isComplete()) {
                int second = player.getPosition() / 1000 + 1; //获得当前的时间
                if ( second <= this.second) {
                    return;
                }
                String lrc = lrcMap.get(second); //当前时间对应的歌词
                if (lastLrc.equals(lrc)){
                    return;
                }
                if (lrc != null) {
                    System.out.println(lrc);
                    textArea.append("\t");
                    textArea.append(lrc);
                    textArea.append(NEWLINE);
                    this.second = second;
                    lastLrc = lrc;
                    JScrollBar jscrollBar = scrollPane.getVerticalScrollBar();
                    if (jscrollBar != null) {
                        jscrollBar.setValue(jscrollBar.getMaximum());
                    }
                    jscrollBar = scrollPane.getHorizontalScrollBar();
                    if (jscrollBar != null) {
                        jscrollBar.setValue(jscrollBar.getMaximum());
                    }

                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void choose2Play() {
        switch (modeBox.getModel().getSelectedItem().toString()) {
            case "顺序播放":
                next();
                break;
            case "单曲循环":
                playFile(getName());
                break;
            case "随机播放":
                randomPlay();
                break;
        }
    }

    private String getName(){
        return jList.getModel().getElementAt(index);
    }
    private void next() {
        thread.stop();
        time.stop();
        if (index < getSize() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        playFile(getName());
    }

    private void previous() {
        thread.stop();
        time.stop();
        if (index > 0) {
            index -= 1;
        } else {
            index =getSize() - 1;
        }
        playFile(getName());
    }
}
