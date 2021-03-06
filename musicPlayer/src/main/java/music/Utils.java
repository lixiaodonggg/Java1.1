package main.java.music;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;


//mp3,wma,ape,wav,midi
public final class Utils {

    /**
     * 将歌曲路径加载到歌曲列表中
     */
    protected static void findAll(String path,
                                  Map<String, String> songMap, Map<String, String> lrcMap) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findAll(file.getAbsolutePath(), songMap, lrcMap);
                } else {
                    String name = file.getName();
                    if (name.endsWith(".mp3") || name.endsWith(".wma") || name.endsWith(".ape") ||
                            name.endsWith(".wav") || name.endsWith(".midi")) {
                        songMap.put(shortCut(file.getName()), file.getAbsolutePath());
                    }
                    if (name.endsWith(".lrc")) {
                        lrcMap.put(shortCut(file.getName()), file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * 对歌曲名称的缩短
     */
    private static String shortCut(String string) {
        String[] str = string.split("-");
        String musicName = "";
        if (str.length > 1) {
            for (String s : str) {
                if (s.contains(".")) {
                    musicName = s.substring(0, s.lastIndexOf(".")).trim();
                    break;
                }
            }
        } else {
            musicName = str[0].substring(0, str[0].lastIndexOf(".")).trim();
        }
        return musicName;
    }

    /**
     * 打开选择的面板
     */
    public static String open() {
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.png");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        JFrame openFrame = new JFrame();
        openFrame.setIconImage(image.getImage());
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.showOpenDialog(openFrame);
        File path = fc.getSelectedFile();
        if (path == null) {
            return null;
        }
        return path.getAbsolutePath();
    }

    /**
     * 保存歌曲文件夹的路径
     */
    protected static void save(List<String> list) {
        File file = new File("musicPath.txt");
        writeList(list, file);
    }

    /**
     * 将歌曲文件夹路径写入
     */
    private static void writeList(List<String> list, File file) {
        try (PrintWriter out = new PrintWriter(file, "utf-8")) {
            for (String str : list) {
                out.println(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载路径列表
     */
    protected static java.util.List<String> load() {

        File file = new File("musicPath.txt");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return readList(file);
    }

    /**
     * 读取列表
     */
    private static java.util.List<String> readList(File file) {
        BufferedReader ios = null;
        java.util.List<String> list = new ArrayList<>();
        try {
            ios = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String s;
            while ((s = ios.readLine()) != null) {
                list.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert ios != null;
                ios.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 获得歌曲的信息
     */
    public static int getMp3Time(String mp3File) {
        try {
            MP3File f = (MP3File) AudioFileIO.read(new File(mp3File));
            MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
            return audioHeader.getTrackLength();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 改变时间的格式
     */
    public static String secToTime(int time) {
        String timeStr;
        int hour;
        int minute;
        int second;
        if (time <= 0) {
            return "00:00";
        } else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) {
                    return "99:59:59";
                }
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(int i) {
        String retStr;
        if (i >= 0 && i < 10) {
            retStr = "0" + i;
        } else {
            retStr = "" + i;
        }
        return retStr;
    }

    /**
     * 对时间字符串的解析
     */
    public static int parseTime(String time) {
        String[] times = time.split(":");
        assert times.length <= 3;
        int t = 0;
        if (times.length == 3) {
            t = Integer.parseInt(times[0]) * 60 * 60 + Integer.parseInt(times[1]) * 60 +
                    Integer.parseInt(times[2]);
        } else if (times.length == 2) {
            t = Integer.parseInt(times[0]) * 60 + Integer.parseInt(times[1]);
        }
        return t;
    }

    /**
     * 读取歌词文件的方法
     */
    public static Map<Integer, String> readLRC(String path) {
        BufferedReader reader = null;
        Map<Integer, String> map = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            String str;
            map = new HashMap<>();
            while ((str = reader.readLine()) != null) {
                if (str.contains("[") && str.contains("]")) {
                    String[] s = str.split(":");
                    switch (s[0].replace("[", "")) {
                        case "ti":
                            map.put("ti".hashCode(), s[1].replace("]", ""));
                            break;
                        case "ar":
                            map.put("ar".hashCode(), s[1].replace("]", ""));
                            break;
                        case "al":
                            map.put("al".hashCode(), s[1].replace("]", ""));
                            break;
                        case "by":
                            map.put("by".hashCode(), s[1].replace("]", ""));
                            break;
                        case "offset":
                            map.put("offset".hashCode(), s[1].replace("]", ""));
                            break;
                        default:
                            String[] s1 = str.split("\\.");
                            String time;
                            String lrc;
                            if (s1.length > 1) {
                                time = s1[0].replace("[", "");
                                String[] s2 = s1[1].split("]");
                                lrc = s2.length > 1 ? s2[1] : "";
                            } else {
                                String[] s2 = s1[0].split("]");
                                lrc = s2.length > 1 ? s2[1] : "";
                                time = s2[0].replace("[", "");
                            }
                            map.put(parseTime(time), lrc);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * 删除文件
     */
    public static boolean deleteSong(String name) {
        File file = new File(name);
        return file.delete();
    }
}

