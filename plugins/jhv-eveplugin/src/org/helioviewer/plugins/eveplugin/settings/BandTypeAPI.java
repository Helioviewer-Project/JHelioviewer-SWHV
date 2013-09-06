package org.helioviewer.plugins.eveplugin.settings;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.plugins.eveplugin.settings.APIAbstract;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.jhv.io.FileDownloader;
import org.json.JSONException;
import org.json.JSONObject;
import org.helioviewer.plugins.eveplugin.view.chart.RadioImagePane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.helioviewer.plugins.eveplugin.EVEPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;


public class BandTypeAPI extends APIAbstract{
	private static final BandTypeAPI singletonInstance = new BandTypeAPI();
	private BandType[] bandtypes;
	private Boolean isUpdated = Boolean.FALSE;	
	public HashMap<String, BandGroup> groups = new HashMap<String, BandGroup>();
	
	private Properties defaultProperties = new Properties();
	
	public static void main(String []args){
		BandTypeAPI bt = new BandTypeAPI();
	}
    public static BandTypeAPI getSingletonInstance() {
        return singletonInstance;
    }	
	public BandTypeAPI() {
		super();
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), false);
        //Settings.getSingletonInstance().load();
        /*Settings.getSingletonInstance().setProperty("eveplugin.baseurl.bandtype","http://127.0.0.1/");
        this.setBaseUrl(Settings.getSingletonInstance().getProperty("eveplugin.baseurl.bandtype"));
		// TODO Auto-generated constructor stub
        System.out.println(this.getBaseUrl());
        */
        this.loadSettings();
		this.setBaseUrl(defaultProperties.getProperty("plugin.eve.dataseturl"));
		//this.setBaseUrl("http://swhv.oma.be");
		this.updateDatasets();
	}

	private void loadSettings() {
        InputStream defaultPropStream = EVEPlugin.class.getResourceAsStream("/settings/eveplugin.properties");
        try {
			defaultProperties.load(defaultPropStream);
		} catch (IOException ex) {
			Log.error(">> Settings.load(boolean) > Could not load settings", ex);
		}
		
	}
	
	public String getDatasetUrl() {
		return this.getBaseUrl() + "/datasets";
	}
	
	public String getUrl() {
		return this.getBaseUrl();
	}
	
	public String readJSON(){
		String string = null;
		URI url = null;
		try {
			url = new URI(this.getDatasetUrl());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        final File dstFile = new File(JHVDirectory.PLUGINS.getPath() + "/EVEPlugin/datasets.json");
        try {
            DownloadStream ds = new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());         
            BufferedReader in = new BufferedReader(new InputStreamReader(ds.getInput()));
            StringBuilder sb = new StringBuilder();
            FileUtils.save(ds.getInput(), dstFile);
        } catch (final IOException e1) {
            Log.error("Error downloading the bandtypes.", e1);
        } catch (URISyntaxException e2) {
			// TODO Auto-generated catch block
        	Log.error("Malformed url", e2);
        }
        try {
        	string = FileUtils.read(dstFile);
        } catch (final IOException e1) {
            Log.error("Error reading the bandtypes.", e1);
        }
        return string;       
	}
	public void updateBandTypes(JSONArray jsonObjectArray){
        this.bandtypes = new BandType[jsonObjectArray.length()];
        try {
	        for(int i=0; i<jsonObjectArray.length(); i++){
	        	this.bandtypes[i] = new BandType();
	        	JSONObject job = (JSONObject)jsonObjectArray.get(i);
	
	        	if(job.has("label"))
	        		this.bandtypes[i].setLabel((String) job.get("label"));
	        	if(job.has("min"))
	        		this.bandtypes[i].setMin( job.getDouble("min") );
	        	if(job.has("max"))
	        		this.bandtypes[i].setMax( job.getDouble("max") );	        	
	        	if(job.has("unitLabel"))
	        		this.bandtypes[i].setUnitLabel((String) job.get("unitLabel"));
	        	if(job.has("baseUrl"))
	        		this.bandtypes[i].setBaseUrl((String) job.get("baseUrl"));
	        	if(job.has("warnLevels")){
	        		JSONArray warnLevels = job.getJSONArray("warnLevels");
	                for(int j=0; j<warnLevels.length(); j++){
	                	JSONObject helpobj =  (JSONObject)warnLevels.get(j);
	                	this.bandtypes[i].warnLevels.put((String)helpobj.get("warnLabel"),helpobj.getDouble("warnValue"));
	                }
	        	}
	        	if(job.has("group")){
	        		BandGroup group = this.groups.get(job.getString("group"));
	        		System.out.println(this.groups);
	        		System.out.println(group);
	        		System.out.println(job.getString("group"));
	        		group.add(this.bandtypes[i]);
	        		this.bandtypes[i].setGroup(group);
	        	}
	        	if(job.has("errorLevels")){
	        		JSONArray errorLevels = job.getJSONArray("errorLevels");
	        		double [] errlevels = new double[errorLevels.length()];
	                for(int j=0; j<errorLevels.length(); j++){	                	
	                	errlevels[j]= errorLevels.getDouble(j);
	                }
	                this.bandtypes[i].setErrorLevels(errlevels);
	        	}	        	
	        }
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
        	Log.error("JSON parsing error", e1);
		}        	       
	}
	public void updateBandGroups(JSONArray jsonGroupArray){
        this.bandtypes = new BandType[jsonGroupArray.length()];
        try {
	        for(int i=0; i<jsonGroupArray.length(); i++){
	        	BandGroup group = new BandGroup();
	        	JSONObject job = (JSONObject)jsonGroupArray.get(i);	
	        	if(job.has("groupLabel"))
	        		group.setGroupLabel(job.getString("groupLabel"));
	        	if(job.has("key")){
	        		group.setKey(job.getString("key"));
	        		this.groups.put(job.getString("key"), group);
	        	}
	        }
		} catch (JSONException e1) {
        	Log.error("JSON parsing error", e1);
		}        	       
	}

	public String updateDatasets_old() {	
        try {
        	String jsonString = readJSON();
            JSONObject jsonmain =  new JSONObject(jsonString);
            JSONArray jsonGroupArray = (JSONArray)jsonmain.get("groups");
            updateBandGroups(jsonGroupArray);
            JSONArray jsonObjectArray = (JSONArray)jsonmain.get("objects");            
            updateBandTypes(jsonObjectArray);
        	for(int j=0; j<this.bandtypes.length; j++){
        		System.out.println(this.bandtypes[j].toString());
        	}            
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
        	Log.error("JSON parsing error", e1);
		}	
        this.isUpdated = true;
        return null;
	}
	
	public void updateDatasets() {	
		final Thread t = new Thread( new Runnable(){ public void run(){
	        try {	        	
	        	String jsonString = readJSON();
	            JSONObject jsonmain =  new JSONObject(jsonString);
	            JSONArray jsonGroupArray = (JSONArray)jsonmain.get("groups");
	            updateBandGroups(jsonGroupArray);
	            JSONArray jsonObjectArray = (JSONArray)jsonmain.get("objects");            
	            updateBandTypes(jsonObjectArray);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
	        	Log.error("JSON parsing error", e1);
			}
		}} );
		t.start();
		try {
			t.join();
			Log.warn("Thread is dead");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        BandTypeAPI.this.isUpdated = true;		
	}
	
	
	
	public BandType[] getDatasets() {
		if(this.bandtypes==null){
			this.updateDatasets();
		}
		return this.bandtypes;
	}
	public BandType[] getBandTypes() {
		return bandtypes;
	}
	public BandType[] getBandTypes(BandGroup group) {
		return group.bandtypes.toArray(new BandType[group.bandtypes.size()]);
	}
	public BandGroup [] getGroups(){
		if(!isUpdated){
			updateDatasets();
		}
		return this.groups.values().toArray(new BandGroup[groups.size()]);
	}
}
