package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;


public class GL3DFont {

    static GL3DFont instance = new GL3DFont();
    HashMap<String, Integer> loadedFontsTextureId = new HashMap<String, Integer>();
    HashMap<String, RectangleDouble[]> loadedFontsCharacterPosition = new HashMap<String, RectangleDouble[]>();
    int fontSize = 64;
    public static GL3DFont getSingletonInstance() {
        if (instance == null) {
            instance = new GL3DFont();
        }
        return instance;
    }

    public void updateFont(String font, GL2 gl, Color textColor, Color backGroundColor) {
        int texture_id;
        GLTextureHelper th = new GLTextureHelper();
        if (!loadedFontsTextureId.containsKey(font)) {
            texture_id = th.genTextureID(gl);
        }
        else{
            texture_id = loadedFontsTextureId.get(font);
        }
        BufferedImage img = getFontBufferedImage(font, textColor, backGroundColor);
        th.moveBufferedImageToGLTexture(gl, img, texture_id);
        loadedFontsTextureId.put(font, texture_id);
    }

    public void bindFont(String font, GL2 gl) {
        int texture_id = loadedFontsTextureId.get(font);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture_id);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
    }

    public RectangleDouble[] getCharacters(String fontName, Color textColor, Color backgroundColor){
        if (!this.loadedFontsCharacterPosition.containsKey(fontName)) {
            this.getFontBufferedImage(fontName, textColor, backgroundColor);
        }
        return this.loadedFontsCharacterPosition.get(fontName);
    }

    private BufferedImage getFontBufferedImage(String font, Color textColor, Color backGroundColor) {
        BufferedImage img = new BufferedImage(256, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.setColor(backGroundColor);
        g2d.fillRect(0, 0, 256, 512);
        g2d.setColor(textColor);

        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setFont(new Font(font, Font.PLAIN, fontSize));
        RectangleDouble[] rects= new RectangleDouble[256];
        FontMetrics fm = g2d.getFontMetrics();

        int maxWidth = 0;
        int maxHeight = 0;
        int width;
        int height;
        for(int i=0; i<256; i++){
            char c = ((char)i);
            String s = Character.toString(c);
            Rectangle2D sb = fm.getStringBounds(s, g2d);
            width =(int)sb.getWidth();
            if(width>maxWidth){
                maxWidth = width;
            }
            height = (int)sb.getHeight();
            if(height>maxHeight){
                maxHeight = height;
            }
        }
        int currentX = 0;
        int currentY = maxHeight;

        char c;
        String s ;
        for(int i=0; i<256; i++){
            c = ((char)i);
            s = Character.toString(c);
            Rectangle2D sb = fm.getStringBounds(s, g2d);
            width =(int)sb.getWidth();
            height = (int)sb.getHeight();

            if(currentX + width>=256){
                currentX = 0;
                currentY +=maxHeight;
            }
            RectangleDouble rect = new org.helioviewer.base.math.RectangleDouble((currentX)/256., (currentY + fontSize/5.)/512., width/256., height/512.);
            rects[i] = rect;
            g2d.drawString(s, currentX, currentY);


                currentX += width;
        }
        loadedFontsCharacterPosition.put(font, rects);
        for(int i=0; i<256; i++){
            //System.out.println(rects[i]);
        }
        return img;
    }


      public static void main(String[] args)
      {
        String fonts[] =
          GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        for ( int i = 0; i < fonts.length; i++ )
        {
          System.out.println(fonts[i]);
        }
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());

        BufferedImage img = GL3DFont.getSingletonInstance().getFontBufferedImage("Monospace", new Color(1.f, 1.f, 1.f, 1.f), new Color(0.f, 0.f, 0.f, 0.f));
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.pack();
        frame.setVisible(true);
      }


}
