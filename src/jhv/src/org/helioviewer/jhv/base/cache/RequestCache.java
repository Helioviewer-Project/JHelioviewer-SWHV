package org.helioviewer.jhv.base.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.helioviewer.jhv.base.interval.Interval;

public class RequestCache {

    private final Map<Date, Interval> requestedAndDownloadedCache;
    private final Map<Date, Interval> requestedCache;

    public RequestCache() {
        requestedAndDownloadedCache = new TreeMap<Date, Interval>();
        requestedCache = new TreeMap<Date, Interval>();
    }

    public List<Interval> adaptRequestCache(Date startDate, Date endDate) {
        ArrayList<Interval> missingIntervals = new ArrayList<Interval>();
        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(new Interval(startDate, endDate));
            requestedAndDownloadedCache.put(startDate, new Interval(startDate, endDate));
            requestedCache.put(startDate, new Interval(startDate, endDate));
        } else {
            missingIntervals = getMissingIntervals(new Interval(startDate, endDate));
            updateRequestCache(startDate, endDate);
        }
        return missingIntervals;
    }

    private void updateRequestCache(final Date startDate, final Date endDate) {
        List<Date> intervalsToRemove = new ArrayList<Date>();
        Date addStart = startDate;
        Date addEnd = endDate;

        Interval previousInterval = null;
        boolean startFound = false;
        boolean endFound = false;

        for (Map.Entry<Date, Interval> entry : requestedAndDownloadedCache.entrySet()) {
            Date iStartDate = entry.getKey();
            Interval rcInterval = entry.getValue();

            // define start
            if (!startFound) {
                if (startDate.before(iStartDate)) {
                    if (previousInterval == null) {
                        startFound = true;
                        previousInterval = rcInterval;
                    } else {
                        // There was a previous interval. Check if start lies
                        // within previous interval
                        if (previousInterval.containsPointInclusive(startDate)) {
                            addStart = previousInterval.start;
                            if (previousInterval.containsPointInclusive(endDate)) {
                                addEnd = previousInterval.end;
                                endFound = true;
                                break;
                            } else {
                                if (endDate.before(iStartDate)) {
                                    addEnd = endDate;
                                    break;
                                } else {
                                    intervalsToRemove.add(iStartDate);
                                    previousInterval = rcInterval;
                                }
                            }
                            startFound = true;
                        } else {
                            addStart = startDate;
                            startFound = true;
                            if (endDate.before(iStartDate)) {
                                addEnd = endDate;
                                endFound = true;
                                break;
                            }
                            previousInterval = rcInterval;
                        }
                    }
                } else {
                    previousInterval = rcInterval;
                }
            } else {
                // define end
                if (endDate.before(previousInterval.start)) {
                    endFound = true;
                    break;
                } else {
                    if (previousInterval.containsPointInclusive(endDate)) {
                        intervalsToRemove.add(previousInterval.start);
                        addEnd = previousInterval.end;
                        endFound = true;
                        break;
                    } else {
                        intervalsToRemove.add(previousInterval.start);
                        previousInterval = rcInterval;
                        continue;
                    }
                }
            }
        }
        if (!startFound) {
            if (previousInterval.containsPointInclusive(startDate)) {
                if (!previousInterval.containsPointInclusive(endDate)) {
                    intervalsToRemove.add(previousInterval.start);
                    addStart = previousInterval.start;
                } else {
                    addStart = previousInterval.start;
                    addEnd = previousInterval.end;
                    endFound = true;
                }
            }
        }
        if (!endFound) {
            if (endDate.before(previousInterval.start)) {
                endFound = true;
            } else {
                if (previousInterval.containsPointInclusive(endDate)) {
                    intervalsToRemove.add(previousInterval.start);
                    addEnd = previousInterval.end;
                } else {
                    if (!startDate.after(previousInterval.end)) {
                        intervalsToRemove.add(previousInterval.start);
                    }
                }
            }
        }
        for (Date toRemove : intervalsToRemove) {
            requestedAndDownloadedCache.remove(toRemove);
            requestedCache.remove(toRemove);
        }
        Interval intervalToAdd = new Interval(addStart, addEnd);
        requestedAndDownloadedCache.put(intervalToAdd.start, intervalToAdd);
        requestedCache.put(intervalToAdd.start, intervalToAdd);
    }

    public void removeRequestedIntervals(Interval remInterval) {
        List<Interval> intervalsToAdd = new ArrayList<Interval>();
        List<Date> intervalsToRemove = new ArrayList<Date>();
        Date start = remInterval.start;

        for (Map.Entry<Date, Interval> entry : requestedAndDownloadedCache.entrySet()) {
            Date isDate = entry.getKey();
            Interval rcInterval = entry.getValue();

            if (start.before(rcInterval.start) || start.equals(rcInterval.start)) {
                if (remInterval.end.after(rcInterval.start)) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.end.before(rcInterval.end) || remInterval.end.equals(rcInterval.end)) {
                        if (!start.equals(rcInterval.start)) {
                            intervalsToAdd.add(new Interval(remInterval.end, rcInterval.end));
                        }
                        break;
                    } else {
                        start = rcInterval.end;
                    }
                }
            } else {
                if (rcInterval.end.after(start)) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.end.before(rcInterval.end)) {
                        intervalsToAdd.add(new Interval(rcInterval.start, start));
                        intervalsToAdd.add(new Interval(remInterval.end, rcInterval.end));
                        break;
                    } else {
                        intervalsToAdd.add(new Interval(rcInterval.start, start));
                        start = rcInterval.end;
                    }
                }
            }
        }

        for (Date date : intervalsToRemove) {
            requestedAndDownloadedCache.remove(date);
            requestedCache.remove(date);
        }
        for (Interval intToAdd : intervalsToAdd) {
            requestedAndDownloadedCache.put(intToAdd.start, intToAdd);
            requestedCache.put(intToAdd.start, intToAdd);
        }
    }

    public Collection<Interval> getAllRequestIntervals() {
        return requestedCache.values();
    }

    public ArrayList<Interval> getMissingIntervals(Interval interval) {
        ArrayList<Interval> missingIntervals = new ArrayList<Interval>();
        Date startDate = interval.start;
        Date endDate = interval.end;
        Date currentStartDate = interval.start;
        boolean endDateUsed = false;
        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(new Interval(startDate, endDate));
        } else {
            Interval previousInterval = null;

            for (Map.Entry<Date, Interval> entry : requestedAndDownloadedCache.entrySet()) {
                Date iStartDate = entry.getKey();
                Interval rcInterval = entry.getValue();

                if (currentStartDate.before(iStartDate)) {
                    if (previousInterval == null) {
                        // No previous interval check if endate is also before
                        // startdate
                        if (endDate.before(iStartDate)) {
                            // complete new interval
                            missingIntervals.add(new Interval(startDate, endDate));
                            previousInterval = rcInterval;
                            break;
                        } else {
                            // overlapping interval => missing interval =
                            // {startDate, iStartDate}
                            // continue with interval = {iStartDate, endDate}
                            currentStartDate = iStartDate;
                            missingIntervals.add(new Interval(startDate, iStartDate));
                            previousInterval = rcInterval;
                            continue;
                        }
                    } else {
                        // 1) start time before or equal previous end time
                        // 2) start time after previous end time
                        if (previousInterval.containsPointInclusive(currentStartDate)) {
                            // 1)
                            // look at end time
                            // 1) end time before or equal previous end time:
                            // internal interval => do nothing break.
                            // 2) end time after previous end time : partial
                            // overlapping internal continue with interval
                            // {previousendtime, endtime}

                            if (previousInterval.containsPointInclusive(endDate)) {
                                // 1))
                                break;
                            } else {
                                if (endDate.before(iStartDate)) {
                                    missingIntervals.add(new Interval(previousInterval.end, endDate));
                                    endDateUsed = true;
                                    break;
                                } else {
                                    missingIntervals.add(new Interval(previousInterval.end, iStartDate));
                                    currentStartDate = iStartDate;
                                }
                                previousInterval = rcInterval;
                                continue;
                            }

                        } else {
                            // 2)
                            // look at end time
                            // 1) endDate before or equal current start time:
                            // missing interval: {previousendtime, enddate}
                            // 2) endDate after current start time : missing
                            // interval: {previous end date, current start
                            // date}, continue with interval: {current start
                            // time, end time}
                            if (!previousInterval.containsPointInclusive(endDate)) {
                                if (endDate.compareTo(iStartDate) <= 0) {
                                    // 1)
                                    if (currentStartDate.after(previousInterval.end)) {
                                        missingIntervals.add(new Interval(currentStartDate, endDate));
                                    } else {
                                        missingIntervals.add(new Interval(previousInterval.end, endDate));
                                    }
                                    endDateUsed = true;
                                    break;
                                } else {
                                    // 2)
                                    missingIntervals.add(new Interval(currentStartDate, iStartDate));
                                    previousInterval = rcInterval;
                                    currentStartDate = iStartDate;
                                    continue;
                                }
                            } else {
                                endDateUsed = true;
                                break;
                            }
                        }
                    }
                } else {
                    previousInterval = rcInterval;
                }
            }
            // check if current start date is after or equal previous (last
            // interval) start date
            if (!endDateUsed && (currentStartDate.after(previousInterval.start) || currentStartDate.equals(previousInterval.start))) {
                // Check if start date is after end date of previous (last)
                // interval
                // 1) true: missing interval : {currentStartDate, endDate}
                // 2) false: check end date
                if (currentStartDate.after(previousInterval.end) || currentStartDate.equals(previousInterval.end)) {
                    // 1)
                    missingIntervals.add(new Interval(currentStartDate, endDate));
                } else {
                    // 2)
                    // 1) endDate after previous end date: missing interval =
                    // {previousenddate, endDate}
                    // 2) internal interval do nothing
                    if (endDate.after(previousInterval.end)) {
                        missingIntervals.add(new Interval(previousInterval.end, endDate));
                    }
                }
            }
        }
        return missingIntervals;
    }

}
