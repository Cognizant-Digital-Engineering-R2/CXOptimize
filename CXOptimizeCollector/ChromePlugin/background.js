/* globals clientConfig, CxperfConfig, clientConfigvariable, configUrl, configDownloadStatus, jsonUploadStatus */

var clientConfigvariable;
var configUrl;
var configDownloadStatus;
var jsonUploadStatus;

var background = {    
    init: function() {
		if (clientConfig.onLoaded !=1){
			//alert("Calling Config First Time");
			configInitialization();
			clientConfig.onLoaded=1;			
		}else{
		//alert("Calling Config Second Time & datasource = " + clientConfigvariable.dataStoreUrl);
		}	
	
    }//init function
};

//Startup when Plugin loaded
background.init();

//Load the Config
function configInitialization(){	
	//alert("Loaded ClientConfig URL: " + clientConfig.beaconUrl);	
	//configUrl = clientConfig.beaconUrl +"PaceProcessor/getConfig?ClientName="+ clientConfig.clientName+"&ProjectName="+ clientConfig.projectName+ "&Scenario="+ clientConfig.scenarioName; 
	
    configUrl = clientConfig.beaconUrl +"getConfig?ClientName="+ clientConfig.clientName+"&ProjectName="+ clientConfig.projectName+ "&Scenario="+ clientConfig.scenarioName; 
    var xhr = new XMLHttpRequest();
        //xhr.setRequestHeader('Access-Control-Allow-Headers', '*');
    xhr.onreadystatechange = function(){
     if(xhr.readyState == 4){
		 try{
			clientConfigvariable = JSON.parse(xhr.response);
				   /* alert("got response = " + clientConfigvariable.staticResourceExtension
						 +", dataStoreUrl : " +clientConfigvariable.dataStoreUrl 
						+ ", Release :" + clientConfigvariable.Release 
						 + ", RunID: " + clientConfigvariable.RunID
						 + ",BuildNumber: " + clientConfigvariable.BuildNumber
						 + ",staticResourceExtension : " + clientConfigvariable.staticResourceExtension
						 + ",imageResourceExtension: " + clientConfigvariable.imageResourceExtension
						 );*/
				configDownloadStatus = ("Success");
		  }catch(err){
			  console.error("Failed to get Config details.. URL = " + configUrl);
			  configDownloadStatus = ("Failed to download Config .. Error : " + err.message);
			  console.log("Failed to download Config .. Error : " + err.message);
		  }
         }            
        };
         xhr.open('GET', configUrl,true); 
		xhr.timeout = 4000;//timeout 5 sec
		try{
         xhr.setRequestHeader('Content-Type', 'application/json');
         xhr.send();
		}catch(err){
			configDownloadStatus = ("Failed to download Config .. Error : " + err.message);
		}
};


var roe = chrome.runtime && chrome.runtime.sendMessage ? 'runtime' : 'extension';

chrome[roe].onMessage.addListener(function(response,sender,sendResponse){
	
	background.init();
    console.log(" OnMessage listener Called");
    //clientConfigvariable , clientConfig
    var localTimingvariable = {};
	var localDetailsvariable = {};
	
    localTimingvariable = (response.timing);
	localDetailsvariable = response.details;
	
	if(clientConfigvariable ==null || clientConfigvariable.staticResourceExtension == null){
		//alert ("ClientConfiguration Not loaded Properly. Status = " +configDownloadStatus + "<br> .Please check the Config URL => " + configUrl);
	
	//SEND ONLY CONFIG LOAD STATUS TO POP UP
		//console.log("ConfigSUploadStatus=" + configDownloadStatus);
		localTimingvariable["ConfigUploadStatus"]=configDownloadStatus;	
		localDetailsvariable["ClientName"] = clientConfig.clientName;
    	localDetailsvariable["ProjectName"] = clientConfig.projectName;
		localDetailsvariable["RunID"] = "N/A";
		localTimingvariable["details"] = localDetailsvariable;
		var JsonStringVariable = JSON.stringify(localTimingvariable);
		
		//console.log("ProjectName = "+ localTimingvariable["details"].ProjectName);
		
		chrome.storage.local.get('cache', function(data) {
				if (!data.cache) data.cache = {};
			   // data.cache['tab'+tabID] = response.timing;
			/***Remember -- "LocalStorage can only store strings, so if you pass anything else to it, it will convert it to a string. " ***/			
			//console.log("tabId = "+ sender.tab.id);  
			data.cache['tab' + sender.tab.id] = JsonStringVariable;
			chrome.storage.local.set(data);
			});
		
	}else {
	
    localDetailsvariable["ClientName"] = clientConfigvariable.ClientName;
    localDetailsvariable["ProjectName"] = clientConfigvariable.ProjectName;
    localDetailsvariable["Scenario"] = clientConfigvariable.Scenario;//clientConfig
    localDetailsvariable["dataStoreUrl"] = clientConfigvariable.dataStoreUrl;
    localDetailsvariable["Release"] = clientConfigvariable.Release;
    localDetailsvariable["RunID"] = clientConfigvariable.RunID;
    localDetailsvariable["BuildNumber"] = clientConfigvariable.BuildNumber;
    localDetailsvariable["staticResourceExtension"] = clientConfigvariable.staticResourceExtension;
    localDetailsvariable["imageResourceExtension"] = clientConfigvariable.imageResourceExtension;
		// ADDED as part of Nov 14changes..
    localDetailsvariable["source"] = "ChromePlugin";
	localDetailsvariable["NavType"] = "Hard";
	//localDetailsvariable["speedIndex"] = "50";
	localTimingvariable["details"] = localDetailsvariable;  
	
	var JsonStringVariable = JSON.stringify(localTimingvariable);
	
	var d = new Date();
	//console.log("Date =" + d);
	var dformat = formatDate(d);
	//console.log(dformat);
	var SnapshotName=localDetailsvariable.Scenario +"_"+localDetailsvariable.transactionName+"_"+dformat
	
	/****** SEND TO  PACE PROCESSOR SERVER ******/	
	//console.log("JsonString = "+ JSON.stringify(localTimingvariable));
	//var paceProcessorURL = clientConfig.beaconUrl +"PaceProcessor/insertStats";
	var beaconUrl = clientConfig.beaconUrl +"insertStats";
	var xhrPaceProcessor = new XMLHttpRequest(); 
	xhrPaceProcessor.onreadystatechange = function(){
     if(xhrPaceProcessor.readyState == 4){
        var paceProcessorResponse = JSON.parse(xhrPaceProcessor.response);
               console.log("response from API = "+ paceProcessorResponse["elasticSearch"].Status);
		 if (paceProcessorResponse["elasticSearch"].Status === "Success")
		 {
			 jsonUploadStatus ="Success";		
    
		 }else{
			 jsonUploadStatus = "Failed";
		 }
		localTimingvariable["ConfigUploadStatus"]=configDownloadStatus;
		localTimingvariable["jsonUploadStatus"] = jsonUploadStatus;
		localTimingvariable["SnapshotName"] = SnapshotName;
		 
		localTimingvariable["downloadFoder"] = clientConfig.downloadFolder;
		
		JsonStringVariable = JSON.stringify(localTimingvariable);
		//console.log("JsonString = "+ JsonStringVariable);
		 
		//var storageKey = 'cache'+sender.tab.id;
		chrome.storage.local.get('cache', function(data) {
				if (!data.cache) data.cache = {};
			   // data.cache['tab'+tabID] = response.timing;
			/***Remember -- "LocalStorage can only store strings, so if you pass anything else to it, it will convert it to a string. " ***/
			console.log("tabId = "+ sender.tab.id);  
				data.cache['tab' + sender.tab.id] = JsonStringVariable;
				chrome.storage.local.set(data);
			});    
      }            
        };
    xhrPaceProcessor.open('POST', beaconUrl,true);       
    xhrPaceProcessor.setRequestHeader('Content-Type', 'application/json');
    xhrPaceProcessor.send(JsonStringVariable); 
     // return true;
	/******************************************/
	if(clientConfig.screenCapture)
	{
		captureSnapshot(SnapshotName);
	}
	 
	
        //chrome.browserAction.setBadgeText({text: r.time, tabId: sender.tab.id});    
        // alert("got response = " + clientConfigvariable);	
 
	}//else over...
});

// cache eviction
chrome.tabs.onRemoved.addListener(function(tabId) {
	//alert("closing tab = " + tabId);
	//var storageKey = 'cache'+tabId;
    chrome.storage.local.get('cache', function(data) {
        if (data.cache) delete data.cache['tab'+ tabId];//+ tabId
        chrome.storage.local.set(data);
    });
});

function formatDate(date) {
        var d = new Date(date),
            month = '' + (d.getMonth() + 1),
            day = '' + d.getDate(),
            year = d.getFullYear();
			hour = d.getHours();
			minute = d.getMinutes();
        if (month.length < 2) month = '0' + month;
        if (day.length < 2) day = '0' + day;
		if (minute.length < 2) day = '0' + minute;
        return [month, day].join('-');
    }

/*chrome.extension.onRequest.addListener(function(response,sender,sendResponse){
    alert(response.loadEventStart);    
    //alert(response);  
    sendResponse();
});*/

function captureSnapshot(SnapshotName){
	console.log("Snapshot saved with name " +  SnapshotName);
	chrome.tabs.captureVisibleTab(null, {"format":"jpeg"}, function(imgUrl) {
	var aLink = document.createElement('a');
    var evt = document.createEvent("HTMLEvents");
    evt.initEvent("click");
    aLink.download = SnapshotName+'.jpeg';
	aLink.href = imgUrl;	
	//aLink.dispatchEvent(evt);
	aLink.click();
	console.log("Snapshot taken - "+ SnapshotName);
		//console.log("ImageURL ="+imgUrl);
 });
};