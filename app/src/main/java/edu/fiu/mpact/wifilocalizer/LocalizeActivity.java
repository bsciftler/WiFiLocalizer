package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;


public class LocalizeActivity extends Activity {
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

    private WifiManager mWifiManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mScanRequested) return;
            if (mAutoLocalizeEnabled) mWifiManager.startScan();

            List<ScanResult> results = mWifiManager.getScanResults();

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
/*            case 5:
                mAlgo.remoteLocalize(results, mMapId);
                break;
            case 6:
                mAlgo.remoteLocalize2(results, mMapId);
                break;
            case 7:
                mAlgo.remotePrivLocalize(results, mMapId, sk, pk);
                break;
            case 8:
                mAlgo.remotePrivLocalize2(results, mMapId, sk, pk);
                break;*/
            default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localize);

        mAlgo = new LocalizationEuclideanDistance();
        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
        final ImageView mapImage = (ImageView) findViewById(R.id.image_map);

        // Check mMapId is valid
        final Cursor cursor = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId), null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, "Invalid mapId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (!cursor.moveToFirst()) {
            Toast.makeText(this, getResources().getText(R.string.toast_map_id_warning), Toast.LENGTH_LONG).show();
            cursor.close();
            finish();
            return;
        }

        // Get and display map image
        final Uri img = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        cursor.close();
        mapImage.setImageURI(img);
        mAttacher = new PhotoViewAttacher(mapImage, Utils.getImageSize(img, getApplicationContext()));

        // Add existing map data to the view
        mCachedMapData = new LocalizationData(getContentResolver(), mMapId);
        mAttacher.addData(mCachedMapData.generateMarkers(getApplicationContext(), mRelative));

        // Setup WifiManager
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        // Load in data from alternative sources than the database
        mFileData = new LocalizationData(getResources().openRawResource(R.raw.readings), mMapId);
        getPoints();

        sk = new PrivateKey(1024);
        pk = new PublicKey();
        Paillier.keyGen(sk, pk);

        mAlgo.setup(mCachedMapData, mFileData, LocalizeActivity.this);

        Utils.createHintIfNeeded(this, Utils.Constants.PREF_LOCALIZE_HINT, R.string.hint_localize);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
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

    public void localizeNow(View _) {
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
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("x", x);
        params.put("y", y);

        client.post(Utils.Constants.DELETE_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Toast.makeText(getApplicationContext(), "message = " + new String(bytes), Toast
                        .LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                Toast.makeText(getApplicationContext(), "Couldn't delete point", Toast.LENGTH_SHORT).show();
                Log.e("deletePoint", "couldn't delete; http code = " + statusCode);
            }
        });
    }

    /**
     * Download the server's copy of points.
     */
    public void getPoints() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        client.post(Utils.Constants.POINTS_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    JSONArray arr = new JSONArray(new String(response));
                    if (arr.length() != 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = (JSONObject) arr.get(i);
                            mAttacher.addData(Utils.createNewMarker(getApplicationContext(),
                                    mRelative, (float) obj.getDouble("mapx"), (float) obj.getDouble("mapy"), R.drawable.grey_x));
                        }
                    }
                } catch (JSONException e) {
                    Log.w("getPoints", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                Toast.makeText(getApplicationContext(), "Couldn't get points", Toast.LENGTH_SHORT).show();
                Log.e("getPoints", "couldn't update; http code = " + statusCode);
            }
        });
    }
}
