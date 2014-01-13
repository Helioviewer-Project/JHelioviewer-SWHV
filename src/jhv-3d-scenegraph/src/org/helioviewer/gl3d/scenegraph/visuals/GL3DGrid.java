package org.helioviewer.gl3d.scenegraph.visuals;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DGrid extends GL3DGroup{
	private int xticks;
	private int yticks;
	private GL3DVec4f color;
	private GL3DVec4d textColor;
	private String font;

	public GL3DGrid(String name, int xticks, int yticks, GL3DVec4f color, GL3DVec4d textColor) {
		super(name);
		this.xticks = xticks;
		this.yticks = yticks; 
		this.color = color;
		this.textColor = textColor;
		this.loadGrid();
	}
	private void loadGrid() {
		GL3DSphere sphere = new GL3DSphere(Constants.SunRadius*1.02,this.xticks,this.yticks, this.color);
		sphere.getDrawBits().on(Bit.Wireframe);
		this.addNode(sphere);
        GL3DText rect;
   	 	double letterSize = 0.05*Constants.SunRadius;
   	 	double size = Constants.SunRadius*1.1; 
   	 	double zdist = Constants.SunRadius*0.001;
 
        for(int i=1;i<this.xticks; i++){
        	double angle = i*Math.PI/this.xticks;
        	String angleStringx = ""+(int)(90-1.0*i/this.xticks *180);
        	rect = new GL3DText(letterSize,letterSize,Math.sin(angle)*size, Math.cos(angle)*size, zdist, angleStringx, this.textColor);
            this.addNode(rect);
            String angleStringy = ""+(int)(90-1.0*i/this.xticks *180);
        	rect = new GL3DText(letterSize,letterSize,-Math.sin(angle)*size, Math.cos(angle)*size, zdist, angleStringy, this.textColor);
            this.addNode(rect);
        }
        for(int i=1;i<this.yticks; i++){
        	double angle = i*Math.PI/(this.yticks/2.0);
        	String angleString = ""+(int)(90-1.0*i/(this.yticks/2.0) *180);
        	rect = new GL3DText(letterSize, letterSize, Math.cos(angle)*size, 0.0, Math.sin(angle)*size,angleString, this.textColor);
        	this.addNode(rect);
        }		
	}
    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }	

}
