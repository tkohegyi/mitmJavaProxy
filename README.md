MITM Java Proxy
============================
This Men-In-The-Middle Java Proxy is a derivative work that was originated from [BrowserMob Proxy](https://github.com/lightbody/browsermob-proxy), then tailored first for the old [**Wilma**](https://github.com/epam/Wilma) versions, 
then reviewed and reworked again for the next generation of **Wilma** which is a proxy/stub/service virtualization tool and relies on this **MitmJavaProxy**. 

**Right now everything seems to be working so can be used**.

Also please be aware that the main aim of creating this MITM Java Proxy is to support the proxy need of [Wilma](https://github.com/epam/Wilma).
Therefore none of the original browsermob-proxy features should be expected as working, on the other side, other features which was not in the original browsermob-proxy become available. Also, this version is prepered for Java 11 (and above) versions, and supports TSL.
In sort, what you can do with it:
- proxying the HTTP/HTTPS messages and 
- by intercepting both requests and responses
- do whatever you want to do with the intercepted messages.
It is possible to alter both the request before it hits the server (what is more - you can alter the target URL too) and the response before it arrives to the client.

Warning!
========
This proxy is a Men-In-The-Middle type of proxy that is able to capture **ANY** http traffic, everything that is transferred between the client and the server, even if it is encrypted. Use it carefully and only when you know what you do, and what you do is allowed/legal, and on environment where you are allowed/authorized to use the Proxy.

Embedded Mode
-------------
If you're using Java, the easiest way to get started is to embed the project directly. The jar is available in **maven central repository**.

Related gradle file should look like similar to the below:

```
repositories {
    mavenCentral()
}
    
dependencies {
    implementation('website.magyar:mitm-java-proxy:2.5.26.113')
}    
```

Release
------------
Latest announced release is available [here](https://github.com/tkohegyi/mitmJavaProxy/releases). 

CI Build
-----------------
Github CI Build status: [![CI](https://github.com/tkohegyi/mitmJavaProxy/actions/workflows/main.yml/badge.svg)](https://github.com/tkohegyi/mitmJavaProxy/actions/workflows/main.yml)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/dashboard?id=tkohegyi_mitmJavaProxy)

Before building the proxy locally, create a `gradle.properties` file in root folder with the following content:
```
org.gradle.jvmargs=-Xmx2048m
```
Then use this command to start the build:
```
./gradlew clean build
```

To publish the library on local machine (in local maven repository):
```
./gradlew clean build publishToMavenLocal
```

To publish the library for public use (into public maven repository), you need the necessary right there and use this command:
```
./gradlew clean build publish -PbuildNumber=x
```
Please note that without specifying the build number, the build will be a SNAPSHOT build.

Detailed User's Guide
----------------
See detailed information of its usage at [wiki pages](https://github.com/tkohegyi/mitmJavaProxy/wiki).

HTTP Request Manipulation
-------------------
Just add a Request Interceptor to the proxy server, and manipulate the request as you wish. See details [here](https://github.com/tkohegyi/mitmJavaProxy/wiki/4.-How-to-manipulate-requests).

HTTP Response Manipulation
-------------------
Just add a Response Interceptor to the proxy server, and you will get access to the responses.

The key to manipulate responses is the **Response Volatility** attribute. 
If a response is volatile, the proxy (or you) must work with the response a lot (call interceptor, extract, manipulate the response, compress, then release response towards the Client). This takes time.
If a response is not volatile, then the proxy don't need to do such things. This of course a much faster method, so in case you don't need to manipulate the response, just leave responses as not volatile.
Response volatility can be set in general via static method: `ProxyServer.setResponseVolatility(boolean)`
Or can be set per request-response pair by using the Request Interceptors. See more details [here](https://github.com/tkohegyi/mitmJavaProxy/wiki/5.-How-to-manipulate-responses).

SSL Support
-----------
While the proxy supports SSL, it requires that a Certificate Authority be installed in to the browser / at client from where you call a server via the proxy.
This allows the client to trust all the SSL traffic coming from the proxy, which will be proxied using a classic man-in-the-middle technique. 

**IT IS CRITICAL THAT YOU DO NOT INSTALL THIS CERTIFICATE AUTHORITY ON A CLIENT/BROWSER THAT IS USED FOR ANYTHING OTHER THAN TESTING.**

It is recommended to use `-Djdk.tls.namedGroups="secp256r1, secp384r1, ffdhe2048, ffdhe3072"` java arguments to address some issues still existing in some JDK implementations.

Please note that in V2.5.x the certificate used by this proxy is upgraded.