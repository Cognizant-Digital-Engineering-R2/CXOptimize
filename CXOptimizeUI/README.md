## CXOptimize UI

CXOptimize UI is java executable which provides front-end of this solution.This is developed using springboot and designed to run as as microservice.

## Build

``` gradlew clean build```

IDE Used : IntelliJ IDEA Community Edition 2017.2.6
Gradle :
```
Gradle:       3.1
Groovy:       2.4.7
Ant:          Apache Ant(TM) version 1.9.6 compiled on June 29 2015
JVM:          1.8.0_73 (Oracle Corporation 25.73-b02)
OS:           Windows 7 6.1 amd64
```


## Documentation

### API Properties

Following are important properties which can be modified by the user based on the requirement.
```
//Port to start API
server.port=9050
management.address=127.0.0.1
//Logging
logging.level.root=INFO
logging.level.org.springframework.web=ERROR
logging.level.org.hibernate=ERROR
logging.file=logs/UI.log
//Configure URL for the API to authenticate user
login.url=http://localhost:9001/
//Workaround to get AuthToken for the UI API calls - dont modify it
auth.request={"username":"cxopuser","password":"zxcvbnm"}
//Setting Server Path for accessing via reverse proxy
server.contextPath=/cxoptimize
//Setting to control cache & compression
#server.session.cookie.max-age=
server.compression.enabled=true
server.compression.mime-types=application/xml,text/html,text/xml,text/plain,text/css,application/octet-stream,application/javascript
//Cookie settings for security standards when executed with https
#server.session.cookie.path=/
#server.session.cookie.secure=true
#server.session.cookie.http-only=true

```



### Documentation


Installation details found here

Userguide is found here

## Development / Contribution

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting your pull requests. It also has details on how to setup your development environment.

## Code of Conduct

To provide clarity on what is expected of our members, Cognizant CXOptimize has adopted the code of conduct defined by the Contributor Covenant. This document is used across many open source communities and we think it articulates our values well. For more, see the [Code of Conduct](CODE_OF_CONDUCT.md).

## Contact

If you have any queries on Cognizant CXOptimize, please post your questions on [Cognizant CXOptimize Google Group](https://groups.google.com/forum/#!forum/cognizant-cxoptimize).

To ask specific questions on project development, to discuss future improvements or for any other technical detail, please join the [Cognizant CXOptimize chat on Gitter](https://gitter.im/Cognizant-CXOptimize).

## License

Cognizant CXOptimize is licensed under [Apache License, Version 2.0](LICENSE)

