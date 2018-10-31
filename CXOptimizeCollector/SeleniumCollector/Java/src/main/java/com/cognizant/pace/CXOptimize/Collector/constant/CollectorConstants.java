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

package com.cognizant.pace.CXOptimize.Collector.constant;

import java.util.List;
import java.util.Arrays;


public class CollectorConstants
{
    private static final String loadEventEndScript = "var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};var timings = performance.timing || {}; return timings.loadEventEnd;";
    private static final String loadEventEndScriptIE = "return JSON.stringify(window.performance.timing.loadEventEnd);";
    private static final String navTimingScript = "var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};var timings = performance.timing || {}; return timings;";
    private static final String navTimingScriptIE = "return JSON.stringify(window.performance.timing);";
    private static final String chromeFirstPaintScript = "var msPaintTime; if(typeof window.chrome == 'undefined') return -9999; if((window.chrome.loadTimes().firstPaintTime - window.chrome.loadTimes().startLoadTime) < 3600) {msPaintTime = window.chrome.loadTimes().firstPaintTime * 1000;}else{msPaintTime = window.performance.timing.loadEventEnd;}return msPaintTime;";
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
    //private static final String speedIndexScript = "var win=win||window,doc=win.document,GetElementViewportRect=function(a){var b=!1;if(a.getBoundingClientRect){var c=a.getBoundingClientRect();b={top:Math.max(c.top,0),left:Math.max(c.left,0),bottom:Math.min(c.bottom,win.innerHeight||doc.documentElement.clientHeight),right:Math.min(c.right,win.innerWidth||doc.documentElement.clientWidth)},b.bottom<=b.top||b.right<=b.left?b=!1:b.area=(b.bottom-b.top)*(b.right-b.left)}return b},CheckElement=function(a,b){if(b){var c=GetElementViewportRect(a);c&&rects.push({url:b,area:c.area,rect:c})}},GetRects=function(){for(var a=doc.getElementsByTagName(\"*\"),b=/url\\(.*(http.*)\\)/gi,c=0;c<a.length;c++){var d=a[c],e=win.getComputedStyle(d);if(\"IMG\"==d.tagName&&CheckElement(d,d.src),e[\"background-image\"]){b.lastIndex=0;var f=b.exec(e[\"background-image\"]);f&&f.length>1&&CheckElement(d,f[1].replace('\"',\"\"))}if(\"IFRAME\"==d.tagName)try{var g=GetElementViewportRect(d);if(g){var h=RUMSpeedIndex(d.contentWindow);h&&rects.push({tm:h,area:g.area,rect:g})}}catch(a){}}},GetRectTimings=function(){for(var a={},b=win.performance.getEntriesByType(\"resource\"),c=0;c<b.length;c++)a[b[c].name]=b[c].responseEnd;for(var d=0;d<rects.length;d++)\"tm\"in rects[d]||(rects[d].tm=void 0!==a[rects[d].url]?a[rects[d].url]:0)},GetFirstPaint=function(){if(\"msFirstPaint\"in win.performance.timing&&(firstPaint=win.performance.timing.msFirstPaint-navStart),\"chrome\"in win&&\"loadTimes\"in win.chrome){var a=win.chrome.loadTimes();if(\"firstPaintTime\"in a&&a.firstPaintTime>0){var b=a.startLoadTime;\"requestTime\"in a&&(b=a.requestTime),a.firstPaintTime>=b&&(firstPaint=1e3*(a.firstPaintTime-b))}}if(void 0===firstPaint||firstPaint<0||firstPaint>12e4){firstPaint=win.performance.timing.responseStart-navStart;for(var c={},d=doc.getElementsByTagName(\"head\")[0].children,e=0;e<d.length;e++){var f=d[e];\"SCRIPT\"==f.tagName&&f.src&&!f.async&&(c[f.src]=!0),\"LINK\"==f.tagName&&\"stylesheet\"==f.rel&&f.href&&(c[f.href]=!0)}for(var g=win.performance.getEntriesByType(\"resource\"),h=!1,i=0;i<g.length;i++)if(h||!c[g[i].name]||\"script\"!=g[i].initiatorType&&\"link\"!=g[i].initiatorType)h=!0;else{var j=g[i].responseEnd;(void 0===firstPaint||j>firstPaint)&&(firstPaint=j)}}firstPaint=Math.max(firstPaint,0)},CalculateVisualProgress=function(){for(var a={0:0},b=0,c=0;c<rects.length;c++){var d=firstPaint;\"tm\"in rects[c]&&rects[c].tm>firstPaint&&(d=rects[c].tm),void 0===a[d]&&(a[d]=0),a[d]+=rects[c].area,b+=rects[c].area}var e=Math.max(doc.documentElement.clientWidth,win.innerWidth||0)*Math.max(doc.documentElement.clientHeight,win.innerHeight||0);if(e>0&&(e=Math.max(e-b,0)*pageBackgroundWeight,void 0===a[firstPaint]&&(a[firstPaint]=0),a[firstPaint]+=e,b+=e),b){for(var f in a)a.hasOwnProperty(f)&&progress.push({tm:f,area:a[f]});progress.sort(function(a,b){return a.tm-b.tm});for(var g=0,h=0;h<progress.length;h++)g+=progress[h].area,progress[h].progress=g/b}},CalculateSpeedIndex=function(){if(progress.length){SpeedIndex=0;for(var a=0,b=0,c=0;c<progress.length;c++){var d=progress[c].tm-a;d>0&&b<1&&(SpeedIndex+=(1-b)*d),a=progress[c].tm,b=progress[c].progress}}else SpeedIndex=firstPaint},rects=[],progress=[],firstPaint,SpeedIndex,pageBackgroundWeight=.1;try{var navStart=win.performance.timing.navigationStart;GetRects(),GetRectTimings(),GetFirstPaint(),CalculateVisualProgress(),CalculateSpeedIndex()}catch(a){}return SpeedIndex.toString();";
    private static final String speedIndexScript = "var firstPaint,SpeedIndex,win=window,doc=win.document,GetElementViewportRect=function(t){var e=!1;if(t.getBoundingClientRect){var r=t.getBoundingClientRect();(e={top:Math.max(r.top,0),left:Math.max(r.left,0),bottom:Math.min(r.bottom,win.innerHeight||doc.documentElement.clientHeight),right:Math.min(r.right,win.innerWidth||doc.documentElement.clientWidth)}).bottom<=e.top||e.right<=e.left?e=!1:e.area=(e.bottom-e.top)*(e.right-e.left)}return e},CheckElement=function(t,e){if(e){var r=GetElementViewportRect(t);r&&rects.push({url:e,area:r.area,rect:r})}},GetRects=function(){for(var t=doc.getElementsByTagName(\"*\"),e=/url\\(.*(http.*)\\)/gi,r=0;r<t.length;r++){var i=t[r],n=win.getComputedStyle(i);if(\"IMG\"==i.tagName&&CheckElement(i,i.src),n[\"background-image\"]){e.lastIndex=0;var a=e.exec(n[\"background-image\"]);a&&a.length>1&&CheckElement(i,a[1].replace('\"',\"\"))}if(\"IFRAME\"==i.tagName)try{var s=GetElementViewportRect(i);if(s){var o=RUMSpeedIndex(i.contentWindow);o&&rects.push({tm:o,area:s.area,rect:s})}}catch(t){}}},GetRectTimings=function(){for(var t={},e=win.performance.getEntriesByType(\"resource\"),r=0;r<e.length;r++)t[e[r].name]=e[r].responseEnd;for(var i=0;i<rects.length;i++)\"tm\"in rects[i]||(rects[i].tm=void 0!==t[rects[i].url]?t[rects[i].url]:0)},GetFirstPaint=function(){try{for(var t=performance.getEntriesByType(\"paint\"),e=0;e<t.length;e++)if(\"first-paint\"==t[e].name){navStart=performance.getEntriesByType(\"navigation\")[0].startTime,firstPaint=t[e].startTime-navStart;break}}catch(t){}if(void 0===firstPaint&&\"msFirstPaint\"in win.performance.timing&&(firstPaint=win.performance.timing.msFirstPaint-navStart),void 0===firstPaint&&\"chrome\"in win&&\"loadTimes\"in win.chrome){var r=win.chrome.loadTimes();if(\"firstPaintTime\"in r&&r.firstPaintTime>0){var i=r.startLoadTime;\"requestTime\"in r&&(i=r.requestTime),r.firstPaintTime>=i&&(firstPaint=1e3*(r.firstPaintTime-i))}}if(void 0===firstPaint||firstPaint<0||firstPaint>12e4){firstPaint=win.performance.timing.responseStart-navStart;var n={},a=doc.getElementsByTagName(\"head\")[0].children;for(e=0;e<a.length;e++){var s=a[e];\"SCRIPT\"==s.tagName&&s.src&&!s.async&&(n[s.src]=!0),\"LINK\"==s.tagName&&\"stylesheet\"==s.rel&&s.href&&(n[s.href]=!0)}for(var o=win.performance.getEntriesByType(\"resource\"),c=!1,m=0;m<o.length;m++)if(c||!n[o[m].name]||\"script\"!=o[m].initiatorType&&\"link\"!=o[m].initiatorType)c=!0;else{var g=o[m].responseEnd;(void 0===firstPaint||g>firstPaint)&&(firstPaint=g)}}firstPaint=Math.max(firstPaint,0)},CalculateVisualProgress=function(){for(var t={0:0},e=0,r=0;r<rects.length;r++){var i=firstPaint;\"tm\"in rects[r]&&rects[r].tm>firstPaint&&(i=rects[r].tm),void 0===t[i]&&(t[i]=0),t[i]+=rects[r].area,e+=rects[r].area}var n=Math.max(doc.documentElement.clientWidth,win.innerWidth||0)*Math.max(doc.documentElement.clientHeight,win.innerHeight||0);if(n>0&&(n=Math.max(n-e,0)*pageBackgroundWeight,void 0===t[firstPaint]&&(t[firstPaint]=0),t[firstPaint]+=n,e+=n),e){for(var a in t)t.hasOwnProperty(a)&&progress.push({tm:a,area:t[a]});progress.sort(function(t,e){return t.tm-e.tm});for(var s=0,o=0;o<progress.length;o++)s+=progress[o].area,progress[o].progress=s/e}},CalculateSpeedIndex=function(){if(progress.length){SpeedIndex=0;for(var t=0,e=0,r=0;r<progress.length;r++){var i=progress[r].tm-t;i>0&&e<1&&(SpeedIndex+=(1-e)*i),t=progress[r].tm,e=progress[r].progress}}else SpeedIndex=firstPaint},rects=[],progress=[],output=[],pageBackgroundWeight=.1;try{var navStart=win.performance.timing.navigationStart;GetRects(),GetRectTimings(),GetFirstPaint(),CalculateVisualProgress(),CalculateSpeedIndex(),output[0]=firstPaint.toFixed(0).toString(),output[1]=SpeedIndex.toFixed(0).toString()}catch(t){}return output;";
    private static final String clearResourceScript = "window.performance.clearResourceTimings();";
    private static final String resTimingScript = "var performance = window.performance || {};var perfResourceTiming = performance.getEntriesByType(\"resource\") || {}; return perfResourceTiming;";
    private static final String resTimingScriptIE = "return JSON.stringify(window.performance.getEntriesByType(\"resource\"));";
    private static final String heapScript = "var performance = window.console || {};var perfjsmemory = performance.memory || {}; return perfjsmemory;";
    private static final String domLengthScript = "return document.getElementsByTagName('*').length;";
    private static final String clearMarkScript = "window.performance.clearMarks();";
    private static final String markTimingScript = "var performance = window.performance || {};var perfResourceTiming = performance.getEntriesByType(\"mark\") || {}; return perfResourceTiming;";
    private static final String markTimingScriptIE = "return JSON.stringify(window.performance.getEntriesByType(\"mark\"));";
    private static final String getDomScript = "return document.documentElement.outerHTML;";
    private static final String resourceLengthScript = "return window.performance.getEntriesByType('resource').length;";
    //Getter & Setter for UserName
    private static String UserName;
    //Getter & Setter for Password
    private static String Password;
    //Getter & Setter for ClientName
    private static String ClientName;
    //Getter & Setter for ProjectName
    private static String ProjectName;
    //Getter & Setter for ScenarioName
    private static String ScenarioName;
    //Getter & Setter for BeaconURL
    private static String BeaconURL;
    //Getter & Setter for LicenseKey
    private static String LicenseKey;
    //Getter & Setter for LicenseKey
    private static String isLoadTest = "false";
    //Getter & Setter for apiToken
    private static int MarkWaitTime;
    //Getter & Setter for apiToken
    private static String ApiToken;
    //Getter & Setter for userAgent
    private static String UserAgent;
    //Getter & Setter for imageExt
    private static String Images;
    //Getter & Setter for StaticFiles
    private static String StaticExt;
    //Getter & Setter for imageExt
    private static List<String>  ImageList;
    //Getter & Setter for StaticFiles
    private static List<String>  StaticList;
    //Getter & Setter for ResourceDurationThreshold
    private static double ResDurThreshold;

    //Getter & Setter for resourceSettleTime
    private static int ResourceSettleTime;

    public static double getResDurThreshold() {
        return ResDurThreshold;
    }

    public static void setResDurThreshold(double resDurThreshold) {
        ResDurThreshold = resDurThreshold;
    }

    public static String getUserName() {
        return UserName;
    }

    public static void setUserName(String userName) {
        UserName = userName;
    }

    public static String getPassword() {
        return Password;
    }

    public static void setPassword(String password) {
        Password = password;
    }

    public static String getClientName() {
        return ClientName;
    }

    public static void setClientName(String clientName) {
        ClientName = clientName;
    }

    public static String getProjectName() {
        return ProjectName;
    }

    public static void setProjectName(String projectName) {
        ProjectName = projectName;
    }

    public static String getScenarioName() {
        return ScenarioName;
    }

    public static void setScenarioName(String scenarioName) {
        ScenarioName = scenarioName;
    }

    public static String getBeaconURL() {
        return BeaconURL;
    }

    public static void setBeaconURL(String beaconURL) {
        BeaconURL = beaconURL;
    }

    public static String getLicenseKey() {
        return LicenseKey;
    }

    public static void setLicenseKey(String licenseKey) {
        LicenseKey = licenseKey;
    }

    public static String getLoadTest() {
        return isLoadTest;
    }

    public static void setLoadTest(String loadTest) {
        isLoadTest = loadTest;
    }

    public static int getResourceSettleTime() {
        return ResourceSettleTime;
    }

    public static void setResourceSettleTime(int settleTime) {
        ResourceSettleTime = settleTime;
    }

    public static int getMarkWaitTime() {
        return MarkWaitTime;
    }

    public static void setMarkWaitTime(int markWaitTime) {
        MarkWaitTime = markWaitTime;
    }

    public static String getApiToken() {
        return ApiToken;
    }

    public static void setApiToken(String apiToken) {
        ApiToken = apiToken;
    }

    public static String getUserAgent() {
        return UserAgent;
    }

    public static void setUserAgent(String userAgent) {
        UserAgent = userAgent;
    }

    public static String getImages() {
        return Images;
    }

    public static List<String> getImageList()
    {
        return Arrays.asList(Images.split(","));
    }

    public static void setImages(String image) {
        Images = image;
    }

    public static String getStaticExt() {
        return StaticExt;
    }

    public static List<String> getStaticList()
    {
        return Arrays.asList(StaticExt.split(","));
    }

    public static void setStaticExt(String staticExt) {
        StaticExt = staticExt;
    }

    public static String getLoadEventEnd() {
        return loadEventEndScript;
    }

    public static String getLoadEventEndIE() {
        return loadEventEndScriptIE;
    }

    //Getter & Setter for Previous Transaction Time
    private static long PrevTxnStartTime = 0;
    //Getter & Setter for Previous Transaction Time
    private static long PrevTxnHeapSize = 0;

    public static long getPrevTxnStartTime() {
        return PrevTxnStartTime;
    }

    public static void setPrevTxnStartTime(long prevTxnStartTime)

    {
        PrevTxnStartTime = prevTxnStartTime;
    }

    public static long getPrevTxnHeapSize() {
        return PrevTxnHeapSize;
    }

    public static void setPrevTxnHeapSize(long prevTxnHeapSize)

    {
        PrevTxnHeapSize = prevTxnHeapSize;
    }

    public static String getNavigationTime() {
        return navTimingScript;
    }

    public static String getNavigationTimeIE() {
        return navTimingScriptIE;
    }

    public static String getFirstPaint() {
        return chromeFirstPaintScript;
    }

    public static String getSpeedIndex() {
        return speedIndexScript;
    }

    public static String clearResourceTiming() {
        return clearResourceScript;
    }

    public static String getResourceTime() {
        return resTimingScript;
    }

    public static String getResourceTimeIE() {
        return resTimingScriptIE;
    }

    public static String getResourceLength() {
        return resourceLengthScript;
    }

    public static String getHeapUsage() {
        return heapScript;
    }

    public static String getDomLength() {
        return domLengthScript;
    }

    public static String clearMarkTiming() {
        return clearMarkScript;
    }

    public static String getMarkTime() {
        return markTimingScript;
    }

    public static String getMarkTimeIE() {
        return markTimingScriptIE;
    }

    public static String getDom() {
        return getDomScript;
    }

    private static String Release;
    public static String getRelease() {
        return Release;
    }
    public static void setRelease(String rel) {
        Release = rel;
    }

    private static String Build;
    public static String getBuild() {
        return Build;
    }
    public static void setBuild(String bul) {
        Build = bul;
    }

    private static String InputLocation;

    public static void setCollectorProperties(String path)
    {
        InputLocation = path;
    }

    public static String getCollectorProperties()
    {
        return InputLocation;
    }

    private static long ScriptStartTime;

    public static long getScriptStartTime()
    {
        return ScriptStartTime;
    }

    public static void setScriptStartTime(long startTime)
    {
        ScriptStartTime = startTime;
    }

    private static String ResourceClear;
    public static String getManualResourceTimeClear() {
        return ResourceClear;
    }
    public static void setManualResourceTimeClear(String resClear) {
        ResourceClear = resClear;
    }

    private static long TokenStartTime;

    public static long getTokenStartTime()
    {
        return TokenStartTime;
    }

    public static void setTokenStartTime(long startTime)
    {
        TokenStartTime = startTime;
    }

    private static long RunStartTime;

    public static long getRunStartTime()
    {
        return RunStartTime;
    }

    public static void setRunStartTime(long startTime)
    {
        RunStartTime = startTime;
    }

    private static long TokenExpiry;

    public static long getTokenExpiry()
    {
        return TokenExpiry;
    }

    public static void setTokenExpiry(long expiry)
    {
        TokenExpiry = expiry;
    }

    private static int TxnCounter = 0;

    public static int getTxnCounter()
    {
        return TxnCounter;
    }

    public static void setTxnCounter(int count)
    {
        TxnCounter = count;
    }


}
