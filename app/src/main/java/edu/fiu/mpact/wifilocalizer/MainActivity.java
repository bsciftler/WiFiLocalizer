package edu.fiu.mpact.wifilocalizer;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.api.AccessPointApi;
import io.swagger.client.api.ReadingApi;
import io.swagger.client.model.AccessPoint;
import io.swagger.client.model.Reading;
import uk.co.senab.photoview.PhotoMarker;


public class MainActivity extends AppCompatActivity {
    private Database mDb;
    private ProgressDialog mDialog;
    private AccessPointApi mApApi;
    private ReadingApi mReadingApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
            .withMaterialShowcase()
            .setStyle(R.style.CustomShowcaseTheme2)
            .setContentTitle("Welcome!")
            .setContentText(R.string.first_welcome_message)
            .hideOnTouchOutside();
        Utils.showHelpOnFirstRun(this, Utils.Constants.PREF_MAIN_HINT, builder);

        // Initialize to EC maps if there are no maps
        if (noMaps()) loadDefaultMaps();

        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);

        mDb = new Database(this);

        mApApi = new AccessPointApi();
        mReadingApi = new ReadingApi();
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
        case R.id.action_options:
            startActivity(new Intent(this, SettingsActivity.class));
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

        new MetadataTask().execute();
    }

    class MetadataTask extends AsyncTask<Void, Void, List<AccessPoint>> {
        protected List<AccessPoint> doInBackground(Void... nothing) {
            try {
                return mApApi.accessPointsGet();
            } catch (ApiExceptionAndroid apiExceptionAndroid) {
                apiExceptionAndroid.printStackTrace();
                return null;
            }
        }

        /**
         * Update metadata database with new mac addresses.
         */
        protected void onPostExecute(@Nullable List<AccessPoint> data) {
            if (data == null) {
                Log.e("onPostExecute", "got null data");
                return;
            }

            final List<ContentValues> toInsert = new ArrayList<>();
            for (AccessPoint ap : data) {
                final ContentValues cv = new ContentValues();
                cv.put("mac", ap.getMacAddress());
            }

            if (toInsert.isEmpty()) return;
            getContentResolver().delete(DataProvider.META_URI, null, null);
            getContentResolver().bulkInsert(DataProvider.META_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
        }
    }

    /**
     * Uploads Readings table to remote database and then waits for a response. Then mark
     * the readings returned in the response as uploaded.
     */
    private void syncDatabase() {
        mDialog.setMessage(getString(R.string.sync_in_progress));
        mDialog.show();

        new ReadingsTask().execute();
    }

    class ReadingsTask extends AsyncTask<Void, Void, List<Integer>> {
        protected List<Integer> doInBackground(Void... nothing) {
            try {
                final Cursor cursor = mDb.getNonUploadedReadings();
                if (cursor == null) return null;
                final List<Reading> nonUploadedReadings = mDb.readingsCursorToJson(cursor);
                cursor.close();

                return mReadingApi.readingsPost(nonUploadedReadings);
            } catch (ApiExceptionAndroid apiExceptionAndroid) {
                apiExceptionAndroid.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(@Nullable List<Integer> data) {
            if (data == null) {
                Log.e("onPostExecute", "got null data");
                return;
            }

            if (data.get(1) > 0)
                Toast.makeText(getApplicationContext(), "Could not insert some readings", Toast.LENGTH_SHORT).show();

            mDb.updateSyncStatus();
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
            final ContentValues values = Utils.createNewMapContentValues(this, floor.first, floor.second);
            getContentResolver().insert(DataProvider.MAPS_URI, values);
        }
    }
}
