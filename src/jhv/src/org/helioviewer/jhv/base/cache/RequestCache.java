package org.helioviewer.jhv.base.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.helioviewer.jhv.base.interval.Interval;

public class RequestCache {

    private final Map<Long, Interval> requestedAndDownloadedCache;
    private final Map<Long, Interval> requestedCache;

    public RequestCache() {
        requestedAndDownloadedCache = new TreeMap<Long, Interval>();
        requestedCache = new TreeMap<Long, Interval>();
    }

    public List<Interval> adaptRequestCache(long startDate, long endDate) {
        ArrayList<Interval> missingIntervals = new ArrayList<Interval>();
        Interval interval = new Interval(startDate, endDate);

        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(interval);
            requestedAndDownloadedCache.put(startDate, interval);
            requestedCache.put(startDate, interval);
        } else {
            missingIntervals = getMissingIntervals(interval);
            updateRequestCache(startDate, endDate);
        }
        return missingIntervals;
    }

    private void updateRequestCache(final long startDate, final long endDate) {
        List<Long> intervalsToRemove = new ArrayList<Long>();
        long addStart = startDate;
        long addEnd = endDate;

        Interval previousInterval = null;
        boolean startFound = false;
        boolean endFound = false;

        for (Map.Entry<Long, Interval> entry : requestedAndDownloadedCache.entrySet()) {
            long iStartDate = entry.getKey();
            Interval rcInterval = entry.getValue();

            // define start
            if (!startFound) {
                if (startDate < iStartDate) {
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
                                if (endDate < iStartDate) {
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
                            if (endDate < iStartDate) {
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
                if (endDate < previousInterval.start) {
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
            if (endDate < previousInterval.start) {
                endFound = true;
            } else {
                if (previousInterval.containsPointInclusive(endDate)) {
                    intervalsToRemove.add(previousInterval.start);
                    addEnd = previousInterval.end;
                } else {
                    if (startDate <= previousInterval.end) {
                        intervalsToRemove.add(previousInterval.start);
                    }
                }
            }
        }
        for (Long toRemove : intervalsToRemove) {
            requestedAndDownloadedCache.remove(toRemove);
            requestedCache.remove(toRemove);
        }
        Interval intervalToAdd = new Interval(addStart, addEnd);
        requestedAndDownloadedCache.put(intervalToAdd.start, intervalToAdd);
        requestedCache.put(intervalToAdd.start, intervalToAdd);
    }

    public void removeRequestedIntervals(Interval remInterval) {
        List<Interval> intervalsToAdd = new ArrayList<Interval>();
        List<Long> intervalsToRemove = new ArrayList<Long>();
        long start = remInterval.start;

        for (Map.Entry<Long, Interval> entry : requestedAndDownloadedCache.entrySet()) {
            Long isDate = entry.getKey();
            Interval rcInterval = entry.getValue();

            if (start <= rcInterval.start) {
                if (remInterval.end > rcInterval.start) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.end <= rcInterval.end) {
                        if (start != rcInterval.start) {
                            intervalsToAdd.add(new Interval(remInterval.end, rcInterval.end));
                        }
                        break;
                    } else {
                        start = rcInterval.end;
                    }
                }
            } else {
                if (rcInterval.end > start) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.end < rcInterval.end) {
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

        for (Long date : intervalsToRemove) {
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
        long startDate = interval.start;
        long endDate = interval.end;
        long currentStartDate = interval.start;
        boolean endDateUsed = false;
        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(new Interval(startDate, endDate));
        } else {
            Interval previousInterval = null;

            for (Map.Entry<Long, Interval> entry : requestedAndDownloadedCache.entrySet()) {
                long iStartDate = entry.getKey();
                Interval rcInterval = entry.getValue();

                if (currentStartDate < iStartDate) {
                    if (previousInterval == null) {
                        // No previous interval check if endate is also before
                        // startdate
                        if (endDate < iStartDate) {
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
                                if (endDate < iStartDate) {
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
                                if (endDate - iStartDate <= 0) {
                                    // 1)
                                    if (currentStartDate > previousInterval.end) {
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
            if (!endDateUsed && (currentStartDate >= previousInterval.start)) {
                // Check if start date is after end date of previous (last)
                // interval
                // 1) true: missing interval : {currentStartDate, endDate}
                // 2) false: check end date
                if (currentStartDate >= previousInterval.end) {
                    // 1)
                    missingIntervals.add(new Interval(currentStartDate, endDate));
                } else {
                    // 2)
                    // 1) endDate after previous end date: missing interval =
                    // {previousenddate, endDate}
                    // 2) internal interval do nothing
                    if (endDate > previousInterval.end) {
                        missingIntervals.add(new Interval(previousInterval.end, endDate));
                    }
                }
            }
        }
        return missingIntervals;
    }

}
