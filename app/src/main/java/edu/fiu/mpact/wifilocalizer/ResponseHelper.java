package edu.fiu.mpact.wifilocalizer;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;


public class ResponseHelper extends AsyncHttpResponseHandler {
    public void onSuccess(String response) {
        throw new UnsupportedOperationException("Override one onSuccess() method, dummy.");
    }

    public void onSuccess(int statusCode, String response) {
        onSuccess(response);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        onSuccess(statusCode, new String(responseBody));
    }

    public void onFailure(int statusCode, String response) {
        throw new UnsupportedOperationException("Override one onFailure() method, please.");
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        onFailure(statusCode, new String(responseBody));
    }
}
