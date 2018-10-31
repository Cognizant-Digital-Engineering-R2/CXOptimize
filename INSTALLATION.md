# Installation Guide

#	CXOptimize Server Components

CXOptimize componenets can be installed as scaled-in or scaled-out mode

##	Pre-Requisities
1.  Windows/Linux VM with 4 cores cpu,8 GB ram and 500GB hard disk (least requirement)
2.  JDK 1.8
3.  Administrator rights on the machine (To run services)
4.  JAVA_HOME system environment variable set

	
##	Scaled In mode

In order to install the server side components, download the cxoptimize_window.rar or cxoptimize_linux.rar from the [Releases](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/releases/tag/v2.0.3) folder.After extracting the files the folder will looks like this
	
![](/Documents/images/installpackage.png "CXOptimize components")

###	Windows
1.  Once extracted,run installer.bat file from command prompt (not by double-clicking it). Make sure the command prompt is opened as Administrator.
2.  This will configure all required properties and install all the component’s necessary.
3.  It is important not to close any of the opened command prompt. The installer.bat automatically validates if all components are up and running.
4.  Use the urls.txt file if you want to manually validate all components.
5.	(Optional) Manually naviage to freegeoip folder and excute freegeoip.exe.This is used to get geo location data for a given IP.It uses open source version of the IP database from MaxMind.More details will be found [here](https://github.com/fiorix/freegeoip)
6.	(Optional) Grafana - alternative to Kibana Dashboard incase if you need rich UI and Dashboard experience.Setup details can be found [here](https://github.com/grafana/grafana)

###	Linux
1.	Navigate to \elasticsearch-5.5.2\bin and execute ./elasticsearch (This can be executed as service as well using tools like systemctl etc)
2.	Navigate to \api and execute java -jar CXOptimizeAPI-2.0.3.jar
3.	Navigate to \kibana-5.5.2-linux-x86_64\bin and execute ./kibana
4.	Navigate to \ui and execute java -jar CXOptimizeUI-2.0.3.jar
5.	Navigate to \caddy and execute ./caddy Caddyfile

### Docker Compose
1. Download [docker-compse.yml](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/blob/dev/docker-compose.yml) file to your machine
2. Make sure docker and docker-compose are installed
3. Execute docker-compose -d up.This should start everything in the required order and application will be accessible in http://localhost:8080/cxoptimize/login
	
##	Scaled Out mode
In order to install the server side components, download the cxoptimize_window.rar or cxoptimize_linux.rar from the [Releases](https://github.com/Cognizant-Digital-Engineering-PACE/CXOptimize/releases/tag/v2.0.3) folder.After extracting the files the folder will looks like this

![](/Documents/images/installpackage.png "CXOptimize components") 
 
### Step 1 - Install Elastic Search:
1.  Copy elasticsearch-5.5.2 folder to machine where you want to deploy Elastic Search
2.  Navigate to elasticsearch-5.5.2/config and update elasticsearch.yml
3.  http.port: ESPORT (any port which is accessible to other machines - default 9200) 
4.  Navigate to elasticsearch-5.5.2/bin in command prompt
5.  Run elasticsearch-service.bat install (This will install ES as service) or ./elasticsearch for linux
6.  Run elasticsearch-service.bat start
7.  Test if ES is up by http://localhost:port

### Step 2 - Install CXOptimize API:
1.   Copy api folder to machine where you want to deploy API tier
2.   Navigate to /api and update application.properties
3.   server.port: APIPORT (any port which is accessible to other machines - default 9000)
4.   elasticsearch.url.default: "ESURL"<Elastic Search URL with port in double quotes”
5.   beacon.host=CURRENTMACHINEIP or localhost
6.   Navigate to /api in command prompt
7.   Run java -jar CXOptimizeAPI-version.jar -Xms256M -Xmx1024M (This will start API tier)
8.   Test if API is up by http://localhost:port/health

### Step 3 - Install Kibana:
1.   Copy kibana-5.5.2-windows-x86 folder to machine where you want to deploy Kibana tier(Can be deployed as same machine as ES also)
2.   Navigate to /kibana-5.5.2-windows-x86/config and update kibana.yml
3.   server.port=KIBANAPORT (any port which is accessible to other machines - default 5601)
4.   server.basePath="/cxoptimize"
5.   elasticsearch.url:ESURL
6.   Navigate to /kibana-5.5.2-windows-x86/bin in command prompt
7.   Run kibana.bat (This will start Kibana tier) or ./kibana for linux

### Step 4 - Install CXOptimize UI:
1.   Copy UI folder to machine where you want to deploy UI tier
2.   Navigate to /UI and update application.properties
3.   server.port: UIPORT (any port which is accessible to other machines - default 9050)
4.   Navigate to /api in command prompt
5.   Run java -jar CXOptimizeUI-<version.jar -Xms256M -Xmx512M (This will start UI tier)
6.   Test if UI is up by http://localhost:port/cxoptimize/login

### Step 5 - Install Caddy as Proxy:
1.   Copy caddy folder to machine where you want to deploy UI tier
2.   Navigate to /caddy and update caddyfile
3.   Just Update MACHINEIP & PORT for each component
4.   Navigate to /caddy in command prompt
5.   Run caddy.exe or ./caddy Caddyfile (This will start Caddy Web Server Proxy from which all the components can be accessed and this ties all components together)
6.   Test if UI is up by http://caddyurl:caddyport/cxoptimize/login (cxopadmin/qwertyuiop)

### Step 6 - Install Optional Components (freegeoip & Grafana):
1.	(Optional) Manually naviage to freegeoip folder and excute freegeoip.exe.This is used to get geo location data for a given IP.It uses open source version of the IP database from MaxMind.More details will be found [here](https://github.com/fiorix/freegeoip)
2.	(Optional) Grafana - alternative to Kibana Dashboard incase if you need rich UI and Dashboard experience.Setup details can be found [here](https://github.com/grafana/grafana)

