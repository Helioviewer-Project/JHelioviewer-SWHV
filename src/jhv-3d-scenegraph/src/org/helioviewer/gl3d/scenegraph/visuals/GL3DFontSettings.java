package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.HashMap;



public class GL3DFontSettings {
    private HashMap<String, String> fonts = new HashMap<String, String>();

    private static GL3DFontSettings instance = new GL3DFontSettings();
    private boolean initialized = false;
    
    private  GL3DFontSettings() {
    	this.setup();
    }
    public static GL3DFontSettings getSingletonInstance(){
    	return instance;
    }
    
	private void setup() {
		this.fonts.put("default", "resources/fonts/default.png");
	}
	
	public String getFont(String key){
		if(fonts.containsKey(key)){
			return fonts.get(key);
		}
		return fonts.get("default");
	}
	
	public HashMap<String, String> getFonts() {
		return this.fonts;
	}
}
