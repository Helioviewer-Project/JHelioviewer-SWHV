package org.helioviewer.plugins.eveplugin.view.chart;


import java.awt.Graphics;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.jhv.io.APIRequestManager;

import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;



public class RadioImagePane {
	public static void main(String []args){
		RadioImagePane pane = new RadioImagePane();
	}

	private int [][] radioImage;
	private int width;
	private int height;
	private int x0;
	private int y0;
	Axis frequencyAxis;
	Axis dateTimeAxis;
	JHVJP2View view;
	public RadioImagePane(){/*
		this.radioImage = new int[100][100];
		for(int j=0;j<100;j++){
			for(int i=0;i<100;i++){
				this.radioImage[j][i] = i;
			}
		}
        view = new JHVJP2View(false);
        URI uri;
		try {
			uri = new URI("jpip://swhv.oma.be:8090/AIA/171/2011/06/01/2011_06_01__23_59_36_34__SDO_AIA_AIA_171.jp2");
		    URI downloadURI = new URI("http://swhv.oma.be/helioviewer/api/?action=getJP2Image&observatory=SDO&instrument=AIA&detector=AIA&measurement=171&date=2013-08-26T07:09:10Z&json=true");
		    JP2Image image = new JP2Image(uri, downloadURI);
		    view.setJP2Image(image);

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	public void display(Graphics g){
		SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(view.getSubimageData());
		for(int i=0;i<20;i++){
			g.drawImage(imageData.getBufferedImage(),0+i*10,0,100,100, null);
		}
	}
	public void setRadioImage(int[][]radioImage){
		this.radioImage = radioImage;
	}
	
}
