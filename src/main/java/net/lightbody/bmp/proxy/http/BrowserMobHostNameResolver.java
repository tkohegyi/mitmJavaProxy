package net.lightbody.bmp.proxy.http;

import org.apache.http.conn.scheme.HostNameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserMobHostNameResolver implements HostNameResolver {
    public static ThreadLocal<Boolean> fakeSlow = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    protected final Logger logger = LoggerFactory.getLogger(BrowserMobHostNameResolver.class);
    private Map<String, String> remappings = new ConcurrentHashMap<String, String>();
    private Map<String, List<String>> reverseMapping = new ConcurrentHashMap<String, List<String>>();

    private Cache cache;
    private Resolver resolver;

    public BrowserMobHostNameResolver(Cache cache) {
        this.cache = cache;
        resolver = new ExtendedResolver();
        remappings.put("localhost", "127.0.0.1");
    }

    @Override
    public InetAddress resolve(String hostname) throws IOException {
        String remapping = remappings.get(hostname);
        if (remapping != null) {
            hostname = remapping;
        }

        try {
            return Address.getByAddress(hostname);
        } catch (UnknownHostException e) {
            // that's fine, this just means it's not an IP address and we gotta look it up, which is common
        }

        boolean isCached = this.isCached(hostname);

        Lookup lookup = new Lookup(Name.fromString(hostname), Type.A);
        lookup.setCache(cache);
        lookup.setResolver(resolver);

        Date start = new Date();
        Record[] records = lookup.run();
        if (fakeSlow.get()) {
            fakeSlow.set(false);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Date end = new Date();

        if (records == null || records.length == 0) {
            throw new UnknownHostException(hostname);
        }

        // assembly the addr object
        ARecord a = (ARecord) records[0];
        InetAddress addr = InetAddress.getByAddress(hostname, a.getAddress().getAddress());

        if (!isCached) {
            // TODO: Associate the the host name with the connection. We do this because when using persistent
            // connections there won't be a lookup on the 2nd, 3rd, etc requests, and as such we wouldn't be able to
            // know what IP address we were requesting.
            RequestInfo.get().dns(start, end, addr.getHostAddress());
        } else {
            // if it is a cached hit, we just record zero since we don't want
            // to skew the data with method call timings (specially under load)
            RequestInfo.get().dns(end, end, addr.getHostAddress());
        }

        return addr;
    }

    public void remap(String source, String target) {
        remappings.put(source, target);
        List<String> list = reverseMapping.get(target);
        if (list == null) {
            list = new ArrayList<String>();
        }
        list.add(source);
        reverseMapping.put(target, list);
    }

    public String remapping(String host) {
        return remappings.get(host);
    }

    public List<String> original(String host) {
        return reverseMapping.get(host);
    }

    public void clearCache() {
        this.cache.clearCache();
    }

    public void setCacheTimeout(int timeout) {
        cache.setMaxCache(timeout);
    }

    public boolean isCached(String hostname) throws TextParseException {
        return cache.lookupRecords(Name.fromString(hostname), Type.ANY, 3).isSuccessful();
    }

    public void addHostToCacheAs(String hostname, String ipAddress) throws UnknownHostException, TextParseException {
        //add an ending period assuming the hostname is truly an absolute hostname
        Name host = new Name(hostname + ".");
        //putting in a good long TTL, and using an A record, but AAAA might be desired as well for IPv6
        Record aRec = new ARecord(host, Type.A, 9999999, getInetAddressFromString(ipAddress));
        Lookup.getDefaultCache(Type.A).addRecord(aRec, Credibility.NORMAL, this);
    }

    private InetAddress getInetAddressFromString(String ip) throws UnknownHostException {
        //Assume we are using IPv4
        byte[] bytes = new byte[4];
        String[] ipParts = ip.split("\\.");
        InetAddress addr = null;
        //if we only have one part, it must actually be a hostname, rather than a real IP
        if (ipParts.length <= 1) {
            addr = InetAddress.getByName(ip);
        } else {
            for (int i = 0; i < ipParts.length; i++) {
                bytes[i] = Byte.parseByte(ipParts[i]);
            }
            addr = InetAddress.getByAddress(bytes);
        }
        return addr;
    }
}
