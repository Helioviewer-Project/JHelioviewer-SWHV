package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKRelatedEvents;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;

/**
 * A download worker will download events for a type of event from a source.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class DownloadWorker implements Runnable {

    private static final SWEKSourceManager sourceManager = SWEKSourceManager.getSingletonInstance();

    private final SWEKEventType eventType;
    private final SWEKSource swekSource;
    private final SWEKSupplier supplier;
    private boolean isStopped;
    private SWEKDownloader downloader;
    private SWEKParser parser;
    private final Date downloadStartDate;
    private final Date downloadEndDate;
    private final JHVEventContainer eventContainer;
    private final List<SWEKParam> params;
    private final List<SWEKRelatedEvents> relatedEvents;
    private final Interval<Date> requestInterval;

    public DownloadWorker(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier, Interval<Date> interval, List<SWEKParam> params, List<SWEKRelatedEvents> relatedEventRules) {
        isStopped = false;
        requestInterval = interval;
        this.swekSource = swekSource;
        this.eventType = eventType;
        downloadStartDate = new Date(interval.getStart().getTime());
        downloadEndDate = new Date(interval.getEnd().getTime());
        eventContainer = JHVEventContainer.getSingletonInstance();
        this.params = params;
        this.supplier = supplier;
        relatedEvents = relatedEventRules;
    }

    public void stopWorker() {
        isStopped = true;
        if (downloader != null) {
            downloader.stopDownload();
        }
        if (parser != null) {
            parser.stopParser();
        }
    }

    @Override
    public void run() {
        if (!isStopped) {
            downloader = sourceManager.getDownloader(swekSource);
            parser = sourceManager.getParser(swekSource);
            boolean moreDownloads = true;
            int page = 0;
            while (moreDownloads && !isStopped) {
                InputStream downloadInputStream = downloader.downloadData(eventType, downloadStartDate, downloadEndDate, params, page);
                if (downloadInputStream == null) {
                    isStopped = true;
                }
                else {
                    SWEKEventStream swekEventStream = parser.parseEventStream(downloadInputStream, eventType, swekSource, supplier, relatedEvents);
                    moreDownloads = swekEventStream.additionalDownloadNeeded();
                    distributeData(swekEventStream);
                    page++;
                }
            }

        }
        if (isStopped) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    SWEKDownloadManager.getSingletonInstance().workerForcedToStop(DownloadWorker.this);
                }
            });
        }
        else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    eventContainer.finishedDownload(false);
                    SWEKDownloadManager.getSingletonInstance().workerFinished(DownloadWorker.this);
                }
            });
        }
    }

    private void distributeData(final SWEKEventStream eventStream) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                while (eventStream.hasEvents() && !isStopped) {
                    eventContainer.addEvent(eventStream.next());
                }
                eventContainer.finishedDownload(true);
            }
        });
    }

    public JHVEventType getJHVEventType() {
        return new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName());
    }

    public Date getDownloadEndDate() {
        return downloadEndDate;
    }

    public SWEKEventType getEventType() {
        return eventType;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

    public Interval<Date> getRequestInterval() {
        return requestInterval;
    }

    @Override
    public int hashCode() {
        return (int) this.downloadStartDate.getTime();
    }
}
