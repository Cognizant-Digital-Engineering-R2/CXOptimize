var clientConfig = {
  "version" : 2.3,
  "onLoaded" : 0,
  "clientName": "chrome",
  "projectName": "cxop",
  "scenarioName": "test",
  "beaconUrl": "http://localhost:9000/",
  "screenCapture": false,
  "downloadFolder": "file:///downloadpath/"
};//
/************** CAREFUL WHILE MODIFYING THIS FILE **************
"http:/10.124.59.192:8080/api/",
**** onLoaded variable should not be touched.. Let it be 0.
**** downloadFolder should be the download directory set in your chrome browser.
			format like "file:///" +<chrome download directory> "
			3 slashes after file is mandatory..
			Should end with 1 slash.
***************/