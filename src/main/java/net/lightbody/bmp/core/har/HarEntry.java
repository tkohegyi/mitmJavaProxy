package net.lightbody.bmp.core.har;

import java.util.Date;

public class HarEntry {
    private String pageref;
    private Date startedDateTime;
    private long time;
    private HarRequest request;
    private HarResponse response;
    private HarTimings timings;
    private String serverIPAddress;
    private String messageId;

    public HarEntry(String pageref, String messageId) {
        this.pageref = pageref;
        this.startedDateTime = new Date();
        this.messageId = messageId;
    }

    public String getPageref() {
        return pageref;
    }

    public void setPageref(String pageref) {
        this.pageref = pageref;
    }

    public Date getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public HarRequest getRequest() {
        return request;
    }

    public void setRequest(HarRequest request) {
        this.request = request;
    }

    public HarResponse getResponse() {
        return response;
    }

    public void setResponse(HarResponse response) {
        this.response = response;
    }

    public HarTimings getTimings() {
        return timings;
    }

    public void setTimings(HarTimings timings) {
        this.timings = timings;
    }

    public String getServerIPAddress() {
        return serverIPAddress;
    }

    public void setServerIPAddress(String serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String wilmaEntryId) {
        this.messageId = wilmaEntryId;
    }
}
