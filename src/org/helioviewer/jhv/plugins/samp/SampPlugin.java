package org.helioviewer.jhv.plugins.samp;

import org.astrogrid.samp.client.DefaultClientProfile;
import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.MaterialDesign;
import org.helioviewer.jhv.gui.components.TopToolBar.ButtonText;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class SampPlugin implements Plugin {

    private final ButtonText notifySamp = new ButtonText(Buttons.toolBar(MaterialDesign.MDI_SHARE_VARIANT), "SAMP", "Send SAMP message");
    private final SampClient sampHub = new SampClient(DefaultClientProfile.getProfile());

    @Override
    public void installPlugin() {
        ImageViewerGui.getToolBar().addPluginButton(notifySamp, e -> sampHub.notifyRequestData());
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getToolBar().removePluginButton(notifySamp);
    }

    @Override
    public String getName() {
        return "SAMP Plugin";
    }

    @Override
    public String getDescription() {
        return "This plugin implements Simple Applications Messaging Protocol";
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

    @Override
    public void saveState(JSONObject jo) {
    }

    @Override
    public void loadState(JSONObject jo) {
    }

}
