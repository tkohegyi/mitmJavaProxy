MitmJavaProxy - For Wilma
============================

This Men-In-The-Middle Java Proxy is a derivative work that was originated from BrowserMob Proxy, then tailored for old Wilma versions, 
then upgraded in order to be prepared for the next generation of Wilma. 

WARNING!!! Under Construction !!!
---------------------------
**Right now HTTP proxy mode is working, HTTPS is still under construction, and works only partially.**

Also please be aware that the main aim of creating this MITM Java proxy is to support the proxy need of Wilma.
Therefore none of the original browsermob-proxy features should be expected as working, 
because the main purpose is just proxying the messages and by intercepting them do what Wilma wants to do with the messages.


Embedded Mode
-------------

If you're using Java, the easiest way to get started is to embed the project directly.
    
    <dependency>
        <groupId>com.epam.mitm</groupId>
        <artifactId>mitmJavaProxy</artifactId>
        <version><versionToBeUsed></version>
    </dependency>

The latest release is:

[ ![Download](https://api.bintray.com/packages/epam/wilma/mitmJavaProxy/images/download.svg?version=V0.10-initial-beta) ](https://bintray.com/epam/wilma/mitmJavaProxy/V0.10-initial-beta/link)

HTTP Request Manipulation
-------------------

TODO

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

While the proxy supports SSL, it requires that a Certificate Authority be installed in to the browser. 
This allows the browser to trust all the SSL traffic coming from the proxy, which will be proxied using a classic man-in-the-middle technique. IT IS CRITICAL THAT YOU NOT INSTALL THIS CERTIFICATE AUTHORITY ON A BROWSER THAT IS USED FOR ANYTHING OTHER THAN TESTING.
