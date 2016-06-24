package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.GZIPUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.EventDatabase.JsonEvent;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;

/**
 * A download worker will download events for a type of event from a source.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class DownloadWorker implements Runnable {

    private static final SWEKSourceManager sourceManager = SWEKSourceManager.getSingletonInstance();
    private final JHVEventType jhvType;
    private final JHVEventCache eventCache;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    public DownloadWorker(JHVEventType _jhvType, Interval interval, List<SWEKParam> params, JHVEventCache _eventCache) {
        requestInterval = interval;
        jhvType = _jhvType;
        eventCache = _eventCache;
        this.params = params;
    }

    public void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        SWEKSource swekSource = jhvType.getSupplier().getSource();
        SWEKDownloader downloader = sourceManager.getDownloader(swekSource);
        boolean success = downloader.extern2db(jhvType, requestInterval.start, requestInterval.end, params);
        if (success) {
            final ArrayList<JHVAssociation> associationList = EventDatabase.associations2Program(requestInterval.start, requestInterval.end, jhvType);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (JHVAssociation assoc : associationList) {
                        eventCache.add(assoc);
                    }
                }
            });
            SWEKParser parser = sourceManager.getParser(swekSource);
            ArrayList<JsonEvent> eventList = EventDatabase.events2Program(requestInterval.start, requestInterval.end, jhvType, params);
            for (JsonEvent event : eventList) {
                final JHVEvent ev = parser.parseEventJSON(JSONUtils.getJSONStream(GZIPUtils.decompress(event.json)), event.type, event.id, event.start, event.end, false);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        eventCache.add(ev);
                    }
                });
            }
        }

        if (!success) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    SWEKDownloadManager.getSingletonInstance().workerForcedToStop(DownloadWorker.this);
                }
            });
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    eventCache.finishedDownload(false);
                    SWEKDownloadManager.getSingletonInstance().workerFinished(DownloadWorker.this);
                }
            });
            EventDatabase.addDaterange2db(requestInterval.start, requestInterval.end, jhvType);
        }
    }

    public JHVEventType getJHVEventType() {
        return jhvType;
    }

    public SWEKEventType getEventType() {
        return jhvType.getEventType();
    }

    public Interval getRequestInterval() {
        return requestInterval;
    }

}
