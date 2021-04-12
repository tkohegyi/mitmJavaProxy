package website.magyar.mitm.proxy.header;

import org.apache.http.Header;

/**
 * This class is to be used when a header should be added.
 *
 * @author Tamas_Kohegyi
 */
public class HttpHeaderToBeAdded extends HttpHeaderChange {

    /**
     * Header to be added.
     *
     * @param headerToAdd is the new Header to be added.
     */
    public HttpHeaderToBeAdded(final Header headerToAdd) {
        super(headerToAdd);
    }

}
