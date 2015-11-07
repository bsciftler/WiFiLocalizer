package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends Activity {
    private Database mController;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create textless dialog object
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);

        // Show welcome message if first run
        Utils.createHintIfNeeded(this, Utils.Constants.PREF_MAIN_HINT, R.string.first_welcome_message);

        // Initialize to EC maps if there are no maps
        if (noMaps()) loadDefaultMaps();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_dbm:
            final Intent dbmIntent = new Intent(this, DatabaseManagerActivity.class);
            startActivity(dbmIntent);
            return true;
        case R.id.action_info:
            Utils.buildDialog(this, R.string.info_string).show();
            return true;
        case R.id.action_syncDB:
            syncSQLiteMySQLDB();
            return true;
        case R.id.action_getMetaData:
            getMetaData();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void getMetaData() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        mDialog.setMessage(getString(R.string.retrieve_in_progress));
        mDialog.show();

        client.post("http://eic15.eng.fiu.edu:80/wifiloc/getmeta.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] response) {
                mDialog.hide();
                updateSQLite(new String(response));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                mDialog.hide();
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! Check internet connection.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void updateSQLite(String response) {
        ArrayList<ContentValues> cache = new ArrayList<>();
        try {
            System.out.println(response);
            JSONArray arr = new JSONArray(response);
            System.out.println(arr.length());

            if (arr.length() != 0) {
                // Loop through each array element, get JSON object which has userid and username
                for (int i = 0; i < arr.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) arr.get(i);
                    //System.out.println(obj.get("mapx"));
                    //System.out.println(obj.get("mapy"));
                    System.out.println(obj.get("mac"));

                    ContentValues cv = new ContentValues();
                    // Add userID extracted from Object
                    //cv.put("mapx", Float.valueOf(obj.get("mapx").toString()));
                    cv.put("mac", obj.get("mac").toString());
                    // Add userName extracted from Object
                    //cv.put("mapy", Float.valueOf(obj.get("mapy").toString()));
                    // Insert User into SQLite DB
                    cache.add(cv);
                }
                if (cache.isEmpty()) return;
                // Add readings
                getContentResolver().delete(DataProvider.META_URI, null, null);
                getContentResolver().bulkInsert(DataProvider.META_URI, cache.toArray(new ContentValues[] {}));
            } else {
                getContentResolver().delete(DataProvider.META_URI, null, null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void syncSQLiteMySQLDB() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        mController = Database.getInstance(getApplicationContext());
        String jsondata = mController.composeJSONfromSQLite();
        if (!jsondata.isEmpty()) {
            if (mController.dbSyncCount() != 0) {
                mDialog.setMessage(getString(R.string.sync_in_progress));
                mDialog.show();

                params.put("readingsJSON", jsondata);
                client.post("http://eic15.eng.fiu.edu:80/wifiloc/insertreading.php", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int i, Header[] headers, byte[] bytes) {
                        onSuccess(new String(bytes));
                    }

                    @Override
                    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                        onFailure(i, throwable, String.valueOf(bytes));
                    }

                    public void onSuccess(String response) {
                        mDialog.hide();

                        try {
                            JSONArray arr = new JSONArray(response);
                            Log.d("onSuccess", "" + arr.length());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = (JSONObject) arr.get(i);
                                mController.updateSyncStatus(obj.get("id").toString(), obj.get("status").toString());
                            }
                            Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error Occured [Server's " +
                                    "JSON" + " response might be invalid]!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @SuppressWarnings("unused")
                    public void onFailure(int statusCode, Throwable error, String content) {
                        mDialog.hide();
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not " + "found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), "Something went wrong at " + "server end", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unexpected Error occcured! " +
                                    "[Most common Error: Device might not be connected to " +
                                    "Internet]", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in " + "Sync!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User " + "name to perform Sync action", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check the number of maps in the Maps database.
     *
     * @return true if zero maps or query was null, false otherwise
     */
    private boolean noMaps() {
        final Cursor cursor = getContentResolver().query(DataProvider.MAPS_URI,
                new String[] {Database.Maps.ID}, null, null, null);
        if (cursor == null) return true;

        final boolean ret = cursor.getCount() == 0;
        cursor.close();
        return ret;
    }

    /**
     * Load locally available maps of the FIU engineering campus.
     */
    private void loadDefaultMaps() {
        final ImmutableList<Pair<String, Integer>> floors = ImmutableList.of(
                new Pair<>("Engineering 1st Floor", R.drawable.ec_1),
                new Pair<>("Engineering 2nd Floor", R.drawable.ec_2),
                new Pair<>("Engineering 3rd Floor", R.drawable.ec_3));

        for (Pair<String, Integer> floor : floors) {
            final ContentValues values = new ContentValues();
            values.put(Database.Maps.NAME, floor.first);
            values.put(Database.Maps.DATA, Utils.resourceToUri(this, floor.second).toString());
            values.put(Database.Maps.DATE_ADDED, System.currentTimeMillis());
            getContentResolver().insert(DataProvider.MAPS_URI, values);
        }
    }
}
