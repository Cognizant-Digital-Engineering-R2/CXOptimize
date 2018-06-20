
/**** ADD LISTENER FOR MODIFYING HEADER ...
***** This extension doesn't use EventPage anymore.. Instead all the .js are treated as background pages now...
***** Had changed persistence=true (default) because webRequest API doesn't work on EventPages....****/
var transactionName="";
chrome.webRequest.onBeforeSendHeaders.addListener(
  function(details) {	 
	/*chrome.tabs.getSelected(null, function(tab) {
        //doing stuff with the url
		//console.log("printing tab details = " + JSON.stringify(tab));
		var site = tab.url.split("/");		
		for (i =3; i < site.length; i++) { transactionName = transactionName +"_"+ site[i];}
		console.log("APM transaction name = "+ transactionName);
		transactionName = transactionName.replace(/[^a-z0-9\w\s]/gi,"_");
		console.log("APM transaction name modified= "+ transactionName);
  	});*/
	  
	var site = details.url.split("/");	
    for (i =3; i < site.length; i++) { transactionName = transactionName +"_"+ site[i];}
	//console.log("APM transaction name = "+ transactionName);
	transactionName = transactionName.replace(/[^a-z0-9\w\s]/gi,"_");
	console.log("APM transaction name modified= "+ transactionName);
	
	//var webURI = window.location.pathname+window.location.search;
	var dynatraceHeader={"name":"X-dynaTrace","value":""};	
	dynatraceHeader.value = "dynaTrace: VU=1;NA="+transactionName;
	transactionName="";
	//console.log("Modifying the request");	
	details.requestHeaders.splice(0,0,dynatraceHeader); //add Dynatrace Header at first index..
		  //details.requestHeader.testHeader="testing";
	//console.log("added Header to the request"); 
	console.log(JSON.stringify(details.requestHeaders))	;
    return { requestHeaders: details.requestHeaders };
  },
  {urls: ['<all_urls>']},
  [ 'blocking', 'requestHeaders']
);//