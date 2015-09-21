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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

public class TrainActivity extends Activity {
    private long mMapId;

    private boolean markerPlaced = false;
    private LinkedList<ContentValues> mCachedResults;
    private LinkedList<ContentValues> tempCachedResults;
    private boolean cancelled = false;
    private boolean scanRequested = false;

    private ImageView mImg;
    private float[] mImgLocation = new float[2];
    private PhotoViewAttacher mAttacher;
    private RelativeLayout mRelative;
    private Database controller;

    private WifiManager mWifiManager;
    private int scanNum = 0;
    private HashSet bssidSet;


    private PhotoMarker mrk;
    private ImageView selMrk;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("sr = " + scanRequested + " can = " + cancelled);
            if (!scanRequested) return;
            if (cancelled) {
                scanRequested = false;
                cancelled = false;
                return;
            }
            final List<ScanResult> results = mWifiManager.getScanResults();
            for (ScanResult result : results) {
                if (bssidSet.contains(result.BSSID)) continue;
                bssidSet.add(result.BSSID);
                ContentValues values = new ContentValues();
                values.put(Database.Readings.DATETIME, System.currentTimeMillis());
                values.put(Database.Readings.MAP_X, mImgLocation[0]);
                values.put(Database.Readings.MAP_Y, mImgLocation[1]);
                values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
                values.put(Database.Readings.AP_NAME, result.SSID);
                values.put(Database.Readings.MAC, result.BSSID);
                values.put(Database.Readings.MAP_ID, mMapId);
                values.put(Database.Readings.UPDATE_STATUS, 0);

                tempCachedResults.add(values);
            }

            System.out.println(bssidSet.size());
            scanNum++;
            mPrgBarDialog.setProgress(scanNum);
            if (scanNum < 8) {
                mWifiManager.startScan();
                return;
            }
            scanRequested = false;
            scanNum = 0;
            mPrgBarDialog.hide();
            Toast.makeText(getApplicationContext(), "If you are done training locations, please "
                    + "don't forget to SAVE above!", Toast.LENGTH_LONG).show();

            mCachedResults.addAll(tempCachedResults);
            tempCachedResults.clear();

            mAttacher.removeLastMarkerAdded();
            final PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(), mRelative,
                    mImgLocation[0], mImgLocation[1], R.drawable.red_x);
            mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PopupMenu popup = new PopupMenu(TrainActivity.this, mrk.marker);
                    popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                            case R.id.action_delete_cmenu:
                                mrk.marker.setVisibility(View.GONE);
                                onDelete(mrk.x, mrk.y);
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
    private ProgressDialog mPrgBarDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);

        mCachedResults = new LinkedList<>();
        tempCachedResults = new LinkedList<>();

        mPrgBarDialog = new ProgressDialog(this);
        mPrgBarDialog.setTitle(getString(R.string.dialog_scanning_title));
        mPrgBarDialog.setMessage(getString(R.string.dialog_scanning_description));
        mPrgBarDialog.setCancelable(true);
        mPrgBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelled = true;
                scanNum = 0;
                mPrgBarDialog.setProgress(0);
                tempCachedResults.clear();
                markerPlaced = true;
                dialog.dismiss();
            }
        });
        mPrgBarDialog.setCanceledOnTouchOutside(false);
        mPrgBarDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelled = true;
            }
        });
        mPrgBarDialog.setProgressStyle(mPrgBarDialog.STYLE_HORIZONTAL);
        mPrgBarDialog.setProgress(0);
        mPrgBarDialog.setMax(8);


        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

        mImg = (ImageView) findViewById(R.id.image_map);
        final Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(DataProvider
                .MAPS_URI, mMapId), null, null, null, null);
        if (!cursor.moveToFirst()) {
            Toast.makeText(this, getResources().getText(R.string.toast_map_id_warning), Toast
                    .LENGTH_LONG).show();
            cursor.close();
            finish();
            return;
        }
        final Uri img = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        cursor.close();

        bssidSet = new HashSet();

        final int[] imgSize = Utils.getImageSize(img, getApplicationContext());
        mImg.setImageURI(img);
        mAttacher = new PhotoViewAttacher(mImg, imgSize);
        mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {

                mImgLocation[0] = x * imgSize[0];
                mImgLocation[1] = y * imgSize[1];

                if (markerPlaced) mAttacher.removeLastMarkerAdded();
                PhotoMarker tmpmrk = Utils.createNewMarker(getApplicationContext(), mRelative,
                        mImgLocation[0], mImgLocation[1]);
                mAttacher.addData(tmpmrk);
                markerPlaced = true;
            }
        });

        Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData = Utils
                .gatherLocalizationData(getContentResolver(), mMapId);
        Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData, getApplicationContext(),
                mRelative);
        for (PhotoMarker mrk : mrkrs) {
            mrk.marker.setAlpha(0.8f);
            mrk.marker.setImageResource(R.drawable.grey_x);
        }
        mAttacher.addData(mrkrs);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

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
            if (markerPlaced) {
                bssidSet = new HashSet();
                markerPlaced = false;
                mPrgBarDialog.setProgress(0);
                mPrgBarDialog.show();
                mWifiManager.startScan();
                cancelled = false;
                scanRequested = true;
            }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void saveTraining() {
        if (mCachedResults.isEmpty()) return;
        // Add readings

        getContentResolver().bulkInsert(DataProvider.READINGS_URI, mCachedResults.toArray(new
                ContentValues[]{}));

    }

    private void onDelete(float x, float y) {
        float cachex, cachey;
        ContentValues val;
        ListIterator<ContentValues> iter = mCachedResults.listIterator();
        while (iter.hasNext()) {
            val = iter.next();
            cachex = val.getAsFloat(Database.Readings.MAP_X);
            cachey = val.getAsFloat(Database.Readings.MAP_Y);
            if (cachex == x && cachey == y) {
                iter.remove();
            }
        }
    }
}
