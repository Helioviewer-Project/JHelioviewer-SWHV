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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.media.opengl.GL;
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

    public void loadFont(String font, GL gl) {
        int texture_id;
        if (!loadedFontsTextureId.containsKey(font)) {
            BufferedImage img = getFontBufferedImage(font);

            GLTextureHelper th = new GLTextureHelper();
            texture_id = th.genTextureID(gl);

            DataBuffer rawBuffer = img.getRaster().getDataBuffer();
            IntBuffer buffer = IntBuffer.wrap(((DataBufferInt) rawBuffer).getData());

            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

            gl.glBindTexture(GL.GL_TEXTURE_2D, texture_id);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, 256, 512, 0, GL.GL_BGRA, GL.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);

            loadedFontsTextureId.put(font, texture_id);
        }
        texture_id = loadedFontsTextureId.get(font);

        gl.glBindTexture(GL.GL_TEXTURE_2D, texture_id);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    }

    public RectangleDouble[] getCharacters(String fontName){
        if (!this.loadedFontsCharacterPosition.containsKey(fontName)) {
            this.getFontBufferedImage(fontName);
        }
        return this.loadedFontsCharacterPosition.get(fontName);

    }

    private BufferedImage getFontBufferedImage(String font) {
        BufferedImage img = new BufferedImage(256, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        //g2d.setPaintMode();
        g2d.setColor(new Color(0.f, 0.f, 0.f, 1.0f));
        g2d.fillRect(0, 0, 256, 512);
        g2d.setColor(new Color(1.f, 0.f, 0.f, 1.f));

        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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

        BufferedImage img = GL3DFont.getSingletonInstance().getFontBufferedImage("Monospace");
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.pack();
        frame.setVisible(true);
      }


}
