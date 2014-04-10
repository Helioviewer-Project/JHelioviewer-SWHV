package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;


import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.DrawControllerListener;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValue;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawControllerListener;
import org.helioviewer.plugins.eveplugin.model.ChartModel;
import org.helioviewer.plugins.eveplugin.model.ChartModelListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;
import org.helioviewer.plugins.eveplugin.radio.model.DrawableAreaMap;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModelListener;
import org.helioviewer.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawGraphPane.GraphPolyline;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.plugins.eveplugin.EVEPlugin;

/**
 * 
 * @author Stephan Pagel
 * */
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener,ChartModelListener{//,RadioPlotModelListener {//EVEDrawControllerListener, MouseInputListener, ComponentListener, RadioPlotModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;
    
    private DrawController drawController;
    
    //private final EVEDrawController drawController;
    private GraphEvent [] events;
    //private Interval<Date> interval = new Interval<Date>(null, null);
    //private boolean intervalAvailable = false;
    //private Band[] bands = new Band[0];
    //private EVEValues[] values = null;
    private Map<YAxisElement, Double> yRatios; 
    
    private final List<GraphPolyline> graphPolylines = Collections.synchronizedList(new LinkedList<ChartDrawGraphPane.GraphPolyline>());
    
    //private double logMinValue = Math.log10(Double.MAX_VALUE);
    //private double logMaxValue = Math.log10(Double.MIN_VALUE);
    
    private Date movieTimestamp = null;
    private int movieLinePosition = -1;
    
    private Point mousePressedPosition = null;
    private Point mouseDragPosition = null;
    private boolean mousePressedOnMovieFrame = false;
    
    private Rectangle graphArea = new Rectangle();
    private Rectangle plotArea = new Rectangle();
    private double ratioX = 0;
    //private double ratioY = 0;
    
    private int lastKnownWidth = -1;
    private int lastKnownHeight = -1;
	public RadioImagePane pane;

    private BufferedImage screenImage = null;
       
    private ZoomManager zoomManager;
    
    private String identifier;
    
    private int twoYAxis = 0;
    
    private ChartModel chartModel;
    
    private PlotAreaSpaceManager plotAreaSpaceManager;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public ChartDrawGraphPane(String identifier) {
    	chartModel = ChartModel.getSingletonInstance();
        //this.drawController = drawController;
    	this.identifier = identifier;
        this.drawController = DrawController.getSingletonInstance();
        initVisualComponents();
        
        //drawController.addDrawControllerListener(this);
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
		pane = new RadioImagePane();
        
        //radioPlotModel = RadioPlotModel.getSingletonInstance();
        //radioPlotModel.addRadioPlotModelListener(this);
        zoomManager = ZoomManager.getSingletonInstance();
        yRatios = new HashMap<YAxisElement, Double>();
        this.drawController.addDrawControllerListener(this,identifier);
        chartModel.addChartModelListener(this);
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
    }
    
    private void initVisualComponents() {
        setOpaque(true);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
    	synchronized (this) {
        	Graphics2D g2 = (Graphics2D)g;
        	//Log.debug("Paint component called");
            super.paintComponent(g);
            
            if (lastKnownWidth != getWidth() || lastKnownHeight != getHeight()) {
                updateGraph();
                
                lastKnownWidth = getWidth();
                lastKnownHeight = getHeight();
            }
            if (screenImage != null) {
            	//Log.debug("Width : "+ getWidth());
            	//Log.debug("Height : " + getHeight());
            	//Log.debug("SCreenWidth : "+ screenImage.getWidth());
            	//Log.debug("ScreenHeight : "+ screenImage.getHeight());
            	g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(screenImage, 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
                
                drawMovieLine(g);
            }
		}
        
    }
    
    private void updateGraph() {
        updateDrawInformation();
        redrawGraph();
    }
    
    private void redrawGraph() {
    	/*Log.debug("redrawGraph called");
    	try {
			throw new Exception();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    	int width = getWidth();
    	int height = getHeight();
        if (width > 0 && height > 0 
        		&& ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE < height
        		&& ChartConstants.GRAPH_LEFT_SPACE + ChartConstants.GRAPH_LEFT_SPACE + ChartConstants.GRAPH_RIGHT_SPACE < width) {
            screenImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
            //Log.debug("--------------------Screen image : " + screenImage+"--------------------");
            //Log.debug("----------------------------this : " + System.identityHashCode(this));
            final Graphics2D g = screenImage.createGraphics();
            drawBackground(g);
            //drawRadio(g);
            BufferedImage plotPart = screenImage.getSubimage(ChartConstants.GRAPH_LEFT_SPACE, ChartConstants.GRAPH_TOP_SPACE, width-ChartConstants.GRAPH_LEFT_SPACE-ChartConstants.GRAPH_RIGHT_SPACE, height-ChartConstants.GRAPH_TOP_SPACE-ChartConstants.GRAPH_BOTTOM_SPACE);
            
            //drawGraphs(g); 
            //drawEvents(g);
            drawData(plotPart.createGraphics());
            drawLabels(g);
            drawZoomBox(g);
        }
        //Log.debug("RedrawGraph finished");
    }
    
    private void drawData(Graphics2D g){
    	Map <DrawableType, List<DrawableElement>> drawableElements = drawController.getDrawableElements(identifier);
    	List<DrawableType> drawTypeList = DrawableType.getZOrderedList();
    	for(DrawableType dt : drawTypeList){
    		List<DrawableElement> del = drawableElements.get(dt);
    		if (del != null){
    			//Log.debug("grapArea when data should be drawn : "+ graphArea);
    			synchronized(del){
    				for (DrawableElement de : del ){
    					de.draw(g, plotArea);    				
    				}
    			}
    		}
    	}
    }
    
    private void updateDrawInformation() {
        updateGraphArea();
        updateRatios();
        //updateGraphsData();
        updateMovieLineInformation();
        updateGraphEvents();
    }
    
    private void drawBackground(final Graphics2D g) {
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());        
        if (mousePressedPosition != null && mouseDragPosition != null) {
            g.setColor(ChartConstants.UNSELECTED_AREA_COLOR);
            g.fillRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);
        }
    }
    
    private void drawZoomBox(final Graphics2D g) {
        if (mousePressedPosition == null || mouseDragPosition == null || mousePressedOnMovieFrame)
            return;
        
        final int x = mousePressedPosition.x < mouseDragPosition.x ? mousePressedPosition.x : mouseDragPosition.x;
        final int y = mousePressedPosition.y < mouseDragPosition.y ? mousePressedPosition.y : mouseDragPosition.y;
        final int width = Math.abs(mouseDragPosition.x - mousePressedPosition.x);
        final int height = Math.abs(mouseDragPosition.y - mousePressedPosition.y);
        
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.drawRect(x, y, width, height);
    }

    private void drawLabels(final Graphics2D g) {
    	Set<YAxisElement> yAxisElements = drawController.getYAxisElements(identifier);
    	Interval <Date> interval = drawController.getInterval();
    	if (!drawController.getIntervalAvailable())
    		return;       
    	        
    	
    	// draw vertical ticks
    	int counter = 0;
    	synchronized (yAxisElements) {	
	    	for(YAxisElement yAxisElement : yAxisElements){
	    		drawVerticalLabels(g, yAxisElement, counter==0?0:1);
	    		if (counter > 1){
	    			break;
	    		} 
	    		counter++;
	    	}
    	}
        
    		// draw horizontal ticks and labels
    	final Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(ChartConstants.FULL_DATE_TIME_FORMAT.format(new Date(interval.getStart().getTime())), g);
    	final int tickTextWidth = (int) tickTextBounds.getWidth();
    	final int tickTextHeight = (int) tickTextBounds.getHeight();
    	final int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
    	final long tickDifferenceHorizontal = (interval.getEnd().getTime() - interval.getStart().getTime()) / (horizontalTickCount - 1);
        
    	for (int i = 0; i < horizontalTickCount; ++i) {
    		final Date tickValue = new Date(interval.getStart().getTime() + i * tickDifferenceHorizontal);
    		final int x = graphArea.x + (int)(i * tickDifferenceHorizontal * ratioX);
    		final String tickText = ChartConstants.FULL_DATE_TIME_FORMAT.format(tickValue);
           
    		g.setColor(ChartConstants.TICK_LINE_COLOR);
    		g.drawLine(x, graphArea.y, x, graphArea.y + graphArea.height + 3);
            
    		g.setColor(ChartConstants.LABEL_TEXT_COLOR);
    		if (i == 0) {
    			g.drawString(tickText, x - 10, graphArea.y + graphArea.height + 2 + tickTextHeight);
    		} else if (i == (horizontalTickCount - 1)) {
    			g.drawString(tickText, getWidth() - 1 - tickTextWidth, graphArea.y + graphArea.height + 2 + tickTextHeight);
    		} else {
    			g.drawString(tickText, x - (tickTextWidth / 2), graphArea.y + graphArea.height + 2 + tickTextHeight);
    		}
    	}
        
    	// inform when no data is available
    	if (!drawController.hasElementsToBeDrawn(identifier)) {
    		final String text = "No band / diode / line selected";
    		//Log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@   Draw No band   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    		final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
    		final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
    		final int y = graphArea.y + graphArea.height / 2;
           
    		g.setColor(ChartConstants.LABEL_TEXT_COLOR);
    		//g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    		g.drawString(text, x, y);
           
    	} 
    	
    	//TODO fit this somewhere in here :-)
    	/*final String text = "No data available";
		final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
		final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
		final int y = graphArea.y + graphArea.height / 2;
        g.setColor(ChartConstants.LABEL_TEXT_COLOR);
		g.drawString(text, x, y);
		*/
    	
    }
    
    private void drawVerticalLabels(Graphics g, YAxisElement yAxisElement, int leftSide){
    	// draw rectangle hiding to big radio image
    	g.setColor(Color.WHITE);
    	if(leftSide == 0){
    		g.fillRect(0, ChartConstants.GRAPH_TOP_SPACE, ChartConstants.GRAPH_LEFT_SPACE, graphArea.height);
    		g.fillRect(ChartConstants.GRAPH_LEFT_SPACE + graphArea.width, ChartConstants.GRAPH_TOP_SPACE,ChartConstants.TWO_AXIS_GRAPH_RIGHT, graphArea.height);
    	}else{
    		g.fillRect(ChartConstants.GRAPH_LEFT_SPACE + graphArea.width, ChartConstants.GRAPH_TOP_SPACE,ChartConstants.TWO_AXIS_GRAPH_RIGHT+ChartConstants.GRAPH_RIGHT_SPACE, graphArea.height);
    	}
    	// draw vertical label
	   	//final String verticalLabel = bands.length > 0 ? "log( " + bands[0].getBandType().getUnitLabel().replace("^2", "������") + " )" : "";
	   	final String verticalLabel = yAxisElement.getLabel();
	   	final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
	   	g.setColor(ChartConstants.LABEL_TEXT_COLOR);
    	g.drawString(verticalLabel, ChartConstants.GRAPH_LEFT_SPACE + leftSide * graphArea.width-leftSide*(int)verticalLabelBounds.getWidth(), (int)verticalLabelBounds.getHeight());
        
    	double logMinValue = yAxisElement.getMinValue();
    	double logMaxValue = yAxisElement.getMaxValue();
    		
    		
    	if (logMaxValue < logMinValue) {
    		g.setColor(ChartConstants.TICK_LINE_COLOR);
        
			final int sizeSteps = graphArea.height / ChartConstants.MIN_VERTICAL_TICK_SPACE;
			final double diff = graphArea.height / (double)ChartConstants.MIN_VERTICAL_TICK_SPACE; 
        
			for (int i = 0; i <= sizeSteps; ++i) {
				g.drawLine(graphArea.x - 3, (graphArea.y + graphArea.height) - (int)(i * ChartConstants.MIN_VERTICAL_TICK_SPACE * diff), graphArea.x + graphArea.width, (graphArea.y + graphArea.height) - (int)(i * ChartConstants.MIN_VERTICAL_TICK_SPACE * diff));    
			}
		} else {
			final int dataSteps = ((int)((logMaxValue - logMinValue) * 100));
			final int sizeSteps = graphArea.height / ChartConstants.MIN_VERTICAL_TICK_SPACE;
			final int verticalTicks = dataSteps < sizeSteps ? dataSteps : sizeSteps;
        
			if (verticalTicks == 0) {
				final int y = graphArea.y + graphArea.height;
            
				g.setColor(ChartConstants.TICK_LINE_COLOR);
				g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
			} else {            
				final double tickDifferenceVertical = (logMaxValue - logMinValue) / verticalTicks;
            
				for (int i = 0; i <= verticalTicks; ++i) {
					final double tickValue = logMinValue + i * tickDifferenceVertical;
					final String tickText = ChartConstants.DECIMAL_FORMAT.format(tickValue);
					Double yAxisRatio = yRatios.get(yAxisElement);
					if (yAxisRatio == null){
						continue;
					}
					final int y = graphArea.y + graphArea.height - (int)(yAxisRatio * (tickValue - logMinValue));
                
					g.setColor(ChartConstants.TICK_LINE_COLOR);
					if(leftSide == 0){
						g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
					}
                
					final Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
					final int x = graphArea.x - 6 - (int)bounds.getWidth()+leftSide * (graphArea.width + (int)bounds.getWidth() + 6);
					g.setColor(ChartConstants.LABEL_TEXT_COLOR);
					g.drawString(tickText, x, y + (int)(bounds.getHeight() / 2));
				}
			}
			
		}
    }
    
    /*@Override
	public void drawBufferedImage(BufferedImage image, DrawableAreaMap map){
    	this.redrawGraph();
	}
    
    @Override
	public void removeDownloadRequestData(long iD) {
		// TODO Auto-generated method stub
    	this.redrawGraph();
	}
    
	@Override
	public void changeVisibility(long iD) {
		this.redrawGraph();
	}*/
    
    /*private void drawGraphs(final Graphics g) {
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
    }*/
    
    private void drawRadio(final Graphics g){
    	pane.display(g);   
    	this.repaint();
    }
    
    private void drawEvents(final Graphics g){

    	
    	/*for(int i=0; i<events.length; i++){
    		final int x1 = (int)((events[i].beginDate.getTime().getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
    		final int x2 = (int)((events[i].endDate.getTime().getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
            g.setColor(Color.RED);

    		/*g.drawLine(x1, 20, x2, 20);
    		for(int j = 0;j<1000;j++){
    			g.drawImage(events[i].icon.getImage(), 100+j, 100, null);
    		}
    	}*/
    	
    	
    }     
    
    private void drawMovieLine(final Graphics g) {
        if (movieLinePosition < 0 || !drawController.getIntervalAvailable()) {
            return;
        }
        
        g.setColor(ChartConstants.MOVIE_FRAME_COLOR);
        g.drawLine(movieLinePosition, graphArea.y, movieLinePosition, graphArea.y + graphArea.height);
    }
    
    private void zoomFromZoomBox() {
    	Interval<Date> interval = drawController.getInterval();
    	//double logMinValue = drawController.getLogMinValue();
    	//double logMaxValue = drawController.getLogMaxValue();
        if (mousePressedPosition == null || mouseDragPosition == null) {
            return;
        }
        
        //if (!drawController.getIntervalAvailable() || logMaxValue < logMinValue)
        //    return;
        
        final int x0 = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, mousePressedPosition.x));
        final int y0 = Math.max(graphArea.y, Math.min(graphArea.y + graphArea.height, mousePressedPosition.y));
        final int x1 = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, mouseDragPosition.x));
        final int y1 = Math.max(graphArea.y, Math.min(graphArea.y + graphArea.height, mouseDragPosition.y));
        
        //final long start = interval.getStart().getTime() + (long)((Math.min(x0, x1) - graphArea.x) / ratioX);
        //final long end = interval.getStart().getTime() + (long)((Math.max(x0, x1) - graphArea.x) / ratioX);
        List<PlotAreaSpace> pass = new ArrayList<PlotAreaSpace>();
        Map<PlotAreaSpace,Double> minTime = new HashMap<PlotAreaSpace, Double>();
        Map<PlotAreaSpace,Double> maxTime = new HashMap<PlotAreaSpace, Double>();
        for(PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()){
        	pass.add(pas);
        	double ratioTime = graphArea.width/(pas.getScaledSelectedMaxTime() - pas.getScaledSelectedMinTime());
        
        	double startTime = pas.getScaledSelectedMinTime()+(Math.min(x0, x1) - graphArea.x)/ratioTime;
        	double endTime = pas.getScaledSelectedMinTime()+(Math.max(x0, x1) - graphArea.x)/ratioTime;
        	
        	minTime.put(pas, startTime);
        	maxTime.put(pas, endTime);
        }
        for(PlotAreaSpace pas : pass){
        	pas.setScaledSelectedTime(minTime.get(pas), maxTime.get(pas));
        }
        
        PlotAreaSpace myPlotAreaSpace = plotAreaSpaceManager.getPlotAreaSpace(identifier);
        double ratioValue = graphArea.height/(myPlotAreaSpace.getScaledSelectedMaxValue() - myPlotAreaSpace.getScaledSelectedMinValue());
        double startValue = (graphArea.y + graphArea.height - Math.max(y0, y1))/ratioValue + myPlotAreaSpace.getScaledSelectedMinValue();
        double endValue = (graphArea.y + graphArea.height - Math.min(y0, y1))/ratioValue + myPlotAreaSpace.getScaledSelectedMinValue();
        
        myPlotAreaSpace.setScaledSelectedValue(startValue, endValue);
        //final double min = Math.pow(10, (graphArea.y + graphArea.height - Math.max(y0, y1)) / ratioY + logMinValue);
        //final double max = Math.pow(10, (graphArea.y + graphArea.height - Math.min(y0, y1)) / ratioY + logMinValue);
        
        //ZoomController.getSingletonInstance().setSelectedInterval(new Interval<Date>(new Date(start), new Date(end)));
        //drawController.setSelectedRange(new Range(min, max));
        //Log.debug("min : "+ min);
        //Log.debug("max : "+ max);
        
    }
    

    
    private void updateGraphArea() {
    	if (drawController.getYAxisElements(identifier).size() >= 2){
    		twoYAxis = 1;
    	}
        final int graphWidth = getWidth() - (ChartConstants.GRAPH_LEFT_SPACE + ChartConstants.GRAPH_RIGHT_SPACE + twoYAxis*ChartConstants.TWO_AXIS_GRAPH_RIGHT);
        final int graphHeight = getHeight() - (ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE);
        graphArea = new Rectangle(ChartConstants.GRAPH_LEFT_SPACE, ChartConstants.GRAPH_TOP_SPACE, graphWidth, graphHeight);
        plotArea = new Rectangle(0, 0, graphWidth, graphHeight);
        zoomManager.setDisplaySize(plotArea, identifier);
    }
    
    private void updateRatios() {
    	Interval<Date> interval = drawController.getInterval();
    	ratioX = !drawController.getIntervalAvailable() ? 0 : (double)graphArea.width / (double)(interval.getEnd().getTime() - interval.getStart().getTime());
    	yRatios = new HashMap<YAxisElement, Double>();
    	for(YAxisElement yAxisElement : drawController.getYAxisElements(identifier)){
    		double logMinValue = yAxisElement.getMinValue();
    		double logMaxValue = yAxisElement.getMaxValue();
    		double ratioY = logMaxValue < logMinValue ? 0 : graphArea.height / (logMaxValue - logMinValue);
    		yRatios.put(yAxisElement, ratioY);
    	}
    }
    
    /*private void updateGraphsData() {
        graphPolylines.clear();
        //if (!intervalAvailable)
        //    return;
        
        for (int i = 0; i < bands.length; ++i) {
            final EVEValue[] eveValues = values[i].getValues();
            final ArrayList<Point> pointList = new ArrayList<Point>();
            final LinkedList<Integer> warnLevels = new LinkedList<Integer>();
            HashMap<String, Double> unconvertedWarnLevels = bands[i].getBandType().getWarnLevels();

            Iterator<Entry<String, Double>> it = unconvertedWarnLevels.entrySet().iterator();
            while (it.hasNext()) {
				Map.Entry<String, Double> pairs = it.next();
                warnLevels.add(computeY(pairs.getValue()));
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
                
                final int x = computeX(eveValues[j].getDate());
                final int y = computeY(eveValues[j].getValue().doubleValue());
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

    private int computeX(Date orig){
    	return (int)((orig.getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
    }
    
    private int computeY(double orig){
    	return graphArea.y + graphArea.height - (int)(ratioY * (Math.log10(orig) - logMinValue));
    }
    */
    private void updateMovieLineInformation() {
    	Interval<Date> interval = drawController.getInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            movieLinePosition = -1;
            return;
        }
        
        movieLinePosition = (int)((movieTimestamp.getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
        
        if (movieLinePosition < graphArea.x || movieLinePosition > (graphArea.x + graphArea.width)) {
            movieLinePosition = -1;
        }
    }
    
    private void updateGraphEvents() {
    	
    	// Prepare an ImageIcons to be used with JComponents or drawImage()   	
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.DATE, -1);
    	Calendar cal2 = Calendar.getInstance();    	
    	GraphEvent event = new GraphEvent( cal, cal2);
    	events = new GraphEvent[1];
    	events[0] = event;
    }   
    
    private void setMovieFrameManually(final Point point) {
    	Interval<Date> interval = drawController.getInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            return;
        }
        
        final int x = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, point.x));        
        final long timestamp = ((long)((x - graphArea.x) / ratioX) + interval.getStart().getTime()) / 1000;
        
        final LinkedMovieManager linkedMovieManager = LinkedMovieManager.getActiveInstance();
        linkedMovieManager.setCurrentFrame(new ImmutableDateTime(timestamp), new ChangeEvent(), false);
    }
    
   
    
    // //////////////////////////////////////////////////////////////////////////////
    // Draw Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    /*public void drawRequest(final Interval<Date> interval, final Band[] bands, final EVEValues[] values, final Range availableRange, final Range selectedRange) {
        this.interval = interval;
        this.intervalAvailable = interval.getStart() != null && interval.getEnd() != null; 
        this.bands = bands;
        this.values = values;
        
        logMinValue = Math.log10(selectedRange.min);
        logMaxValue = Math.log10(selectedRange.max);
        
        updateGraph();
        repaint();
    }

    public void drawRequest(final Date movieTimestamp) {
        this.movieTimestamp = movieTimestamp;
        
        updateMovieLineInformation();
        
        repaint();
    }*/
    
    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Input Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void mouseClicked(final MouseEvent e) {
        if (graphArea.contains(e.getPoint())) {
            setMovieFrameManually(e.getPoint());    
        }
    }

    public void mouseEntered(final MouseEvent e) {}

    public void mouseExited(final MouseEvent e) {}

    public void mousePressed(final MouseEvent e) {
        final Rectangle movieFrame = new Rectangle(movieLinePosition - 1, graphArea.y, 3, graphArea.height);
        
        mousePressedOnMovieFrame = movieFrame.contains(e.getPoint());
        mousePressedPosition = graphArea.contains(e.getPoint()) ? e.getPoint() : null;
    }

    public void mouseReleased(final MouseEvent e) {
        boolean repaintFlag = false;
        
        if (mousePressedPosition != null && mouseDragPosition != null && !mousePressedOnMovieFrame) {
            zoomFromZoomBox();            
            repaintFlag = true;
        }
        
        mousePressedPosition = null;
        mouseDragPosition = null;
        
        if (repaintFlag) {
            updateGraph();
            repaint();
        }
    }

    public void mouseDragged(final MouseEvent e) {
        mouseDragPosition = e.getPoint();
        
        if (mousePressedPosition != null && mousePressedOnMovieFrame) {
            setMovieFrameManually(mouseDragPosition);
        }
        
        if (mousePressedPosition != null && !mousePressedOnMovieFrame) {
            updateGraph();
            repaint();    
        }
    }

    public void mouseMoved(final MouseEvent e) {
        final Rectangle frame = new Rectangle(movieLinePosition - 1, graphArea.y, 3, graphArea.height);
        
        if (movieLinePosition >= 0 && drawController.getIntervalAvailable() && frame.contains(e.getPoint())) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Component Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void componentHidden(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {}

    public void componentResized(ComponentEvent e) {
    	synchronized(this){
	        lastKnownWidth = getWidth() - (ChartConstants.GRAPH_LEFT_SPACE + ChartConstants.GRAPH_RIGHT_SPACE + twoYAxis * ChartConstants.TWO_AXIS_GRAPH_RIGHT);
	        lastKnownHeight = getHeight() - (ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE);
	        zoomManager.setDisplaySize(new Rectangle(0,0,
	        		lastKnownWidth, lastKnownHeight),identifier);        
	        updateGraph();
    	}
    }

    public void componentShown(ComponentEvent e) {
    	synchronized(this){
    		updateGraph();
    	}
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
    
    // //////////////////////////////////////////////////////////////////////////////
    // Graph Event
    // //////////////////////////////////////////////////////////////////////////////
    
    private class GraphEvent {
    
        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////
        
    	private  Calendar beginDate;
    	private  Calendar endDate;
        private ImageIcon icon;
        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////
        
        public GraphEvent(Calendar beginDate, Calendar endDate) {
        	this.beginDate = beginDate;
        	this.endDate = endDate;
        	URL url = EVEPlugin.getResourceUrl("/images/ar_icon.png");
        	if (icon == null)
        		icon = new ImageIcon(url); 
        }
    }

	@Override
	public void drawRequest() {
		synchronized (this) {
			/*Log.debug("repaint on "+ this);
			revalidate();
			*/
			updateGraph();
			repaint();	
		}	
	}

	@Override
	public void chartRedrawRequested() {
		synchronized (this) {
			this.redrawGraph();	
		}		
	}

	@Override
	public void drawMovieLineRequest(Date time) {
		synchronized (this) {
			this.movieTimestamp = time;
	        
	        updateMovieLineInformation();
	        
	        repaint();
		}		
	}
}
