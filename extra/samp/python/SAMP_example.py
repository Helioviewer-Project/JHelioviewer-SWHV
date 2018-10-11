
# coding: utf-8

# In[ ]:

import sunpy
import sunpy.map
from sunpy.net import vso

import datetime
from datetime import datetime
from datetime import timedelta

import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt

#get_ipython().magic('matplotlib inline')

import astropy.units as u
from astropy.io import fits
from astropy.vo.samp import SAMPIntegratedClient

from functools import reduce


# ### Config
# You can change the location where the fits-file will be downloaded by changing the value of the following variable

# In[ ]:

downloadPath = '/Data/{source}/{instrument}/{time.start}/{file}'


# ### Setup
# The following Cells contain used classes as well as the code responsible for connecting to a SAMP-Hub.
# 
# They need only to be executed once before JHelioviewer sends a SAMP-Message.

# In[ ]:

def parseDate(dateString):
    DATE_TIME_FORMAT_NO_MS = '%Y-%m-%dT%H:%M:%S'
    DATE_TIME_FORMAT = '%Y-%m-%dT%H:%M:%S.%f'
    
    try:
        timestamp = datetime.strptime(dateString, DATE_TIME_FORMAT)
    except ValueError:
        timestamp = datetime.strptime(dateString, DATE_TIME_FORMAT_NO_MS)
    
    return timestamp    


# In[ ]:

class CutoutInfo(object):
    def __init__(self, enabled, x0=0, y0=0, w=2458, h=2458):
        self.enabled = enabled
        self.x0 = x0 * u.arcsec
        self.y0 = y0 * u.arcsec
        self.h  = h  * u.arcsec
        self.w  = w  * u.arcsec
        
        self.a = u.Quantity([self.x0-self.w/2, self.x0+self.w/2])
        self.b = u.Quantity([self.y0-self.h/2, self.y0+self.h/2])                             


# In[ ]:

class LayerHandler(object):
    def __init__(self, client, timestamp, observatory, instrument, detector, measurement, cutout):        
        self.timestamp = parseDate(timestamp)
        self.observatory = observatory
        self.instrument = instrument
        self.detector = detector
        self.measurement = measurement
        self.cutout = cutout
        self.dataDownloaded = False
        
        self.__attr = self.__createAttr()
        self.__client = client
    
    def __createAttr(self):
        if(self.instrument == 'AIA'
           or self.instrument == 'EIT' 
           or self.instrument == 'SWAP'):
            return self.__createWaveAttr()
        
        elif(self.instrument == 'XRT'):
            #TODO: Filter wheel not supported yet
            return self.__createNoneAttr()
        
        elif(self.instrument == 'HMI'):
            #TODO: how to query HMI
            return self.__createPhysobsAttr()
        
        elif(self.instrument == 'LASCO'):
            return self.__createDetectorAttr()
            
        elif(self.instrument == 'MDI'):
            return self.__createPhysobsAttr()
           
        elif(self.instrument == 'SECCHI' 
             and self.detector == 'EUVI'):
            return self.__createFullAttr()
        
        elif(self.instrument == 'SECCHI'):
            return self.__createSecchiAttr()
        
        elif(self.instrument == 'TRACE'):
            #TODO: trace
            return self.__createWaveAttr()
        
        elif(self.instrument == 'SXT'):
            #TODO: sxt
            return self.__createNoneAttr()
            
            
    def __createWaveAttr(self):
        wave = int(self.measurement)
        return (vso.attrs.Instrument(self.instrument) 
                & vso.attrs.Wave(wave * u.AA, wave * u.AA))
    
    def __createDetectorAttr(self):
        return (vso.attrs.Instrument(self.instrument)
               & vso.attrs.Detector(self.detector))
    
    def __createPhysobsAttr(self):
        obs = self.measurement
        #TODO: 
        if (obs == 'CONTINUUM INTENSITY' or obs == 'FD_Continuum'):
            obs = 'intensity'
        elif (obs == 'MAGNETOGRAM' or obs == 'FD_Magnetogram' or obs == 'FD_Magnetogram_Sum'):
            obs = 'LOS_magnetic_field'
        return (vso.attrs.Instrument(self.instrument)
               & vso.attrs.Physobs(obs))
    
    def __createSecchiAttr(self):
        return (vso.attrs.Instrument(self.instrument)
               & vso.attrs.Source(self.observatory)
               & vso.attrs.Detector(self.detector))
    
    def __createFullAttr(self):
        wave = int(self.measurement)
        return (vso.attrs.Instrument(self.instrument)
               & vso.attrs.Source(self.observatory)
               & vso.attrs.Detector(self.detector)
               & vso.attrs.Wave(wave * u.AA, wave * u.AA))
    
    def __createNoneAttr(self):
        return vso.attrs.Instrument(self.instrument)
    
    def createQuery(self, start, end):
        self.dataDownloaded = False
        self.__fullAttr = vso.attrs.Time(start, end) & self.__attr
        self.query = self.__client.query(self.__fullAttr)
        return self.query
        
    def createQueryDelta(self, deltaFrom=timedelta(minutes = 2), deltaTo=timedelta(seconds=0)):        
        self.createQuery(self.timestamp - deltaFrom, self.timestamp + deltaTo)
        return self.query
    
    def createQuerySingleResult(self):          
        deltaFromCur = timedelta(minutes = 2)
        deltaFromMin = timedelta(seconds = 0)
        
        # images are usually stored all 3 minutes
        # unless something interesting happens, then it is every 3 seconds
        self.createQueryDelta(deltaFrom = deltaFromCur)
        
        if(len(self.query) > 1):
            self.createQueryDelta(deltaFrom = timedelta(seconds=5))
            if(len(self.query) != 1):
                deltaFromCur = timedelta(seconds=5)
        
        counter = 0
        while(len(self.query) != 1 and counter <= 10):
            
            if(len(self.query) < 1):
                deltaFromMin = deltaFromCur
                deltaFromCur = deltaFromCur * 2
            else:
                deltaFromCur = (deltaFromMin + deltaFromCur)/2
            self.createQueryDelta(deltaFrom = deltaFromCur)
            counter = counter+1
        return self.query
    
    def showQuery(self):
        print(self.query)
    
    def downloadData(self):
        self.files = self.__client.get(self.query, path=downloadPath).wait()
        self.dataDownloaded = True
    
    def showData(self, figsize=(15,15)):
        if(not self.dataDownloaded):
            self.downloadData()
        
        self.smap = list()
        
        if(self.cutout.enabled):
            self.submap = list()
                    
        for file in self.files:
            try:
                curMap = sunpy.map.Map(file)            
            except Exception as err:
                print("File could not be read as sunpy.map.Map(file)")
                print(file)
                print(err)
            else:
                self.smap.append(curMap)

                if(self.cutout.enabled):
                    curMap = curMap.submap(self.cutout.a, self.cutout.b)
                    self.submap.append(curMap)

                # TODO: make figsize configurable beforehand
                plt.figure(figsize=figsize)

                curMap.plot()
                curMap.draw_limb()

                plt.colorbar()
                plt.show()


# In[ ]:

class VSOHandler(object):
    def __init__(self, client, start, end, timestamp, cadence, layers):
        self.client = client
        self.timestamp = parseDate(timestamp)
        self.start     = parseDate(start)
        self.end       = parseDate(end)
        self.cadence   = cadence
        self.layers    = layers
    
    def createQuery(self):
        for layer in self.layers:
            layer.createQuery(self.start, self.end)
        
    def createQueryDelta(self, deltaFrom=timedelta(minutes = 2), deltaTo=timedelta(seconds=0)):        
        for layer in self.layers:
            layer.createQueryDelta(deltaFrom, deltaTo)
    
    def createQuerySingleResult(self):
        for layer in self.layers:
            layer.createQuerySingleResult()
    
    def showQuery(self):              
        attrs = reduce(lambda x,y: x|y, map(lambda z: z._LayerHandler__fullAttr, self.layers))
        return self.client.query(attrs)
    
    def downloadData(self):
        for layer in self.layers:
            layer.downloadData()
    
    def showData(self, figsize=(15,15)):                    
        for layer in self.layers:
            layer.showData(figsize)


# In[ ]:

class Receiver(object):
    def __init__(self, client):
        self.client = client
        self.received = False
    def receive_call(self, private_key, sender_id, msg_id, mtype, params, extra):
        self.params = params
        self.received = True
        self.client.reply(msg_id, {"samp.status": "samp.ok", "samp.result": {}})
    def receive_notification(self, private_key, sender_id, mtype, params, extra):
        self.params = params
        self.received = True
    
    def createHandler(self):
        if self.received:
            params = self.params
            self.received = False
            
            client = vso.VSOClient()
            timestamp = params['timestamp']
            start = params['start']
            end = params['end']
            cadence = params['cadence']
            layers = list()
            
            cutoutEnabled = bool(params['cutout.set'])
            if(cutoutEnabled):
                cutout = CutoutInfo(cutoutEnabled, 
                                    float(params['cutout.x0']),
                                    float(params['cutout.y0']),
                                    float(params['cutout.w']),
                                    float(params['cutout.h']))
            else:
                cutout = CutoutInfo(cutoutEnabled)
            
            for layerInfo in params['layers']:
                observatory = layerInfo['observatory']
                instrument = layerInfo['instrument']
                detector = layerInfo['detector']
                measurement = layerInfo['measurement']
                layerTimeStamp = layerInfo['timestamp']
                layers.append(LayerHandler(client, layerTimeStamp, observatory, instrument, detector, measurement, cutout))
            
            handler = VSOHandler(client, start, end, timestamp, cadence, layers)
            
            return handler


# The next cell connects to an active Hub (see http://docs.astropy.org/en/stable/vo/samp/example_table_image.html)

# In[ ]:

client = SAMPIntegratedClient()
client.connect()

r = Receiver(client)
client.bind_receive_call("jhv.vso.load", r.receive_call)
client.bind_receive_notification("jhv.vso.load", r.receive_notification)


# ### Downloading the data
# When a message is sent over SAMP, run the following to create a new Handler with the parameters given over SAMP.
# 
# A Handler object named handler will be created which offers several utility methods to create a VSOQuery and download the data.
# * __handler.createQuerySingleResult()__   
#   Creates a VSO-Query to only retrieve the currently show images in JHelioviewer for each active Layer
# * __handler.createQuery()__   
#   Creates a VSO-Query for the complete timespan visible in JHelioviewer
# * __handler.showQuery()__   
#   Can be used to check the query result before downloading the actual data
# * __handler.downloadData()__   
#   Downloads the data found by the current query
# * __handler.showData(figsize)__   
#   Downloads and shows the data by the current query
# 
# The start, endtime and timestamp can also be read on the handler.

# In[ ]:

# We test every 0.1s to see if the hub has sent a message
import time
while True:
    time.sleep(0.1)
    if r.received:
        handler = r.createHandler()
        break


# In[ ]:

print("Start:\t\t", handler.start)
print("Timestamp:\t", handler.timestamp)
print("End:\t\t", handler.end)


# First, we create a Query for fetching only the actual data for each Layer and check the results afterwards

# In[ ]:

#handler.createQuery()
handler.createQuerySingleResult()


# In[ ]:

handler.showQuery()


# The data can then be downloaded and displayed with showData().

# In[ ]:

#handler.downloadData()
handler.showData(figsize=(7,7))


# ### Using the data
# For each Layer that was visible in JHelioviewer, a corresponding Layer is created on the handler. They can be accessed indiviualy and provides the same method as the handler. Additionaly, the actual data as well as metadata is accessible.
# 
# The metadata can easily be accessed for each layer

# In[ ]:

layer = handler.layers[0]
print("Timestamp:\t", layer.timestamp)
print("Observatory:\t", layer.observatory)
print("Instrument:\t", layer.instrument)
print("Detector:\t", layer.detector)
print("Measurement:\t", layer.measurement)


# The data itself for each layer is available as a sunpy.Map in a list named smap. The data itself for each layer is available as a sunpy.Map in a list named smap.

# In[ ]:

print("Map:")
print(layer.smap[0])
print("Submap:")
print(layer.submap[0])

