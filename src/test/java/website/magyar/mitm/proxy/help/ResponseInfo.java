package website.magyar.mitm.proxy.help;

import org.apache.http.Header;

/**
 *
 * @author Tamas_Kohegyi
 */
public class ResponseInfo {
    private final int statusCode;
    private final Header contentEncoding;
    private final String body;

    public ResponseInfo(int statusCode, String body, Header contentEncoding) {
        super();
        this.statusCode = statusCode;
        this.body = body;
        this.contentEncoding = contentEncoding;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + statusCode;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResponseInfo other = (ResponseInfo) obj;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        return statusCode == other.statusCode;
    }

    @Override
    public String toString() {
        return "ResponseInfo [statusCode=" + statusCode + ", body=" + body + "]";
    }

    public Header getContentEncoding() {
        return contentEncoding;
    }
}
