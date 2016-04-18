package edu.fiu.mpact.wifilocalizer;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.google.common.collect.ImmutableList;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private Database mDb;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);

        mDb = new Database(this);

        // Initialize to EC maps if there are no maps
        if (noMaps()) {
            loadDefaultMaps();

            new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setStyle(R.style.CustomShowcaseTheme2)
                .setContentTitle("Welcome!")
                .setContentText(R.string.first_welcome_message)
                .hideOnTouchOutside()
                .build();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_info:
            Utils.buildDialog(this, R.string.info_string).show();
            return true;
        case R.id.action_sync_database:
            syncDatabase();
            return true;
        case R.id.action_get_metadata:
            getMetaData();
            return true;
        case R.id.action_add_map:
            startActivity(new Intent(this, AddMapActivity.class));
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

        Unirest.post(Utils.Constants.METADATA_URL)
            .header("accept", "application/json")
            .asJsonAsync(new Callback<JsonNode>() {
                public void failed(UnirestException e) {
                    mDialog.hide();
                    Toast.makeText(getApplicationContext(), "Couldn't update metadata", Toast.LENGTH_SHORT).show();
                    Log.e("metadataUpdate", "couldn't update; http code = " + e.getMessage());
                }

                public void completed(HttpResponse<JsonNode> response) {
                    mDialog.hide();
                    updateSQLite(response.getBody().getArray());
                }

                public void cancelled() {}
            });
    }

    /**
     * Update metadata database with new mac addresses.
     *
     * @param arr a valid JSON array
     */
    public void updateSQLite(JSONArray arr) {
        final ArrayList<ContentValues> toInsert = new ArrayList<>();

        try {
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
        final String nonUploadedReadings = mDb.readingsCursorToJson(cursor);
        cursor.close();

        mDialog.setMessage(getString(R.string.sync_in_progress));
        mDialog.show();

        Unirest.post(Utils.Constants.INSERT_URL)
            .header("accept", "application/json")
            .field("readingsJSON", nonUploadedReadings)
            .asJsonAsync(new Callback<JsonNode>() {
                public void failed(UnirestException e) {
                    mDialog.hide();

                    Toast.makeText(getApplicationContext(), "Couldn't sync databases", Toast.LENGTH_SHORT).show();
                    Log.e("syncUpdate", "couldn't update; http code = " + e.getMessage());
                }

                public void completed(HttpResponse<JsonNode> response) {
                    JsonNode body = response.getBody();
                    body.getArray();

                    try {
                        JSONArray arr = body.getArray();
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

                public void cancelled() {}
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
            final ContentValues values = Utils.createNewMapContentValues(this, floor.first, floor.second);
            getContentResolver().insert(DataProvider.MAPS_URI, values);
        }
    }
}
