const axios = require("axios");
const fs = require("fs");
const os = require("os");
const url = require('url');
const replaceall = require("replaceall");
const CleanCSS = require('clean-css');
const UglifyJS = require("uglify-es");
const https = require('https');
const PropertiesReader = require('properties-reader');


//Global Getter & Setter
var collectorconstants = {
	configReadFlag: false,
	globalConfig: '',
	scriptStart: '',
	runStart: '',
	txnCounter: 0,
	userAgent: '',
	beaconUrl: '',
	user: '',
	encPass: '',
	authToken: '',
	clientName: '',
	projectName: '',
	scenarioName: '',
	prevTxnStartTime: 0,
	prevTxnHeapSize: 0,
	resourceSettleTime: 2000,
	markWaitTime: 5000,
	staticExt: '',
	imageExt: '',
	resDurationThreshold: 10
};

const getAuthToken = async () => {
	try {
		const agent = new https.Agent({  
			rejectUnauthorized: false
		});
		var reqBody = {
			username: collectorconstants.user,
			password: collectorconstants.encPass
		}
		await axios.post(collectorconstants.beaconUrl + "/authToken",reqBody,{httpsAgent: agent }).then(function (response) {
			collectorconstants.authToken = response.headers.authorization;
		  });
	} catch (error) {
		console.log("CXOP - - Error getting authToken from CXOptimize API");
	}
}

const getConfiguration = async () => {
  	var config = {};
  	var configUrl = collectorconstants.beaconUrl + "/getConfig?ClientName=" + collectorconstants.clientName + "&ProjectName=" + collectorconstants.projectName + "&Scenario=" + collectorconstants.scenarioName;
	try {
		const agent = new https.Agent({  
			rejectUnauthorized: false
		});
		var reqHeaders = {
			'Content-Type': 'application/json',
			'Authorization': collectorconstants.authToken 
		}
  		await axios.get(configUrl,{ headers: reqHeaders,httpsAgent: agent }).then(response => {
			  //console.log(response);
			  if(response.data.Status != "Failed") {
				config = response.data;
			  } else {
				console.log("CXOP - - No configuration available for the given combination %s - %s - %s",collectorconstants.clientName,collectorconstants.projectName,collectorconstants.scenarioName);
			  }
	 		
		  });
	} catch (error) {
		console.log("CXOP - - Error getting configuration data from CXOptimize API");
	}
  	return config;
};

const readConfig = async () => {
	
	var config = PropertiesReader('Collector.properties');
	collectorconstants.clientName = config.get('clientName');
	collectorconstants.projectName = config.get('projectName');
	collectorconstants.scenarioName = config.get('scenarioName');
	collectorconstants.user = config.get('UserName');
	collectorconstants.encPass = config.get('Password');
	collectorconstants.beaconUrl = config.get('beaconUrl');
	if(config.get('resourceSettleTime')){
		collectorconstants.resourceSettleTime = config.get('resourceSettleTime');
	}
	if(config.get('markWaitTime')){
		collectorconstants.markWaitTime = config.get('markWaitTime');
	}
	
	await getAuthToken();
	if(collectorconstants.authToken != '') {
		var backConfig = await getConfiguration();
		collectorconstants.staticExt = backConfig.staticExt;
		collectorconstants.imageExt = backConfig.imageExt;
		if(backConfig.resDurationThreshold) {
			collectorconstants.resDurationThreshold = backConfig.resDurationThreshold;
		}
		collectorconstants.configReadFlag = true;
		collectorconstants.globalConfig = backConfig;
	} else {
		console.log("CXOP - - Failed to get AuthToken for  %s",collectorconstants.user);
	}
	
};

const getResourceDetails = async (resource) => {

	var staticResrcStatus = false;
	var isImage = false;
	var flagSet = false;
	var resourceType = "others";
	var isCompressible = false;
	var truncUrl = resource.name.toLowerCase().split("?")[0];

	if(resource.duration < collectorconstants.resDurationThreshold) {
		resource.IsCached = true;
	} else {
		resource.IsCached = false;
	}

	if(resource.responseStart != 0 && resource.encodedBodySize == 0 && resource.transferSize == 0){
		resource.IsCached = true;
	}

	for(img in collectorconstants.globalConfig.imageResourceExtension.split(",")) {
		if(truncUrl.includes(img.toLowerCase())) {
			isImage = true;
			staticResrcStatus = true;
			flagSet = true;
			resourceType = img.toLowerCase();
			break;
		}

	}

	if(resourceType.includes(".svg")){
		isCompressible = true;
	}

	if(!flagSet){
		for(stat in collectorconstants.globalConfig.staticResourceExtension.split(",")) {
			if(truncUrl.includes(stat.toLowerCase())) {
				staticResrcStatus = true;
				isImage = false;
				resourceType = stat.toLowerCase();
				isCompressible = true;
				break;
			}
	
		}

	}

	resource.IsStaticResrc = staticResrcStatus;
	resource.IsImage = isImage;
	resource.ResourceType = resourceType;
	resource.HostName = url.parse(truncUrl).hostname;

	
	if(staticResrcStatus || isImage) {
		var reqHeader = {};
		reqHeader["User-Agent"] = collectorconstants.userAgent;
		if(isCompressible) {
			reqHeader["Accept-Encoding"] = "gzip,deflate,sdch";
		}
		const agent = new https.Agent({  
			rejectUnauthorized: false
		  });

		try {
			await axios.get(resource.name,{ headers: reqHeader,httpsAgent: agent }).then(response => {
				if(response.status == 200) {
					if(response.headers["last-modified"]){
						resource["Last-Modified"] = replaceall(",","",response.headers["last-modified"]);
					}
					if(response.headers["content-length"]){
						resource["Content-Length"] = response.headers["content-length"];
					}
					if(response.headers["connection"]){
						resource["Connection"] = response.headers["connection"];
					} else {
						resource["Connection"] = "keep-alive";
					}
					if(response.headers["cache-control"]){
						resource["Cache-Control"] = replaceall(",","#",response.headers["cache-control"]);
					}
					if(response.headers["etag"]){
						resource["ETag"] = replaceall("\"","",response.headers["etag"]);
					}
					if(response.headers["expires"]){
						resource["Expires"] = replaceall(",","",response.headers["expires"]);
					}
					if(isImage && isCompressible) {
						if(response.headers["content-encoding"]) {
							resource["Content-Encoding"] = replaceall(",","",response.headers["content-encoding"]);
						}
							
					} else {
						if(response.headers["content-encoding"] && response.headers["content-encoding"].includes("gzip")) {
							resource["Content-Encoding"] = replaceall(",","",response.headers["content-encoding"]);
						} else {
							if(response.headers["content-encoding"]){
								resource["Content-Encoding"] = replaceall(",","",response.headers["content-encoding"]);
							}
							resource["OrgSize"] = response.data.length;
							if(resource.name.toLowerCase().includes(".js") || resource.name.toLowerCase().includes(".css")){
								if(resource.name.toLowerCase().includes(".css")){
									try {
										var output = new CleanCSS().minify(response.data);
										resource["MinfSize"] = output.length;
									} catch (error){
										console.log("Error minifying the CSS : %s",response.name);
									}
									
								}
								if(resource.name.toLowerCase().includes(".js")){
									try {
										var result = UglifyJS.minify(response.data);
										if(result.error === undefined){
											resource["MinfSize"] = result.code.length;
										}
									} catch (error){
										console.log("Error minifying the JS : %s",response.name);
									}
								}
								
							}
						}
					}
				}
			});
	  } catch (error) {

		  
		  console.log("CXOP - - Error getting details for the resource %s due to HTTP status %s",resource.name,error.response.status);
	  }

	}
	return resource;

}


const resourceCrawl = async (resourcesOrg) => {
	var resources = [];
	if (!resourcesOrg || !resourcesOrg.length) {
		return resources;
	}


	for (var i = 0; i < resourcesOrg.length; i++) {
		var res = resourcesOrg[i];
		resources.push(await getResourceDetails(res));
	}
	Promise.all(resources).then(function() {
	});

	return resources;
}

const persistData = async (collectedData) => {
	var hostDetails = {};
	hostDetails.name = os.hostname();
	var others = {};
	others.domElementCount = collectedData.DomElements;
	others.dom = collectedData.DOMContent;
	if(collectedData.msFirstPaint){
		others.msFirstPaint = collectedData.msFirstPaint;
	}
	var details = {};
	details.ClientName = collectedData.ClientName;
	details.ProjectName = collectedData.ProjectName;
	details.Scenario = collectedData.Scenario;
	details.transactionName = collectedData.TxnName;
	details.url = collectedData.Url;
	details.txnStatus = collectedData.txnStatus;
	details.Release = collectedData.Release;
	details.RunID = collectedData.RunID;
	details.RunTime = collectedData.RunTime;
	details.BuildNumber = collectedData.BuildNumber;
	details.staticResourceExtension = collectedData.staticResourceExtension;
	details.imageResourceExtension = collectedData.imageResourceExtension;
	details.resourceDurationThreshold = collectedData.resourceDurationThreshold;
	details.source = "SeleniumNodeJS";
	details.ScriptTime = collectedData.ScriptTime;
	if(collectedData.NavType){
		details.NavType = "Hard";
		details.speedIndex = collectedData.SpeedIndex;
	} else {
		details.NavType = "Soft";
	}
	details.StartTime = collectedData.StartTime;
	details.resourceLoadTime = collectedData.resourceLoadTime;
	details.visuallyComplete = collectedData.visuallyComplete;

	var jsonDoc = {};
	jsonDoc.details = details;
	jsonDoc.host = hostDetails;
	jsonDoc.platform = collectedData.Browser;
	jsonDoc.memory = collectedData.Memory;
	jsonDoc.others = others;
	jsonDoc.navtime = collectedData.NavigationTime;
	if(collectedData.MarkTime) {
		jsonDoc.marktime = collectedData.MarkTime;
	}
	if(collectedData.isResourceCrawlingEnabled) {
		jsonDoc.resources = await resourceCrawl(collectedData.ResourceTime);
	} else {
		jsonDoc.resources = collectedData.ResourceTime;
	}
	var headers = {
		'Content-Type': 'application/json',
		'Authorization': collectorconstants.authToken 
	}

	const agent = new https.Agent({  
		rejectUnauthorized: false
	  });


	try {
		await axios.post(collectorconstants.beaconUrl + "/insertStats",jsonDoc,{headers: headers,httpsAgent: agent}).then(function (response) {
			if(response.status == 200 && response.data.Status == "Success") {
				console.log("CXOP - %s -  Data uploaded succesfully",collectedData.TxnName);
			} else {
				console.log("CXOP - %s - The data could not be uploaded. The response from data store",collectedData.TxnName);
			}
			
		  });
	} catch (error) {
		console.log("CXOP - %s -  Error uploading data to cxoptimize api",collectedData.TxnName);
	}
	
}

function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}


function reduceFetchStarts(resources) {
	var times = [];

	if (!resources || !resources.length) {
		return times;
	}

	for (var i = 0; i < resources.length; i++) {
		var res = resources[i];

		// if there is a subsequent resource with the same fetchStart, use
		// its value instead (since pre-sort guarantee is that it's end
		// will be >= this one)
		if (i !== resources.length - 1 &&
			res.fetchStart === resources[i + 1].fetchStart) {
			continue;
		}

		// track just the minimum fetchStart and responseEnd
		times.push({
			fetchStart: res.fetchStart,
			responseEnd: res.responseStart || res.responseEnd
		});
		
	}

	return times;
}

function calculateResourceTimingUnion(resourcesOrg,navType) {
	var i;
	var resourceTime = {};

	if (!resourcesOrg || !resourcesOrg.length) {
		resourceTime["totalTime"] = 0;
		resourceTime["backendTime"] = 0;
		return resourceTime;
	}

	var resources = [];
	for (i = 0; i < resourcesOrg.length; i++) {
		e = resourcesOrg[i];
		if (e.name.indexOf("about:") === 0 ||
			    e.name.indexOf("javascript:") === 0 ||
			    e.name.indexOf("res:") === 0) {
				continue;
			}
			resources.push(e);
	}

	// First, sort by start time, then end time
	resources.sort(function(a, b) {
		if (a.fetchStart !== b.fetchStart) {
			return a.fetchStart - b.fetchStart;
		}
		else {
			var ae = a.responseStart || a.responseEnd;
			var be = b.responseStart || b.responseEnd;

			return ae - be;
		}
	});

	// Next, find all resources with the same start time, and reduce
	// them to the largest end time.
	var times = reduceFetchStarts(resources);

	// Third, for every resource, if the start is less than the end of
	// any previous resource, change its start to the end.  If the new start
	// time is more than the end time, we can discard this one.
	var times2 = [];
	var furthestEnd = 0;

	for (i = 0; i < times.length; i++) {
		var res = times[i];

		if (res.fetchStart < furthestEnd) {
			res.fetchStart = furthestEnd;
		}

		// as long as this resource has > 0 duration, add it to our next list
		if (res.fetchStart < res.responseEnd) {
			times2.push(res);

			// keep track of the furthest end point
			furthestEnd = res.responseEnd;
		}
	}

	// Reduce down again to same start times again, and now we should
	// have no overlapping regions
	var times3 = reduceFetchStarts(times2);

	// Finally, calculate the overall time from our non-overlapping regions
	var backendTime = 0;
	var fetchStart = 0;
	var responseEnd = 0;
	for (i = 0; i < times3.length; i++) {
		backendTime += times3[i].responseEnd - times3[i].fetchStart;
		if(i === 0) {
			fetchStart = times3[i].fetchStart;
		}
		if(i === (times3.length - 1)) {
			responseEnd = times3[i].fetchStart;
		}
	}
	if(navType)
	{
		resourceTime["totalTime"] = responseEnd;
	} else {
		resourceTime["totalTime"] = responseEnd - fetchStart;
	}
	
	resourceTime["backendTime"] = backendTime;

	return resourceTime;
}

exports.StartTransaction  = async function(txnName,browser) {
	console.log("CXOP - %s -  Clearing Performance Data if available",txnName);
	collectorconstants.scriptStart = (new Date()).getTime();
	if(collectorconstants.txnCounter == 0){
		collectorconstants.runStart = (new Date()).getTime();
		collectorconstants.txnCounter = collectorconstants.txnCounter + 1;
	}
	await browser.executeScript("window.performance.clearResourceTimings();");
	return 0;
};

exports.EndTransaction = async function(txnName,browser,txnStatus) {
	console.log("CXOP - %s -  Started Collecting Performance Data",txnName);
	collectorconstants.userAgent = await browser.executeScript("return navigator.userAgent;");
	//console.log(collectorconstants.userAgent);
	if(collectorconstants.userAgent === undefined || collectorconstants.userAgent == '') {
		console.log("CXOP - %s -  No performance data will be collected since UserAgent is %s", txnName, collectorconstants.userAgent);
	}
	//console.log("CXOP - %s -  No performance data will be collected since UserAgent is %s", txnName, collectorconstants.userAgent);
	var collectedData = {};
	var navType = false;
	if(!collectorconstants.configReadFlag){
		await readConfig();
	}
	collectedData = collectorconstants.globalConfig;
	collectedData["TxnName"] = txnName;
	collectedData["txnStatus"] = txnStatus;
	collectedData["Url"] = await browser.getCurrentUrl();

	// Collecting Navigation Timing Data
	var navigationDetails = await browser.executeScript("return window.performance.timing;");
	
	//Check if the navigation is hard or soft
	if(navigationDetails["navigationStart"] > collectorconstants.prevTxnStartTime) {
		collectorconstants.prevTxnStartTime = navigationDetails["navigationStart"];
		navType = true;
		collectedData["StartTime"] = navigationDetails["navigationStart"];
		var speedIdx = await browser.executeScript("var firstPaint,SpeedIndex,win=window,doc=win.document,GetElementViewportRect=function(t){var e=!1;if(t.getBoundingClientRect){var r=t.getBoundingClientRect();(e={top:Math.max(r.top,0),left:Math.max(r.left,0),bottom:Math.min(r.bottom,win.innerHeight||doc.documentElement.clientHeight),right:Math.min(r.right,win.innerWidth||doc.documentElement.clientWidth)}).bottom<=e.top||e.right<=e.left?e=!1:e.area=(e.bottom-e.top)*(e.right-e.left)}return e},CheckElement=function(t,e){if(e){var r=GetElementViewportRect(t);r&&rects.push({url:e,area:r.area,rect:r})}},GetRects=function(){for(var t=doc.getElementsByTagName(\"*\"),e=/url\\(.*(http.*)\\)/gi,r=0;r<t.length;r++){var i=t[r],n=win.getComputedStyle(i);if(\"IMG\"==i.tagName&&CheckElement(i,i.src),n[\"background-image\"]){e.lastIndex=0;var a=e.exec(n[\"background-image\"]);a&&a.length>1&&CheckElement(i,a[1].replace('\"',\"\"))}if(\"IFRAME\"==i.tagName)try{var s=GetElementViewportRect(i);if(s){var o=RUMSpeedIndex(i.contentWindow);o&&rects.push({tm:o,area:s.area,rect:s})}}catch(t){}}},GetRectTimings=function(){for(var t={},e=win.performance.getEntriesByType(\"resource\"),r=0;r<e.length;r++)t[e[r].name]=e[r].responseEnd;for(var i=0;i<rects.length;i++)\"tm\"in rects[i]||(rects[i].tm=void 0!==t[rects[i].url]?t[rects[i].url]:0)},GetFirstPaint=function(){try{for(var t=performance.getEntriesByType(\"paint\"),e=0;e<t.length;e++)if(\"first-paint\"==t[e].name){navStart=performance.getEntriesByType(\"navigation\")[0].startTime,firstPaint=t[e].startTime-navStart;break}}catch(t){}if(void 0===firstPaint&&\"msFirstPaint\"in win.performance.timing&&(firstPaint=win.performance.timing.msFirstPaint-navStart),void 0===firstPaint&&\"chrome\"in win&&\"loadTimes\"in win.chrome){var r=win.chrome.loadTimes();if(\"firstPaintTime\"in r&&r.firstPaintTime>0){var i=r.startLoadTime;\"requestTime\"in r&&(i=r.requestTime),r.firstPaintTime>=i&&(firstPaint=1e3*(r.firstPaintTime-i))}}if(void 0===firstPaint||firstPaint<0||firstPaint>12e4){firstPaint=win.performance.timing.responseStart-navStart;var n={},a=doc.getElementsByTagName(\"head\")[0].children;for(e=0;e<a.length;e++){var s=a[e];\"SCRIPT\"==s.tagName&&s.src&&!s.async&&(n[s.src]=!0),\"LINK\"==s.tagName&&\"stylesheet\"==s.rel&&s.href&&(n[s.href]=!0)}for(var o=win.performance.getEntriesByType(\"resource\"),c=!1,m=0;m<o.length;m++)if(c||!n[o[m].name]||\"script\"!=o[m].initiatorType&&\"link\"!=o[m].initiatorType)c=!0;else{var g=o[m].responseEnd;(void 0===firstPaint||g>firstPaint)&&(firstPaint=g)}}firstPaint=Math.max(firstPaint,0)},CalculateVisualProgress=function(){for(var t={0:0},e=0,r=0;r<rects.length;r++){var i=firstPaint;\"tm\"in rects[r]&&rects[r].tm>firstPaint&&(i=rects[r].tm),void 0===t[i]&&(t[i]=0),t[i]+=rects[r].area,e+=rects[r].area}var n=Math.max(doc.documentElement.clientWidth,win.innerWidth||0)*Math.max(doc.documentElement.clientHeight,win.innerHeight||0);if(n>0&&(n=Math.max(n-e,0)*pageBackgroundWeight,void 0===t[firstPaint]&&(t[firstPaint]=0),t[firstPaint]+=n,e+=n),e){for(var a in t)t.hasOwnProperty(a)&&progress.push({tm:a,area:t[a]});progress.sort(function(t,e){return t.tm-e.tm});for(var s=0,o=0;o<progress.length;o++)s+=progress[o].area,progress[o].progress=s/e}},CalculateSpeedIndex=function(){if(progress.length){SpeedIndex=0;for(var t=0,e=0,r=0;r<progress.length;r++){var i=progress[r].tm-t;i>0&&e<1&&(SpeedIndex+=(1-e)*i),t=progress[r].tm,e=progress[r].progress}}else SpeedIndex=firstPaint},rects=[],progress=[],output=[],pageBackgroundWeight=.1;try{var navStart=win.performance.timing.navigationStart;GetRects(),GetRectTimings(),GetFirstPaint(),CalculateVisualProgress(),CalculateSpeedIndex(),output[0]=firstPaint.toFixed(0).toString(),output[1]=SpeedIndex.toFixed(0).toString()}catch(t){}return output;");
		if(speedIdx.length > 0) {
			if(speedIdx[0] > 0){
				collectedData["SpeedIndex"] = speedIdx[0];
			} else {
				if(navigationDetails["loadEventEnd"] > 0){
					collectedData["SpeedIndex"] = navigationDetails["loadEventEnd"] - navigationDetails["navigationStart"];
				} else {
					collectedData["SpeedIndex"] = navigationDetails["domComplete"] - navigationDetails["navigationStart"];
				}
			}
			if(speedIdx[1] > 0){
				collectedData["msFirstPaint"] = speedIdx[0];
			} else {
				if(collectorconstants.userAgent.includes("Trident")){
					collectedData["msFirstPaint"] = navigationDetails["msFirstPaint"] - navigationDetails["navigationStart"];
				} else {
					if(navigationDetails["loadEventEnd"] > 0){
						collectedData["msFirstPaint"] = navigationDetails["loadEventEnd"] - navigationDetails["navigationStart"];
					} else {
					collectedData["msFirstPaint"] = navigationDetails["domComplete"] - navigationDetails["navigationStart"];
					}
				}
			}
		} else {
			if(collectorconstants.userAgent.includes("Trident")){
				if(navigationDetails["loadEventEnd"] > 0){
					collectedData["SpeedIndex"] = navigationDetails["loadEventEnd"] - navigationDetails["navigationStart"];
				} else {
					collectedData["SpeedIndex"] = navigationDetails["domComplete"] - navigationDetails["navigationStart"];
				}
				collectedData["msFirstPaint"] = navigationDetails["msFirstPaint"] - navigationDetails["navigationStart"];
			} else {
				if(navigationDetails["loadEventEnd"] > 0){
					collectedData["SpeedIndex"] = navigationDetails["loadEventEnd"] - navigationDetails["navigationStart"];
					collectedData["msFirstPaint"] = navigationDetails["loadEventEnd"] - navigationDetails["navigationStart"];
				} else {
					collectedData["SpeedIndex"] = navigationDetails["domComplete"] - navigationDetails["navigationStart"];
					collectedData["msFirstPaint"] = navigationDetails["domComplete"] - navigationDetails["navigationStart"];
				}
			}
		}
		collectedData["NavigationTime"] = navigationDetails;
	} else {
		navType = false;
		collectedData["StartTime"] = collectorconstants.scriptStart;
		navigationDetails = null;
	}
	
	collectedData["NavType"] = navType;

	//Resource Settle Time
	var beforeLength = 0, afterLength = 0;
	do {
		beforeLength = await browser.executeScript("return window.performance.getEntriesByType('resource').length;");
		//Sleep
		await sleep(collectorconstants.resourceSettleTime);
		afterLength = await browser.executeScript("return window.performance.getEntriesByType('resource').length;");
	} while(beforeLength < afterLength);

	if(afterLength != 0) {
		var resourceDetails = await browser.executeScript("return performance.getEntriesByType('resource');");
		collectedData["ResourceTime"] = resourceDetails;
		await browser.executeScript("window.performance.clearResourceTimings();");
		var resourceTime = calculateResourceTimingUnion(resourceDetails,navType)
		collectedData["resourceLoadTime"] = resourceTime["backendTime"];
		collectedData["visuallyComplete"] = resourceTime["totalTime"];
	}

	var memoryDetails = {};
	if(navigationDetails === undefined && afterLength == 0) {
		console.log("CXOP - %s - probably did not make a server request and hence will be ignored.", txnName);
	}

	if(collectorconstants.userAgent.includes("Chrome")) {
		var heapUsage = await browser.executeScript("return window.console.memory;");
		if(heapUsage["totalJSHeapSize"]) {
			memoryDetails["jsHeapSizeLimit"] = heapUsage["jsHeapSizeLimit"];
			memoryDetails["totalJSHeapSize"] = heapUsage["totalJSHeapSize"];
			memoryDetails["usedJSHeapSize"] = heapUsage["usedJSHeapSize"];
			if(collectorconstants.prevTxnHeapSize == 0) {
				collectorconstants.prevTxnHeapSize = heapUsage["usedJSHeapSize"];
				memoryDetails["currentPageUsage"] = heapUsage["usedJSHeapSize"];
			} else {
				memoryDetails["currentPageUsage"] = heapUsage["usedJSHeapSize"] - collectorconstants.prevTxnHeapSize;
				collectorconstants.prevTxnHeapSize = heapUsage["usedJSHeapSize"];
			}
			

		} else {
			memoryDetails["jsHeapSizeLimit"] = 0;
			memoryDetails["totalJSHeapSize"] = 0;
			memoryDetails["usedJSHeapSize"] = 0;
			memoryDetails["currentPageUsage"] = 0;

		}

	} else {
		memoryDetails["jsHeapSizeLimit"] = 0;
		memoryDetails["totalJSHeapSize"] = 0;
		memoryDetails["usedJSHeapSize"] = 0;
		memoryDetails["currentPageUsage"] = 0;
	}

	collectedData["Memory"] = memoryDetails;
	collectedData["DomElements"] = await browser.executeScript("return document.getElementsByTagName('*').length;");;
	var browserDetails = {};
	browserDetails["UserAgent"] = collectorconstants.userAgent;
	collectedData["Browser"] = browserDetails;

	if(collectedData["isMarkAPIEnabled"])
	{
		//sleep
		await sleep(collectorconstants.markWaitTime);
		var mark = await browser.executeScript("return window.performance.getEntriesByType('mark');");
		if(mark.size > 0) {
			collectedData["MarkTime"] = mark;
		}
		await browser.executeScript("window.performance.clearMarks();");
	}

	if(collectedData["isDOMNeeded"])
	{
		if(navType) {
			collectedData["DOMContent"] = await browser.executeScript("return document.documentElement.outerHTML;");
		}
	}
	collectedData["ScriptTime"] = (new Date()).getTime() - collectorconstants.scriptStart;
	await persistData(collectedData);
	//console.log("End");
	return 0;

};
