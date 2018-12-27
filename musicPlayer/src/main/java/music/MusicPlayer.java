package music;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

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

import javax.swing.*;


public class MusicPlayer extends PlaybackListener implements ActionListener {

    String Location = "";
    private JFrame jFrame = new JFrame("播放器");
    private JButton input = new JButton("导入");
    private JLabel song = new JLabel("曲名");
    private JButton play = new JButton("播放");
    private JButton stop = new JButton("停止");
    private JButton next = new JButton("下一首");
    private JButton previous = new JButton("上一首");
    private JPanel buttonPanel = new JPanel(); //按钮面板
    private JPanel listPanel = new JPanel(); //列表面板
    private JPanel sliderPanel = new JPanel();//进度条面板
    private JPanel textPanel = new JPanel();//歌词面板
    private AdvancedPlayer player;
    private Thread thread;
    private Thread time;
    private List list = new List(10);
    int index;
    private Map<String, String> map = new HashMap<>();
    boolean changed = false;
    private java.util.List<String> saveList;
    private int totalTime; //总时间
    private int start = 0;
    private int end = 100;
    private JSlider slider = new JSlider();
    private JLabel leftLabel = new JLabel(Utils.secToTime(0));
    private JLabel rightLabel = new JLabel(Utils.secToTime(0));
    private AudioDevice audio;
    private int frame;

    public MusicPlayer() {

        jFrame.setBounds(500, 500, 300, 350);
        listPanel.add(list);
        sliderPanel.add(leftLabel, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(rightLabel, BorderLayout.EAST);
        textPanel.add(song);
        buttonPanel.setLayout(new GridLayout(2, 2));
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(next);
        buttonPanel.add(stop);
        buttonPanel.add(input);
        jFrame.setLayout(new FlowLayout());
        jFrame.add(listPanel);
        jFrame.add(textPanel);
        jFrame.add(sliderPanel);
        jFrame.add(buttonPanel);
        jFrame.setVisible(true);
        play.addActionListener(this);
        stop.addActionListener(this);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    playFile(list.getSelectedItem());
                }
            }
        });
        next.addActionListener(this);
        input.addActionListener(this);
        previous.addActionListener(this);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSong();
                System.exit(0);
            }
        });
        if (list.getItemCount() == 0) {
            saveList = Utils.load();
            if (saveList != null) {
                for (String s : saveList) {
                    Utils.findAll(list, s, map);
                }
            }
        }
    }

    public static void main(String[] args) {
        new MusicPlayer();
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
            Utils.findAll(list, Location, map);
            changed = true;
            saveSong();
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
        }
        try {
            audio = FactoryRegistry.systemRegistry().createAudioDevice();
            player = new AdvancedPlayer(new FileInputStream(map.get(musicName)), audio);
            totalTime = Utils.getMp3Time(map.get(musicName));
            frame = (totalTime * 1000) / 26;
            System.out.println(musicName + "一共" + frame + "帧");
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
            while (audio != null) {
                leftLabel.setText(Utils.secToTime(audio.getPosition() / 1000));
                slider.setValue(audio.getPosition() / 1000);
            }
        });
        time.start();
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

    public void playbackStarted(PlaybackEvent evt) {
        System.out.println("开始了");
    }

    public void playbackFinished(PlaybackEvent evt) {
        if (audio == null) {
            next();
        }
    }
}