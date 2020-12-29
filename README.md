MitmJavaProxy
============================
This Men-In-The-Middle Java Proxy is a derivative work that was originated from [**BrowserMob Proxy**](https://github.com/lightbody/browsermob-proxy), then tailored for old [*Wilma*](https://github.com/epam/Wilma) versions, 
then upgraded in order to be prepared for the next generation of *Wilma*. 

**Right now everything seems to be working so can be used in experimental mode**, but still changes need to be expected before the first real release.

Also please be aware that the main aim of creating this MITM Java proxy is to support the proxy need of [Wilma](https://github.com/epam/Wilma).
Therefore none of the original browsermob-proxy features should be expected as working, on the other side, other features which was not in the original browsermob-proxy become available. Also, this version is prepered for Java >8 versions, and supports TSL.
In sort, what you can do with it:
- proxying the HTTP/HTTPS messages and 
- by intercepting both requests and responses
- do whatever you want to do with the intercepted messages.
It is possible to alter both the request before it hits the server (what is more - you can alter the target URL too) and the response before it arrives to the client.

Embedded Mode
-------------
If you're using Java, the easiest way to get started is to embed the project directly.
    
    <dependency>
	    <groupId>org.rockhill.mitm</groupId>
	    <artifactId>mitmJavaProxy</artifactId>
	    <version>2.0.17.57</version>
	    <type>pom</type>
    </dependency>

Release
------------
Latest announced release is available here:
[ ![Download](https://api.bintray.com/packages/tkohegyi2/maven/mitmJavaProxy/images/download.svg?version=V2.0.17.57) ](https://bintray.com/tkohegyi2/maven/mitmJavaProxy/V2.0.17.57/link)

Travis CI Build Status (applicable for latest build from source): [![Build Status](https://travis-ci.com/tkohegyi/mitmJavaProxy.svg?branch=master)](https://travis-ci.com/tkohegyi/mitmJavaProxy)

HTTP Request Manipulation
-------------------
Just add a request interceptor to the proxy server, and manipulate the request as you wish...

HTTP Response Manipulation
-------------------
The key is the Response Volatility. 
If volatile, proxy must extract, call interceptor, compress, then release response towards Client. This is SLOW.
If not volatile, proxy may release the response towards the Client, meanwhile extracting the response and notify interceptors.
So if not volatile, maybe the response arrives to Client before the interceptor actually is called. This ensures the fastest method.
Response volatility can be set in general via static method: ProxyServer.SetResponseVolatility
Or can be set per message via ...

SSL Support
-----------
While the proxy supports SSL, it requires that a Certificate Authority be installed in to the browser - or at client from where you call a server via the proxy.
This allows the client to trust all the SSL traffic coming from the proxy, which will be proxied using a classic man-in-the-middle technique. 
IT IS CRITICAL THAT YOU NOT INSTALL THIS CERTIFICATE AUTHORITY ON A CLIENT/BROWSER THAT IS USED FOR ANYTHING OTHER THAN TESTING.
