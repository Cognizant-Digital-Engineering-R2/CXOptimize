# Introduction

 This document is intended for anyone who wants to use CXOptimize. 


# Step 1 - Installation

Install CXOptimize as per the steps from [INSTALLATION.md](INSTALLATION.md)
    
# Step 2 - Create Configuration

Create configuration first using CXOptimizeUI by logging into it as ADMIN (refer UI section below)

# Step 3 - Configure Collector

The configuration created in the above step will be used in the collector.

[Download CXOptimize Collector Executables from here](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/releases/tag/v2.0.3)

##	1.  UI Automation Script (Selenium Java/Selenium C#/Selenium VB.Net/CodedUI)

CXOptimize supports 3 different types of collectors and they support different set of features as given below.Please select your collector based on the support matrix provided below.

| Metrics       | Selenium Java           | Selenium C#/VB.Net   | Selenium NodeJS   | CodedUI   | Chrome Plugin   | JavascriptCollector  | Comments   |
| ------------- |:-------------:| -----:| -----:| -----:| -----:| -----:| ---------:|
| SPA Support   | Yes | Yes | Yes | Yes |  No | Yes | no support for Angular2/Angular4
| Resource Header   | Yes | Yes | Yes | Yes | No | No |
| Navigation Timing   | Yes | Yes | Yes | Yes | Yes | Yes |
| Resource Timing   | Yes | Yes | Yes | Yes | Yes | Yes |
| RunID Support   | Yes | Yes | Yes | Yes | No | No |
| Custom Markers   | Yes | Yes | Yes | Yes | No | No |
    
### 1. Following are important properties which are required for the Collector module to work properly.
        ```
        * clientName=(any user defined name created in step 2)
        * projectName=(any user defined name created in step 2)
        * cenarioName=(any user defined name created in step 2)
        All the above three properties uniquely represent data for a given application.These values needs to be created first in CXOptimize Settings Page else the collector will throw exception saying no configuration defined.
        * beaconUrl=http://webserverproxyhost:port/cxoptimize/api (if web server proxy is used)
        * beaconUrl=http://cxoptimizeapihost:port/ (if no web server proxy is used)
        * UserName=username created in the system
        * Password=password created in the system
        
        *All the above properties are mandatory*
        ```
    
### 2.  Following are the optional parameters in the properties file
    	
        ```
        *LicenseKey= (not used)
        * isLoadTest=false (set it to true when you want to execute load test using Selenium with Collector enabled.This will collect limited data compared to non-load test mode)
        * markWaitTime=5000 (if your web page has any custom markers defined using performance.mark(name); this instructs collector to wait configured milli-seconds even after onLoadEvent is fired)
        * resourceSettleTime=2000 (for SPA's we use return window.performance.getEntriesByType('resource').length; to see if all the resources are fired as part of the page.This time controls how many milli-second we need to wait between subsequent length check.This is an temporary workaround and the more accurate way to implement Mutation Observer to identify when soft is complete in your page)
        ```
    
### 3.  Following are the steps to include Collector Module to existing Selenium script and use then for collecting performance metrics.
####    1.  Add Collector-version.jar/CXOptimizeCollector.dll to your solution.For NodeJS use npm to install @cognizantcxoptimize/collector package from public npm registry.
####    2.  Add ```import com.cognizant.pace.CXOptimize.Collector.*; or using com.cognizant.pace.CXOptimize.Collector``` to your class file where you want to use it.For NodeJS add this to your script ```const cxoptimize = require('@cognizantcxoptimize/collector');```
####    3.  Provide input path to read the Collector.properties.There are multiple ways to do this
            a.  Set it via Setter.Using 
			```
			CollectorConstants.setCollectorProperties(path)
			CollectorConstants.setBuild(build); //if not set defaults to yyyyMMddHH
            CollectorConstants.setRelease(release); //if not set defaults to yyyyMM
			``` 
			before calling any other Collector methods.Path is just the path with out Collector.properties in it.
            b.  Create collectordependency folder within your project root and place jar and properties file there.
            c.  Set them via Setter provided 
    		        
                ``` 
                CollectorConstants.setClientName(cname);
                CollectorConstants.setProjectName(pname);
                CollectorConstants.setScenarioName(sname);
                CollectorConstants.setBeaconURL(url);
                CollectorConstants.setBuild(build);
                CollectorConstants.setRelease(release);
                CollectorConstants.setLoadTest(loadTest);
                CollectorConstants.setMarkWaitTime(cname);
                CollectorConstants.setResourceSettleTime(cname);
                ```
            d.For NodeJS place the Collector.properties in the project's root folder where your test.js file is present.    		
####    4.   Add the following lines of code before and after page navigation using any selenium commands like ```get(),click(),navigate()``` 
            ```
            CXOptimizeCollector.StartTransaction("Home", driver); //TransactionName & WebDriver Object for Selenium/BrowserWindow object for CodedUI
            driver.get("https://www.wikipedia.org/");
            CXOptimizeCollector.EndTransaction("Home", driver); //TransactionName &WebDriver Object for Selenium/BrowserWindow object for CodedUI - this also has overload method to pass transaction status as integer 0 or 1 based on certain check
			
			NodeJS snippet
			await cxoptimize.StartTransaction('Home', driver);
			await driver.get('http://en.wikipedia.org/');
			await cxoptimize.EndTransaction('Home', driver,1);
            
            ```

####    5.  (Optional) Adding WebDriver wait like below also improves the accuracy of data collection when SPA's are involved
                		
            ```
            Selenium Java Snippet
            CXOptimizeCollector.StartTransaction("Home", driver); //TransactionName & WebDriver Object for Selenium/BrowserWindow object for CodedUI
            driver.get("https://www.wikipedia.org/");
            new WebDriverWait(driver).until(ExpectedConditions.visibilityOfElementLocated((By.id(condition)))); //This is just reference and check that you want to have can be added here
            CXOptimizeCollector.EndTransaction("Home", driver); //TransactionName & WebDriver Object - this also has overload method to pass transaction status as integer 0 or 1 based on certain check
            
            Selenium C# Snippet
            CXOptimizeCollector.StartTransaction("Home", driver); //TransactionName & WebDriver Object for Selenium/BrowserWindow object for CodedUI
            driver.get("https://www.wikipedia.org/");
            new WebDriverWait(driver).Until(ExpectedConditions.ElementExists((By.Id(condition)))); //This is just reference and check that you want to have can be added here
            CXOptimizeCollector.EndTransaction("Home", driver); //TransactionName & WebDriver Object - this also has overload method to pass transaction status as integer 0 or 1 based on certain check
            
            CodedUI C# Snippet
            CXOptimizeCollector.StartTransaction("Home", driver); //TransactionName & BrowserWindow object for CodedUI
            driver.Launch(new Uri("https://www.wikipedia.org/"));
            //Add WebDriver wait equivalent of CodedUI
            CXOptimizeCollector.EndTransaction("Home", driver); //TransactionName & BrowserWindow Object - this also has overload method to pass transaction status as integer 0 or 1 based on certain check
            
            ```
    		        
####    6.  When Selenium/CodedUI script is executed this log ```CXOP - Home -  Data uploaded succesfully```.SLF4J is implemented for detailed logging incase of any issues.

##	2.  Browser Plugin

Steps to install and configure Chrome plugin to chrome browser.
### Step 1:
Copy ChromePlugin folder from CXOptimizeCollector to local folder in your machine.

### Step 2:
Update config.js file with following mandatory parameters.

	```
	"clientName": (any user defined name created in UI),
	"projectName": (any user defined name created in UI),
	"scenarioName": (any user defined name created in UI),
	"beaconUrl": "http://webserverproxyhost:port/cxoptimize/api (if web server proxy is used)",
	```
### Step 3:
Update config.js file with following optional parameters if you want to capture screenshot for the current transaction

	```
	"screenCapture": false, //true to enable capture
	"downloadFolder": "file:///downloadpath/"
	```
### Step 4:
Update manifest.json file for matches node in content_scripts parent node the domain name/pages that you want to enable plugin to capture performance metrics 

	```
	"content_scripts": [
	{
	"matches": [
	"*://*.org/*"  //this will capture all domains with org - it takes Javascript regex to identify what pages to capture and what to ignore
	],
	"js": [
	"myscript.js","rum-speedindex.js"
	],
	"run_at": "document_idle"
	}
	]
	```
### Step 5:

Open chrome tab and type chrome://extensions and just drag the entire ChromePlugin folder from your local and drop it in the extensions tab.This will automatically install the plugin and ready to capture data for the configured pages in Step 4.Just navigate your application after this manually to collect performance metrics

![](/Documents/images/plugininstall.png "CXOptimize Chrome Plugin Install")
	
##	3.  Javascript
Steps to configure Javascript based collector
### Step 1:
Update cxoptimizeconfig.js file with the following values.

	```
	beacon_url: 'https://cxoptimizehost/cxoptimize/api/insertBoomerangStats'
	client: '(any user defined name created in UI)',
	project: '(any user defined name created in UI)',
	scenario: '(any user defined name created in UI)',
	```

### Step 2:

Build the collector script using gruntfile.js

	```
	grunt
	```

This will generate cxopcollector.js file

### Step 3:

Copy the generated cxopcollector.js into the web page by adding it as first statement in <head></head>

### Step 4:

For Single Page Applications like AngularJS,React etc in addition to above follow the steps specified as per this link

[Click here to get SOASTA Documentation](https://docs.soasta.com/boomerang/#single-page-apps)


## CXOptimize API

CXOptimize API is heart/brain of the entire system which provides RESTful API for the Collector and UI to function properly.In addition to that entire business logic is part of the API tier.There are multiple APIs as part of this which can be used to consume the data from CXOptimize with lot of other systems.

[CXOptimize API properties found here](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/master/CXOptimizeAPI/README.md)

[CXOptimize API Documentation found here](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/tree/master/Documents)


## CXOptimize UI

CXOptimize UI is frontend for the solution which provides graphical interface for viewing the analyzed data.This UI also allows the user to create configuration for new project and edit variety of configuration for the existing project.

###	Login: 
(Enter Username/Password here - default system defined is cxopadmin/qwertyuiop and enter login)

![](/Documents/images/login.png "CXOptimize Login Page")

###	Landing:

This page pulls all the clients (top-level hierarchy in uniquely identifying given run along with project and scenario) which are congiured for the logged in user.

1. Choose Analysis Type
	1.	Run (default) - compares data against two runs
	2.	Transaction - this compares the transaction from current run against best performing samples of the same transaction across multiple previous runs from the start.
	3.	Time - compares data against selected time windows

	![](/Documents/images/landing.png "CXOptimize Landing Page")
	
2. Select ClientName/Project Name/Scenario Name/Scenario
	If there are any previous data against this combination it is will be displayed below if not there willnt be anything except Dashboard and Setting button.
	Clicking Settings button will take you to settings page where all configuration related stuffs are available.This page is only available to users with ADMIN access.
	Clicking Dashboard will take to report page

	![](/Documents/images/selectdetails.png "Select Options")

###	Settings:

1.	Create New User

	![](/Documents/images/createuser.png "Create User")

2.	Manage User (to assign existing clients to the existing user)

	![](/Documents/images/manageuser.png "Manage User")

3.	Create New Configuration (This step is mandatory before you want to collect and analyze data for given application)

	![](/Documents/images/createconfig.png "Create Configuration")

4.	Edit Configuration (to modify default system defined configuration)

	![](/Documents/images/editconfig.png "Edit Configuration")

5. Edit Transaction SLA

	![](/Documents/images/editsla.png "Edit Transaction SLA")

6. Edit Generic Rules (can exclude rules,modify priority and weightage)

	![](/Documents/images/editrules.png "Edit Rules")

7. Edit Soft Navigation specific rules (can exclude rules,modify priority and weightage)

	![](/Documents/images/editsoftrules.png "Edit Soft Navigation Rules")


###	Dashboard:

This page displays analysis data for the given run.

1.	Summary View - provides highlevel summary of the current run with respect to performance.

	![](/Documents/images/summaryview.png "Summary View")
	![](/Documents/images/summaryview2.png "Summary View")

2.	Response Time View - provides detailed performance metrics for the given transaction (the value provided here are from 95th percentile sample which can be chnage to Average or 90th percentile in Settings page based on your requirement)

	![](/Documents/images/responsetimeview.png "Response Time View")
	![](/Documents/images/timedistribution.png "Time Distribution View")

3. Comparison View - provides comparison analysis between current run and baseline run.This also compares the individual resources called in the page.

	![](/Documents/images/comparisonview.png "Comparison View")
	![](/Documents/images/resourcecomparisonview.png "Resource Time Comparison")
	![](/Documents/images/resourcecomparisonview2.png "Resource Size Comparison")

4. Cross Browser Comparison - if a given transaction is executed across multiple browser then we can compare their performance as well.Transaction Name across the different browser,device,platform needs to be same for using to work

	![](/Documents/images/xbrowsercomparison.png "Cross Browser Comparison View")

5. Recommendation View - this provides score for the transcation based on pre-defined ruleset.

	![](/Documents/images/recommendation.png "Recommendation View")

6. HAR View

	![](/Documents/images/harview.png "HAR View")

7.	Trending View - predefined Kibana dashboard

	![](/Documents/images/trendingview.png "Trending View")
