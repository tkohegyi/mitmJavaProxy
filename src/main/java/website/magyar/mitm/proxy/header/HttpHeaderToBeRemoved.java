package website.magyar.mitm.proxy.header;

import org.apache.http.Header;

/**
 * This class is to be used when a header should be removed.
 *
 * @author Tamas_Kohegyi
 */
public class HttpHeaderToBeRemoved extends HttpHeaderChange {

    public HttpHeaderToBeRemoved(final Header header) {
        super(header);
    }
}
