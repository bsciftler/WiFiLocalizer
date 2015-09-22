package edu.fiu.mpact.wifilocalizer;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

public class TrainActivity extends Activity {
    private long mMapId;
    private int mScanNum = 0;
    private final int SCAN_PASSES = 8;

    /**
     * true iff we've placed a marker but haven't saved it yet...still in buffer
     **/
    private boolean mIsMarkerPlaced = false;
    /**
     * TODO
     **/
    private boolean mIsCancelled = false;
    /**
     * TODO
     **/
    private boolean mIsScanRequested = false;

    private float[] mImgLocation = new float[2];
    private PhotoViewAttacher mAttacher;
    private RelativeLayout mRelative;

    /**
     * All unique access point BSSIDs (aka MAC) for this session.
     **/
    private Set<String> mBssidSet = new HashSet<>();
    /**
     * TODO
     **/
    private Deque<ContentValues> mCachedResults = new ArrayDeque<>();
    /**
     * TODO
     **/
    private Deque<ContentValues> mTempCachedResults = new ArrayDeque<>();

    private ProgressDialog mPrgBarDialog;
    private WifiManager mWifiManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mIsScanRequested) {
                return;
            } else if (mIsCancelled) {
                mIsScanRequested = false;
                mIsCancelled = false;
                return;
            }

            final List<ScanResult> results = mWifiManager.getScanResults();
            for (ScanResult result : results) {
                if (mBssidSet.contains(result.BSSID)) {
                    continue;
                }
                mBssidSet.add(result.BSSID);

                final ContentValues values = new ContentValues();
                values.put(Database.Readings.DATETIME, System.currentTimeMillis());
                values.put(Database.Readings.MAP_X, mImgLocation[0]);
                values.put(Database.Readings.MAP_Y, mImgLocation[1]);
                values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
                values.put(Database.Readings.AP_NAME, result.SSID);
                values.put(Database.Readings.MAC, result.BSSID);
                values.put(Database.Readings.MAP_ID, mMapId);
                values.put(Database.Readings.UPDATE_STATUS, 0);
                mTempCachedResults.add(values);
            }

            mScanNum++;
            mPrgBarDialog.setProgress(mScanNum);
            if (mScanNum < SCAN_PASSES) {
                mWifiManager.startScan();
                return;
            }
            mIsScanRequested = false;
            mScanNum = 0;
            mPrgBarDialog.hide();

            mCachedResults.addAll(mTempCachedResults);
            mTempCachedResults.clear();

            mAttacher.removeLastMarkerAdded();
            final PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(), mRelative,
                    mImgLocation[0], mImgLocation[1], R.drawable.red_x);
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
                                mrk.marker.setVisibility(View.GONE);

                                for (ContentValues val : mCachedResults) {
                                    float cachex = val.getAsFloat(Database.Readings.MAP_X);
                                    float cachey = val.getAsFloat(Database.Readings.MAP_Y);
                                    if (cachex == mrk.x && cachey == mrk.y) {
                                        mCachedResults.remove(val);
                                    }
                                }
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

            mAttacher.addData(mrk);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
        setupProgressBarDialog();

        // Get map URI or die trying
        final Uri img;
        final Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(DataProvider
                .MAPS_URI, mMapId), null, null, null, null);
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
        ImageView imageView = (ImageView) findViewById(R.id.image_map);
        imageView.setImageURI(img);
        mAttacher = new PhotoViewAttacher(imageView, imgSize);
        mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                mImgLocation[0] = x * imgSize[0];
                mImgLocation[1] = y * imgSize[1];

                // FIXME document
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
                (getContentResolver(), mMapId), getApplicationContext(), mRelative);
        for (PhotoMarker mrk : mrkrs) {
            mrk.marker.setAlpha(0.8f);
            mrk.marker.setImageResource(R.drawable.grey_x);
        }
        mAttacher.addData(mrkrs);

        // Setup WiFi manager and callbacks
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        // Create hint if first time running
        Utils.createHintIfNeeded(this, Utils.Constants.PREF_TRAIN_HINT, R.string.hint_train);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mPrgBarDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.train, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_save:
            saveTraining();
            setResult(RESULT_OK);
            finish();

            return true;
        case R.id.action_lock:
            if (mIsMarkerPlaced) {
                mBssidSet.clear();
                mIsMarkerPlaced = false;
                mPrgBarDialog.setProgress(0);
                mPrgBarDialog.show();
                mWifiManager.startScan();
                mIsCancelled = false;
                mIsScanRequested = true;
            }

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void setupProgressBarDialog() {
        if (mPrgBarDialog != null) {
            return;
        }

        mPrgBarDialog = new ProgressDialog(this);
        mPrgBarDialog.setTitle(R.string.dialog_scanning_title);
        mPrgBarDialog.setMessage(getString(R.string.dialog_scanning_description));
        mPrgBarDialog.setCancelable(true);
        mPrgBarDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mIsCancelled = true;
            }
        });
        mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mTempCachedResults.clear();

                        mIsCancelled = true;

                        mScanNum = 0;
                        mPrgBarDialog.setProgress(0);

                        mIsMarkerPlaced = true;
                        dialog.dismiss();
                    }
                });
        mPrgBarDialog.setCanceledOnTouchOutside(false);
        mPrgBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mPrgBarDialog.setProgress(0);
        mPrgBarDialog.setMax(SCAN_PASSES);
    }

    /**
     * Bulk insert contents of mCachedResults into Readings table iff data was collected.
     */
    private void saveTraining() {
        if (mCachedResults.isEmpty()) {
            return;
        }

        getContentResolver().bulkInsert(DataProvider.READINGS_URI, mCachedResults.toArray(new
                ContentValues[]{}));
    }
}
