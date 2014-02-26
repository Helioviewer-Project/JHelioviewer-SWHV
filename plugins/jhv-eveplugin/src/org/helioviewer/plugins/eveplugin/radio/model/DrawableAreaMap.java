package org.helioviewer.plugins.eveplugin.radio.model;

public class DrawableAreaMap {
	private int sourceX0;
	private int sourceY0;
	private int sourceX1;
	private int sourceY1;
	private int destinationX0;
	private int destinationY0;
	private int destinationX1;
	private int destinationY1;
	private long ID;
	
	public DrawableAreaMap(int sourceX0, int sourceY0, int sourceX1,
			int sourceY1, int destinationX0, int destinationY0,
			int destinationX1, int destinationY1, long ID) {
		super();
		this.sourceX0 = sourceX0;
		this.sourceY0 = sourceY0;
		this.sourceX1 = sourceX1;
		this.sourceY1 = sourceY1;
		this.destinationX0 = destinationX0;
		this.destinationY0 = destinationY0;
		this.destinationX1 = destinationX1;
		this.destinationY1 = destinationY1;
		this.ID = ID;
	}	
	
	public long getID() {
		return ID;
	}


	public void setID(long iD) {
		ID = iD;
	}


	public int getSourceX0() {
		return sourceX0;
	}
	
	public void setSourceX0(int sourceX0) {
		this.sourceX0 = sourceX0;
	}
	
	public int getSourceY0() {
		return sourceY0;
	}
	
	public void setSourceY0(int sourceY0) {
		this.sourceY0 = sourceY0;
	}
	
	public int getSourceX1() {
		return sourceX1;
	}
	
	public void setSourceX1(int sourceX1) {
		this.sourceX1 = sourceX1;
	}
	
	public int getSourceY1() {
		return sourceY1;
	}
	
	public void setSourceY1(int sourceY1) {
		this.sourceY1 = sourceY1;
	}
	
	public int getDestinationX0() {
		return destinationX0;
	}
	
	public void setDestinationX0(int destinationX0) {
		this.destinationX0 = destinationX0;
	}
	
	public int getDestinationY0() {
		return destinationY0;
	}
	
	public void setDestinationY0(int destinationY0) {
		this.destinationY0 = destinationY0;
	}
	
	public int getDestinationX1() {
		return destinationX1;
	}
	
	public void setDestinationX1(int destinationX1) {
		this.destinationX1 = destinationX1;
	}
	
	public int getDestinationY1() {
		return destinationY1;
	}
	
	public void setDestinationY1(int destinationY1) {
		this.destinationY1 = destinationY1;
	}
	
	@Override
	public String toString(){
		return "[source: x0: "+ sourceX0 +" , y0: "+ sourceY0 +" , x1: "+ sourceX1 +", y1: "+ sourceY1 +", width: "+ (sourceX1-sourceX0) +", heigth "+(sourceY1-sourceY0)+";]" +
			   "[destination: x0: "+destinationX0+", y0: "+destinationY0+", x1: "+destinationX1+", y1: "+destinationY1+", width: "+(destinationX1-destinationX0)+", height:"+(destinationY1-destinationY0)+"]";
	}
}
