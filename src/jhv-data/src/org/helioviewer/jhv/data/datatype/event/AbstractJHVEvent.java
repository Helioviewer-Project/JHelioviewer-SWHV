package org.helioviewer.jhv.data.datatype.event;

import java.awt.EventQueue;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJHVEvent implements JHVEvent {

    protected boolean highlighted;
    protected Set<JHVEventHighlightListener> listeners;
    private Object owner;

    public AbstractJHVEvent() {
        highlighted = false;
        listeners = new HashSet<JHVEventHighlightListener>();
    }

    @Override
    public void highlight(boolean isHighlighted, Object owner) {
        if ((isHighlighted || (!isHighlighted && owner == this.owner)) && isHighlighted != highlighted) {
            highlighted = isHighlighted;
            this.owner = owner;
            fireHighlightChanged();
        }
    }

    @Override
    public void addHighlightListener(JHVEventHighlightListener l) {
        listeners.add(l);
    }

    @Override
    public void removeHighlightListener(JHVEventHighlightListener l) {
        listeners.remove(listeners);
    }

    @Override
    public boolean isHighlighted() {
        return highlighted;
    }

    private void fireHighlightChanged() {
        // TODO Auto-generated method stub
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (JHVEventHighlightListener l : listeners) {
                    l.eventHightChanged(AbstractJHVEvent.this);
                }
            }
        });
    }
}
