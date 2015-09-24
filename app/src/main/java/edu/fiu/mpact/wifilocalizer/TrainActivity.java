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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.Deque;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

public class TrainActivity extends Activity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    private boolean mIsMarkerPlaced = false;
    private boolean mSaveScanData = false;
    private int mScanNum = 0;
    private int mStashSize = 0;
    private long mMapId;
    private enum COLLECTION_MODES {CONTINUOUS, PASSES, SAMPLES};

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
                return;
            }

            final int newRows = mDataBuffer.stashScanResults(mWifiManager.getScanResults(),
                    mImgLocation);
            final boolean keepCapturing = updateProgressDialog(newRows);

            if (keepCapturing) {
                mWifiManager.startScan();
                return;
            } else {
                resetProgressDialog();
                mDataBuffer.saveStash();
            }

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
                                mDataBuffer.removeByCoordinate(mrk.x, mrk.y);
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

            // Show the new marker
            mAttacher.addData(mrk);
        }
    };

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //        TODO
    }

    // ***********************************************************************

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

        // Setup buffer to stash and to keep results
        mDataBuffer = new ScanResultBuffer(mMapId);

        // Create hint if first time running
        Utils.createHintIfNeeded(this, Utils.Constants.PREF_TRAIN_HINT, R.string.hint_train);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
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

    private void resetProgressDialog() {
//        TODO
        return;
    }

    private void setupProgressBarDialog() {
        mPrgBarDialog = new ProgressDialog(this);
        mPrgBarDialog.setTitle(R.string.dialog_scanning_title);
        mPrgBarDialog.setMessage(getString(R.string.dialog_scanning_description));
        mPrgBarDialog.setCancelable(true);
        mPrgBarDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mSaveScanData = false;
            }
        });
        mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //                        TODO
                    }
                });
        mPrgBarDialog.setCanceledOnTouchOutside(false);
        mPrgBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        resetProgressDialog();
    }

    protected boolean updateProgressDialog(int n) {
//        TODO
        return false;
    }
}
