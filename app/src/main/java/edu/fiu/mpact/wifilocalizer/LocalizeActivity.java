package edu.fiu.mpact.wifilocalizer;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.amlcurran.showcaseview.ShowcaseView;

import java.util.List;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.api.ReadingApi;
import io.swagger.client.model.Reading;
import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;


public class LocalizeActivity extends AppCompatActivity {
    private long mMapId;
    private RelativeLayout mRelative;
    private PhotoViewAttacher mAttacher;

    private boolean mHavePlacedMarker = false;
    private boolean mScanRequested = false;
    private boolean mAutoLocalizeEnabled = false;
    private int mLocalizationMode = 1;
    private PrivateKey sk;
    private PublicKey pk;

    protected LocalizationData mCachedMapData;
    protected LocalizationData mFileData;
    protected LocalizationEuclideanDistance mAlgo = null;

    final ReadingApi mReadingApi = new ReadingApi();

    private WifiManager mWifiManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mScanRequested) return;
            if (mAutoLocalizeEnabled) mWifiManager.startScan();

            final List<ScanResult> results = mWifiManager.getScanResults();

            switch (mLocalizationMode) {
            case R.id.rbLocal:
                mAlgo.localize(results);
                break;
            case R.id.rbLocal2:
                mAlgo.localize2(results);
                break;
            case R.id.rbFile:
                mAlgo.fileLocalize(results);
                break;
            case R.id.rbFile2:
                mAlgo.fileLocalize2(results);
                break;
            case R.id.rbRemote:
                mAlgo.remoteLocalize(results, mMapId);
                break;
            case R.id.rbRemote2:
                mAlgo.remoteLocalize2(results, mMapId);
                break;
            case R.id.rbPrivate:
                mAlgo.remotePrivLocalize(results, mMapId, sk, pk);
                break;
            case R.id.rbPrivate2:
                mAlgo.remotePrivLocalize2(results, mMapId, sk, pk);
                break;
            default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localize);

        final ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
            .withMaterialShowcase()
            .setStyle(R.style.CustomShowcaseTheme2)
            .setContentTitle(R.string.hint_localize_title)
            .setContentText(R.string.hint_localize)
            .hideOnTouchOutside();
        Utils.showHelpOnFirstRun(this, Utils.Constants.PREF_LOCALIZE_HINT, builder);

        mAlgo = new LocalizationEuclideanDistance();
        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

        // Check mMapId is valid
        final Cursor cursor = getContentResolver().query(
            ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId), null, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, getResources().getText(R.string.toast_map_id_warning), Toast.LENGTH_LONG).show();
            if (cursor != null) cursor.close();
            finish();
            return;
        }

        // Get and display map image
        final Uri img = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        final String mapName = cursor.getString(cursor.getColumnIndex(Database.Maps.NAME));
        final ImageView mapImage = (ImageView) findViewById(R.id.image_map);
        if (mapImage != null) mapImage.setImageURI(img);
        mAttacher = new PhotoViewAttacher(mapImage, Utils.getImageSize(img, getApplicationContext()));
        cursor.close();

        // Try to change action bar title
        final ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(mapName);

        // Add existing map data to the view
        mCachedMapData = new LocalizationData(getContentResolver(), mMapId);
        mAttacher.addData(mCachedMapData.generateMarkers(getApplicationContext(), mRelative));
        // Load in data from alternative sources
        mFileData = new LocalizationData(getResources().openRawResource(R.raw.readings), mMapId);
        getPoints();
        mAlgo.setup(mCachedMapData, mFileData, LocalizeActivity.this);

        // Setup WifiManager
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        sk = new PrivateKey(1024);
        pk = new PublicKey();
        Paillier.keyGen(sk, pk);
    }

    // ***********************************************************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.localize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.localize_help:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_localize_help_title)
                .setMessage(R.string.dialog_localize_help_message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // ***********************************************************************

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        } catch (RuntimeException e) {
            // intentionally blank
        }
    }

    public void onClickedCheckBox(View view) {
        mLocalizationMode = view.getId();
    }

    public void onToggleClickedAuto(View view) {
        if (((ToggleButton) view).isChecked()) {
            mAutoLocalizeEnabled = true;
            localizeNow();
        } else {
            mAutoLocalizeEnabled = false;
        }
    }

    public void localizeNow() {
        final int numTrainingLocations = mCachedMapData.numLocations();

        if ((mLocalizationMode == 1 && numTrainingLocations < 3) || (mLocalizationMode == 7 && numTrainingLocations < 3)) {
            Toast.makeText(LocalizeActivity.this, getResources().getText(R.string
                .toast_not_enough_data), Toast.LENGTH_LONG).show();
            return;
        }

        mScanRequested = true;
        mWifiManager.startScan();
    }

    public void localizeNow(View unused) {
        localizeNow();
    }

    public void drawMarkers(float[] markerlocs) {
        float cx = markerlocs[0] * markerlocs[6] + markerlocs[2] * markerlocs[7] + markerlocs[4] * markerlocs[8];
        float cy = markerlocs[1] * markerlocs[6] + markerlocs[3] * markerlocs[7] + markerlocs[5] * markerlocs[8];
        final PhotoMarker mark = Utils.createNewMarker(getApplicationContext(), mRelative, cx, cy, R.drawable.o);
        final PhotoMarker bestguess = Utils.createNewMarker(getApplicationContext(), mRelative,
            markerlocs[0], markerlocs[1], R.drawable.red_x);

        bestguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popup = new PopupMenu(LocalizeActivity.this, bestguess.marker);
                popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.action_delete_cmenu:
                            bestguess.marker.setVisibility(View.GONE);
                            deletePoint(bestguess.x, bestguess.y);
                            // remove from file data
                            if (mFileData.numLocations() > 0)
                                mFileData.removeLocation(new LocalizationData.Location(bestguess.x, bestguess.y));
                            return true;
                        default:
                            return true;
                        }
                    }
                });
                popup.show();
                return true;
            }
        });

        final PhotoMarker secondguess = Utils.createNewMarker(getApplicationContext(), mRelative,
            markerlocs[2], markerlocs[3], R.drawable.bluegreen_x);
        secondguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popup = new PopupMenu(LocalizeActivity.this, secondguess.marker);
                popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.action_delete_cmenu:
                            secondguess.marker.setVisibility(View.GONE);
                            deletePoint(secondguess.x, secondguess.y);
                            // remove from file data
                            if (mFileData.numLocations() > 0)
                                mFileData.removeLocation(new LocalizationData.Location(secondguess.x, secondguess.y));
                            return true;
                        default:
                            return true;
                        }
                    }
                });
                popup.show();
                return true;
            }
        });

        final PhotoMarker thirdguess = Utils.createNewMarker(getApplicationContext(), mRelative,
            markerlocs[4], markerlocs[5], R.drawable.bluegreen_x);
        thirdguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popup = new PopupMenu(LocalizeActivity.this, thirdguess.marker);
                popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.action_delete_cmenu:
                            thirdguess.marker.setVisibility(View.GONE);
                            deletePoint(thirdguess.x, thirdguess.y);
                            // remove from file data
                            if (mFileData.numLocations() > 0)
                                mFileData.removeLocation(new LocalizationData.Location(thirdguess.x, thirdguess.y));
                            return true;
                        default:
                            return true;
                        }
                    }
                });
                popup.show();
                return true;
            }
        });

        // Remove the last four we added in preparation for adding the new results
        if (mHavePlacedMarker) {
            for (int i = 0; i < 4; i++)
                mAttacher.removeLastMarkerAdded();
        }
        mAttacher.addData(mark);
        mAttacher.addData(bestguess);
        mAttacher.addData(secondguess);
        mAttacher.addData(thirdguess);
        mHavePlacedMarker = true;
    }

    private void deletePoint(float x, float y) {
        Log.d("deletePoint", "trying to delete " + x + "," + y);

//        Unirest.post(Utils.Constants.DELETE_URL)
//            .field("x", x)
//            .field("y", y)
//            .asJsonAsync(new Callback<JsonNode>() {
//                public void failed(UnirestException e) {
//                    Toast.makeText(getApplicationContext(), "Couldn't delete point", Toast.LENGTH_SHORT).show();
//                    Log.e("deletePoint", "couldn't delete; http code = " + e.getMessage());
//                }
//
//                public void completed(HttpResponse<JsonNode> response) {
//                    Toast.makeText(getApplicationContext(), "message = " + response.getStatusText(), Toast
//                        .LENGTH_LONG).show();
//                }
//
//                public void cancelled() {}
//            });
    }

    /**
     * Download the server's copy of points.
     */
    public void getPoints() {
        new RetrieveReadingsTask().execute();
    }

    class RetrieveReadingsTask extends AsyncTask<Void, Void, List<Reading>> {
        protected List<Reading> doInBackground(Void... nothing) {
            try {
                final int mapId = (int) mMapId;
                return mReadingApi.readingsGet(mapId, null);
            } catch (ApiExceptionAndroid apiExceptionAndroid) {
                apiExceptionAndroid.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(@Nullable List<Reading> data) {
            if (data == null) {
                Log.e("onPostExecute", "got null data");
                return;
            }

            Log.d("onPostExecute", "got " + data.size() + " readings");
            for (Reading r : data) {
                double x = r.getMapX();
                double y = r.getMapY();
                final PhotoMarker marker = Utils.createNewMarker(getApplicationContext(), mRelative,
                    (float) x, (float) y, R.drawable.grey_x);
                mAttacher.addData(marker);
            }
        }
    }
}
