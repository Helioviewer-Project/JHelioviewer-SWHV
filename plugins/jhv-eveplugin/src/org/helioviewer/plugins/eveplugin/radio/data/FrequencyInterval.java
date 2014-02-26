package org.helioviewer.plugins.eveplugin.radio.data;

/**
 * @author bramb
 *
 */
public class FrequencyInterval {
	private int start;
	private int end;
	
	public static final FrequencyInterval ALL = new FrequencyInterval(Integer.MIN_VALUE, Integer.MAX_VALUE);
	
	public FrequencyInterval(){
		this.start = 0;
		this.end = 0;
	}
	
	public FrequencyInterval(int start, int end){
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}
	
	public boolean equals(FrequencyInterval interval){
		return interval.getStart() == start && interval.getEnd() == end; 
	}
	
	public boolean overlaps(FrequencyInterval otherInterval){
		return !(this.getStart() > otherInterval.getEnd() || this.getEnd() < otherInterval.getStart());
	}
}
