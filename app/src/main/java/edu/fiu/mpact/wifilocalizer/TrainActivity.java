package edu.fiu.mpact.wifilocalizer;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.Locale;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

public class TrainActivity extends Activity {
    private boolean mIsMarkerPlaced = false;
    private boolean mSaveScanData = false;

    private SettingsActivity.COLLECTION_MODES mMode = SettingsActivity.COLLECTION_MODES.CONTINUOUS;
    private int mModePasses = -1;
    private int mCurrentCollectionCount = 0;

    private float[] mImgLocation = new float[2];
    private PhotoViewAttacher mAttacher;
    private RelativeLayout mRelative;
    private ProgressDialog mPrgBarDialog;
    private WifiManager mWifiManager;
    private ScanResultBuffer mDataBuffer;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mSaveScanData) {
                Log.i("mReceiver", "not saving scan data");
                return;
            }
            Log.i("mReceiver", "saving scan data");

            final int newRows = mDataBuffer.stashScanResults(mWifiManager.getScanResults(),
                    mImgLocation);
            final boolean keepCapturing = updateProgressDialog(newRows);

            if (keepCapturing) {
                mWifiManager.startScan();
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
        final Uri img;
        final Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(DataProvider
                .MAPS_URI, mapId), null, null, null, null);
        if (!cursor.moveToFirst()) {
            Toast.makeText(this, getResources().getText(R.string.toast_map_id_warning), Toast
                    .LENGTH_LONG).show();
            cursor.close();
            finish();
            return;
        } else {
            img = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
            cursor.close();
        }

        //  Setup PhotoViewAttacher and listeners
        final int[] imgSize = Utils.getImageSize(img, getApplicationContext());
        final ImageView imageView = (ImageView) findViewById(R.id.image_map);
        // FIXME this is incredibly slow
        imageView.setImageURI(img);
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
        final Deque<PhotoMarker> mrkrs = Utils.generateMarkers(Utils.gatherLocalizationData
                (getContentResolver(), mapId), getApplicationContext(), mRelative);
        for (PhotoMarker mrk : mrkrs) {
            mrk.marker.setAlpha(0.8f);
            mrk.marker.setImageResource(R.drawable.grey_x);
        }
        mAttacher.addData(mrkrs);

        // Create hint if first time running
        Utils.createHintIfNeeded(this, Utils.Constants.PREF_TRAIN_HINT, R.string.hint_train);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i("onResume", "adding intent filter...");
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

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
            }
            return true;
        case R.id.action_options:
            startActivity(new Intent(this, SettingsActivity.class));
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
                    .dialog_save), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDataBuffer.saveStash();
                    mDataBuffer.saveTrainingToDatabase(getContentResolver());

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
            mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string
                    .no), new DialogInterface.OnClickListener() {
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

    public void collect(View view) {
        final String url = Utils.Constants.PINEAPPLE_URL + ":" + Utils.Constants.PINEAPPLE_SCRAPER_PORT;

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    final String response = new String(responseBody, "UTF8");
                    Utils.PineappleResponse data = new Gson().fromJson(response, Utils.PineappleResponse.class);

                    // TODO
                } catch (UnsupportedEncodingException e) {
                    Log.e("PineDebug", "response had a weird encoding; headers = " + headers);
                    return;
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                Toast.makeText(getApplicationContext(), "Get failed with HTTP Code " + statusCode, Toast.LENGTH_LONG).show();
            }
        });
    }
}
