package org.helioviewer.swhv.metadata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.helioviewer.swhv.objects3d.JPXViewWatcher;
import org.helioviewer.swhv.time.GlobalTimeListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

public class SWHVMetadataContainer implements GlobalTimeListener {
    private final static HashMap<String, ArrayList<SWHVMetadata>> layerMetadata = new HashMap<String, ArrayList<SWHVMetadata>>();
    private final static HashMap<String, Integer> layerNumbers = new HashMap<String, Integer>();
    private final static HashMap<String, ArrayList<JPXViewWatcher>> jpxViewWatchers = new HashMap<String, ArrayList<JPXViewWatcher>>();

    private static int lastLayer = -1;
    private static SWHVMetadataContainer singletonInstance = new SWHVMetadataContainer();
    private static long beginTime = Long.MAX_VALUE;
    private static long endTime = 0;
    private static long curTime = 0;

    private SWHVMetadataContainer() {

    }

    public static SWHVMetadataContainer getSingletonInstance() {
        return singletonInstance;
    }

    public Integer parseMetadata(final JPXViewWatcher jpxViewWatcher) {
        final JHVJPXView jpxView = jpxViewWatcher.getJPXView();
        String buildUid = null;
        try {
            buildUid = jpxView.getJP2Image().getValueFromXML("INSTRUME", "fits", 1);
            buildUid += jpxView.getJP2Image().getValueFromXML("TELESCOP", "fits", 1);
            buildUid += jpxView.getJP2Image().getValueFromXML("DETECTOR", "fits", 1);
            buildUid += jpxView.getJP2Image().getValueFromXML("WAVELNTH", "fits", 1);
        } catch (JHV_KduException e) {
            e.printStackTrace();
        }
        final String uid = buildUid;
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Integer currLayer = 0;
                try {
                    for (int i = 1; i <= jpxView.getMaximumFrameNumber(); i++) {
                        String date = jpxView.getJP2Image().getValueFromXML("DATE-OBS", "fits", i);

                        double hgltobs = 0.;
                        try {
                            String hgltobsString = jpxView.getJP2Image().getValueFromXML("CRLT_OBS", "fits", i);
                            if (hgltobsString == null) {
                                hgltobsString = jpxView.getJP2Image().getValueFromXML("HGLT_OBS", "fits", i);
                            }
                            if (hgltobsString == null) {
                                hgltobsString = jpxView.getJP2Image().getValueFromXML("HGLT-OBS", "fits", i);
                            }
                            hgltobs = Double.parseDouble(hgltobsString);
                        } catch (Exception e) {
                        }
                        double hglnobs = 0.;
                        try {
                            String hglnobsString = jpxView.getJP2Image().getValueFromXML("CRLN_OBS", "fits", i);
                            if (hglnobsString == null) {
                                hglnobsString = jpxView.getJP2Image().getValueFromXML("HGLN_OBS", "fits", i);
                            }
                            if (hglnobsString == null) {
                                hglnobsString = jpxView.getJP2Image().getValueFromXML("HGLN-OBS", "fits", i);
                            }
                            hgltobs = Double.parseDouble(hglnobsString);
                        } catch (Exception e) {
                        }
                        double dsun;
                        try {
                            String dsunString = jpxView.getJP2Image().getValueFromXML("DSUN_OBS", "fits", i);
                            if (dsunString == null) {
                                dsunString = jpxView.getJP2Image().getValueFromXML("DSUN", "fits", i);
                            }
                            dsun = Double.parseDouble(dsunString);
                        } catch (Exception e) {
                        }

                        double CRPIX1 = 0.;
                        try {
                            String crpixString = jpxView.getJP2Image().getValueFromXML("CRPIX1", "fits", i);
                            CRPIX1 = Double.parseDouble(crpixString);
                        } catch (Exception e) {
                        }
                        double CRPIX2 = 0.;
                        try {
                            String crpixString = jpxView.getJP2Image().getValueFromXML("CRPIX2", "fits", i);
                            CRPIX2 = Double.parseDouble(crpixString);
                        } catch (Exception e) {
                        }
                        double CDELT1 = 1.;
                        try {
                            String cdeltString = jpxView.getJP2Image().getValueFromXML("CDELT1", "fits", i);
                            CDELT1 = Double.parseDouble(cdeltString);
                        } catch (Exception e) {
                        }
                        double CDELT2 = 1.;
                        try {
                            String cdeltString = jpxView.getJP2Image().getValueFromXML("CDELT2", "fits", i);
                            CDELT2 = Double.parseDouble(cdeltString);
                        } catch (Exception e) {
                        }

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        SWHVMetadata md = new SWHVMetadata(dateFormat.parse(date), hgltobs, hglnobs, CRPIX1, CRPIX2, CDELT1, CDELT2, jpxView, i);
                        if (md.getDate().getTime() < beginTime) {
                            beginTime = md.getDate().getTime();
                            curTime = beginTime;
                        }
                        if (md.getDate().getTime() > endTime) {
                            endTime = md.getDate().getTime();
                        }
                        if (layerMetadata.containsKey(uid)) {
                            layerMetadata.get(uid).add(md);
                        } else {
                            ArrayList<SWHVMetadata> al = new ArrayList<SWHVMetadata>();
                            al.add(md);
                            layerMetadata.put(uid, al);
                        }

                        if (!layerNumbers.containsKey(uid)) {
                            lastLayer = lastLayer + 1;
                            layerNumbers.put(uid, lastLayer);
                        }
                        if (!jpxViewWatchers.containsKey(uid)) {
                            ArrayList<JPXViewWatcher> al = new ArrayList<JPXViewWatcher>();
                            al.add(jpxViewWatcher);
                            jpxViewWatchers.put(uid, al);
                        } else {
                            jpxViewWatchers.get(uid).add(jpxViewWatcher);
                        }
                        currLayer = layerNumbers.get(uid);
                    }
                    Collections.sort(layerMetadata.get(uid));

                } catch (JHV_KduException e1) {
                    e1.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                try {
                    while (jpxView.getMaximumAccessibleFrameNumber() != jpxView.getMaximumFrameNumber()) {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        thread.run();

        return layerNumbers.get(uid);

    }

    @Override
    public void currentTimeChanged(long timestamp) {
        curTime += (endTime - beginTime) / 100;
        if (curTime > endTime) {
            curTime = beginTime;
        }
        for (String key : layerMetadata.keySet()) {
            if (layerMetadata.containsKey(key)) {
                JHVJPXView jpxv = null;
                for (SWHVMetadata swhvmd : layerMetadata.get(key)) {
                    jpxv = swhvmd.getView();
                    if (swhvmd.getDate().getTime() > curTime) {
                        swhvmd.getView().setCurrentFrameNumber(swhvmd.getFrameNumber(), null, true);
                        break;
                    }
                }
                for (JPXViewWatcher jpxvw : this.jpxViewWatchers.get(key)) {
                    if (jpxvw.getJPXView() == jpxv) {
                        jpxvw.setActive(true);
                    } else {
                        jpxvw.setActive(false);
                    }

                }
            }
        }

    }

    @Override
    public void beginTimeChanged(long beginTime) {
    }

    @Override
    public void endTimeChanged(long endTime) {
    }

}
