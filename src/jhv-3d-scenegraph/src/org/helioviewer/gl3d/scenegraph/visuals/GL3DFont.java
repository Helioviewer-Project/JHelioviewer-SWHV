package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.net.URL;

import java.util.HashMap;


import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;


public class GL3DFont {

	static GL3DFont instance = new GL3DFont();
	HashMap<String, Integer> loadedFonts = new HashMap<String, Integer>();
	
    public static GL3DFont getSingletonInstance() {
        if (instance == null) {
        	instance = new GL3DFont();
        }
        return instance;
    }
    

	public void loadFont(String font, GL gl) {
		int texture_id;
		if(!loadedFonts.containsKey(font)){
			System.out.print(font);
			
			BufferedImage img = getFontBufferedImage(font);
    		GLTextureHelper th = new GLTextureHelper();
	    	texture_id = th.genTextureID(gl);
	    	th.moveBufferedImageToGLTexture(gl, img, texture_id);
	    	loadedFonts.put(font, texture_id);
		}
		texture_id = loadedFonts.get(font);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture_id);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);	
	}
	
    private  BufferedImage getFontBufferedImage(String font) {
		try {
			URL url;
			BufferedImage img;
			
			GL3DFontSettings settt = GL3DFontSettings.getSingletonInstance();
			String fontt = GL3DFontSettings.getSingletonInstance().getFont(font);
			System.out.print(fontt);
			url = GL3DFont.class.getResource(fontt);
			img = ImageIO.read(url);
			return img;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
        
    }
	
}
