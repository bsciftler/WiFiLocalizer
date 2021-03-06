package edu.fiu.mpact.wifilocalizer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import io.swagger.client.ApiExceptionPineapple;
import io.swagger.client.api.PineappleApi;
import io.swagger.client.model.Probe;
import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;


public class TrainActivity extends AppCompatActivity {
    // Current state stuff for determining stage of train
    private boolean mIsMarkerPlaced = false;
    private boolean mSaveScanData = false;
    private boolean mPineappleEnabled = false;

    // Sample collection stuff
    private SettingsActivity.COLLECTION_MODES mMode = SettingsActivity.COLLECTION_MODES.CONTINUOUS;
    private int mModePasses = -1;
    private int mCurrentCollectionCount = 0;

    // Layout stuff
    private float[] mImgLocation = new float[2];
    private PhotoViewAttacher mAttacher;
    private RelativeLayout mRelative;
    private ProgressDialog mPrgBarDialog;

    // Network stuff
    private WifiManager mWifiManager;
    private ScanResultBuffer mDataBuffer;
    private List<Probe> mPineappleData = null;
    private PineappleApi mPineapple;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mSaveScanData) return;

            // Show toast for pineapple feedback
            if (mPineappleEnabled && mPineappleData != null) {
                final String toastText = "got " + mPineappleData.size() + " " + "results";
                Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            }

            final int newRows = mDataBuffer.stashScanResults(mWifiManager.getScanResults(), mImgLocation, mPineappleData);
            final boolean keepCapturing = updateProgressDialog(newRows);
            mWifiManager.startScan();

            if (keepCapturing) {
                mWifiManager.startScan();
                getPineappleProbes();
                return;
            }

            resetProgressDialog();
            mSaveScanData = false;
            mPrgBarDialog.cancel();
            mDataBuffer.saveStash();

            addPermanentMarker();
        }
    };

    // ***********************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        final long mapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
        // Setup WiFi manager (but do not start scanning)
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mDataBuffer = new ScanResultBuffer(mapId);

        // Get map URI or die trying
        final Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(DataProvider
            .MAPS_URI, mapId), null, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, getResources().getText(R.string.toast_map_id_warning), Toast
                .LENGTH_LONG).show();
            finish();
            return;
        }
        final Uri img = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        final String mapName = cursor.getString(cursor.getColumnIndex(Database.Maps.NAME));
        cursor.close();

        // Setup ActionBar
        final ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(mapName);

        //  Setup PhotoViewAttacher and listeners
        final int[] imgSize = Utils.getImageSize(img, getApplicationContext());
        final ImageView imageView = (ImageView) findViewById(R.id.image_map);
        if (imageView != null) imageView.setImageURI(img);
        else Log.e("onCreate", "Couldn't get ImageView");
        mAttacher = new PhotoViewAttacher(imageView, imgSize);
        mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                mImgLocation[0] = x * imgSize[0];
                mImgLocation[1] = y * imgSize[1];

                // If we've already placed a marker without locking it down,
                // remove that old marker and replace with this one
                if (mIsMarkerPlaced) {
                    mAttacher.removeLastMarkerAdded();
                }
                mAttacher.addData(Utils.createNewMarker(getApplicationContext(), mRelative,
                    mImgLocation[0], mImgLocation[1]));
                mIsMarkerPlaced = true;
            }
        });

        // Get data in Readings table for previously trained points and draw them on the map
        final Deque<PhotoMarker> mrkrs = new LocalizationData(
            getContentResolver(), mapId).generateMarkers(getApplicationContext(), mRelative);
        for (PhotoMarker mrk : mrkrs) {
            mrk.marker.setAlpha(0.8f);
            mrk.marker.setImageResource(R.drawable.grey_x);
        }
        mAttacher.addData(mrkrs);

        // Create hint if first time running
        ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
            .withMaterialShowcase()
            .setStyle(R.style.CustomShowcaseTheme2)
            .setContentTitle(R.string.hint_train_title)
            .setContentText(R.string.hint_train)
            .hideOnTouchOutside();
        Utils.showHelpOnFirstRun(this, Utils.Constants.PREF_TRAIN_HINT, builder);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Do we want to capture pineapple data?
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPineappleEnabled = prefs.getBoolean("pref_pineapple_enabled", false);
        if (mPineappleEnabled) mPineapple = new PineappleApi();
        Log.i("onResume", "mPineappleEnabled = " + mPineappleEnabled);

        // Listen for Wifi scan results
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        // Setup dialog depending on preferences
        setupProgressBarDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrgBarDialog.dismiss();
    }

    // ***********************************************************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.train, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_save:
            mDataBuffer.saveTrainingToDatabase(getContentResolver());
            setResult(RESULT_OK);
            finish();
            return true;
        case R.id.action_lock:
            if (mIsMarkerPlaced) {
                resetProgressDialog();
                mPrgBarDialog.show();

                mSaveScanData = true;
                mWifiManager.startScan();
                getPineappleProbes();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // ***********************************************************************

    protected boolean updateProgressDialog(int newRows) {
        switch (mMode) {
        case CONTINUOUS:
            mCurrentCollectionCount += newRows;
            mPrgBarDialog.setMessage(String.format(Locale.US, getString(R.string
                .dialog_scanning_continuous_message), mCurrentCollectionCount));
            return true;
        case SAMPLES:
            mCurrentCollectionCount += newRows;
            mPrgBarDialog.setProgress(mCurrentCollectionCount);
            return mCurrentCollectionCount < mModePasses;
        case PASSES:
            mCurrentCollectionCount++;
            mPrgBarDialog.setProgress(mCurrentCollectionCount);
            return mCurrentCollectionCount < mModePasses;
        default:
            return false;
        }
    }

    protected void resetProgressDialog() {
        mCurrentCollectionCount = 0;
    }

    protected void setupProgressBarDialog() {
        // Read preferences
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_samples_toggle", false)) {
            mMode = SettingsActivity.COLLECTION_MODES.SAMPLES;
            mModePasses = prefs.getInt("pref_n_samples", 1);
        } else if (prefs.getBoolean("pref_passes_toggle", false)) {
            mMode = SettingsActivity.COLLECTION_MODES.PASSES;
            mModePasses = prefs.getInt("pref_n_passes", 1);
        } else {
            mMode = SettingsActivity.COLLECTION_MODES.CONTINUOUS;
            mModePasses = -1;
        }
        Log.i("setupPBDialog", "mMode = " + mMode);
        Log.i("setupPBDialog", "mModePasses = " + mModePasses);

        // Setup common dialog preferences
        mPrgBarDialog = new ProgressDialog(this);
        mPrgBarDialog.setTitle(R.string.dialog_scanning_title);
        mPrgBarDialog.setMessage(getString(R.string.dialog_scanning_description));
        mPrgBarDialog.setCancelable(true);
        mPrgBarDialog.setCanceledOnTouchOutside(false);

        switch (mMode) {
        case CONTINUOUS:
            mPrgBarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mPrgBarDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string
                .dialog_done), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDataBuffer.saveStash();

                    addPermanentMarker();
                    mSaveScanData = false;
                    resetProgressDialog();
                    dialog.cancel();
                }
            });
            mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string
                .no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSaveScanData = false;
                    resetProgressDialog();
                    dialog.cancel();
                }
            });
            break;
        case PASSES:
        case SAMPLES:
        default:
            mPrgBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mPrgBarDialog.setMax(mModePasses);
            mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSaveScanData = false;
                    mDataBuffer.clearStash();
                    resetProgressDialog();
                    dialog.cancel();
                }
            });
            break;
        }
    }

    private void addPermanentMarker() {
        // Remove the temporary marker
        mAttacher.removeLastMarkerAdded();
        mIsMarkerPlaced = false;
        // In the same location, place a permanent marker
        final PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(), mRelative,
            mImgLocation[0], mImgLocation[1], R.drawable.red_x);
        // Create a popup that will allow deletion of the marker from the map
        mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final PopupMenu popup = new PopupMenu(TrainActivity.this, mrk.marker);
                popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.popup_delete_marker:
                            // Make the marker invisible. Since the data is gone from the
                            // buffer, we won't end up saving or seeing this point.
                            mrk.marker.setVisibility(View.GONE);
                            final int removed = mDataBuffer.removeByCoordinate(mrk.x, mrk.y);
                            Log.i("onLongClick", "removed " + removed + " contentvalues");
                            return true;
                        default:
                            return false;
                        }
                    }
                });

                popup.show();
                return true;
            }
        });
        mAttacher.addData(mrk);
    }

    // ***********************************************************************


    class RetrieveProbeTask extends AsyncTask<Void, Void, List<Probe>> {
        protected List<Probe> doInBackground(Void... nothing) {
            try {
                final List<Probe> data = mPineapple.probesGet().getData();
                return data;
            } catch (ApiExceptionPineapple e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(@Nullable List<Probe> data) {
            mPineappleData = data;
        }
    }

    public void getPineappleProbes() {
        if (!mPineappleEnabled) return;

        new RetrieveProbeTask().execute();
    }

    // ***********************************************************************


    class ScanResultWithExtras {
        private final List<ScanResult> mScanResults;
        private final float[] mPos;
        private final Long mTime;
        private final List<Probe> mPineappleResponse;

        ScanResultWithExtras(List<ScanResult> ls, float[] pos, Long time, List<Probe> resp) {
            mScanResults = ls;
            mPos = pos;
            mTime = time;
            mPineappleResponse = resp;
        }

        public List<ScanResult> getScanResults() {
            return mScanResults;
        }

        public float[] getPosition() {
            return mPos;
        }

        public Long getTime() {
            return mTime;
        }

        public List<Probe> getPineappleResponse() {
            return mPineappleResponse;
        }
    }


    class ScanResultBuffer {
        protected final long mMapId;
        private final Deque<ScanResultWithExtras> mStash = new ArrayDeque<>();
        private Deque<ContentValues> mToCommit = new ArrayDeque<>();
        private Deque<ContentValues> mProbesToCommit = new ArrayDeque<>();

        public ScanResultBuffer(long mapId) {
            mMapId = mapId;
        }

        public int removeByCoordinate(float x, float y) {
            final Deque<ContentValues> commitCopy = new ArrayDeque<>(mToCommit.size());

            for (ContentValues values : mToCommit) {
                if (values.getAsFloat(Database.Readings.MAP_X) != x && values.getAsFloat(Database.Readings.MAP_Y) != y) {
                    commitCopy.add(values);
                }
            }

            final int diff = mToCommit.size() - commitCopy.size();
            mToCommit = commitCopy;

            return diff;
        }

        public void clearStash() {
            mStash.clear();
        }

        public int stashScanResults(List<ScanResult> resultsToStash, float[] loc, List<Probe> response) {
            mStash.add(new ScanResultWithExtras(resultsToStash, loc, System.currentTimeMillis(), response));
            return resultsToStash.size();
        }

        public int saveStash() {
            int rowsInserted = 0;

            for (ScanResultWithExtras x : mStash) {
                rowsInserted += insertScanResults(
                    x.getScanResults(),
                    x.getPosition(),
                    x.getTime());
                insertProbeResults(
                    x.getPosition(),
                    x.getPineappleResponse());
            }

            return rowsInserted;
        }

        private ContentValues srToCv(ScanResult result, float[] loc, long time) {
            final ContentValues values = new ContentValues();
            values.put(Database.Readings.DATETIME, time);
            values.put(Database.Readings.MAP_X, loc[0]);
            values.put(Database.Readings.MAP_Y, loc[1]);
            values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
            values.put(Database.Readings.AP_NAME, result.SSID);
            values.put(Database.Readings.MAC, result.BSSID);
            values.put(Database.Readings.MAP_ID, mMapId);
            values.put(Database.Readings.UPDATE_STATUS, 0);

            return values;
        }

        public int insertScanResults(List<ScanResult> results, float[] location, long time) {
            int rowsInserted = 0;

            for (ScanResult result : results) {
                mToCommit.add(srToCv(result, location, time));
                rowsInserted++;
            }

            return rowsInserted;
        }

        public int insertProbeResults(float[] location, List<Probe> response) {
            if (response == null)
                return 0;

            int rowsInserted = 0;
            for (Probe p : response) {
                String[] splitFingerprint = p.toString().split(" ", 2);
                final String rssi = splitFingerprint[0];
                final String fingerprint = splitFingerprint[1];

                final ContentValues values = new ContentValues();
                values.put(Database.Probes.MAP_X, location[0]);
                values.put(Database.Probes.MAP_Y, location[1]);
                values.put(Database.Probes.SIGNAL_STRENGTH, rssi);
                values.put(Database.Probes.FINGERPRINT, fingerprint);
                values.put(Database.Probes.MAP_ID, mMapId);
                mProbesToCommit.add(values);

                rowsInserted++;
            }

            return rowsInserted;
        }

        /**
         * Bulk insert contents of caches into Readings and Probes table.
         *
         * @param resolver ContentResolver to use to save
         */
        public void saveTrainingToDatabase(ContentResolver resolver) {
            resolver.bulkInsert(DataProvider.PROBES_URI, mProbesToCommit.toArray(new ContentValues[mProbesToCommit.size()]));
            resolver.bulkInsert(DataProvider.READINGS_URI, mToCommit.toArray(new ContentValues[mToCommit.size()]));
        }
    }
}
