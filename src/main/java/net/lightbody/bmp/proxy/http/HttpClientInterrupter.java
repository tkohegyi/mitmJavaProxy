package net.lightbody.bmp.proxy.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class HttpClientInterrupter {
    protected static final Logger logger = LoggerFactory.getLogger(HttpClientInterrupter.class);
    private static Set<BrowserMobHttpClient> clients = new CopyOnWriteArraySet<BrowserMobHttpClient>();

    static {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    for (BrowserMobHttpClient client : clients) {
                        try {
                            client.checkTimeout();
                        } catch (Exception e) {
                            logger.error("Unexpected problem while checking timeout on a client", e);
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // this is OK
                    }
                }
            }
        }, "HttpClientInterrupter Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public static void watch(BrowserMobHttpClient client) {
        clients.add(client);
    }

    public static void release(BrowserMobHttpClient client) {
        clients.remove(client);
    }
}
