package org.helioviewer.gl3d.view;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
import org.helioviewer.gl3d.model.GL3DArtificialObjects;
import org.helioviewer.gl3d.model.GL3DFramebufferImage;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.gl3d.model.image.GL3DImageLayerFactory;
import org.helioviewer.gl3d.model.image.GL3DImageLayers;
import org.helioviewer.gl3d.model.image.GL3DImageMesh;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DModel;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DArrow;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodel.view.opengl.GLView;

/**
 * This is the most important view in the 3D viewchain. It assembles all 3D
 * Models in a hierarchical scene graph. Also it automatically adds new nodes (
 * {@link GL3DImageMesh}) to the scene when a new layer is added to the
 * {@link GL3DLayeredView}. Furthermore it takes care of setting the currently
 * active image region by performing a ray casting using the
 * {@link GL3DRayTracer} to find the maximally spanning image region within the
 * displayed scene.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSceneGraphView extends AbstractGL3DView implements GL3DView {
	private GL3DGroup root;

	// private GL3DImageGroup imageMeshes;
	private GLOverlayView overlayView = null;
	private GL3DImageLayers imageLayers;
	private GL3DHitReferenceShape hitReferenceShape;
	private GL3DFramebufferImage framebuffer;
	private GL3DGroup artificialObjects;

	private List<GL3DImageTextureView> layersToAdd = new ArrayList<GL3DImageTextureView>();
	private List<GL3DImageTextureView> layersToRemove = new ArrayList<GL3DImageTextureView>();

	private List<GL3DNode> nodesToDelete = new ArrayList<GL3DNode>();

	public GL3DSceneGraphView() {
		this.root = createRoot();
		// this.root = createTestRoot();

		printScenegraph();

		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				root.getDrawBits().toggle(Bit.BoundingBox);
				Displayer.getSingletonInstance().display();
				Log.debug("Toggling BoundingBox");
			}
		}, KeyEvent.VK_B);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				root.getDrawBits().toggle(Bit.Wireframe);
				Displayer.getSingletonInstance().display();
				Log.debug("Toggling Wireframe");
			}
		}, KeyEvent.VK_W);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				root.getDrawBits().toggle(Bit.Normals);
				Displayer.getSingletonInstance().display();				
				Log.debug("Toggling Normals");
			}
		}, KeyEvent.VK_N);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				framebuffer.getDrawBits().toggle(Bit.Hidden);
				Displayer.getSingletonInstance().display();				
				Log.debug("Toggling Framebuffer");
			}
		}, KeyEvent.VK_F);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				imageLayers.getDrawBits().toggle(Bit.Hidden);
				Displayer.getSingletonInstance().display();				
				Log.debug("Toggling Images");
			}
		}, KeyEvent.VK_I);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				toggleCoronaVisibility();
				Displayer.getSingletonInstance().display();				
				Log.debug("Toggling Corona Visibility");
			}
		}, KeyEvent.VK_C);
	}

	public void render3D(GL3DState state) {

		GL gl = state.gl;

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		gl.glBlendEquation(GL.GL_FUNC_ADD);
		deleteNodes(state);

		if (this.getView() != null) {
			state.pushMV();
			this.renderChild(gl);
			this.addLayersToSceneGraph(state);
			this.removeLayersFromSceneGraph(state);

			state.popMV();
		}

		if (state.getActiveCamera() == null) {
			Log.warn("GL3DSceneGraph: Camera not ready, aborting renderpass");
			return;
		}
		// gl.glBlendFunc(GL.GL_ONE, GL.GL_DST_ALPHA);
		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);

		state.pushMV();
		state.loadIdentity();
		this.root.update(state);
		state.popMV();

		state.pushMV();
		state.getActiveCamera().applyPerspective(state);
		state.getActiveCamera().applyCamera(state);

		if (overlayView != null)
			overlayView.preRender3D(state.gl);

		this.root.draw(state);

		if (overlayView != null)
			overlayView.postRender3D(state.gl);

		// Draw the camera or its interaction feedbacks
		state.getActiveCamera().drawCamera(state);

		// Resume Previous Projection
		state.getActiveCamera().resumePerspective(state);

		state.popMV();

		gl.glEnable(GL.GL_BLEND);
	}

	private void deleteNodes(GL3DState state) {
		for (GL3DNode node : this.nodesToDelete) {
			node.delete(state);
		}
		this.nodesToDelete.clear();
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		Log.debug("GL3DSceneGraphView.ViewChanged: Sender=" + newView
				+ " Event=" + changeEvent);

		// Add Handler of Layer Events. Automatically add new Meshes for each
		// Layer
		if (newView.getAdapter(LayeredView.class) != null) {
			LayeredView layeredView = ((LayeredView) newView
					.getAdapter(LayeredView.class));
			layeredView.addViewListener(new ViewListener() {

				public void viewChanged(View sender, ChangeEvent aEvent) {
					// Log.debug("viewChange: sender : " + sender);
					if (aEvent.reasonOccurred(LayerChangedReason.class)) {
						LayerChangedReason reason = aEvent
								.getLastChangedReasonByType(LayerChangedReason.class);
						handleLayerChange(reason);
					}
				}
			});

			for (int i = 0; i < layeredView.getNumLayers(); i++) {
				View layer = layeredView.getLayer(i);
				if (layer != null)
					this.addNewLayer(layer
							.getAdapter(GL3DImageTextureView.class));
				Log.debug("GL3DSceneGraphView: Adding Layer to Scene form LayeredView "
						+ layer);
			}
		}
	}

	private void handleLayerChange(LayerChangedReason reason) {
		GL3DImageTextureView imageTextureView = reason.getSubView().getAdapter(
				GL3DImageTextureView.class);
		if (imageTextureView != null) {

			switch (reason.getLayerChangeType()) {
			case LAYER_ADDED:
				addNewLayer(imageTextureView);
				break;
			case LAYER_REMOVED:
				removeLayer(imageTextureView);
				break;
			case LAYER_VISIBILITY:
				toggleLayerVisibility(imageTextureView);
				break;
			case LAYER_MOVED:
				moveLayerToIndex(imageTextureView, reason.getLayerIndex());
				break;
			default:
				break;
			}
		} else {
			Log.warn("GL3DSceneGraphView: Cannot handle Layer Change for Layers without a GL3DImageTextureView!");
		}
	}

	private void moveLayerToIndex(GL3DImageTextureView view, int layerIndex) {
		Log.debug("GL3DSceneGraphView.moveLayerToIndex " + layerIndex);
		this.imageLayers.moveImages(view, layerIndex);
	}

	private void toggleLayerVisibility(GL3DImageTextureView view) {
		GL3DNode node = this.imageLayers.getImageLayerForView(view);
		node.getDrawBits().toggle(Bit.Hidden);
	}

	private void removeLayersFromSceneGraph(GL3DState state) {
		synchronized (this.layersToRemove) {
			for (GL3DImageTextureView imageTextureView : this.layersToRemove) {
				((GL3DCameraView) getAdapter(GL3DCameraView.class))
						.removeCameraListener(this.imageLayers
								.getImageLayerForView(imageTextureView));
				this.imageLayers.removeLayer(state, imageTextureView);
			}
			this.layersToRemove.clear();
		}
	}

	private void addLayersToSceneGraph(GL3DState state) {
		GL3DCamera camera = state.getActiveCamera();

		synchronized (this.layersToAdd) {
			for (GL3DImageTextureView imageTextureView : this.layersToAdd) {
				GL3DImageLayer imageLayer = GL3DImageLayerFactory
						.createImageLayer(state, imageTextureView);

				((GL3DCameraView) getAdapter(GL3DCameraView.class))
						.addCameraListener(imageLayer);

				this.imageLayers.insertLayer(imageLayer);

				imageTextureView.addViewListener(framebuffer);

			}
			if (!this.layersToAdd.isEmpty()) {
				// If there is data, zoom to fit
				MetaDataView metaDataView = getAdapter(MetaDataView.class);
				if (metaDataView != null && metaDataView.getMetaData() != null) {
					Region region = metaDataView.getMetaData()
							.getPhysicalRegion();
					double halfWidth = region.getWidth() / 2;
					double halfFOVRad = Math.toRadians(camera.getFOV() / 2);
					double distance = halfWidth
							* Math.sin(Math.PI / 2 - halfFOVRad)
							/ Math.sin(halfFOVRad);
					distance = -distance - camera.getZTranslation();
					// Log.debug("GL3DZoomFitAction: Distance = "+distance+" Existing Distance: "+camera.getZTranslation());
					camera.addCameraAnimation(new GL3DCameraZoomAnimation(
							distance, 500));
				}
			}
			this.layersToAdd.clear();
		}
	}

	private void addNewLayer(GL3DImageTextureView imageTextureView) {
		synchronized (this.layersToAdd) {
			this.layersToAdd.add(imageTextureView);
		}
	}

	private void removeLayer(GL3DImageTextureView imageTextureView) {
		synchronized (this.layersToRemove) {
			if (!this.layersToRemove.contains(imageTextureView)
					&& this.imageLayers.getImageLayerForView(imageTextureView) != null) {
				this.layersToRemove.add(imageTextureView);
			}
		}
	}

	public int getNumberOfModels() {
		if (this.root == null) {
			return 0;
		}
		return this.root.getNumberOfChilds(GL3DModel.class);
	}

	public GL3DModel getModelAt(int index) {
		if (this.root == null) {
			return null;
		}
		return this.root.getModelAt(index);
	}

	public GL3DGroup getRoot() {
		return this.root;
	}

	public void deactivate(GL3DState state) {
		super.deactivate(state);
		this.getRoot().delete(state);
	}

	private GL3DGroup createRoot() {
		GL3DGroup root = new GL3DGroup("Scene Root");

		artificialObjects = new GL3DArtificialObjects();
		root.addNode(artificialObjects);

		this.imageLayers = new GL3DImageLayers();
		root.addNode(this.imageLayers);

		// this.overlayPlugins = new GL3DOverlayPlugins();
		// root.addNode(this.overlayPlugins);

		this.hitReferenceShape = new GL3DHitReferenceShape(true);
		root.addNode(this.hitReferenceShape);

		GL3DGroup indicatorArrows = new GL3DModel("Arrows",
				"Arrows indicating the viewspace axes");
		artificialObjects.addNode(indicatorArrows);

		GL3DShape north = new GL3DArrow("Northpole", Constants.SunRadius / 16,
				Constants.SunRadius, Constants.SunRadius / 2, 32,
				new GL3DVec4f(1.0f, 0.2f, 0.1f, 1.0f));
		north.modelView().rotate(-Math.PI / 2, GL3DVec3d.XAxis);
		indicatorArrows.addNode(north);

		GL3DShape south = new GL3DArrow("Southpole", Constants.SunRadius / 16,
				Constants.SunRadius, Constants.SunRadius / 2, 32,
				new GL3DVec4f(0.1f, 0.2f, 1.0f, 1.0f));
		south.modelView().rotate(Math.PI / 2, GL3DVec3d.XAxis);
		indicatorArrows.addNode(south);

		GL3DModel sunModel = new GL3DModel("Sun",
				"Spherical Grid depicting the Sun");
		artificialObjects.addNode(sunModel);
		// Create the sungrid
		// this.sun = new GL3DSphere("Sun-grid", Constants.SunRadius, 200, 200,
		// new GL3DVec4f(1.0f, 0.0f, 0.0f, 0.0f));
		// this.sun = new GL3DSunGrid(Constants.SunRadius,200,200, new
		// GL3DVec4f(0.8f, 0.8f, 0, 0.0f));

		// sunModel.addNode(this.sun);

		framebuffer = new GL3DFramebufferImage();
		artificialObjects.addNode(framebuffer);
		framebuffer.getDrawBits().on(Bit.Hidden);

		return root;
	}

	public GL3DHitReferenceShape getHitReferenceShape() {
		return hitReferenceShape;
	}

	public void toggleCoronaVisibility() {
		this.imageLayers.setCoronaVisibility(!this.imageLayers.getCoronaVisibility());
	}

	public void printScenegraph() {
		System.out.println("PRINTING SCENEGRAPH =======================>");

		printNode(root, 0);

	}

	public void setGLOverlayView(GLOverlayView overlayView) {
		this.overlayView = overlayView;
	}

	private void printNode(GL3DNode node, int level) {
		for (int i = 0; i < level; ++i)
			System.out.print("   ");

		if (node == null) {
			System.out.println("NULL");
			return;
		}

		System.out.println(node.getClass().getName() + " (" + node.getName()
				+ ")");

		/*
		 * GL3DNode sibling = node; while((sibling = sibling.getNext()) != null)
		 * { for(int i=0; i<level; ++i) System.out.print("   ");
		 * 
		 * System.out.println("Sibling: " + sibling.getClass().getName() + " ("
		 * + node.getName() + ")"); }
		 */

		if (node instanceof GL3DGroup) {
			GL3DGroup grp = (GL3DGroup) node;
			for (int i = 0; i < grp.numChildNodes(); ++i) {
				printNode(grp.getChild(i), level + 1);
			}
		}
	}

	protected void renderChild(GL gl) {
		if (view instanceof GLView) {
			((GLView) view).renderGL(gl, true);
		} else {
			textureHelper.renderImageDataToScreen(gl,
					view.getAdapter(RegionView.class).getRegion(), view
							.getAdapter(SubimageDataView.class)
							.getSubimageData(), view.getAdapter(JHVJPXView.class));
		}
	}

}
