package main.java.music;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

class MusicFrame implements ActionListener {

    JFrame frame;
    JButton input;
    JLabel song;
    JButton play;
    JButton stop;
    JButton next;
    JButton previous;
    JButton delete;
    JButton deleteFile;
    JButton lrcButton; //歌词开关按钮
    JComboBox<String> modeBox; //模式选项
    JSlider slider;    //进度条
    JLabel leftLabel;
    JLabel rightLabel;
    JScrollPane scrollPaneList;
    DefaultListModel<String> list = new DefaultListModel<>();
    JList<String> jList = new JList<>(list);
    JFrame lrcFrame;
    private Point lrcXY;
    JLabel lrcLabel;

    /**
     * 主面板
     */
    public void mainFrame() {
        frame = new JFrame("GFMusic");
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.png");
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
     * 歌词面板
     */
    private JFrame createLrcFrame() {
        URL resource = MusicPlayer.class.getClassLoader().getResource("icon.jpg");
        assert resource != null;
        ImageIcon image = new ImageIcon(resource);
        JFrame lrcFrame = new JFrame("歌词");
        lrcFrame.setIconImage(image.getImage());
        lrcFrame.setBounds(400, 900, 1000, 60);
        lrcLabel = new JLabel("歌词", JLabel.CENTER);
        lrcLabel.setForeground(new Color(51, 51, 51));
        lrcLabel.setFont(new Font("微软雅黑", Font.PLAIN, 38));
        lrcFrame.add(lrcLabel);
        lrcFrame.setUndecorated(true);
        lrcFrame.setBackground(new Color(0, 0, 0, 0));
        lrcXY = new Point();
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
     * 歌曲列表面板
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
        jList.setSelectionBackground(new Color(255, 128, 128));
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
        song.setForeground(new Color(255, 128, 128));
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
        lrcPanel.add(sliderPanel, BorderLayout.SOUTH);
        return lrcPanel;
    }

    /**
     * 按钮样式
     */
    private void setButton(JButton button) {
        String pic = "default.png";
        String press = "press.png";
        String roll = "put.png";
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

    /**
     * 按钮面板
     */
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

    /**
     * 按钮面板
     */
    private void buttonBoxInit() {
        input = new JButton("导入");
        play = new JButton("播放");
        stop = new JButton("停止");
        next = new JButton("下一首");
        previous = new JButton("上一首");
        delete = new JButton("删除");
        deleteFile = new JButton("删除文件");
        lrcButton = new JButton("歌词:开");
        setButton(play);
        setButton(previous);
        setButton(next);
        setButton(stop);
        setButton(input);
        setButton(delete);
        setButton(deleteFile);
        setButton(lrcButton);
        String[] modeName = {"顺序播放", "单曲循环", "随机播放"};
        modeBox = new JComboBox<>(modeName);
        modeBox.setBackground(new Color(255, 255, 255));
        modeBox.setPreferredSize(new Dimension(85, 25));
        modeBox.setOpaque(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    class MySliderUI extends BasicSliderUI {
        private final Color BACKGROUND01 = new Color(255, 76, 95);   //new Color(0,30,255);
        private final Color BACKGROUND02 = new Color(234, 236, 246);

        MySliderUI(JSlider arg0) {
            super(arg0);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, BACKGROUND02, 0, thumbRect.height, BACKGROUND01);
            g2d.setPaint(gp);
            g2d.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 10, 10);
            BasicStroke stroke1 = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke1);
            g2d.drawLine(8, thumbRect.height / 2, thumbRect.x + 8, thumbRect.height / 2);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            // 设定渐变
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2d.setPaint(
                    new GradientPaint(0, 0, BACKGROUND02, 0, trackRect.height, BACKGROUND01, true));
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(8, trackRect.height / 2 + 1, trackRect.width + 8, trackRect.height / 2 + 1);
        }
    }
}