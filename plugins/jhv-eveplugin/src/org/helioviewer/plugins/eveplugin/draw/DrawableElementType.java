package org.helioviewer.plugins.eveplugin.draw;

public enum DrawableElementType {
	LINE(DrawableType.LINE),
	RADIO(DrawableType.FULL_IMAGE),
	ICON(DrawableType.ICON),
	EVENT(DrawableType.TEXT_ICON);
	
	private DrawableType type;
	
	DrawableElementType(DrawableType type) {
		this.type = type;
	}
	
	public DrawableType getLevel(){
		return type;
	}
	
	public void setLevel(DrawableType type){
		this.type = type;
	}
	
	
}
