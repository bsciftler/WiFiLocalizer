package io.swagger.client;

import org.apache.http.client.methods.*;

public class HttpPatchPineapple extends HttpPost {
    public static final String METHOD_PATCH = "PATCH";

    public HttpPatchPineapple(final String url) {
        super(url);
    }

    @Override
    public String getMethod() {
        return METHOD_PATCH;
    }
}
