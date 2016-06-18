package org.helioviewer.jhv.input;

import java.awt.EventQueue;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.JPanel;

import com.jogamp.newt.Window;

public class KeyShortcuts {

    private static final HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

    public static void registerKey(KeyStroke key, Action act) {
        actionMap.put(key, act);
    }

    public static void unregisterKey(KeyStroke key) {
        actionMap.remove(key);
    }

    // this is delicate
    private KeyShortcuts() {
        final JPanel dummy = new JPanel();
        final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                if (e.getSource() instanceof Window && handleKeyStroke(keyStroke, e.getSource(), e.getID())) {
                    kfm.redispatchEvent(dummy, e);
                    return true;
                }
                return false;
            }
        });
    }

    static boolean handleKeyStroke(KeyStroke keyStroke, Object source, int id) {
        if (actionMap.containsKey(keyStroke)) {
            final Action a = actionMap.get(keyStroke);
            final ActionEvent ae = new ActionEvent(source, id, null);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    a.actionPerformed(ae);
                }
            });
            return true;
        }
        return false;
    }

    private static final KeyShortcuts instance = new KeyShortcuts();

    public static KeyShortcuts getSingletonInstance() {
        return instance;
    }

}
