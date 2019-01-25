package main.java.music;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;

import java.awt.*;
import java.io.*;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

import javax.swing.*;


//mp3,wma,ape,wav,midi
public class Utils {
    public static void findAll(java.awt.List list, String path, Map<String, String> songMap,
            Map<String, String> lrcMap) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findAll(list, file.getAbsolutePath(), songMap, lrcMap);
                } else {
                    String name = file.getName();
                    if (name.endsWith(".mp3") || name.endsWith(".wma") || name.endsWith(".ape") ||
                            name.endsWith(".wav") || name.endsWith(".midi")) {
                        String success =
                                songMap.put(shortCut(file.getName()), file.getAbsolutePath());
                        if (success == null) {
                            list.add(shortCut(file.getName()));
                        }
                    }
                    if (name.endsWith(".lrc")) {
                        lrcMap.put(shortCut(file.getName()), file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static String shortCut(String string) {
        String cut;
        String[] strs = string.split("-");
        if (strs.length > 1) {
            cut = strs[1].substring(0, strs[1].lastIndexOf(".")).trim();
        } else {
            cut = strs[0].substring(0, strs[0].lastIndexOf(".")).trim();
        }
        return cut;
    }

    private static String open() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.showOpenDialog(new Frame());
        File path = fc.getSelectedFile();
        if (path == null) {
            return null;
        }
        return path.getAbsolutePath();
    }

    private static void save(java.util.List list) {
        File file = new File("musicPath.txt");
        writeList(list, file);
    }

    private static void writeList(java.util.List<String> list, File file) {
        try (PrintWriter out = new PrintWriter(file, "utf-8")) {
            for (String str : list) {
                out.println(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static java.util.List<String> load() {

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

    public static int getMp3Time(String mp3File) {
        try {
            MP3File f = (MP3File) AudioFileIO.read(new File(mp3File));
            MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
            return audioHeader.getTrackLength();
        } catch (Exception e) {
            return -1;
        }
    }

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
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }


    public static Map<String, String> readLRC(String path) {
        BufferedReader reader = null;
        Map<String, String> map = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            String str;
            map = new HashMap<>();
            while ((str = reader.readLine()) != null) {
                if (str.contains("[") && str.contains("]")) {
                    String[] s = str.split(":");
                    if (str.contains("ti")) {
                        map.put("ti", s[1].replace("]", ""));
                    } else if (str.contains("ar")) {
                        map.put("ar", s[1].replace("]", ""));
                    } else if (str.contains("al")) {
                        map.put("al", s[1].replace("]", ""));
                    } else if (str.contains("by")) {
                        map.put("by", s[1].replace("]", ""));
                    } else if (str.contains("offset")) {
                        map.put("offset", s[1].replace("]", ""));
                    } else {
                        String[] s1 = str.split("\\.");
                        String time = s1[0].replace("[", "");
                        String[] s2 = s1[1].split("]");
                        String lrc = s2.length > 1 ? s2[1] : "";
                        if (lrc.isEmpty()) {
                            continue;
                        }
                        map.put(time, lrc);
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

    public static boolean deleteSong(String name) {
        File file = new File(name);
        return file.delete();
    }

    public static String getHeader(Map<String, String> map) {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("  ").append("标题:").append(map.get("ti")).append("\n");
        stringBuffer.append("  ").append("作者:").append(map.get("ar")).append("\n");
        stringBuffer.append("  ").append("歌词制作:").append("QAQ").append("\n");
        return stringBuffer.toString();
    }
}

