package edu.fiu.mpact.wifilocalizer;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;


public class AsyncHelper {
    private String url;
    private RequestParams params;
    private AsyncHttpResponseHandler handler;

    private AsyncHttpClient client;

    public AsyncHelper(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        this.client = new AsyncHttpClient();

        this.url = url;
        this.params = params;
        this.handler = handler;
    }

    public RequestHandle post() {
        return this.client.post(this.url, this.params, this.handler);
    }

    public static class AsyncHelperBuilder {
        private String nestedUrl = null;
        private RequestParams nestedParams = new RequestParams();
        private AsyncHttpResponseHandler nestedHandler = null;

        public AsyncHelperBuilder url(String url) {
            nestedUrl = url;
            return this;
        }

        public AsyncHelperBuilder param(String key, String value)    {
            this.nestedParams.put(key, value);
            return this;
        }

        public AsyncHelperBuilder params(RequestParams params) {
            nestedParams = params;
            return this;
        }

        public AsyncHelperBuilder handler(AsyncHttpResponseHandler handler) {
            nestedHandler = handler;
            return this;
        }

        public AsyncHelper create() {
            if (nestedUrl == null || nestedHandler == null) {
                throw new UnsupportedOperationException("Need to call url() and params() before create()");
            }

            return new AsyncHelper(nestedUrl, nestedParams, nestedHandler);
        }
    }
}
