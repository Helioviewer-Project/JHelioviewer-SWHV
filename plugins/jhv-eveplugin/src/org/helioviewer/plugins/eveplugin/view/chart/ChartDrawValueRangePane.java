package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.event.MouseInputListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawControllerListener;
import org.helioviewer.plugins.eveplugin.lines.model.EVEValueRangeModel;
import org.helioviewer.plugins.eveplugin.lines.model.EVEValueRangeModelListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;

/**
 * @author Stephan Pagel
 * */
public class ChartDrawValueRangePane extends JComponent implements EVEValueRangeModelListener,MouseInputListener,PlotAreaSpaceListener{//,EVEDrawControllerListener{

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;
    
    private EVEDrawController drawController;
    
    private Range availableRange = new Range();
    private Range selectedRange = new Range();
    
    private boolean mouseOverComponent = false;
    private boolean mouseOverRange = false;
    private boolean mouseOverTopGraspPoint = false;
    private boolean mouseOverBottomGraspPoint = false;
    
    private Point mousePressed = null;
    
    private int topRangeBorderPosition = -10;
    private int bottomRangeBorderPosition = -10;
    
    private final HintPopup minLabel = new HintPopup();
    private final HintPopup maxLabel = new HintPopup();
    
    private EVEValueRangeModel valueRangeModel;
    
    private PlotAreaSpaceManager plotAreaSpacemanager;
    private String plotIdentifier;
    
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ChartDrawValueRangePane(final EVEDrawController drawController, String plotIdentifier) {
        this.drawController = drawController;
        this.plotIdentifier = plotIdentifier;
        plotAreaSpacemanager = PlotAreaSpaceManager.getInstance();
        plotAreaSpacemanager.getPlotAreaSpace(plotIdentifier).addPlotAreaSpaceListener(this);
        
        initVisualComponents();
        
        addMouseListener(this);
        addMouseMotionListener(this);
        
        //drawController.addDrawControllerListener(this);
        //valueRangeModel = ValueRangeModel.getInstance();
        
    }
    
    private void initVisualComponents() {
        setPreferredSize(new Dimension(ChartConstants.RANGE_SELECTION_WIDTH, getPreferredSize().height));
        setSize(getPreferredSize());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        drawBackground(g);
        
        if (availableRange.isPositivRange() && selectedRange.isPositivRange()) {
            computeRangeBorderPositions();
            
            drawRange(g);
            drawIntervalGraspPoints(g);
            //showLabelText();
        }
    }
    
    private void computeRangeBorderPositions() {
        final double differenceAvailableValues = availableRange.max - availableRange.min;
        final int availableRangeSpace = getHeight() - 1 - ChartConstants.GRAPH_TOP_SPACE - ChartConstants.GRAPH_BOTTOM_SPACE;

        final double min = selectedRange.min - availableRange.min;
        final double max = selectedRange.max - availableRange.min;
        
        //topRangeBorderPosition = ChartConstants.GRAPH_TOP_SPACE;
        //bottomRangeBorderPosition = ChartConstants.GRAPH_TOP_SPACE + availableRangeSpace;
        
        topRangeBorderPosition = (availableRangeSpace - (int)((max / differenceAvailableValues) * availableRangeSpace)) + ChartConstants.GRAPH_TOP_SPACE;
        bottomRangeBorderPosition = (availableRangeSpace - (int)((min / differenceAvailableValues) * availableRangeSpace)) + ChartConstants.GRAPH_TOP_SPACE;
    }
    
    private void drawBackground(Graphics g) {
        final int availableRangeSpace = getHeight() - 1 - ChartConstants.GRAPH_TOP_SPACE - ChartConstants.GRAPH_BOTTOM_SPACE;
        
        g.setColor(ChartConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, ChartConstants.GRAPH_TOP_SPACE, getWidth(), availableRangeSpace);
    }
    
    private void drawRange(Graphics g) {
        // draw Background
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, topRangeBorderPosition, getWidth() - 1, bottomRangeBorderPosition - topRangeBorderPosition);
        
        // draw Border
        g.setColor(ChartConstants.BORDER_COLOR);        
        g.drawLine(0, 0, 0, topRangeBorderPosition);
        g.drawLine(0, topRangeBorderPosition, getWidth() - 1, topRangeBorderPosition);
        g.drawLine(getWidth() - 1, topRangeBorderPosition, getWidth() - 1, bottomRangeBorderPosition);
        g.drawLine(getWidth() - 1, bottomRangeBorderPosition, 0, bottomRangeBorderPosition);
        g.drawLine(0, bottomRangeBorderPosition, 0, getHeight() - 1);
    }
    
    private void drawIntervalGraspPoints(Graphics g) {
        if (!mouseOverComponent)
            return;
        
        Polygon p0 = new Polygon();
        p0.addPoint(getWidth() / 2 - 5, topRangeBorderPosition);
        p0.addPoint(getWidth() / 2    , topRangeBorderPosition - 5);
        p0.addPoint(getWidth() / 2 + 5, topRangeBorderPosition);
        p0.addPoint(getWidth() / 2    , topRangeBorderPosition + 5);
        
        Polygon p1 = new Polygon();
        p1.addPoint(getWidth() / 2 - 5, bottomRangeBorderPosition);
        p1.addPoint(getWidth() / 2    , bottomRangeBorderPosition - 5);
        p1.addPoint(getWidth() / 2 + 5, bottomRangeBorderPosition);
        p1.addPoint(getWidth() / 2    , bottomRangeBorderPosition + 5);
        
        g.setColor(ChartConstants.GRASP_POINT_COLOR);
        g.fillPolygon(p0);
        g.fillPolygon(p1);
    }
    
    private void showLabelText() {
        if (mouseOverRange && mousePressed != null && selectedRange != null && selectedRange.isPositivRange()) {
            minLabel.setVisible(true);
            maxLabel.setVisible(true);
            
            final double logMinValue = Math.log10(selectedRange.min);
            final double logMaxValue = Math.log10(selectedRange.max);
            
            minLabel.setHintText("10^" + ChartConstants.DECIMAL_FORMAT.format(logMinValue));
            maxLabel.setHintText("10^" + ChartConstants.DECIMAL_FORMAT.format(logMaxValue));
            
            final Point componentLocationOnScreen = getLocationOnScreen();
            
            minLabel.setLocation(componentLocationOnScreen.x + getWidth(), componentLocationOnScreen.y + bottomRangeBorderPosition - maxLabel.getHeight() / 2);
            maxLabel.setLocation(componentLocationOnScreen.x + getWidth(), componentLocationOnScreen.y + topRangeBorderPosition - minLabel.getHeight() / 2);
        } else {
            minLabel.setVisible(false);
            maxLabel.setVisible(false);
        }
    }
    
    private void moveSelectedRange(final Point newMousePosition) {
        final int diffPixel = mousePressed.y > newMousePosition.y ? mousePressed.y - newMousePosition.y : newMousePosition.y - mousePressed.y;
        final double diffRange = availableRange.max - availableRange.min;
        final int availableRangeSpace = getHeight() - 1 - (ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE);
        final double movedValues = (diffPixel / (double)availableRangeSpace) * diffRange;
        
        final Range newRange = new Range();
        
        if (mousePressed.y > newMousePosition.y) {
            newRange.min = selectedRange.min + movedValues;
            newRange.max = selectedRange.max + movedValues;
        } else {
            newRange.min = selectedRange.min - movedValues;
            newRange.max = selectedRange.max - movedValues;
        }
        
        if (availableRange.contains(newRange)) {
            //drawController.setSelectedRange(newRange);
        	plotAreaSpacemanager.getPlotAreaSpace(plotIdentifier).setScaledSelectedValue(newRange.min, newRange.max);
            mousePressed = newMousePosition;
        }
    }
    
    private synchronized void resizeSelectedRange(final Point newMousePosition) {
    	if (mouseOverTopGraspPoint) {
            if (newMousePosition.y >= bottomRangeBorderPosition || newMousePosition.y < ChartConstants.GRAPH_TOP_SPACE )
        	//if (newMousePosition.y <= ChartConstants.GRAPH_TOP_SPACE || topRangeBorderPosition == bottomRangeBorderPosition )
                return;
            
            final int availableRangeSpace = getHeight() - 1 - (ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE);
            final double diffRange = availableRange.max - availableRange.min;
            final int position = (availableRangeSpace - (newMousePosition.y - ChartConstants.GRAPH_TOP_SPACE));
            final double max = availableRange.min + ((position / (double)availableRangeSpace) * diffRange);
            
            //valueRangeModel.setSelectedInterval(new Range(selectedRange.min, max));
            plotAreaSpacemanager.getPlotAreaSpace(plotIdentifier).setScaledSelectedValue(selectedRange.min, max);
        } else if (mouseOverBottomGraspPoint) {
        	if (newMousePosition.y < topRangeBorderPosition || newMousePosition.y > getHeight()-ChartConstants.GRAPH_BOTTOM_SPACE+1)
        	//if (newMousePosition.y >= getHeight()-ChartConstants.GRAPH_BOTTOM_SPACE  || topRangeBorderPosition == bottomRangeBorderPosition )
                return;

            final int availableRangeSpace = getHeight() - 1 - (ChartConstants.GRAPH_TOP_SPACE + ChartConstants.GRAPH_BOTTOM_SPACE);
            final double diffRange = availableRange.max - availableRange.min;
            final double min = availableRange.min + (((availableRangeSpace - (newMousePosition.y - ChartConstants.GRAPH_TOP_SPACE)) / (double)availableRangeSpace) * diffRange);        
            
            //valueRangeModel.setSelectedInterval(new Range(min, selectedRange.max));
            plotAreaSpacemanager.getPlotAreaSpace(plotIdentifier).setScaledSelectedValue(min, selectedRange.max);
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Input Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void mouseClicked(MouseEvent arg0) {}

    public void mouseEntered(MouseEvent arg0) {
        mouseOverComponent = true;
        repaint();
    }

    public void mouseExited(MouseEvent arg0) {
        mouseOverComponent = false;
        mouseOverRange = false;
        mouseOverTopGraspPoint = false;
        mouseOverBottomGraspPoint = false;
        
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    public void mousePressed(MouseEvent arg0) {
        mousePressed = arg0.getPoint();
    }

    public void mouseReleased(MouseEvent arg0) {
        mousePressed = null;
    }

    public void mouseDragged(MouseEvent arg0) {
        if (mouseOverTopGraspPoint || mouseOverBottomGraspPoint)
            resizeSelectedRange(arg0.getPoint());
        else if (mouseOverRange)
            moveSelectedRange(arg0.getPoint());
    }

    public void mouseMoved(MouseEvent e) {
        mouseOverRange = false;
        mouseOverTopGraspPoint = false;
        mouseOverBottomGraspPoint = false;
            
        // is mouse cursor above selected range?
        if (e.getPoint().y >= topRangeBorderPosition && e.getPoint().y <= bottomRangeBorderPosition) {
            mouseOverRange = true;
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
            
        // is mouse cursor above of one of the grasp points?
        if (e.getPoint().y >= topRangeBorderPosition - 5 && e.getPoint().y <= topRangeBorderPosition + 5 && e.getPoint().x >= (getWidth() / 2 - 5) && e.getPoint().x <= (getWidth() / 2 + 5)) {
            mouseOverTopGraspPoint = true;
            setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
        }
            
        if (e.getPoint().y >= bottomRangeBorderPosition - 5 && e.getPoint().y <= bottomRangeBorderPosition + 5 && e.getPoint().x >= (getWidth() / 2 - 5) && e.getPoint().x <= (getWidth() / 2 + 5)) {
            mouseOverBottomGraspPoint = true;
            setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
        }
        
        // reset cursor if it does not point to the range area
        if (!mouseOverRange)
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Draw Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void drawRequest(final Interval<Date> interval, final Band[] bands, final EVEValues[] values, final Range availableRange, final Range selectedRange) {
        this.availableRange = availableRange;
        this.selectedRange = selectedRange;
        
        repaint();
    }

    public void drawRequest(final Date movieTimestamp) {}

    public void drawRequest(final boolean activeDownload) {}
    
    // //////////////////////////////////////////////////////////////////////////////
    // Hint Popup
    // //////////////////////////////////////////////////////////////////////////////
    
    /**
     * This class represents the hint area when a thumb is clicked
     * */
    private class HintPopup extends JWindow {
        
        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;
        
        private final JLabel label = new JLabel();
        
        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////
        
        public HintPopup() {
            final JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout());
            panel.setBackground(ChartConstants.GRASP_POINT_COLOR);
            panel.add(label, FlowLayout.LEFT);
            
            this.add(panel);
            
            label.setForeground(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        }
        
        public void setHintText(final String text) {
            final Rectangle2D stringBounds = label.getGraphics().getFontMetrics().getStringBounds(text, label.getGraphics());
            label.setText(text);
            
            setPreferredSize(new Dimension((int)stringBounds.getWidth() + 10, (int)stringBounds.getHeight() + 8));
            setSize(getPreferredSize());
        }
    }

	@Override
	public void availableRangeChanged(Range availableRange) {
		this.availableRange = availableRange;
		repaint();
		
	}

	@Override
	public void selectedRangeChanged(Range selectedRange) {
		this.selectedRange = selectedRange;
		repaint();
	}

	@Override
	public void plotAreaSpaceChanged(double scaledMinValue,
			double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
			double scaledSelectedMinValue, double scaledSelectedMaxValue,
			double scaledSelectedMinTime, double scaledSelectedMaxTime) {
		
		this.availableRange = new Range(scaledMinValue, scaledMaxValue);
		this.selectedRange = new Range(scaledSelectedMinValue, scaledSelectedMaxValue);
		repaint();
	}
}
