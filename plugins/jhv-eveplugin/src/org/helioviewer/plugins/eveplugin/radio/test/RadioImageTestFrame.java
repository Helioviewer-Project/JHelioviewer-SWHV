package org.helioviewer.plugins.eveplugin.radio.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.helioviewer.base.logging.Log;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

import javax.swing.JTabbedPane;

public class RadioImageTestFrame extends JFrame implements ViewListener {

    private JPanel contentPane;
    private ResolutionSetting rs;
    private JTabbedPane imageTabbedPane;
    private int count;
    private byte[] previousData;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    RadioImageTestFrame frame = new RadioImageTestFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public RadioImageTestFrame() {
        previousData = new byte[0];
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 870, 473);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        imageTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(imageTabbedPane, BorderLayout.CENTER);
        count = 0;
        setVisible(true);
    }

    @Override
    public synchronized void viewChanged(View sender, ChangeEvent aEvent) {
        StringBuffer text = new StringBuffer();
        text.append("Sender object = " + sender).append("\n");
        text.append("Event number : " + aEvent.hashCode()).append("\n");
        for (ChangedReason cr : aEvent.getAllChangedReasonsByType(CacheStatusChangedReason.class)) {
            text.append("CacheStatusChangedReasons : \n");
            text.append(cr.toString()).append("\n");
        }
        Log.debug("Sender object = " + sender);
        Log.debug("Event number : " + aEvent.hashCode());
        try {
            throw new Exception();
        } catch (Exception e1) {
            Log.error("Who send the event", e1);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e1.printStackTrace(pw);
            text.append("Who send the event \n").append(sw.toString()).append("\n");
            // e1.printStackTrace();
        }
        Log.debug("What event" + aEvent);
        text.append("What event : ").append(aEvent).append("\n");
        if (rs != null) {
            JHVJPXView jpxView = sender.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                byte[] data = new byte[0];
                SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jpxView.getSubimageData());
                if (imageData != null) {
                    Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
                    data = bytetrs.getByte8PixelData();
                    comparePreviousData(data, text);
                    previousData = data;
                    BufferedImage bi = createBufferedImage(rs.getWidth(), rs.getHeight(), data, 0);
                    TestRadioPane tempPane = new TestRadioPane(bi);
                    tempPane.setMinimumSize(new Dimension(rs.getWidth(), rs.getHeight()));
                    TextImageTestPanel titp = new TextImageTestPanel(text, tempPane);
                    // JScrollPane tempSP = new JScrollPane(tempPane);
                    imageTabbedPane.add("" + count, titp);
                    repaint();
                    count++;
                    return;
                } else {
                    text.append("Imagedata was null \n");
                    Log.debug("image data was null " + count);
                }
            } else {
                JHVJP2View jp2View = sender.getAdapter(JHVJP2View.class);
                byte[] data = new byte[0];
                SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData) (jp2View.getSubimageData());
                if (imageData != null) {
                    Byte8ImageTransport bytetrs = (Byte8ImageTransport) imageData.getImageTransport();
                    data = bytetrs.getByte8PixelData();
                    comparePreviousData(data, text);
                    previousData = data;
                    BufferedImage bi = createBufferedImage(rs.getWidth(), rs.getHeight(), data, 0);
                    TestRadioPane tempPane = new TestRadioPane(bi);
                    tempPane.setMinimumSize(new Dimension(rs.getWidth(), rs.getHeight()));
                    TextImageTestPanel titp = new TextImageTestPanel(text, tempPane);
                    // JScrollPane tempSP = new JScrollPane(tempPane);
                    imageTabbedPane.add("" + count, titp);
                    repaint();
                    count++;
                    return;
                } else {
                    text.append("Imagedata was null \n");
                    Log.debug("Imagedata was null " + count);
                }
            }
        } else {
            Log.debug("rs was null");
            text.append("rs was null");
        }
        TextImageTestPanel titp = new TextImageTestPanel(text);
        // JScrollPane tempSP = new JScrollPane(tempPane);
        imageTabbedPane.add("" + count, titp);
        repaint();
        count++;
    }

    private void comparePreviousData(byte[] data, StringBuffer text) {
        boolean different = false;
        int c = 0;
        text.append("Comparing this data with the previous data \n");
        if (data.length == previousData.length) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] != previousData[i]) {
                    text.append("Data different at position ").append(i).append("\n");
                    different = true;
                    c++;
                }
                if (c > 200) {
                    text.append("...\n");
                    break;
                }
            }
            if (different) {
                text.append("The data was different see result \n");
            } else {
                text.append("The data was not different \n");
            }
        } else {
            text.append("Data length was different \n");
        }

    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data, long ID) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        // Log.debug("databuffer = " + Arrays.toString(dataBuffer.getData()));
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    public void setResolutionSetting(ResolutionSetting rs) {
        this.rs = rs;
    }

    public synchronized void changeToFrame(int frame) {
        StringBuffer str = new StringBuffer("Jumped to frame " + frame);
        imageTabbedPane.add(new TextImageTestPanel(str), "Framejump " + count);
        count++;
        repaint();
    }
}
