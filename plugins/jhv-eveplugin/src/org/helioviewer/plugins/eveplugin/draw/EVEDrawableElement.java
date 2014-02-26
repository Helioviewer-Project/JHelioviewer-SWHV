package org.helioviewer.plugins.eveplugin.draw;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.EVEDrawControllerListener;
import org.helioviewer.plugins.eveplugin.controller.EVEValues;
import org.helioviewer.plugins.eveplugin.model.EVEValue;

public class EVEDrawableElement implements DrawableElement{
	
	private final List<GraphPolyline> graphPolylines = Collections.synchronizedList(new LinkedList<EVEDrawableElement.GraphPolyline>());
	private boolean intervalAvailable = false;
    private Band[] bands = new Band[0];
    private EVEValues[] values = null;
    private Interval<Date> interval;
    private YAxisElement yAxisElement;  
	
	public EVEDrawableElement(Interval<Date> interval, Band[] bands,
			EVEValues[] values, YAxisElement yAxisElement){
		this.interval = interval;
		this.bands = bands;
		this.values = values;
		this.yAxisElement = yAxisElement;
		intervalAvailable = interval.getStart() != null && interval.getEnd() != null; 
	}

	public EVEDrawableElement(){
		this.interval = new Interval<Date>(Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
		this.bands = new Band[0];
		this.values = new EVEValues[0];
		this.yAxisElement = new YAxisElement();
	}
	
	@Override
	public DrawableElementType getDrawableElementType() {
		return DrawableElementType.LINE;
	}

	@Override
	public void draw(Graphics g, Rectangle graphArea) {
		// TODO Auto-generated method stub
		updateGraphsData(interval, graphArea);
		drawGraphs(g, graphArea);
	}

	private void updateGraphsData(Interval <Date> interval,Rectangle graphArea) {
		double logMinValue = Math.log10(yAxisElement.getSelectedRange().min);
        double logMaxValue = Math.log10(yAxisElement.getSelectedRange().max);
		
		double ratioX = !intervalAvailable ? 0 : (double)graphArea.width / (double)(interval.getEnd().getTime() - interval.getStart().getTime());
        double ratioY = logMaxValue < logMinValue ? 0 : graphArea.height / (logMaxValue - logMinValue);
		
        graphPolylines.clear();
        //if (!intervalAvailable)
        //    return;
        
        for (int i = 0; i < bands.length; ++i) {
        	if(bands[i].isVisible()){
	            final EVEValue[] eveValues = values[i].getValues();
	            final ArrayList<Point> pointList = new ArrayList<Point>();
	            final LinkedList<Integer> warnLevels = new LinkedList<Integer>();
	            HashMap<String, Double> unconvertedWarnLevels = bands[i].getBandType().getWarnLevels();
	
	            Iterator<Entry<String, Double>> it = unconvertedWarnLevels.entrySet().iterator();
	            while (it.hasNext()) {
					Map.Entry<String, Double> pairs = it.next();
	                warnLevels.add(computeY(pairs.getValue(),interval, graphArea,ratioY, logMinValue));
	                //it.remove(); // avoids a ConcurrentModificationException
	            }
	
	
	            int counter = 0;
	            
	            for (int j = 0; j < eveValues.length; j++) {
	                final Double value = eveValues[j].getValue();
	                
	                if (value == null) {
	                    if (counter > 1) {
	                        graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels));
	                    }
	                    
	                    pointList.clear();
	                    counter = 0;
	                    
	                    continue;
	                }
	                
	                final int x = computeX(eveValues[j].getDate(), interval, graphArea, ratioX);
	                final int y = computeY(eveValues[j].getValue().doubleValue(),interval, graphArea,ratioY, logMinValue);
	                final Point point = new Point(x, y);
	                
	                if (graphArea.contains(point)) {
	                    pointList.add(point);
	                    counter++;
	                }
	            }
	
	            
	            if (counter > 0) {
	                graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels));
	            }
        	}
        }
	}
	
	private void drawGraphs(final Graphics g, Rectangle graphArea) {
        //if (!intervalAvailable)
        //    return;
    	synchronized(graphPolylines){
    		Iterator<GraphPolyline> i = graphPolylines.iterator();
    		while (i.hasNext()) {
    			GraphPolyline line = i.next();
	            g.setColor(line.color);
	            g.drawPolyline(line.xPoints, line.yPoints, line.numberOfPoints);
	            for(int j=0;j<line.warnLevels.length; j++){
	            	g.drawLine(graphArea.x,line.warnLevels[j], graphArea.x + graphArea.width, line.warnLevels[j]);
	            }
	        }
    	}
    }
	
    private int computeX(Date orig, Interval <Date> interval, Rectangle graphArea, double ratioX){
        return (int)((orig.getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
    }
        
    private int computeY(double orig, Interval <Date> interval, Rectangle graphArea, double ratioY, double logMinValue){
        return graphArea.y + graphArea.height - (int)(ratioY * (Math.log10(orig) - logMinValue));
    }
    
 // //////////////////////////////////////////////////////////////////////////////
    // Graph Polyline
    // //////////////////////////////////////////////////////////////////////////////
    
    public class GraphPolyline {
    
        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////
        
        public final int numberOfPoints;
        public final int numberOfWarnLevels;
        public final int[] xPoints;
        public final int[] yPoints;
        public final int[] warnLevels;
        
        public final Color color;
        
        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////
        
        public GraphPolyline(final List<Point> points, final Color color, final List<Integer> warnLevels) {
            this.numberOfPoints = points.size();
            this.numberOfWarnLevels = warnLevels.size();
            this.xPoints = new int[numberOfPoints];
            this.yPoints = new int[numberOfPoints];
            this.color = color;
            this.warnLevels = new int[numberOfWarnLevels];
            
            int counter = 0;
            for(final Point point : points) {
                xPoints[counter] = point.x;
                yPoints[counter] = point.y;
                counter++;
            }
            
            counter = 0;
            for(final Integer warnLevel : warnLevels) {
            	this.warnLevels[counter] = warnLevel;
                counter++;
            }
        }
    }

	@Override
	public void setYAxisElement(YAxisElement yAxisElement) {
		this.yAxisElement = yAxisElement;
	}

	@Override
	public YAxisElement getYAxisElement() {
		return this.yAxisElement;
	}

	@Override
	public boolean hasElementsToDraw() {
		return bands.length > 0;
	}

	public void set(Interval<Date> interval, Band[] bands,
			EVEValues[] values, YAxisElement yAxisElement){
		this.interval = interval;
		this.bands = bands;
		this.values = values;
		this.yAxisElement = yAxisElement;
		intervalAvailable = interval.getStart() != null && interval.getEnd() != null; 
	}
}
