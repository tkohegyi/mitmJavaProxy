Proxy - FOR WILMA
============================

This Proxy is a derivative work that was originated from BrowserMob Proxy, then tailored for old Wilma versions, then now we try to upgarde it to be used with new Wilma version.

WARNING!!! Major Changes!!!
---------------------------
**The main aim to create this proxy is to support the proxy need of Wilma.**

Therefore none of the original browsermob-proxy features should be expected as working, 
because the main purpose is just proxying the messages and by intercepting them do what Wilma wants to do with the messages.

Embedded Mode
-------------

If you're using Java, the easiest way to get started is to embed the project directly.

    
    <dependency>
        <groupId>com.epam.wilma</groupId>
        <artifactId>proxy</artifactId>
        <version>0.1.0-alpha-wilma-2.0.DEV</version>
    </dependency>


HTTP Request Manipulation
-------------------

TODO

SSL Support
-----------

While the proxy supports SSL, it requires that a Certificate Authority be installed in to the browser. 
This allows the browser to trust all the SSL traffic coming from the proxy, which will be proxied using a classic man-in-the-middle technique. IT IS CRITICAL THAT YOU NOT INSTALL THIS CERTIFICATE AUTHORITY ON A BROWSER THAT IS USED FOR ANYTHING OTHER THAN TESTING.
