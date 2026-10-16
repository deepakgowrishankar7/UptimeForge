package com.pulsewatch.dto;

import com.pulsewatch.entity.MonitorType;

public class TestCheckRequest {
    private String url;
    private MonitorType type = MonitorType.HTTP;
    private String httpMethod = "GET";
    private int timeoutMs = 10000;
    private String expectedStatusCodes = "200,201,204";
    private String expectedText;
    private String headers;
    private String requestBody;

    public TestCheckRequest() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public MonitorType getType() { return type; }
    public void setType(MonitorType type) { this.type = type; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public String getExpectedStatusCodes() { return expectedStatusCodes; }
    public void setExpectedStatusCodes(String expectedStatusCodes) { this.expectedStatusCodes = expectedStatusCodes; }

    public String getExpectedText() { return expectedText; }
    public void setExpectedText(String expectedText) { this.expectedText = expectedText; }

    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
}
