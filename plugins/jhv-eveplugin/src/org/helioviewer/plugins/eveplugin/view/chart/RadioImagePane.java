package org.helioviewer.plugins.eveplugin.view.chart;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collection;


import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.model.DrawableAreaMap;
import org.helioviewer.plugins.eveplugin.radio.model.PlotConfig;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModelListener;

import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;

public class RadioImagePane implements ImageObserver, RadioPlotModelListener, DrawableElement{
	public static void main(String []args){
		RadioImagePane pane = new RadioImagePane();
	}

	private int [][] radioImage;
	private int width;
	private int height;
	private int x0;
	private int y0;
	private Axis frequencyAxis;
	private Axis dateTimeAxis;
	private JHVJPXView view;
	private JP2Image image;
	private boolean eventReceived = false;
	private boolean acceptEvents = false;
	private boolean first = true;
	ArrayList<BufferedImage> views = new ArrayList<BufferedImage>();
	//private RadioPlotModel plotModel;
	private YAxisElement yAxitElement;
	
	public RadioImagePane(){
		//this.plotModel = RadioPlotModel.getSingletonInstance();
	/*	try {
			throw new Exception();
		} catch (Exception e1) {
			//e1.printStackTrace();
			Log.error("Who created me", e1);
		}
		this.radioImage = new int[100][100];
		for(int j=0;j<100;j++){
			for(int i=0;i<100;i++){
				this.radioImage[j][i] = i;
			}
		}
        view = new JHVJPXView(true,null);
        Log.debug("view object = " + view);
        URI uri;
		try {
			//uri = new URI("jpip://swhv.oma.be:8090/AIA/171/2011/06/01/2011_06_01__23_59_36_34__SDO_AIA_AIA_171.jp2");
		    //uri = new URI("file:///data/localjp2/callisto/2011/09/2011_09_24__00_00_00__ROB-Humain_CALLISTO_CALLISTO_RADIOGRAM.jp2");
			//URI downloadURI = new URI("http://swhv.oma.be/helioviewer/api/?action=getJP2Image&observatory=SDO&instrument=AIA&detector=AIA&measurement=171&date=2013-08-26T07:09:10Z&json=true");
			//uri = new URI("jpip://swhv.oma.be:8091/movies/SDO_AIA_AIA_171_F2013-12-05T00.00.00Z_T2013-12-06T00.00.00ZB3600.jpx");
			//URI downloadURI = new URI("jpip://swhv.oma.be:8091/movies/SDO_AIA_AIA_171_F2013-12-05T00.00.00Z_T2013-12-06T00.00.00ZB3600.jpx");
			uri = new URI("jpip://swhv.oma.be:8091/movies/ROB-Humain_CALLISTO_CALLISTO_RADIOGRAM_F2011-09-24T00.00.00Z_T2011-09-26T00.00.00ZB3600.jpx");
			URI downloadURI = new URI("jpip://swhv.oma.be:8091/movies/ROB-Humain_CALLISTO_CALLISTO_RADIOGRAM_F2011-09-24T00.00.00Z_T2011-09-26T00.00.00ZB3600.jpx");
			
			this.image = new JP2Image(uri, downloadURI);
			ResolutionSet rSet = image.getResolutionSet();
			Log.debug(rSet.getMaxResolutionLevels());
		    view.setJP2Image(image);
		    view.setViewport(new ViewportAdapter(new StaticViewport(new Vector2dInt(42400,152))), new ChangeEvent());
		    //view.setRegion(new RegionAdapter(new StaticRegion(80, 80, new Vector2dDouble(120,120))),new ChangeEvent());
		    //view.setViewport(new ViewportAdapter(new StaticViewport(new Vector2dInt(1024, 1024))), new ChangeEvent());
		    //view.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
		    Log.debug(view.getMaximumNumQualityLayers());

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		view.addViewListener(this);*/
	}
	
	public void display(Graphics g){
		/*try {
			throw new Exception();
		} catch (Exception e1) {
			//e1.printStackTrace();
			Log.error("Who called me", e1);
		}
		
		if(first){
			for(int i=0;i<3;i++){
				//JHVJPXView temp = new JHVJPXView(true,null);
				//temp.setJP2Image(image);
				//temp.setCurrentFrame(i, null,true);
				Log.debug("Jump to frame "+ i*1);
				acceptEvents = true;
				//view.setRegion(new RegionAdapter(new StaticRegion(80, 80, new Vector2dDouble(120,120))),new ChangeEvent());
			    view.setCurrentFrame(i, new ChangeEvent(), true);
				byte[] data = new byte[0];
				byte[] newData = new byte[0];;
				while(!eventReceived){
					Log.debug("Wait for event");
					/*SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(view.getSubimageData());
					Byte8ImageTransport bytetrs =  (Byte8ImageTransport) imageData.getImageTransport();
					data = bytetrs.getByte8PixelData();
					newData  = new byte[data.length];
					System.arraycopy(data, 0, newData, 0, data.length);
					Log.debug(Arrays.toString(newData));*/
					//Log.debug(newData.length);		
/*					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				eventReceived = false;
				acceptEvents = false;
				//SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(temp.getSubimageData());
				SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(view.getSubimageData());
				Byte8ImageTransport bytetrs =  (Byte8ImageTransport) imageData.getImageTransport();
				//byte[] data = bytetrs.getByte8PixelData();
				//byte[] newData  = new byte[data.length];
				data = bytetrs.getByte8PixelData();
				newData  = new byte[data.length];
				System.arraycopy(data, 0, newData, 0, data.length);
				//Log.debug(Arrays.toString(newData));
				Log.debug(newData.length);
				Log.debug("max = " + RadioImagePane.max(newData));
				Log.debug("min = " + RadioImagePane.min(newData));
				int width = imageData.getWidth();
				int height = imageData.getHeight();
				Log.debug("width = "+ width);
				Log.debug("height = " + height);
				//g.drawImage(imageData.getBufferedImage(),0+i*200,0,200,200, null);
				BufferedImage temp = createBufferedImageFromImageTransport(width,height,newData);
				//System.out.println(g.drawImage(temp,0+i*400,0,(i+1)*400,400,0,0,128,128, null));
				views.add(temp);
			}
			first = false;
		}
		for(int i=0;i<3;i++){
			Log.debug(views.get(i));
			Log.debug("x1 = " + 0 + " y1 = "+ (i*96)+ " x2 = "+ 1000 + " y2 = "+ ((i+1)*96));
			Log.debug(g.drawImage(views.get(i),0,i*96,1000,(i+1)*96,0,0,43200,190,this ));
			g.setColor(Color.blue);
			g.drawRect(0, i*96, 1000, 96);
			//System.out.println(g.drawImage(views.get(0),0,0,200,200,0,0,12,2700, null));
		}*/
		Collection<PlotConfig> configs = RadioPlotModel.getSingletonInstance().getPlotConfigurations();
		for(PlotConfig pc : configs){
			
			pc.draw(g);
		}
	}
	
	/*public void setRadioImage(int[][]radioImage){
		this.radioImage = radioImage;
	}
	
	private BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 System.out.println(raster.hashCode());
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	private BufferedImage createBufferedImageFromImageTransport( int width, int height, byte[] data) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        //Log.debug("databuffer = " +  Arrays.toString(dataBuffer.getData()));
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

	@Override
	public void viewChanged(View sender, ChangeEvent aEvent) {
		Log.debug("View object = " + view);
		try {
			throw new Exception();
		} catch (Exception e1) {
			Log.error("Who send the event", e1);
			//e1.printStackTrace();
		}
		if(acceptEvents){ //we only accept events if we changed the framenumber
			if(aEvent.reasonOccurred(SubImageDataChangedReason.class) && aEvent.reasonOccurred(TimestampChangedReason.class)){ 
				Log.info("new view changed event");
				Log.info(sender.toString());
				Log.info(aEvent.toString());
				eventReceived = true;
			}else{
				Log.info("ignore event");
				Log.info(sender.toString());
				Log.info(aEvent.toString());
			}
		}else{
			Log.debug("Event not excepted. Not changed the framenumber");
			Log.info(sender.toString());
			Log.info(aEvent.toString());
		}
		/*eventReceived = true;
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	/*}
	
	public static int min(byte[] args) {
        int m = Integer.MAX_VALUE;
        for (int a : args) {
            m = Math.min(m, a);
        }
        return m;
    }

    public static int max(byte[] args) {
        int m = Integer.MIN_VALUE;
        for (int a : args) {
            m = Math.max(m, a);
        }
        return m;
    }
*/
	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		//Log.debug("Image was updated "+ img);
		return false;
	}

	@Override
	public void drawBufferedImage(BufferedImage image, DrawableAreaMap map) {}

	@Override
	public void changeVisibility(long iD) {}

	@Override
	public void removeDownloadRequestData(long iD) {}

	@Override
	public DrawableElementType getDrawableElementType() {
		return DrawableElementType.RADIO;
	}

	@Override
	public void draw(Graphics g, Rectangle graphArea) {
		Collection<PlotConfig> configs = RadioPlotModel.getSingletonInstance().getPlotConfigurations();
		for(PlotConfig pc : configs){
			//Log.debug("Width when drawn is requested : " + pc.getDrawWidth());
			pc.draw(g);
		}		
	}

	@Override
	public void setYAxisElement(YAxisElement yAxisElement) {
		this.yAxitElement = yAxisElement;
		
	}

	@Override
	public YAxisElement getYAxisElement() {
		return this.yAxitElement;
	}

	@Override
	public boolean hasElementsToDraw() {
		boolean temp1 = RadioPlotModel.getSingletonInstance().getPlotConfigurations() == null;
		boolean temp2 = RadioPlotModel.getSingletonInstance().getPlotConfigurations().isEmpty();
		Log.debug("null? " + temp1 + " empty? "+ temp2);		
		return !(temp1 || temp2); 
	}
}
