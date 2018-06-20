# Features

CXOptimize is a framework that allows for the identification & resolution of performance issues for web/mobile applications, early in the development lifecycle\. Some of the features are as listed below:

1. Automated Analysis
	1. Threshold Analysis – Comparison against threshold values like SLAs and Baseline
	2. Correlative Analysis - Analyze response time break down across different tiers and at component level
	3. Comparative Analysis - Compare against previous build performance and provides insight into deviation 
2. Collects and presents Comprehensive list of metrics like:
	1. Total Pageload Time
	2. Page Onload Time
	3. Frontend Time (time taken in client)
	4. Backend Time (time taken for resource calls)
	5. SpeedIndex
	6. Time to first byte
	7. Time to first paint
	8. Granular Navigation Timing
	9. No of HTTP calls
	10. Total Page Payload
	11. Cache header
	12. Compression
	13. Style Sheet & Script placement
	14. CSS, Javascript, Image size
	15. No\. of DOM elements
	16. HAR Viewer
	17. Time distribution for each domain

3. Integrates seamlessly with a variety of test automation tools like Selenium Java,Selenium C#,CodedUI et al\.
4. Integrates with several Cognizant QA Frameworks such as Spritz,CRAFT et al\. 

#	Known Product Limitations

	1.	No HTTP 2.0 support
	2.	The product works only on the browsers which support both Navigation & Resource Timing API support.
	3.	Browser plugin is available only for Chrome.
	4.	Browser plugin doesnt support any Single Page Application(SPA's).
	5.	Browser Plugin & Javascript Collector cannot get details header details of each resource compared to UI Automation Collection approach.Due to this while evaluating the score for a transaction the rules related to those headers will not be considered.
	6.	Javascript collector cannot support Angular 2 & Angular 4.

# Benefits

![](/Documents/images/benefits.png "CXOptimize Benefits")

#	 Why

![](/Documents/images/why.png "Why to use CXOptimize")

# How it Works

CXOptimize can be implemented in three different ways:

1. In conjunction with automation frameworks such as Selenium,CodedUI to provide client side performance metrics along with recommendations\. 
2. As a chrome plugin
3. As JavaScript’s which can be injected to collect performance data for each of the pages\. 


![](/Documents/images/srevideo.gif "How it Works")

# What It Does

![](/Documents/images/whatitdoes.png "What it does")

## Usage with Automation Framework 

The entire process can be explained in terms of four steps:

1. Test Automation Engineer injects Asynchronous collectors \(to collect performance test metrics\) and statements to collect performance metrics into the automated test script\. 
2. Test Automation Engineer executes automated test scripts as usual\. Single\-user Performance Testing metrics are automatically measured and collected\.
3. Collected metrics are pushed asynchronously to the CXOptimize Analytical Engine for treatment and consolidation into the centralized database\.
4. Performance Test Engineer can now view single\-user performance test statistics using the CXOptimize Dashboard\. 

## Chrome Plugin

 CXOptimize can also be implemented via the means of a chrome plugin\. The plugin will capture performance statistics of each page navigated in different tabs of the browser\. 

## JavaScript injection to Page

You can also implement CXOptimize by injecting custom JavaScript’s at head of the web page either statically or dynamically\. This will allow for metrics to be captured for different applications deployed onto your server\. 

# CXOptimize – Components and Architecture

## Architecture 


The various components of CXOptimize are 

![](/Documents/images/architecture.png "Architecture")

1. Collector – Contains utilities to collect and persist data via API\.
2. Caddy – lightweight Web Server
3. CXOptimize API – API Layer which stores and processes data within  data store
4. CXOptimize UI – Presentation tier to summarize the current execution data with respect to performance
5. Elastic Search (5.5.2) – Data storage 
6. Kibana (5.5.2) – trending dashboard
7. (optional) Grafana - trending dashboard
8. (optional) GeoIp - golang utility to provide geolocation data.

## Overall Score

Below is the approach used to calculate CXOptimize Overall score.

	Overall Score = (%Total Score Compliance * Weightage) + (%Individual Score Compliance * Weightage) + (%Comparison Compliance * Weightage) + (%SLA Compliance * Weightage)

| Compliance      | Description           | Weightage   | Calculation Details  |
| ------------- |:-------------|:-----:| ------ |
| Total Score | Average score of all individual transactions within given RunID is above configured threshold | 25% | calculated as aggregated average value (Txn1 + ..+ Txn N)/N
| Individual Score | Number of transactions within given RunID is above configured threshold for individual transaction score | 25% | calculated as count of transactions within configured score threshold/total transactions	
| Comparison | Number of transactions within given RunID is above configured threshold for comparison deviation  | 25% | calculated as count of transactions within configured comparison deviation threshold/total transactions
| SLA | Number of transactions within given RunID is above configured SLA | 25% | calculated as count of transactions within configured SLA threshold/total transactions

	Each compliance has default thrshold of 80% which can be modified as part of centralized configuration (Settings Page - Edit config Tab in UI)
	All weightages can be changed as part of centralized configuration (Settings Page - Edit SLA in UI)
	
## Rules

Following industry standard best practice  are created as rules as part of the CXOptimize API and each web transaction is compared against these rules to calculate transaction score.These rules are widely used industry standard tools like YSlow,Pagespeed,WebPageTest etc.
For CXOptimize these rules score calculation are customized as below

These rules are applicable to Hard navigation & Soft navigation with weightage varying for them respectively

| Rule ID       | Description           | Weightage (Hard)   | Weightage (Soft)   | Calculation Details  |
| ------------- |:-------------|:-----:|:-----:| ------ |
| 1  | Avoid CSS @import | 2% | NA | score = (100 - count * 10),where count = no of occurance of @image,if score < 0 then 0 |
| 2  | Minify HTML document | 5% | NA | score = (100 - ((htmlsize - compressedHtmlsize)/htmlSize) , only if htmlSize > 14 KB |
| 3  | Minimize iFrames Usage | 1% | NA | score = (100 - count * 5),where count = no of occurance of IFRAMEs,if score < 0 then 0 |
| 4  | Avoid empty SRC or HREF | 2% | NA |  |
| 5  | Specify image dimensions | 6% | NA | score = (100 - count * 5),where count = no of occurance of IMG tag without width,height or style,if score < 0 then 0 |
| 6  | Do not scale images in HTML | 3% | NA | score = (100 - count * 1),where count = no of occurance of IMG with scaling,if score < 0 then 0 |
| 7  | Remove unnecessary comments | 1% | NA | score = (100 - ((htmlsize - commentsize)/htmlSize) |
| 8  | Avoid a character set in the meta tag | 5% | NA | |
| 9  | Make CSS external | 3% | NA | score = (100 - count * 10),where count = no of occurance of inline CSS,if score < 0 then 0 |
| 10  | Put CSS at the top | 8% | NA | score = (100 - count * 10),where count = no of occurance of STYLE in Body,if score < 0 then 0 |
| 11  | Make JS external | 3% | NA | score = (100 - count * 5),where count = no of occurance of inline Javascript,if score < 0 then 0 |
| 12  | Put JavaScript at bottom | 8% | NA | score = (100 - count * 5),where count = no of occurance of javascript in HEAD without async/defer,if score < 0 then 0 |
| 13  | Prefer asynchronous resources | 8% | NA | score = (100 - count * 2),where count = no of occurance of javascript without async,if score < 0 then 0 |
| 14  | Reduce the number of DOM elements | 1% | NA | score = (100 - domsize),where domsize = (domelements-900)/250,if score < 0 then 0 |
| 15  | Avoid bad request | 3% | 6% | score = (100 - count * 10),where count = no of occurance of 400s,if score < 0 then 0 |
| 16  | Make fewer HTTP requests | 10% | 10% | score = (100 - (count - 20) * 10),where count = no of resources called in the page,if score < 0 then 0 |
| 17  | Optimize images PNG/JPG | 1% | 6% | score = (100 - (count - 9) * 11),where count = no of occurance of PNG and JPG,if score < 0 then 0 |
| 18  | Use PNG/JPG instead of GIF | 1% | 10% | score = (100 - count * 5),where count = no of occurance of GIF,if score < 0 then 0 |
| 19  | Parallelize downloads across hostnames | 2% | NA | score = ((count/4 * 100)),where count = no of domain used to serve resources,if score < 0 then 0 |
| 20  | Avoid Landing Page redirects | 1% | NA |  |
| 21  | Avoid URL redirects resources | 1% | 6% | score = (100 - count * 5),where count = no of occurance of resources redirected,if score < 0 then 0 |
| 22  | Set Proper Cache Header | 5% | 10% | score = (100 - count * 10),where count = no of occurance of resources without proper cache header,if score < 0 then 0 |
| 23  | Specify a cache validator | 2% | 10% | score = (100 - count * 10),where count = no of occurance of  resources without proper cache validator,if score < 0 then 0 |
| 24  | Specify a Vary: Accept-Encoding header | 1% | 2% | score = (100 - count * 10),where count = no of occurance of resources without Vary header,if score < 0 then 0 |
| 25  | Enable Keep Alive | 2% | 2% | score = (100 - count * 11),where count = no of occurance of resources without Keepalive header,if score < 0 then 0 |
| 26  | Avoid Duplicate Resources | 1% | 6% | score = (100 - count * 20),where count = no of occurance of duplicate resources,if score < 0 then 0 |
| 27  | Avoid Resources with Query String | 1% | 2% | score = (100 - count * 10),where count = no of occurance of resource with query string,if score < 0 then 0 |
| 28  | Avoid Flash | 1% | 2% | score = (100 - count * 10),where count = no of occurance of Flash,if score < 0 then 0 |
| 29  | Compress Static files | 4% | 10% | score = (100 - count * 10),where count = no of occurance of resource without compression,if score < 0 then 0 |
| 30  | Minify CSS and JS | 4% | 10% | score = (100 - count * 10),where count = no of occurance of resource without minification,if score < 0 then 0 |
| 31  | Increase caching duration | 4% | 8% | score = (100 - count * 10),where count = no of occurance of resources with expiray less than hour,if score < 0 then 0 |

## Elastic Search Index Details

| Index Name       | Table Name           | Purpose   | 
| ------------- |:-------------| ------ |
| masterconfig | config | used to store centralized configuration specific to client,project and scenario|
| masterconfig | rules | used to store default configuration data which will be used by any new project while creation|
| statsindex | run | used to store the raw data collected from different source of collectors|
| markindex | mark | used to store data custom performance marker data collected by collectors|
| baselineindex | baseline | used to store baseline value for each transaction for the given combination|
| analysisindex | analysis | used to persist analysis results of each run inorder to cache them|
| userdetails | user | used to store user and password details for the system|
| auditlog | audit | used to store audit logs of tools usage which can be used for custom dashboard|

