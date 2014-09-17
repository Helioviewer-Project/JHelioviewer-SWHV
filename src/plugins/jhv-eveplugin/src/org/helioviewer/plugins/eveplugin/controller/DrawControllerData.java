package org.helioviewer.plugins.eveplugin.controller;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;

public class DrawControllerData {

    private int nrOfDrawableElements;
    private Set<YAxisElement> yAxisSet;
    private Map<DrawableType, Set<DrawableElement>> drawableElements;
    private List<DrawControllerListener> listeners;

    public DrawControllerData() {
        nrOfDrawableElements = 0;
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new HashSet<YAxisElement>();
    }

    public int getNrOfDrawableElements() {
        return nrOfDrawableElements;
    }

    public void setNrOfDrawableElements(int nrOfDrawableElements) {
        this.nrOfDrawableElements = nrOfDrawableElements;
    }

    public Set<YAxisElement> getyAxisSet() {
        synchronized (yAxisSet) {
            return yAxisSet;
        }
    }

    public void setyAxisSet(Set<YAxisElement> yAxisSet) {
        this.yAxisSet = yAxisSet;
    }

    public Map<DrawableType, Set<DrawableElement>> getDrawableElements() {
        return drawableElements;
    }

    public void setDrawableElements(Map<DrawableType, Set<DrawableElement>> drawableElements) {
        this.drawableElements = drawableElements;
    }

    public List<DrawControllerListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<DrawControllerListener> listeners) {
        this.listeners = listeners;
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        listeners.add(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        listeners.remove(listener);

    }

    public void addDrawableElement(final DrawableElement element) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
                if (elements == null) {
                    elements = Collections.synchronizedSet(new HashSet<DrawableElement>());
                    drawableElements.put(element.getDrawableElementType().getLevel(), elements);
                }

                elements.add(element);

                Set<YAxisElement> tempSet = new HashSet<YAxisElement>(yAxisSet);
                tempSet.add(element.getYAxisElement());
                yAxisSet = tempSet;
            }

        });
        nrOfDrawableElements++;
    }

    public void removeDrawableElement(final DrawableElement element) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
                if (elements != null) {
                    synchronized (elements) {
                        elements.remove(element);
                        nrOfDrawableElements--;
                        if (elements.isEmpty()) {
                            synchronized (yAxisSet) {
                                yAxisSet.remove(element.getYAxisElement());
                            }
                        }
                    }
                }
            }
        });
    }
}
