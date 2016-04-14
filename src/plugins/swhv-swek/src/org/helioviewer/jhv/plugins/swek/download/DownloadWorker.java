package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.database.JHVDatabase;
import org.helioviewer.jhv.database.JHVDatabase.JsonEvent;
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
    private volatile boolean isStopped;
    private final Date downloadStartDate;
    private final Date downloadEndDate;
    private final JHVEventContainer eventContainer;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    public DownloadWorker(JHVEventType jhvType, Interval interval, List<SWEKParam> params) {
        isStopped = false;
        requestInterval = interval;
        this.jhvType = jhvType;
        downloadStartDate = new Date(interval.start.getTime());
        downloadEndDate = new Date(interval.end.getTime());
        eventContainer = JHVEventContainer.getSingletonInstance();
        this.params = params;
    }

    public void stopWorker() {
        isStopped = true;
    }

    @Override
    public void run() {
        boolean success = false;
        if (!isStopped) {
            SWEKSource swekSource = jhvType.getSupplier().getSource();
            SWEKDownloader downloader = sourceManager.getDownloader(swekSource);
            success = downloader.extern2db(jhvType, downloadStartDate, downloadEndDate, params);
            if (success) {

                final ArrayList<JHVAssociation> associationList = JHVDatabase.associations2Program(downloadStartDate.getTime(), downloadEndDate.getTime(), jhvType);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (JHVAssociation assoc : associationList) {
                            JHVEventCache.getSingletonInstance().add(assoc);
                        }

                    }
                });
                SWEKParser parser = sourceManager.getParser(swekSource);
                ArrayList<JsonEvent> eventList = JHVDatabase.events2Program(downloadStartDate.getTime(), downloadEndDate.getTime(), jhvType, params);
                for (JsonEvent event : eventList) {
                    final JHVEvent ev = parser.parseEventJSON(JHVDatabase.decompress(event.json), event.type, event.id, event.start, event.end);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!isStopped) {
                                JHVEventCache.getSingletonInstance().add(ev);
                            }
                        }
                    });
                }
            }
        }
        if (isStopped || !success) {
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
                    eventContainer.finishedDownload(false);
                    SWEKDownloadManager.getSingletonInstance().workerFinished(DownloadWorker.this);
                }
            });
            JHVDatabase.addDaterange2db(downloadStartDate, downloadEndDate, jhvType);
        }
    }

    public JHVEventType getJHVEventType() {
        return jhvType;
    }

    public Date getDownloadEndDate() {
        return downloadEndDate;
    }

    public SWEKEventType getEventType() {
        return jhvType.getEventType();
    }

    public Interval getRequestInterval() {
        return requestInterval;
    }

    @Override
    public int hashCode() {
        return (int) downloadStartDate.getTime();
    }

}
