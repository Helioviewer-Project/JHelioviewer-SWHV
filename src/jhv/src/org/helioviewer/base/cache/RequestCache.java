package org.helioviewer.base.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.helioviewer.base.interval.Interval;

public class RequestCache {

    private final Map<Date, Interval<Date>> requestedAndDownloadedCache;
    private final Map<Date, Interval<Date>> requestedCache;

    public RequestCache() {
        requestedAndDownloadedCache = new TreeMap<Date, Interval<Date>>();
        requestedCache = new TreeMap<Date, Interval<Date>>();
    }

    public List<Interval<Date>> adaptRequestCache(Date startDate, Date endDate) {
        ArrayList<Interval<Date>> missingIntervals = new ArrayList<Interval<Date>>();
        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(new Interval<Date>(startDate, endDate));
            requestedAndDownloadedCache.put(startDate, new Interval<Date>(startDate, endDate));
            requestedCache.put(startDate, new Interval<Date>(startDate, endDate));
        } else {
            missingIntervals = getMissingIntervals(new Interval<Date>(startDate, endDate));
            updateRequestCache(startDate, endDate);
        }
        return missingIntervals;
    }

    private void updateRequestCache(Date startDate, Date endDate) {
        List<Date> intervalsToRemove = new ArrayList<Date>();
        Interval<Date> intervalToAdd = new Interval<Date>(startDate, endDate);
        Interval<Date> previousInterval = null;
        boolean startFound = false;
        boolean endFound = false;

        for (Map.Entry<Date, Interval<Date>> entry : requestedAndDownloadedCache.entrySet()) {
            Date iStartDate = entry.getKey();
            Interval<Date> rcInterval = entry.getValue();

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
                            intervalToAdd.setStart(previousInterval.getStart());
                            if (previousInterval.containsPointInclusive(endDate)) {
                                intervalToAdd.setEnd(previousInterval.getEnd());
                                endFound = true;
                                break;
                            } else {
                                if (endDate.before(iStartDate)) {
                                    intervalToAdd.setEnd(endDate);
                                    break;
                                } else {
                                    intervalsToRemove.add(iStartDate);
                                    previousInterval = rcInterval;
                                }
                            }
                            startFound = true;
                        } else {
                            intervalToAdd.setStart(startDate);
                            startFound = true;
                            if (endDate.before(iStartDate)) {
                                intervalToAdd.setEnd(endDate);
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
                if (endDate.before(previousInterval.getStart())) {
                    endFound = true;
                    break;
                } else {
                    if (previousInterval.containsPointInclusive(endDate)) {
                        intervalsToRemove.add(previousInterval.getStart());
                        intervalToAdd.setEnd(previousInterval.getEnd());
                        endFound = true;
                        break;
                    } else {
                        intervalsToRemove.add(previousInterval.getStart());
                        previousInterval = rcInterval;
                        continue;
                    }
                }
            }
        }
        if (!startFound) {
            if (previousInterval.containsPointInclusive(startDate)) {
                if (!previousInterval.containsPointInclusive(endDate)) {
                    intervalsToRemove.add(previousInterval.getStart());
                    intervalToAdd.setStart(previousInterval.getStart());
                } else {
                    intervalToAdd = previousInterval;
                    endFound = true;
                }
            }
        }
        if (!endFound) {
            if (endDate.before(previousInterval.getStart())) {
                endFound = true;
            } else {
                if (previousInterval.containsPointInclusive(endDate)) {
                    intervalsToRemove.add(previousInterval.getStart());
                    intervalToAdd.setEnd(previousInterval.getEnd());
                } else {
                    if (!startDate.after(previousInterval.getEnd())) {
                        intervalsToRemove.add(previousInterval.getStart());
                    }
                }
            }
        }
        for (Date toRemove : intervalsToRemove) {
            requestedAndDownloadedCache.remove(toRemove);
            requestedCache.remove(toRemove);
        }
        requestedAndDownloadedCache.put(intervalToAdd.getStart(), intervalToAdd);
        requestedCache.put(intervalToAdd.getStart(), intervalToAdd);
    }

    public void removeRequestedIntervals(Interval<Date> remInterval) {
        // Log.debug("remove interval : " + remInterval);
        List<Interval<Date>> intervalsToAdd = new ArrayList<Interval<Date>>();
        List<Date> intervalsToRemove = new ArrayList<Date>();
        Date start = remInterval.getStart();

        for (Map.Entry<Date, Interval<Date>> entry : requestedAndDownloadedCache.entrySet()) {
            Date isDate = entry.getKey();
            Interval<Date> rcInterval = entry.getValue();

            if (start.before(rcInterval.getStart()) || start.equals(rcInterval.getStart())) {
                if (remInterval.getEnd().after(rcInterval.getStart())) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.getEnd().before(rcInterval.getEnd()) || remInterval.getEnd().equals(rcInterval.getEnd())) {
                        if (!start.equals(rcInterval.getStart())) {
                            intervalsToAdd.add(new Interval<Date>(remInterval.getEnd(), rcInterval.getEnd()));
                        }
                        break;
                    } else {
                        start = rcInterval.getEnd();
                    }
                }
            } else {
                if (rcInterval.getEnd().after(start)) {
                    intervalsToRemove.add(isDate);
                    if (remInterval.getEnd().before(rcInterval.getEnd())) {
                        intervalsToAdd.add(new Interval<Date>(rcInterval.getStart(), start));
                        intervalsToAdd.add(new Interval<Date>(remInterval.getEnd(), rcInterval.getEnd()));
                        break;
                    } else {
                        intervalsToAdd.add(new Interval<Date>(rcInterval.getStart(), start));
                        start = rcInterval.getEnd();
                    }
                }
            }
        }

        for (Date date : intervalsToRemove) {
            requestedAndDownloadedCache.remove(date);
        }
        for (Interval<Date> intToAdd : intervalsToAdd) {
            requestedAndDownloadedCache.put(intToAdd.getStart(), intToAdd);
        }
    }

    public Collection<Interval<Date>> getAllRequestIntervals() {
        return requestedCache.values();
    }

    public ArrayList<Interval<Date>> getMissingIntervals(Interval<Date> interval) {
        ArrayList<Interval<Date>> missingIntervals = new ArrayList<Interval<Date>>();
        Date startDate = interval.getStart();
        Date endDate = interval.getEnd();
        Date currentStartDate = interval.getStart();
        boolean endDateUsed = false;
        if (requestedAndDownloadedCache.isEmpty()) {
            missingIntervals.add(new Interval<Date>(startDate, endDate));
        } else {
            Interval<Date> previousInterval = null;
            for (Date iStartDate : requestedAndDownloadedCache.keySet()) {
                if (currentStartDate.before(iStartDate)) {
                    if (previousInterval == null) {
                        // No previous interval check if endate is also before
                        // startdate
                        if (endDate.before(iStartDate)) {
                            // complete new interval
                            missingIntervals.add(new Interval<Date>(startDate, endDate));
                            previousInterval = requestedAndDownloadedCache.get(iStartDate);
                            break;
                        } else {
                            // overlapping interval => missing interval =
                            // {startDate, iStartDate}
                            // continue with interval = {iStartDate, endDate}
                            currentStartDate = iStartDate;
                            missingIntervals.add(new Interval<Date>(startDate, iStartDate));
                            previousInterval = requestedAndDownloadedCache.get(iStartDate);
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
                                    missingIntervals.add(new Interval<Date>(previousInterval.getEnd(), endDate));
                                    endDateUsed = true;
                                    break;
                                } else {
                                    missingIntervals.add(new Interval<Date>(previousInterval.getEnd(), iStartDate));
                                    currentStartDate = iStartDate;
                                }
                                previousInterval = requestedAndDownloadedCache.get(iStartDate);
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
                                if (endDate.before(iStartDate) || endDate.equals(iStartDate)) {
                                    // 1)
                                    if (currentStartDate.after(previousInterval.getEnd())) {
                                        missingIntervals.add(new Interval<Date>(currentStartDate, endDate));
                                    } else {
                                        missingIntervals.add(new Interval<Date>(previousInterval.getEnd(), endDate));
                                    }
                                    endDateUsed = true;
                                    break;
                                } else {
                                    // 2)
                                    missingIntervals.add(new Interval<Date>(currentStartDate, iStartDate));
                                    previousInterval = requestedAndDownloadedCache.get(iStartDate);
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
                    previousInterval = requestedAndDownloadedCache.get(iStartDate);
                }
            }
            // check if current start date is after or equal previous (last
            // interval) start date
            if (!endDateUsed && (currentStartDate.after(previousInterval.getStart()) || currentStartDate.equals(previousInterval.getStart()))) {
                // Check if start date is after end date of previous (last)
                // interval
                // 1) true: missing interval : {currentStartDate, endDate}
                // 2) false: check end date
                if (currentStartDate.after(previousInterval.getEnd()) || currentStartDate.equals(previousInterval.getEnd())) {
                    // 1)
                    missingIntervals.add(new Interval<Date>(currentStartDate, endDate));
                } else {
                    // 2)
                    // 1) endDate after previous end date: missing interval =
                    // {previousenddate, endDate}
                    // 2) internal interval do nothing
                    if (endDate.after(previousInterval.getEnd())) {
                        missingIntervals.add(new Interval<Date>(previousInterval.getEnd(), endDate));
                    }
                }
            }
        }
        return missingIntervals;
    }

}
