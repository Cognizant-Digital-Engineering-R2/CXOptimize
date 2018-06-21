## CXOptimize Javascript Collector

This collector is based on javascript which can be copied directly to each page head similar to any analytics script to collect performance data about the web page.

We have re-used boomerang library created and maintained by SOASTA Inc and Open Source Community along with our own custom plugin created to collect extra data and to provide configuration.

https://github.com/SOASTA/boomerang

Please look into the [PRODUCTDETAILS.md](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/master/PRODUCTDETAILS.md) for plugin constraints and capability of chrome plug in [USERGUIDE.md](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/master/USERGUIDE.md)

## Steps to configure Javascript based collector

The following is the simplest way to add cxoptimize javascript collector to your web page as inline script.There are other ways to inject this script into the web page which is given in the github SOASTA.

### Step 1:

Update cxoptimizeconfig.js file with the following values.

```
beacon_url: 'http://cxoptimizehost/cxoptimize/api/insertBoomerangStats'

client: 'Cognizant',
project: 'ILPB',
scenario: 'RUM',
build: '2.0.0',
release: '2.0.0',

```

###Step 2:

Build the collector script using gruntfile.js

```
grunt
```

###Step 3:

Copy the generated script into the web page by adding it as first statement in <head>

###Step 4:

For Single Page Applications like Angular,React etc in addition to above follow the steps specified as per this link

https://docs.soasta.com/boomerang/