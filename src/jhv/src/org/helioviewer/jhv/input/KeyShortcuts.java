package org.helioviewer.jhv.input;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.KeyStroke;

public class KeyShortcuts {

    private static final HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

    public static void registerKey(KeyStroke key, Action act) {
        actionMap.put(key, act);
    }

    public static void unregisterKey(KeyStroke key) {
        actionMap.remove(key);
    }

    static boolean handleKeyStroke(KeyStroke keyStroke, final Object source, final int id) {
        final Action action = actionMap.get(keyStroke);
        if (action != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    action.actionPerformed(new ActionEvent(source, id, null));
                }
            });
            return true;
        }
        return false;
    }

}
