package org.helioviewer.jhv.display;

import java.util.LinkedList;

public class Displayer{
	
    private static Displayer instance = new Displayer();
    private final LinkedList<DisplayListener> listeners = new LinkedList<DisplayListener>();
    

	public static Displayer getSingletonInstance() {
        if (instance == null) {
            throw new NullPointerException("Displayer not initialized");
        }
        return instance;
    }
    public void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(final DisplayListener listener) {
        listeners.remove(listener);
    }
    
    public void display(){
        for(final DisplayListener listener : listeners) {
            listener.display();
        }    	
    }

}
