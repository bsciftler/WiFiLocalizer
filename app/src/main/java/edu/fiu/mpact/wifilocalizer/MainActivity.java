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

import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends Activity {
    private Database mDb;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create textless dialog object
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);

        mDb = new Database(this);

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
            syncDatabase();
            return true;
        case R.id.action_getMetaData:
            getMetaData();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Attempt to retrieve metadata from internet source. Will update db if successfully retrieved.
     */
    private void getMetaData() {
        mDialog.setMessage(getString(R.string.retrieve_in_progress));
        mDialog.show();

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(Utils.Constants.METADATA_URL, new RequestParams(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] response) {
                mDialog.hide();
                updateSQLite(new String(response));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                mDialog.hide();
                Toast.makeText(getApplicationContext(), "Couldn't update metadata", Toast.LENGTH_SHORT).show();
                Log.e("metadataUpdate", "couldn't update; http code = " + statusCode);
            }
        });
    }

    /**
     * Update metadata database with new mac addresses.
     *
     * @param response a valid JSON array
     */
    public void updateSQLite(String response) {
        final ArrayList<ContentValues> toInsert = new ArrayList<>();

        try {
            JSONArray arr = new JSONArray(response);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = (JSONObject) arr.get(i);

                ContentValues cv = new ContentValues();
                cv.put("mac", obj.get("mac").toString());

                toInsert.add(cv);
            }

            if (toInsert.isEmpty()) return;
            getContentResolver().delete(DataProvider.META_URI, null, null);
            getContentResolver().bulkInsert(DataProvider.META_URI,
                    toInsert.toArray(new ContentValues[arr.length()]));
        } catch (JSONException e) {
            Log.e("metadataUpdate", "malformed JSON");
        }
    }

    /**
     * Uploads Readings table to remote database and then waits for a response. Then mark
     * the readings returned in the response as uploaded.
     */
    private void syncDatabase() {
        final Cursor cursor = mDb.getNonUploadedReadings();
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Already synced!", Toast.LENGTH_LONG).show();
            cursor.close();
            return;
        }

        mDialog.setMessage(getString(R.string.sync_in_progress));
        mDialog.show();

        final RequestParams params = new RequestParams();
        params.put("readingsJSON", mDb.readingsCursorToJson(cursor));
        cursor.close();

        final AsyncHttpClient client = new AsyncHttpClient();
        client.post(Utils.Constants.INSERT_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] bytes) {
                mDialog.hide();

                try {
                    JSONArray arr = new JSONArray(new String(bytes));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = (JSONObject) arr.get(i);
                        mDb.updateSyncStatus(obj.get("id").toString(), obj.get("status").toString());
                    }

                    Toast.makeText(getApplicationContext(), "Sync complete!", Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't sync databases", Toast.LENGTH_SHORT).show();
                    Log.e("syncUpdate", "couldn't update");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                mDialog.hide();

                Toast.makeText(getApplicationContext(), "Couldn't sync databases", Toast.LENGTH_SHORT).show();
                Log.e("syncUpdate", "couldn't update; http code = " + statusCode);
            }
        });
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
