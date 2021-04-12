package website.magyar.mitm.proxy.header;

import org.apache.http.Header;

import java.io.Serializable;

/**
 * This class is for handling Http header changes (add, change, remove).
 *
 * @author Tamas_Kohegyi
 */
public class HttpHeaderChange implements Serializable {
    private final Header header;
    public HttpHeaderChange(final Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }
}
