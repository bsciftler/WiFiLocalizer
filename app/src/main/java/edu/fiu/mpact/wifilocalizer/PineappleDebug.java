package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.UnsupportedEncodingException;

public class PineappleDebug extends Activity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pineapple_debug);
    }

    public void getData(View _) {
        final String url = Utils.Constants.PINEAPPLE_URL + ":" + Utils.Constants
                .PINEAPPLE_SCRAPER_PORT;
        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    final String response = new String(responseBody, "UTF8");
                    Utils.PineappleResponse data = new Gson().fromJson(response, Utils
                            .PineappleResponse.class);

                    // TODO
                } catch (UnsupportedEncodingException e) {
                    Log.e("PineDebug", "response had a weird encoding; headers = " + headers);
                    return;
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable
                    throwable) {
                Toast.makeText(getApplicationContext(), "Get failed with HTTP Code " +
                        statusCode, Toast.LENGTH_LONG).show();
            }
        });
    }
}
