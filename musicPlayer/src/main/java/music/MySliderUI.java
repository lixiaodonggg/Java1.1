package main.java.music;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;

public class MySliderUI extends BasicSliderUI {
    private static final Color BACKGROUND01=	new Color(	255,76,95);   //new Color(0,30,255);
    private static final Color BACKGROUND02=new Color(234,236,246);
    public MySliderUI(JSlider arg0) {
        super(arg0);
    }
    @Override
    public void paintThumb(Graphics g){
        Graphics2D g2d=(Graphics2D) g;
        BasicStroke stroke=new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
        g2d.setStroke(stroke);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp=new GradientPaint(0,0,BACKGROUND02,0,thumbRect.height,BACKGROUND01);
        g2d.setPaint(gp);
        g2d.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 10, 10);
        BasicStroke stroke1=new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
        g2d.setStroke(stroke1);
        g2d.drawLine(8, thumbRect.height/2,thumbRect.x+8 , thumbRect.height/2);


    }
    @Override
    public void paintTrack(Graphics g){
        Graphics2D g2d=(Graphics2D) g;
        // 设定渐变
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.3f));
        g2d.setPaint(new GradientPaint(0, 0,BACKGROUND02 , 0,trackRect.height,BACKGROUND01, true));
        g2d.setStroke(new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2d.drawLine(8, trackRect.height/2+1, trackRect.width+8, trackRect.height/2+1);
    }
}
