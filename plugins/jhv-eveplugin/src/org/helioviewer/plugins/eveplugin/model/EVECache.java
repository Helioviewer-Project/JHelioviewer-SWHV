package org.helioviewer.plugins.eveplugin.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.EVEValues;

/**
 * 
 * @author Stephan Pagel
 * */
public class EVECache {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private final HashMap<Integer, EVEDataOfDay> cacheMap = new HashMap<Integer, EVEDataOfDay>();
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public void add(final EVEValue[] values) {
        GregorianCalendar calendar = new GregorianCalendar();
        if(values != null){
	        for (int i = 0; i < values.length; i++) {
	            calendar.setTime(values[i].date);
	            final Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
	            
	            EVEDataOfDay cache = cacheMap.get(key);
	            
	            if (cache == null) {
	                cache = new EVEDataOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
	                cacheMap.put(key, cache);
	            }
	            
	            cache.setValue(values[i]);
	        }
        }
    }
    
    public EVEValues getValuesInInterval(final Interval<Date> interval, double[] errorlevels) {
        final EVEValues result = new EVEValues();
        
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        final Integer keyEnd = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        
        calendar.setTime(interval.getStart());
        Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        
        while (key <= keyEnd) { 
            EVEDataOfDay cache = cacheMap.get(key);
            
            if (cache == null) {
                cache = new EVEDataOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            } 
            
            final EVEValue[] values = cache.getValuesInInterval(interval);
            if(errorlevels!=null){
	            for (int i = 0; i < values.length; i++) {
	            	boolean isGood = true;
	            	for(int j=0;j<errorlevels.length;j++){	            			            			
	            		if( values[i]==null||values[i].value ==null || values[i].value == errorlevels[j])
	            			isGood=false;
	                	
	            	}
	            	if(isGood){
	            		result.addValue(values[i]);
	            	}
	            }
            }
            else{
	            for (int i = 0; i < values.length; i++) {
	                	result.addValue(values[i]);
	            }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        }
        
        return result;
    }
    
    public Range getMinMaxInInterval(final Interval<Date> interval) {
        final Range result = new Range();
        
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        final Integer keyEnd = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        
        calendar.setTime(interval.getStart());
        Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        
        while (key <= keyEnd) {
            final EVEDataOfDay cache = cacheMap.get(key);
            
            if (cache == null) {
                continue;
            } 
            
            result.combineWith(cache.getMinMaxInInterval(interval));
        
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        }
        
        return result;
    }
    
    public List<Interval<Date>> getMissingDatesInInterval(final Interval<Date> interval) {
        LinkedList<Interval<Date>> result = new LinkedList<Interval<Date>>();
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        Integer keyEnd = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        
        calendar.setTime(interval.getStart());
        Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));        
        
        Interval<Date> gap = null;
        
        while (key <= keyEnd) {
            if (!cacheMap.containsKey(key)) {
                if (gap == null) {
                    gap = new Interval<Date>(calendar.getTime(), calendar.getTime());
                } else {
                    gap.setEnd(calendar.getTime());
                }
            } else {
                if (gap != null) {
                    result.add(gap);
                    gap = null;
                }
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        }
        
        if (gap != null) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            gap.setEnd(calendar.getTime());
            result.add(gap);
        }
        
        return result;
    }
}
