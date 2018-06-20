/* globals tabId */
var tabId;
var temp_timingObject;

function populateData(){   
	
	//var storageKey = 'cache'+ tabId;
    chrome.storage.local.get('cache', function(data) {
     var timings = JSON.parse(data.cache['tab'+ tabId]);
		/*** Delete the cache now ***/
		chrome.storage.local.remove('tab'+tabId); //wont remove.. check background.js for correct line of code..
		/****************************/
		
	 var navigationTiming = timings["navtime"];		
	
	document.getElementById("OnContentLoad").innerHTML = (navigationTiming.domContentLoadedEventEnd-navigationTiming.navigationStart);
		
	 document.getElementById("redirectTime").innerHTML = (navigationTiming.redirectEnd-navigationTiming.redirectStart);
     document.getElementById("CacheFetchTime").innerHTML = (navigationTiming.domainLookupStart-navigationTiming.fetchStart);
     document.getElementById("dnsLookupTime").innerHTML = (navigationTiming.domainLookupEnd-navigationTiming.domainLookupStart);
     document.getElementById("ServerTime_ttfb").innerHTML = (navigationTiming.responseStart-navigationTiming.requestStart);
     document.getElementById("ServerTime_ttlb").innerHTML = (navigationTiming.responseEnd-navigationTiming.requestStart);
     document.getElementById("DownloadTime").innerHTML = (navigationTiming.responseEnd-navigationTiming.responseStart);
     document.getElementById("DomProcessingTime").innerHTML = (navigationTiming.domComplete-navigationTiming.domLoading);
     document.getElementById("OnLoad").innerHTML = (navigationTiming.loadEventEnd-navigationTiming.loadEventStart);
     document.getElementById("TotalPageLoadTime").innerHTML = (navigationTiming.loadEventEnd - navigationTiming.navigationStart);
     document.getElementById("ClientTime").innerHTML =((navigationTiming.loadEventEnd - navigationTiming.navigationStart)-(navigationTiming.responseEnd-navigationTiming.requestStart)); 
     
	 document.getElementById("NetworkLatency").innerHTML =(navigationTiming.responseEnd - navigationTiming.fetchStart); 
        console.log("###################PopupJS########################");
        console.log("PopupJS : totalJSHeapSize  "+ (timings.totalJSHeapSize));
        console.log("PopupJS : ResourceDetails  "+ (timings.resources));
        console.log("###################PopupJS########################");
	
	var HeapDetails = timings["memory"];		
    document.getElementById("TotalJSHeapSize").innerHTML =(HeapDetails.totalJSHeapSize); 
    
	
	document.getElementById("ResourceDetails").innerHTML =(timings["host"].name);
    /*document.getElementById("domElementCount").innerHTML =(timings["others"].domElementCount);
    document.getElementById("platform").innerHTML =(timings["platform"].Platform);
    document.getElementById("userAgent").innerHTML =(timings["platform"].UserAgent); 
	*/
    
        /*********** FOLLOWING CODE WILL EXECUTE CALL ONCE PLUGIN ICON IS CLICKED **********
        var xhr = new XMLHttpRequest();
        //xhr.setRequestHeader('Access-Control-Allow-Headers', '*');

        xhr.open('POST', 'http://localhost:8080/SampleWebApplication/AcceptUserInput.jsp',true);
       
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JsonString);
        *******************************/
	document.getElementById("ClientName").innerHTML =  JSON.stringify(timings["details"].ClientName);
	document.getElementById("ProjectName").innerHTML =  JSON.stringify(timings["details"].ProjectName);
    document.getElementById("RunID").innerHTML =  JSON.stringify(timings["details"].RunID);
		
	if((JSON.stringify(timings["ConfigUploadStatus"])).includes("Failed")) {
		document.getElementById("Status").innerHTML = "getConfigStatus = " + JSON.stringify(timings["ConfigUploadStatus"]);
		}
	/*else if ( ((JSON.stringify(timings["ConfigUploadStatus"])).replace(/['"]+/g, '') === "Success") &&  ((JSON.stringify(timings["ConfigUploadStatus"])).replace(/['"]+/g, '') === "Sucess"))*/
	else if (( (JSON.stringify(timings["ConfigUploadStatus"])).includes("Success"))  && 
	( (JSON.stringify(timings["jsonUploadStatus"])).includes("Success")))	
	{
		document.getElementById("Status").innerHTML =JSON.stringify(timings["jsonUploadStatus"]);
	} 
	else{		
		document.getElementById("Status").innerHTML = "getConfigStatus = " + JSON.stringify(timings["ConfigUploadStatus"]) + "\n .. PaceProcessorUpload Status = " + JSON.stringify(timings["jsonUploadStatus"]);
	}
		
	
	document.getElementById("TransactionName").innerHTML = JSON.stringify(timings["details"].transactionName);
	document.getElementById("TransactionName").href = (JSON.stringify(timings["downloadFoder"])).split('"').join('')+ (JSON.stringify(timings["SnapshotName"])).split('"').join('')+".jpeg";
	//"file:///C:/Z_Nitin/01.Download/"
		
		///// added on Jan 23rd 2017 to pass SpeedIndex.
	document.getElementById("SpeedIndex").innerHTML = JSON.stringify(timings["details"].speedIndex);
	
});
	
	chrome.storage.local.get('TransName', function(data) {
		console.log("Fetching value from storage for transaction Name");
		/** if checkbox is unselected or TransName storage doesnt exist then auto pick transaction Name from URL..**/
		if (!data.TransName){			document.getElementById('cb_transactionName').checked  =flase;
		}else{			document.getElementById('cb_transactionName').checked =true;		
		}		
	}); 	
};


chrome.tabs.getSelected(null, function (tab){	
	tabId = tab.id;
	temp_timingObject = "";
	populateData();
	addEventListenerToCB();
});
/*****
document.addEventListener() method works and can send data to content scripts.. but we dont want it because this method is called everytime when tab is refreshed or a new tab is called. However best for us will be to call a method only when the checkbox is selected or unchecked. But that is not possible as it would require additional permission and may not be a wise security violation.
Hence addEventListenerToCB() method is written. This method is called everytime the icon is clicked. It is better than calling everytime tab is loaded (document.addEventListener()). 
***/
/*document.addEventListener('DOMContentLoaded', function() {
    var link = document.getElementById('cb_transactionName');	
    // onClick's logic below:
    link.addEventListener('click', talktoContent);
});*/


function addEventListenerToCB(){
	var link = document.getElementById('cb_transactionName');	
    // onClick's logic below:
    link.addEventListener('change', talktoContent);
};

/**** chrome.tabs.query() method is able to send and recieved ack from Content script but it is not persistent. hence, information sent is lost when tab is reloaded or new tab is called... hence use Storage approach and never clear the storage... ***/

function talktoContent(){
	console.log("Setting up link with ContentScript..");
	if(cb_transactionName.checked){
		/*chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
			chrome.tabs.sendMessage(tabs[0].id, {enableTransaction: true}, function(response) {
			console.log("Received ack back from content script" + response.farewell);
			});
		});*/
		chrome.storage.local.set({'TransName': true}, function(){
			console.log("successfully enabled the transaction popup");
			chrome.storage.local.get('TransName', function (result) {   
				//alert(result.TransName);
   			});
		});
	}else{
		/*chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
			chrome.tabs.sendMessage(tabs[0].id, {enableTransaction: false}, function(response) {
			console.log("Received ack back from content script" + response.farewell);
			});
		});	*/	
		chrome.storage.local.set({'TransName': false}, function(){
			console.log("successfully disabled the transaction popup");
			chrome.storage.local.get('TransName', function (result) {
        		//alert(result.TransName);
   			});
		});
	}
};