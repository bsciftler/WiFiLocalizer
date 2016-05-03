package io.swagger.client;

import org.apache.http.client.methods.*;

public class HttpPatchAndroid extends HttpPost {
    public static final String METHOD_PATCH = "PATCH";

    public HttpPatchAndroid(final String url) {
        super(url);
    }

    @Override
    public String getMethod() {
        return METHOD_PATCH;
    }
}
