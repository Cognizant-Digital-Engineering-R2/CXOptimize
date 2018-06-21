## CXOptimize Collector - Chrome Plugin

Please look into the [PRODUCTDETAILS.md](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/master/PRODUCTDETAILS.md) for plugin constraints and capability of chrome plug in [USERGUIDE.md](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/master/USERGUIDE.md)

Contributions for other browser plugins are welcomed.Please find resources at for developing the plugins for other industry leading browsers.

[Firefox Web Extension](https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Your_first_WebExtension)
[Internet Explorer 11](https://www.codeproject.com/Articles/23536/Creating-and-Installing-Internet-Explorer-Context)
[Edge](https://docs.microsoft.com/en-us/microsoft-edge/extensions/getting-started)

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
	


## Development / Contribution

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting your pull requests. It also has details on how to setup your development environment.

## Code of Conduct

To provide clarity on what is expected of our members, Cognizant CXOptimize has adopted the code of conduct defined by the Contributor Covenant. This document is used across many open source communities and we think it articulates our values well. For more, see the [Code of Conduct](CODE_OF_CONDUCT.md).

## Contact

If you have any queries on Cognizant CXOptimize, please post your questions on [Cognizant CXOptimize Google Group](https://groups.google.com/forum/#!forum/cognizant-cxoptimize).

To ask specific questions on project development, to discuss future improvements or for any other technical detail, please join the [Cognizant CXOptimize chat on Gitter](https://gitter.im/Cognizant-CXOptimize).

## License

Cognizant CXOptimize is licensed under [Apache License, Version 2.0](LICENSE)