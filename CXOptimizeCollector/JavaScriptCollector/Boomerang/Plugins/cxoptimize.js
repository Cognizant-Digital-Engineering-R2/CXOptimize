/*
 * Copyright 2014 - 2018 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    var impl;

	var originalXHRRequest;
	var initialLoad = true;
    BOOMR = BOOMR || {};
    BOOMR.plugins = BOOMR.plugins || {};
    if (BOOMR.plugins.CXOptimize) return;

	function hookBeaconReport() {

	    //// Holding the original XHR Request before hooking into XHR
	    //// Checks whether BOOMR.orig_XMLHttpRequest exists. If yes, takes it otherwise original XHR request from Window
	    
		originalXHRRequest = (BOOMR.orig_XMLHttpRequest || BOOMR.window.XMLHttpRequest);

	    proxyXHRForBeaconReport = function () {
	        var req, resource = { timing: {}, initiator: "xhr" }, orig_open, orig_send, orig_setRequestHeader;

	        req = new originalXHRRequest();

	        orig_open = req.open;
	        orig_send = req.send;
	        orig_setRequestHeader = req.setRequestHeader;

	        req.setRequestHeader = function (header, value) {
	                var overriddenArguments = ["Content-type", "application/json"];
	                orig_setRequestHeader.apply(req, overriddenArguments);
	        };

	        req.open = function (method, url, async) {
	            // call the original open method
	            try {
	                return orig_open.apply(req, arguments);
	            }
	            catch (e) { }
	        };

	        /**
			 * overrides the beacon send: converts the arguments into 
             * JSON and groups the arguments according to plugin 
			 */
	        req.send = function () {
	            var jsonData = prepareJSONData(arguments);
				//var overriddenArguments = ["Authorization", "Basic " + impl.apiToken];
				var overriddenArguments = ["Accept", "*/*"];
	            orig_setRequestHeader.apply(req, overriddenArguments);
	            return orig_send.apply(req, [jsonData]);
	        };

	        return req;
	    };

	    BOOMR.orig_XMLHttpRequest = proxyXHRForBeaconReport;
	}

    function isString(objInst) {

        try {
            if (objInst.split)
                return true;
        } catch (e) { }

        return false;
    }

	function DecodeEntry(data){
		var decodedData = decodeURIComponent(data);
		try {
			return JSON.parse(decodedData);		
		}
		catch(e){}
		return decodedData;
	}
	
	function clearBeacons()	{
		var p = BOOMR.getPerformance();

			BOOMR.removeVar("restiming");
			
					
			if (p) {
				var clearResourceTimings = p.clearResourceTimings || p.webkitClearResourceTimings;
				if (clearResourceTimings && typeof clearResourceTimings === "function") {
					clearResourceTimings.call(p);
				}
			}
	}
	
    function prepareJSONData(data) {
        var jsonData = {};
        if (isString(data[0])) {

            var beaconParams = data[0].split('&');

            for (count = 0; count < beaconParams.length; ++count) {

                var current = beaconParams[count];
                var nameValue = current.split('=');

                var current = jsonData;
                var property = DecodeEntry(nameValue[0]);
				
				// not adding default restime beacon as
				// we are adding the detailed resource timing.
				if (property === 'restiming') continue;

                while (property.indexOf('_') >= 0) {
                    property = property.replace('_', '.');
                }

                var innerProperties = property.split('.');
				var index = 0;
                for (index = 0; index < innerProperties.length - 1; ++index) {

                    if (!current[innerProperties[index].toString()])
                        current[innerProperties[index].toString()] = {};

                    var propertyValue = current[innerProperties[index].toString()];
                    if (typeof (propertyValue) !== "object") {
                        var _nweObj = {};
                        _nweObj[innerProperties[index].toString()] = propertyValue;
                        current[innerProperties[index].toString()] = _nweObj;
                    }

                    current = current[innerProperties[index].toString()];
                }

                current[innerProperties[index].toString()] = DecodeEntry(nameValue[1]);
            }

        } else
            jsonData = data[0];

        if (!jsonData.cxoptimize) {
            jsonData.cxoptimize = {}
		}

		var resLoadTime = 0;
		if (initialLoad && impl.includeResourceTiming) {
			var resParams = captureParameters();
			jsonData.cxoptimize.resourcetiming = resParams["resourcetiming"];
			jsonData.cxoptimize.navstart = resParams["navstart"];
			resLoadTime = Math.round(BOOMR.plugins.ResourceTiming.calculateResourceTimingUnion(jsonData.cxoptimize.resourcetiming));
			initialLoad = false;
		}
		
		var navType = 0;
		
		if (jsonData && jsonData.cxoptimize && jsonData.cxoptimize.details && jsonData.cxoptimize.details.initiator)
		{
			if (jsonData.cxoptimize.details.initiator === 'spa-hard') navType = 0;
			if (jsonData.cxoptimize.details.initiator === 'spa')  navType = 1;
			if (jsonData.cxoptimize.details.initiator === 'xhr' || jsonData.cxoptimize.details.initiator === 'submit')  navType = 2;
		}
		
		jsonData.cxoptimize.details = captureCXOptimizeDetails();
		if (jsonData.cxoptimize.xhr && jsonData.cxoptimize.xhr.transactionName){
			jsonData.cxoptimize.details.transactionName = jsonData.cxoptimize.xhr.transactionName;
			delete jsonData.cxoptimize.xhr.transactionName;
		}
		
		if (jsonData.cxoptimize.spa && jsonData.cxoptimize.spa.initiator === "spa")
			delete jsonData.cxoptimize.details.pageIndex;
		
		if (jsonData.nt && jsonData.nt.nav )
			jsonData.nt.nav['type'] = navType;
		
		jsonData.cxoptimize.details.navType = navType; 
		if (resLoadTime) jsonData.cxoptimize.details.resourceLoadTime  = resLoadTime;
		
        return JSON.stringify(jsonData);
    }

    function captureCXOptimizeDetails() {
        var details = {};

        if (impl.includeDOM) details.dom = BOOMR.window.document.documentElement.innerHTML;
        if (impl.includeDOMCount) details.domCount = BOOMR.window.document.getElementsByTagName('*').length;
        
		details.host = BOOMR.window.location.host;
        details.useragent = navigator.userAgent;
        details.scenario = impl.scenario;
        details.project = impl.project;
        details.client = impl.client;
        details.build = impl.build;
		details.release = impl.release;
        details.application = impl.application;
        details.transactionName = getTransactionName() ;
        details.runID = impl.runID;
        details.pageIndex = getSpeedIndex();
        
		return details;
    }

	function getTransactionName() {
		var transactionName = "";
		
		if (BOOMR.window.document.title)
			transactionName += BOOMR.window.document.title;
		
		transactionName += BOOMR.window.location.toString().replace(BOOMR.window.location.origin, '');
		
		return transactionName;
	}
	
    function unHookXHR() {
        if (originalXHRRequest) {
            BOOMR.window.XMLHttpRequest = originalXHRRequest;
            window.XMLHttpRequest = originalXHRRequest;
            BOOMR.orig_XMLHttpRequest = originalXHRRequest;
        }
    }

    /**
	 * Attempts to get the navigationStart time for a frame.
	 * @returns navigationStart time, or 0 if not accessible
	 */
    function getNavStartTime(frame) {
        var navStart = 0, frameLoc;

        try {
            // Try to access location.href first to trigger any Cross-Origin
            // warnings.  There's also a bug in Chrome ~48 that might cause
            // the browser to crash if accessing X-O frame.performance.
            // https://code.google.com/p/chromium/issues/detail?id=585871
            // This variable is not otherwise used.
            frameLoc = frame.location && frame.location.href;

            if (("performance" in frame) &&
			frame.performance &&
			frame.performance.timing &&
			frame.performance.timing.navigationStart) {
                navStart = frame.performance.timing.navigationStart;
            }
        }
        catch (e) {
            // empty
        }

        return navStart;
    }

    /**
 * Gets all of the performance entries for a frame and its subframes
 *
 * @param [Frame] frame Frame
 * @param [boolean] top This is the top window
 * @param [string] offset Offset in timing from root IFRAME
 * @param [number] depth Recursion depth
 * @return [PerformanceEntry[]] Performance entries
 */
    function findPerformanceEntriesForFrame(frame, isTopWindow, offset, depth) {
        var entries = [], i, navEntries, navStart, frameNavStart, frameOffset,
		    navEntry, t, frameLoc;

        if (typeof isTopWindow === "undefined") {
            isTopWindow = true;
        }

        if (typeof offset === "undefined") {
            offset = 0;
        }

        if (typeof depth === "undefined") {
            depth = 0;
        }

        if (depth > 10) {
            return entries;
        }

        try {
            navStart = getNavStartTime(frame);

            // get sub-frames' entries first
            if (frame.frames) {
                for (i = 0; i < frame.frames.length; i++) {
                    frameNavStart = getNavStartTime(frame.frames[i]);
                    frameOffset = 0;
                    if (frameNavStart > navStart) {
                        frameOffset = offset + (frameNavStart - navStart);
                    }

                    entries = entries.concat(findPerformanceEntriesForFrame(frame.frames[i], false, frameOffset, depth + 1));
                }
            }

            try {
                // Try to access location.href first to trigger any Cross-Origin
                // warnings.  There's also a bug in Chrome ~48 that might cause
                // the browser to crash if accessing X-O frame.performance.
                // https://code.google.com/p/chromium/issues/detail?id=585871
                // This variable is not otherwise used.
                frameLoc = frame.location && frame.location.href;

                if (!("performance" in frame) ||
				   !frame.performance ||
				   typeof frame.performance.getEntriesByType !== "function") {
                    return entries;
                }
            }
            catch (e) {
                // NOP
                return entries;
            }

            // add an entry for the top page
            if (isTopWindow) {
                navEntries = frame.performance.getEntriesByType("navigation");
                if (navEntries && navEntries.length === 1) {
                    navEntry = navEntries[0];

                    // replace document with the actual URL
                    entries.push({
                        name: frame.location.href,
                        startTime: 0,
                        initiatorType: "html",
                        redirectStart: navEntry.redirectStart,
                        redirectEnd: navEntry.redirectEnd,
                        fetchStart: navEntry.fetchStart,
                        domainLookupStart: navEntry.domainLookupStart,
                        domainLookupEnd: navEntry.domainLookupEnd,
                        connectStart: navEntry.connectStart,
                        secureConnectionStart: navEntry.secureConnectionStart,
                        connectEnd: navEntry.connectEnd,
                        requestStart: navEntry.requestStart,
                        responseStart: navEntry.responseStart,
                        responseEnd: navEntry.responseEnd
                    });
                }
                else if (frame.performance.timing) {
                    // add a fake entry from the timing object
                    t = frame.performance.timing;

                    //
                    // Avoid browser bugs:
                    // 1. navigationStart being 0 in some cases
                    // 2. responseEnd being ~2x what navigationStart is
                    //    (ensure the end is within 60 minutes of start)
                    //
                    if (t.navigationStart !== 0 &&
						t.responseEnd <= (t.navigationStart + (60 * 60 * 1000))) {
                        entries.push({
                            name: frame.location.href,
                            startTime: 0,
                            initiatorType: "html",
                            redirectStart: t.redirectStart ? (t.redirectStart - t.navigationStart) : 0,
                            redirectEnd: t.redirectEnd ? (t.redirectEnd - t.navigationStart) : 0,
                            fetchStart: t.fetchStart ? (t.fetchStart - t.navigationStart) : 0,
                            domainLookupStart: t.domainLookupStart ? (t.domainLookupStart - t.navigationStart) : 0,
                            domainLookupEnd: t.domainLookupEnd ? (t.domainLookupEnd - t.navigationStart) : 0,
                            connectStart: t.connectStart ? (t.connectStart - t.navigationStart) : 0,
                            secureConnectionStart: t.secureConnectionStart ? (t.secureConnectionStart - t.navigationStart) : 0,
                            connectEnd: t.connectEnd ? (t.connectEnd - t.navigationStart) : 0,
                            requestStart: t.requestStart ? (t.requestStart - t.navigationStart) : 0,
                            responseStart: t.responseStart ? (t.responseStart - t.navigationStart) : 0,
                            responseEnd: t.responseEnd ? (t.responseEnd - t.navigationStart) : 0
                        });
                    }
                }
            }

            // offset all of the entries by the specified offset for this frame
            var frameEntries = frame.performance.getEntriesByType("resource"),
			    frameFixedEntries = [];

            for (i = 0; frameEntries && i < frameEntries.length; i++) {
                t = frameEntries[i];
                frameFixedEntries.push({
                    name: t.name,
                    initiatorType: t.initiatorType,
                    startTime: t.startTime + offset,
                    redirectStart: t.redirectStart ? (t.redirectStart + offset) : 0,
                    redirectEnd: t.redirectEnd ? (t.redirectEnd + offset) : 0,
                    fetchStart: t.fetchStart ? (t.fetchStart + offset) : 0,
                    domainLookupStart: t.domainLookupStart ? (t.domainLookupStart + offset) : 0,
                    domainLookupEnd: t.domainLookupEnd ? (t.domainLookupEnd + offset) : 0,
                    connectStart: t.connectStart ? (t.connectStart + offset) : 0,
                    secureConnectionStart: t.secureConnectionStart ? (t.secureConnectionStart + offset) : 0,
                    connectEnd: t.connectEnd ? (t.connectEnd + offset) : 0,
                    requestStart: t.requestStart ? (t.requestStart + offset) : 0,
                    responseStart: t.responseStart ? (t.responseStart + offset) : 0,
                    responseEnd: t.responseEnd ? (t.responseEnd + offset) : 0
                });
            }

            entries = entries.concat(frameFixedEntries);
        }
        catch (e) {
            return entries;
        }

        return entries;
    }

    /**
 * Finds all remote resources in the selected window that are visible, and returns an object
 * keyed by the url with an array of height,width,top,left as the value
 *
 * @param [Window] win Window to search
 * @return [Object] Object with URLs of visible assets as keys, and Array[height, width, top, left] as value
 */
    function getVisibleEntries(win) {
        var els = ["IMG", "IFRAME"], entries = {}, x, y, doc = win.document;

        // https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollX
        // https://developer.mozilla.org/en-US/docs/Web/API/Element/getBoundingClientRect
        x = (win.pageXOffset !== undefined) ? win.pageXOffset : (doc.documentElement || doc.body.parentNode || doc.body).scrollLeft;
        y = (win.pageYOffset !== undefined) ? win.pageYOffset : (doc.documentElement || doc.body.parentNode || doc.body).scrollTop;

        // look at each IMG and IFRAME
        els.forEach(function (elname) {
            var elements = doc.getElementsByTagName(elname), el, i, rect;

            for (i = 0; i < elements.length; i++) {
                el = elements[i];

                // look at this element if it has a src attribute, and we haven't already looked at it
                if (el && el.src && !entries[el.src]) {
                    rect = el.getBoundingClientRect();

                    // Require both height & width to be non-zero
                    // IE <= 8 does not report rect.height/rect.width so we need offsetHeight & width
                    if ((rect.height || el.offsetHeight) && (rect.width || el.offsetWidth)) {
                        entries[el.src] = [el.offsetHeight, el.offsetWidth, Math.round(rect.top + y), Math.round(rect.left + x)];
                    }
                }
            }
        });

        return entries;
    }

    /**
	 * Gathers a filtered list of performance entries.
	 * @param [number] from Only get timings from
	 * @param [number] to Only get timings up to
	 * @param [string[]] initiatorTypes Array of initiator types
	 * @return [ResourceTiming[]] Matching ResourceTiming entries
	 */
    function getResourceTiming(from, to, initiatorTypes) {
        var entries = findPerformanceEntriesForFrame(BOOMR.window, true, 0, 0),
		    i, e, results = {}, initiatorType, url, data,
		    navStart = getNavStartTime(BOOMR.window);

        if (!entries || !entries.length) {
            return [];
        }

        var filteredEntries = [];
        for (i = 0; i < entries.length; i++) {
            e = entries[i];

            // skip non-resource URLs
            if (e.name.indexOf("about:") === 0 ||
			    e.name.indexOf("javascript:") === 0) {
                continue;
            }

            // skip boomerang.js and config URLs
            if (e.name.indexOf(BOOMR.url) > -1 ||
			    e.name.indexOf(BOOMR.config_url) > -1 ||
			    (typeof BOOMR.getBeaconURL === "function" && BOOMR.getBeaconURL() && e.name.indexOf(BOOMR.getBeaconURL()) > -1)) {
                continue;
            }

            // if the user specified a "from" time, skip resources that started before then
            if (from && (navStart + e.startTime) < from) {
                continue;
            }

            // if we were given a final timestamp, don't add any resources that started after it
            if (to && (navStart + e.startTime) > to) {
                // We can also break at this point since the array is time sorted
                break;
            }

            // if given an array of initiatorTypes to include, skip anything else
            if (typeof initiatorTypes !== "undefined" && initiatorTypes !== "*" && initiatorTypes.length) {
                if (!e.initiatorType || !BOOMR.utils.inArray(e.initiatorType, initiatorTypes)) {
                    continue;
                }
            }

            filteredEntries.push(e);
        }

        return {
            restime: filteredEntries,
            navStart: navStart
        };
    }

    function captureParameters() {
        var r = getResourceTiming();
        var cxoptimizeParams = {};

        if (r) {
            BOOMR.info("Client supports Resource Timing API", "restiming");

            if (impl.includeResourceTiming)
                cxoptimizeParams["resourcetiming"] = r['restime'];
            cxoptimizeParams["navstart"] = r['navStart'];
        }

        return cxoptimizeParams;
    }
    
    impl = {
        complete: false,
        initialized: false,
        supported: false,
        done: function (pageEvent) {        },
        clearMetrics: function (vars) { },
        clear: function () {
            BOOMR.removeVar("details");
            BOOMR.removeVar("navstart");
            BOOMR.removeVar("resourcetiming");
            this.complete = false;
        }
    };

    BOOMR.plugins.CXOptimize = {
        init: function (config) {
            BOOMR.constants.MAX_GET_LENGTH = 2; // overriding MAX_GET_LENGTH to force POST submit
            var p = BOOMR.getPerformance();

            BOOMR.utils.pluginConfig(impl, config, "CXOptimize", ["beacon_url", "client", "project", "scenario", "build", "application", "includeResourceTiming", "includeDOM", "includeDOMCount", "includedRTPlugin", "runID"]);

            if (impl.initialized) {
                return this;
            }

            hookBeaconReport();

            impl.initialized = true;

            return this;
        },
		groupXHRRequests: false,
		groupXHRTimeout: 1500,
        useJSON: true,
        is_complete: function () {
            return true;
        },
        is_supported: function () {
            return impl.initialized && impl.supported;
        },
        // exports for test
        findPerformanceEntriesForFrame: findPerformanceEntriesForFrame,
        getResourceTiming: getResourceTiming
    };

    function getSpeedIndex() {
		
		/*		
*****************************************************************************
Copyright (c) 2014, Google Inc.
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the <ORGANIZATION> nor the names of its contributors
    may be used to endorse or promote products derived from this software
    without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
******************************************************************************

******************************************************************************
*******************************************************************************
  Calculates the Speed Index for a page by:
  - Collecting a list of visible rectangles for elements that loaded
    external resources (images, background images, fonts)
  - Gets the time when the external resource for those elements loaded
    through Resource Timing
  - Calculates the likely time that the background painted
  - Runs the various paint rectangles through the SpeedIndex calculation:
    https://sites.google.com/a/webpagetest.org/docs/using-webpagetest/metrics/speed-index
  TODO:
  - Improve the start render estimate
  - Handle overlapping rects (though maybe counting the area as multiple paints
    will work out well)
  - Detect elements with Custom fonts and the time that the respective font
    loaded
  - Better error handling for browsers that don't support resource timing
*******************************************************************************
*****************************************************************************
*/
        var win = win || BOOMR.window;
		var doc = win.document,

        GetElementViewportRect = function (a) {
            var b = !1;
            if (a.getBoundingClientRect) {
                var c = a.getBoundingClientRect();
                b = {
                    top: Math.max(c.top, 0),
                    left: Math.max(c.left, 0),
                    bottom: Math.min(c.bottom, win.innerHeight || doc.documentElement.clientHeight),
                    right: Math.min(c.right, win.innerWidth || doc.documentElement.clientWidth)
                },
                b.bottom <= b.top || b.right <= b.left ? b = !1 : b.area = (b.bottom - b.top) * (b.right - b.left)
            }
            return b
        },

        CheckElement = function (a, b) {
            if (b) {
                var c = GetElementViewportRect(a);
                c && rects.push({
                    url: b,
                    area: c.area,
                    rect: c
                })
            }
        },

        GetRects = function () {
            for (
                var a = doc.getElementsByTagName("*"),
                b = /url\(.*(http.*)\)/gi, c = 0;
                c < a.length; c++) {
                var d = a[c],
                e = win.getComputedStyle(d);
                if ("IMG" == d.tagName && CheckElement(d, d.src), e["background-image"]) {
                    b.lastIndex = 0;
                    var f = b.exec(e["background-image"]);
                    f && f.length > 1 && CheckElement(d, f[1].replace('"', ""))
                }
                if ("IFRAME" == d.tagName)
                    try {
                        var g = GetElementViewportRect(d);
                        if (g) {
                            var h = RUMSpeedIndex(d.contentWindow);
                            h && rects.push({
                                tm: h,
                                area: g.area,
                                rect: g
                            })
                        }
                    } catch (a) { }

            }

        },

        GetRectTimings = function () {
            for (var a = {},
                b = win.performance.getEntriesByType("resource"), c = 0;
                c < b.length; c++)
                a[b[c].name] = b[c].responseEnd;

            for (var d = 0; d < rects.length; d++)
                "tm" in rects[d] || (rects[d].tm = void 0 !== a[rects[d].url] ? a[rects[d].url] : 0)
        },

        GetFirstPaint = function () {
            if ("msFirstPaint" in win.performance.timing && (firstPaint = win.performance.timing.msFirstPaint - navStart), "chrome" in win && "loadTimes" in win.chrome) {
                var a = win.chrome.loadTimes();
                if ("firstPaintTime" in a && a.firstPaintTime > 0) {
                    var b = a.startLoadTime;
                    "requestTime" in a && (b = a.requestTime),
                    a.firstPaintTime >= b && (firstPaint = 1e3 * (a.firstPaintTime - b))
                }

            }
            if (void 0 === firstPaint || firstPaint < 0 || firstPaint > 12e4) {
                firstPaint = win.performance.timing.responseStart - navStart;
                for (var c = {}, d = doc.getElementsByTagName("head")[0].children, e = 0; e < d.length; e++) {
                    var f = d[e];
                    "SCRIPT" == f.tagName && f.src && !f.async && (c[f.src] = !0),
                    "LINK" == f.tagName && "stylesheet" == f.rel && f.href && (c[f.href] = !0)
                }
                for (var g = win.performance.getEntriesByType("resource"), h = !1, i = 0; i < g.length; i++)
                    if (h || !c[g[i].name] || "script" != g[i].initiatorType && "link" != g[i].initiatorType)
                        h = !0;
                    else {
                        var j = g[i].responseEnd;
                        (void 0 === firstPaint || j > firstPaint) && (firstPaint = j)
                    }

            }
            firstPaint = Math.max(firstPaint, 0)
        },

        CalculateVisualProgress = function () {
            for (var a = {
                    0: 0
            }, b = 0, c = 0; c < rects.length; c++) {
                var d = firstPaint;
                "tm" in rects[c] && rects[c].tm > firstPaint && (d = rects[c].tm),
                void 0 === a[d] && (a[d] = 0),
                a[d] += rects[c].area,
                b += rects[c].area
            }
            var e = Math.max(doc.documentElement.clientWidth, win.innerWidth || 0) * Math.max(doc.documentElement.clientHeight, win.innerHeight || 0);
            if (e > 0 && (e = Math.max(e - b, 0) * pageBackgroundWeight, void 0 === a[firstPaint] && (a[firstPaint] = 0), a[firstPaint] += e, b += e), b) {
                for (var f in a)
                    a.hasOwnProperty(f) && progress.push({
                        tm: f,
                        area: a[f]
                    });
                progress.sort(function (a, b) {
                    return a.tm - b.tm
                });
                for (var g = 0, h = 0; h < progress.length; h++)
                    g += progress[h].area, progress[h].progress = g / b
            }

        },

        CalculateSpeedIndex = function () {
            if (progress.length) {
                SpeedIndex = 0;
                for (var a = 0, b = 0, c = 0; c < progress.length; c++) {
                    var d = progress[c].tm - a;
                    d > 0 && b < 1 && (SpeedIndex += (1 - b) * d),
                    a = progress[c].tm,
                    b = progress[c].progress
                }

            } else
                SpeedIndex = firstPaint
        },

        rects = [], progress = [], firstPaint, SpeedIndex, pageBackgroundWeight = .1;

        try {
            var navStart = win.performance.timing.navigationStart;
            GetRects(),
            GetRectTimings(),
            GetFirstPaint(),
            CalculateVisualProgress(),
            CalculateSpeedIndex()
        } catch (a) { }

        return SpeedIndex;
    }
}());