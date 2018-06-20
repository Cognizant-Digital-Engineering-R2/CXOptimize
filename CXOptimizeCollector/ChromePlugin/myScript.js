var timingsLoop={};
var myVar;
var enableTransactionName=true;

function measure(){
    console.log("loading performance api javascript in current window");
    //navigationalAPI();
	navigationalAPI_loop();
    
};

function navigationalAPI_loop(){
	var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};
    timingsLoop= performance.timing;
	if(timingsLoop.loadEventEnd ==0){
			console.log("loadEvent = "+ timingsLoop.loadEventEnd + " ..going in loop of 0.1 sec");
	myVar = setInterval(runPerformanceAPI, 100);
	}else{
		runPerformanceAPI();
	}	
	
};
function runPerformanceAPI() {
	var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};
    timingsLoop= performance.timing;
	//console.log("calling runPerformanceAPI() ");
    if(timingsLoop.loadEventEnd >0){	
		
		console.log("loadEvent load and value is  = "+ timingsLoop.loadEventEnd);
		clearInterval(myVar);
		var roe = chrome.runtime && chrome.runtime.sendMessage ? 'runtime' : 'extension';
        
        var t = {};
		var navigationAPIMetrics = {};
        for (var p in timingsLoop) {
              console.log("Printing timings variables "+ p + " : value = "+ timingsLoop[p]);
              if (typeof(timingsLoop[p]) !== "function") {
                   //t[p] = timings[p];
				  navigationAPIMetrics[p] = timingsLoop[p];
                 }
             }
		t["navtime"]=navigationAPIMetrics;
		
		//////////// MEMORY DETAILS
        var performance = window.console || {};
        var perfjsmemory = performance.memory || {}; 
        var memoryDetails = {};
        for (var p in perfjsmemory) {
              console.log("Printing timings variables "+ p + " : value = "+ perfjsmemory[p]);
              if (typeof(perfjsmemory[p]) !== "function") {
                   memoryDetails[p] = perfjsmemory[p];   
                 }
             }
		t["memory"] = memoryDetails;
    
		/////////////RESOURCE DETAILS
		var perfResource = window.performance || {};
		var resourceTemp = perfResource.getEntriesByType("resource") ||{};		
		var resourceArrayMapper = [{
			
		}];		
		
		var EndIteration = 0;
		//set EndIteration variable to know what is the last element in resourceTemp [].		
		if(resourceTemp.length >1){
			EndIteration = resourceTemp.length -1;
		}else{
			EndIteration = 0;
		}		
		var firstReqFetchTime = resourceTemp[0].fetchStart;
		console.log("First request fetch start : " +firstReqFetchTime);
		var lastReqResponseEnd;
		
		for (var i=0; i < resourceTemp.length; i++) {	
			
			if(i == EndIteration){
				lastReqResponseEnd=resourceTemp[i].responseEnd;
				console.log("last request response end: " +lastReqResponseEnd);
			}
			//console.log("ith resource: "+ resourceTemp[i].name) ;
		
			resourceArrayMapper[i]={};			
			for(j in resourceTemp[i]){
				 //console.log(" j = "+ j + " : value = "+ resourceTemp[i][j]);
				 if (typeof(resourceTemp[i][j]) !== "function") {
					 resourceArrayMapper[i][j]=resourceTemp[i][j];
				 }			 			 
			 }			
		}
		//console.log("Iteration Over...." + JSON.stringify(resourceArrayMapper))	;		
		t["resources"]=resourceArrayMapper;
		
		/******************************/
		
		////////////DOM ELEMENT COUNT
		var domDetails = {};
		var domElementLength = document.getElementsByTagName('*').length;
		console.log("DOM element Length = "+ domElementLength);
		domDetails["domElementCount"] = domElementLength;
    
		//////// PLATFORM DETAILS..
		var platformDetails ={};
		var nplatform = navigator.platform|| {};
		console.log("Platform Details = " + nplatform);
		platformDetails["Platform"]=nplatform;
    
		//////////// USER AGENT	
		var nAgt = navigator.userAgent|| {};		
		console.log("UserAgent Details Captured Final = "+ nAgt);
		platformDetails["UserAgent"] = nAgt;
    
		t["platform"] = platformDetails;
	
		///////////PAGE SOURCE
		//var pageSource = document.documentElement.innerHTML;
		var pageSource = document.documentElement.outerHTML;
		//var pageSource = document.all[0].outerHTML;
		
		//console.log("page source = "+ pageSource);
		domDetails["dom"] = pageSource;

		//t["others"] = domDetails;
		
		///////////Get paint time

		var msPaintTime; 
		if((window.chrome.loadTimes().firstPaintTime - window.chrome.loadTimes().startLoadTime) < 3600) 
		{
			msPaintTime = window.chrome.loadTimes().firstPaintTime * 1000;
		}
		else
		{
			msPaintTime = window.performance.timing.loadEventEnd;
		}
		domDetails["msFirstPaint"] = msPaintTime;
		
		t["others"] = domDetails;
    
		
		//console.log("JsonString = "+ JsonStringVariable);
		
		
		
		///////////Get URL and URI details.
		var protocolUsed = window.location.protocol; 
		var webHost = window.location.host; 		
	    var webURI =   window.location.pathname+window.location.search;
		var pathArray = window.location.href.split(webHost);
		var detailsMap = {};
		var host = {};
		
		
		/////////// Get Page Speed Index...
		var speedIndexResult = RUMSpeedIndex();
   		console.log("Rum Speed Index = " + speedIndexResult);
		//speedIndexResult = Math.round(speedIndexResult);
		speedIndexResult = Math.max(speedIndexResult,0)
		
		detailsMap["speedIndex"]= speedIndexResult;
		detailsMap["resourceLoadTime"] = lastReqResponseEnd-firstReqFetchTime;
		
		host["name"]=webHost;
		t["host"] = host;
		webURI = pathArray[1];
		//console.log("URL = "+ webURI);
		//console.log("Tranasction name = "+ webURI.replace(/[^a-z0-9\w\s]/gi,"_"));
    	//detailsMap["transactionName"]= webURI.replace(/[^a-z0-9\w\s]/gi,"_");
		
		var transaction_name;
		/**** call the local storage to check if Transaction Name pop to be launched or not.. since it is a async call, hence rest of the code is return in the call back function.... if you write chrome.sendMessage() outside the callback function then there is a great chance that chrome.sendMessage() will send transaction_name as undefined variable..	
		***/
		chrome.storage.local.get('TransName', function(data) {
			console.log("Fetching value from storage for transaction Name");
			console.log(data);
			/** if checkbox is unselected or TransName storage doesnt exist then auto pick transaction Name from URL..**/
				if (!data.TransName) {enableTransactionName=false;}
			    else{enableTransactionName = data.TransName;
					console.log("got value from storage for transaction Name ="+ enableTransactionName);
					}
			   
				console.log("value of enableTransactionName =" + enableTransactionName);
				if(enableTransactionName)
				{transaction_name = TransactionNamePopUP();}
				else{
				 transaction_name= webURI.replace(/[^a-z0-9\w\s]/gi,"_"); //working code..
				}
			
		console.log("TransactionName ="+ transaction_name);
		detailsMap["transactionName"]= transaction_name;
   		detailsMap["url"]=window.location.href;	
		
		
			
		var JsonStringVariable = JSON.stringify(t);
    
        chrome[roe].sendMessage({timing: t, JsonString: JsonStringVariable, details: detailsMap}, function(responseText){console.log("Message Sent to Background Script.. " + JSON.stringify(responseText));});
		
		}); 
		
					
		
			
	}else{
	console.log("runPerformanceAPI loadEvent = "+ timingsLoop.loadEventEnd + " ..going in loop of 0.1 sec");
   }
};
//timingAPI();
measure();


function TransactionNamePopUP(){
	var transaction_name = prompt("Please enter the transaction name", " ");
	return transaction_name;
	
}//TransacationNamePopUP() ends...

/****** call it if using "run_at": "document_end".. more details in manifest.json end..
window.onload = function(){
  setTimeout(function(){
    measure();
  }, 0);
}
**/

/******** to get title of the page 
chrome.runtime.sendMessage(document.getElementsByTagName('title')[0].innerText);
************/

/***** Legacy code.. not requried.. will be removed in future versions ...*****/
function timingAPI(){
	
	 if (document.readyState == "complete") {
        console.log("READY COMPLETE");
        measure();
    } else {
       window.addEventListener("load", measure);
    }       
};
function navigationalAPI(){
        var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};
        timings= performance.timing;
        var roe = chrome.runtime && chrome.runtime.sendMessage ? 'runtime' : 'extension';
        
        var t = {};
		var navigationAPIMetrics = {};
        for (var p in timings) {
              console.log("Printing timings variables "+ p + " : value = "+ timings[p]);
              if (typeof(timings[p]) !== "function") {
                   //t[p] = timings[p];
				  navigationAPIMetrics[p] = timings[p];
                 }
             }
		t["navtime"]=navigationAPIMetrics;
        console.log("redirectTime: "+ (timings.redirectEnd-timings.redirectStart));
        console.log("CacheFetchTime: "+ (timings.domainLookupStart-timings.fetchStart));
        console.log("dnsLookupTime: "+ (timings.domainLookupEnd-timings.domainLookupStart));
        console.log("ServerTime_ttfb: "+ (timings.responseStart-timings.requestStart));
        console.log("ServerTime_ttlb: "+ (timings.responseEnd-timings.requestStart));
        console.log("DownloadTime: "+ (timings.responseEnd-timings.responseStart));
        console.log("DomProcessingTime: "+ (timings.domComplete-timings.domLoading));
        console.log("OnLoadTime: "+ (timings.loadEventEnd-timings.loadEventStart));
        console.log("TotalPageLoadTime: "+ (timings.loadEventEnd - timings.navigationStart));
        console.log("ClientTime: "+ ((timings.loadEventEnd - timings.navigationStart)-(timings.responseEnd-timings.requestStart)));
    
    /****************** Writing Code on 5/19 **************/
    //////////// MEMORY DETAILS
        var performance = window.console || {};
        var perfjsmemory = performance.memory || {}; 
        var memoryDetails = {};
        for (var p in perfjsmemory) {
              console.log("Printing timings variables "+ p + " : value = "+ perfjsmemory[p]);
              if (typeof(perfjsmemory[p]) !== "function") {
                   memoryDetails[p] = perfjsmemory[p];   
                 }
             }
	t["memory"] = memoryDetails;
    
    /////////////RESOURCE DETAILS
    var perfResource = window.performance || {};
    /*var perfResourceTiming = perfResource.getEntriesByType("resource") || {}; 
    console.log("ResourceTiming length = " +perfResourceTiming.length );
    
    for(i=0;i<perfResourceTiming.length;i++){
    console.log("ResourceTiming Captured = "+ perfResourceTiming[i].name);
    loadtime = perfResourceTiming[i].duration,
    dns = perfResourceTiming[i].domainLookupEnd - perfResourceTiming[i].domainLookupStart,
    tcp = perfResourceTiming[i].connectEnd - perfResourceTiming[i].connectStart,
    ttfb = perfResourceTiming[i].responseStart - perfResourceTiming[i].startTime;  */ 
        t["resources"]  = perfResource.getEntriesByType("resource") ||{};        
   /* console.log(loadtime+","+dns+","+tcp+","+ttfb); 
    } */
    
    
    ////////////DOM ELEMENT COUNT
	var domDetails = {};
    var domElementLength = document.getElementsByTagName('*').length;
    console.log("DOM element Length = "+ domElementLength);
    domDetails["domElementCount"] = domElementLength;
    
    //////// PLATFORM DETAILS..
	var platformDetails ={};
    var nplatform = navigator.platform|| {};
    console.log("Platform Details = " + nplatform);
    platformDetails["Platform"]=nplatform;
    
    //////////// USER AGENT
	
    var nAgt = navigator.userAgent|| {};
    for(i=0;i<nAgt.length;i++){
        console.log("UserAgent Details Captured = "+ nAgt[i]);
         
    }
    console.log("UserAgent Details Captured Final = "+ nAgt);
    platformDetails["UserAgent"] = nAgt;
    
	t["platform"] = platformDetails;
	
    ///////////PAGE SOURCE
    var pageSource = document.documentElement.innerHTML;
    //console.log("page source = "+ pageSource);
    domDetails["dom"] = pageSource;
	
	t["others"] = domDetails;
    
     var JsonStringVariable = JSON.stringify(t);
     console.log("JsonString = "+ JsonStringVariable);
   
    ///////////Get URL and URI details.
    var protocolUsed = window.location.protocol; 
    var webHost = window.location.host; 
    var webURI = window.location.pathname+window.location.search;
	var pathArray = window.location.pathname.split( '/' );
	/*var webURI = "";
	for (i = 0; i < pathArray.length; i++) {
	  webURI += "/";
	  webURI += pathArray[i];
	}*/
	var detailsMap = {};
	var host = {}
    host["name"]=webHost;
	t["host"] = host;
	
    detailsMap["transactionName"]= webURI;	
   // t["url"] = protocolUsed + "//" +     webHost + "/" + webURI;
	detailsMap["url"]=window.location.href;
    ///////////SEND JSON message..
   /* var xhr = new XMLHttpRequest();
        //xhr.setRequestHeader('Access-Control-Allow-Headers', '*');

        xhr.open('POST', 'http://localhost:8080/SampleWebApplication/AcceptUserInput.jsp',true);
       
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JsonStringVariable);*/
      
    /***************************************************/
     
    
        chrome[roe].sendMessage({timing: t, JsonString: JsonStringVariable, details: detailsMap}, function(responseText){console.log("Status of Messaage Sent to PaceProcessor.. " + JSON.stringify(responseText));});
    };

/*chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    console.log(sender.tab ?
                "from a content script:" + sender.tab.url :
                "from the extension");
    if (request.enableTransaction)
	{
		console.log("received popup.js message to enable transaction name");
		enableTransactionName=true;
		sendResponse({farewell: "true"});}
	else if (!(request.enableTransaction))
	{
		console.log("received popup.js message to disable transaction name");
		enableTransactionName=false;
		sendResponse({farewell: "false"});}
  });*/