package org.helioviewer.gl3d;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.gl3d.gui.GL3DCameraMouseController;

/**
 * This singleton receives all KeyEvents by the
 * {@link GL3DCameraMouseController}. Register a GL3DKeyListener to be informed
 * about KeyEvents.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DKeyController extends KeyAdapter {
    private static final GL3DKeyController instance = new GL3DKeyController();

    private final HashMap<Integer, List<GL3DKeyListener>> listenerMap = new HashMap<Integer, List<GL3DKeyListener>>();

    private GL3DKeyController() {
    }

    public static GL3DKeyController getInstance() {
        return instance;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Log.debug("GL3DKeyController: KeyTyped: "+e.getKeyChar()+", "+e.getKeyCode());
        if (listenerMap.containsKey(e.getKeyCode())) {
            List<GL3DKeyListener> listeners = listenerMap.get(e.getKeyCode());
            // Log.debug("GL3DKeyController: Found List: "+e.getKeyChar());
            for (GL3DKeyListener l : listeners) {
                l.keyHit(e);
            }
        }
    }

    /**
     * Use the java.awt.event.KeyEvent constants as Keys
     *
     * @param listener
     * @param key
     */
    public void addListener(GL3DKeyListener listener, Integer key) {
        if (!listenerMap.containsKey(key)) {
            listenerMap.put(key, new ArrayList<GL3DKeyListener>());
        }
        listenerMap.get(key).add(listener);
    }

    public static interface GL3DKeyListener {

        public void keyHit(KeyEvent e);
    }
}
