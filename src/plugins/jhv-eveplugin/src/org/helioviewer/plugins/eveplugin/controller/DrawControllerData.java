package org.helioviewer.plugins.eveplugin.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private List<DrawControllerListener> listeners;

    public DrawControllerData() {
        nrOfDrawableElements = 0;
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = Collections.synchronizedList(new ArrayList<DrawControllerListener>());
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

    public List<DrawControllerListener> getListeners() {
        synchronized (listeners) {
            List<DrawControllerListener> temp = new ArrayList<DrawControllerListener>(listeners);
            return temp;
        }

    }

    public void setListeners(List<DrawControllerListener> listeners) {
        synchronized (this.listeners) {
            this.listeners = listeners;
        }
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }

    }

    public void addDrawableElement(final DrawableElement element) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements == null) {
            elements = Collections.synchronizedSet(new HashSet<DrawableElement>());
            drawableElements.put(element.getDrawableElementType().getLevel(), elements);
        }

        elements.add(element);

        Set<YAxisElement> tempSet = new HashSet<YAxisElement>(yAxisSet);
        if (element.getYAxisElement() != null) {
            tempSet.add(element.getYAxisElement());
            yAxisSet = tempSet;
        }

        nrOfDrawableElements++;
    }

    public void removeDrawableElement(final DrawableElement element) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null) {
            synchronized (elements) {
                elements.remove(element);
                nrOfDrawableElements--;
                synchronized (yAxisSet) {
                    createYAxisSet();
                }
            }
        }
    }

    public Date getLastDateWithData() {
        Date lastDate = null;
        for (Set<DrawableElement> des : drawableElements.values()) {
            for (DrawableElement de : des) {
                if (de.getLastDateWithData() != null) {
                    if (lastDate == null || de.getLastDateWithData().before(lastDate)) {

                        lastDate = de.getLastDateWithData();
                    }
                }
            }
        }
        return lastDate;
    }

    private void createYAxisSet() {
        Set<YAxisElement> tempSet = new HashSet<YAxisElement>();
        for (Set<DrawableElement> elementsSet : drawableElements.values()) {
            for (DrawableElement de : elementsSet) {
                if (de.getYAxisElement() != null) {
                    tempSet.add(de.getYAxisElement());
                }
            }
        }
        yAxisSet = tempSet;
    }
}
