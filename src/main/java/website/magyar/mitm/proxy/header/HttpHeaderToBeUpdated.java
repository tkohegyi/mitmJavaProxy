package website.magyar.mitm.proxy.header;

import org.apache.http.Header;

/**
 * This class is to be used when an existing header should be updated.
 *
 * @author Tamas_Kohegyi
 */
public class HttpHeaderToBeUpdated extends HttpHeaderChange {
    private String newValue;

    /**
     * Update a header.
     *
     * @param headerToUpdate is the existing header that must be updated
     * @param newValue is the new value of the header.
     */
    public HttpHeaderToBeUpdated(final Header headerToUpdate, final String newValue) {
        super(headerToUpdate);
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

}
