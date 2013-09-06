package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL;
import javax.vecmath.Vector3d;

/**
 * 
 * Free flight cam
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 10.05.2011
 * 
 */
public class FreeFlightCamera extends AbstractCamera {

    private Vector3d center = new Vector3d(0, 0, 20);

    private int oldMouseX = 0;
    private int oldMouseY = 0;

    float yrot = 0;
    float xrot = 0;

    float mouseSense = 0.3f;
    float flySense = 2.2f;

    public void setView(GL gl) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glRotated(-xrot, 1, 0, 0);
        gl.glRotated(-yrot, 0, 1, 0);
        gl.glTranslated(center.x, center.y, center.z);

    }

    public void setProjection(GL gl, int width, int height) {
        double yxRatio = (float) width / height;
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45, yxRatio, near, far);
        gl.glViewport(0, 0, width, height);
    }

    public void mouseReleased(MouseEvent e) {
        oldMouseX = 0;
        oldMouseY = 0;
    }

    public void mouseDragged(MouseEvent e) {

        if (oldMouseX == 0 && oldMouseY == 0) {
            oldMouseX = e.getX();
            oldMouseY = e.getY();
            return;
        }

        int diffx = e.getX() - oldMouseX;
        int diffy = e.getY() - oldMouseY;
        oldMouseX = e.getX();
        oldMouseY = e.getY();

        xrot += diffy * mouseSense;
        yrot += diffx * mouseSense;

        clampRotation();
    }

    private void clampRotation() {
        if (xrot > 80) {
            xrot = 80;
        } else if (xrot < -80) {
            xrot = -80;
        }
        if (yrot < -360) {
            yrot += 360;
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == 'q') {
            xrot += 1;
            if (xrot > 360)
                xrot -= 360;
        }

        if (e.getKeyChar() == 'z') {
            xrot -= 1;
            if (xrot < -360)
                xrot += 360;
        }

        if (e.getKeyChar() == 'w') {
            float xrotrad, yrotrad;
            yrotrad = yrot / 180f * 3.141592654f;
            xrotrad = xrot / 180f * 3.141592654f;
            center.x -= (float) Math.sin(yrotrad) * flySense;
            center.y += (float) Math.sin(xrotrad) * flySense;
            center.z += (float) Math.cos(yrotrad) * flySense;
        }

        if (e.getKeyChar() == 's') {
            float xrotrad, yrotrad;
            yrotrad = yrot / 180 * 3.141592654f;
            xrotrad = xrot / 180 * 3.141592654f;
            center.x += (float) Math.sin(yrotrad) * flySense;
            center.y -= (float) Math.sin(xrotrad) * flySense;
            center.z -= (float) Math.cos(yrotrad) * flySense;
        }

        if (e.getKeyChar() == 'd') {
            float yrotrad;
            yrotrad = (yrot / 180 * 3.141592654f);
            center.x -= (float) Math.cos(yrotrad) * flySense;
            center.z -= (float) Math.sin(yrotrad) * flySense;
        }

        if (e.getKeyChar() == 'a') {
            float yrotrad;
            yrotrad = (yrot / 180 * 3.141592654f);
            center.x += (float) Math.cos(yrotrad) * flySense;
            center.z += (float) Math.sin(yrotrad) * flySense;
        }
    }

    public String getLabel() {
        return "Free Flight Camera";
    }

}
