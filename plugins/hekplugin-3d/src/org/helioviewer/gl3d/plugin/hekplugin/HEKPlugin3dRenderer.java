package org.helioviewer.gl3d.plugin.hekplugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Console;
import java.util.Date;
import java.util.Vector;


import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.SphericalCoord;
import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.wcs.conversion.SphericalToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;

/**
 * The solar event renderer provides a possibility to draw solar events with
 * there associated icons.
 * 
 * @author Malte Nuhn
 */
public class HEKPlugin3dRenderer extends PhysicalRenderer3d {
	private SphericalCoordinateSystem sphericalCS = new SphericalCoordinateSystem();
    private SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();
    private float scale = 1;
    private double height = -1000;
    SphericalToSolarSphereConversion conversion = (SphericalToSolarSphereConversion) sphericalCS.getConversion(solarSphereCS);

	/**
	 * Default constructor.
	 */
	public HEKPlugin3dRenderer() {
	}

	/**
	 * The actual rendering routine
	 * 
	 * @param g
	 *            - PhysicalRenderGraphics to render to
	 * @param evt
	 *            - Event to draw
	 * @param now
	 *            - Current point in time
	 */
	public void drawPolygon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {

		if (evt != null && evt.isVisible(now)) {

			String type = evt.getString("event_type");
			Color eventColor = HEKConstants.getSingletonInstance()
					.acronymToColor(type, 128);

			Vector<HEKEvent.GenericTriangle<Vector3dDouble>> triangles = evt.getTriangulation3D(now);

			if (triangles != null) {
				g.setColor(eventColor);
				for (GenericTriangle<Vector3dDouble> triangle : triangles) {
					Vector3dDouble tri[] = { triangle.A, triangle.B, triangle.C };
					g.fillPolygon(tri);
				}
			}

			// draw bounds
			g.setColor(new Color(255, 255, 255, 255));
			

			Vector<SphericalCoord> outerBound = evt.getStonyBound(now);
			Vector3dDouble oldBoundaryPoint3d = null;

			if (outerBound != null) {
				for (SphericalCoord boundaryPoint : outerBound) {
					Vector3dDouble boundaryPoint3d = HEKEvent.convertToSceneCoordinates(boundaryPoint, now, 1.01);
					
					if (oldBoundaryPoint3d != null) {
						g.drawLine3d(oldBoundaryPoint3d.getX(), oldBoundaryPoint3d.getY(),oldBoundaryPoint3d.getZ(), boundaryPoint3d.getX(), boundaryPoint3d.getY(), boundaryPoint3d.getZ());
						
					}

					oldBoundaryPoint3d = boundaryPoint3d;
				}
			}

		}

	}

	/**
	 * The actual rendering routine
	 * 
	 * @param g
	 *            - PhysicalRenderGraphics to render to
	 * @param evt
	 *            - Event to draw
	 * @param now
	 *            - Current point in time
	 */
	public void drawIcon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {
		if (evt != null && evt.isVisible(now)) {
			boolean large = evt.getShowEventInfo();
			BufferedImage icon = evt.getIcon(large);
			if (icon != null) {
				SphericalCoord stony = evt.getStony(now);
				Vector3dDouble coords = HEKEvent.convertToSceneCoordinates(stony, now);
				g.drawImage3d(icon, coords.getX(), coords.getY(), coords.getZ(), scale);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Draws all available and visible solar events with there associated icon.
	 */
	public void render(PhysicalRenderGraphics g) {
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		if (masterView != null && masterView.getCurrentFrameDateTime() != null) {
			Date currentDate = masterView.getCurrentFrameDateTime().getTime();

			if (currentDate != null) {
				Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance()
						.getModel().getActiveEvents(currentDate);
				Log.info(toDraw);
				for (HEKEvent evt : toDraw) {
					drawPolygon(g, evt, currentDate);
				}

				for (HEKEvent evt : toDraw) {
					drawIcon(g, evt, currentDate);
				}
			}
		}
	}
	
	public void viewChanged(View view){
		GL3DLayeredView layeredView = ViewHelper.getViewAdapter(view, GL3DLayeredView.class);
		Log.debug("view : " + view);
		Log.debug("layeredView : " + layeredView);
		if (layeredView != null){
			GL3DImageTextureView imageTextureView = (GL3DImageTextureView) layeredView.getLayer(0);
			Region region = imageTextureView.getAdapter(RegionView.class).getRegion();
			if (region != null){
				if (height < 0) height = region.getHeight();
				else scale = (float) (region.getHeight() / height);
				Log.debug("regionHeight : " + region.getHeight());
				Log.debug("regionHeight : " + height);
				
			}
			Log.debug("layer : " + layeredView.getLayer(0));
			Log.debug("region : " + region);
		}
		/*if (imageTextureView != null){
			Region region =  imageTextureView.getAdapter(RegionView.class).getRegion();
		
			double heigth = region.getHeight() / (2 * Constants.SunRadius);
		double width = region.getWidth() / (2 * Constants.SunRadius);
		Log.debug("region : " + region);
		Log.debug("heigth : " + heigth);
		Log.debug("width  : " + width);
		if (heigth > 1.0) heigth = 1.0;
		if (width > 1.0) width = 1.0;
		scale = (float)Math.max(heigth, width);
		}
		*/
    }
	
}
