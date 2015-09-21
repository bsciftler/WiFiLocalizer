package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Second activity in normal activity lifecycle. Lists each map with a
 * interactive thumbnail and gives option of adding another training session or
 * the option of starting a localization session.
 *
 * @author oychang
 */
public class ViewMapActivity extends Activity {
    private long mMapId;
    private RelativeLayout mRelative;
    private ImageView mImageView;
    private PhotoViewAttacher mAttacher;
    private ImageView selMrk;
    private TextView mTextView;
    private ListView mListView;
    private Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData;
    private ArrayList<Utils.APValue> aparray;
    private Database controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_map);

        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);

        // Get URI for map image.
        // The cursor that handles the list of sessions is created in
        // SessionListFragment.java
        final Uri queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId);
        final Cursor cursor = getContentResolver().query(queryUri, null, null, null, null);
        if (!cursor.moveToFirst()) {
            Toast.makeText(this, getString(R.string.toast_map_id_warning), Toast.LENGTH_LONG)
                    .show();
            cursor.close();
            finish();
            return;
        }
        final Uri mapUri = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        cursor.close();

        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

        mImageView = (ImageView) findViewById(R.id.img_map_preview);
        mImageView.setImageURI(mapUri);
        // mAttacher = new PhotoViewAttacher(mImageView);

        mAttacher = new PhotoViewAttacher(mImageView, Utils.getImageSize(mapUri,
                getApplicationContext()));

        // gathersamples is buggy

        mTextView = (TextView) findViewById(R.id.marker_location_text);
        mListView = (ListView) findViewById(R.id.marker_rss_list);

        aparray = new ArrayList<>();
        updateMarkers();

        Utils.createHintIfNeeded(this, Utils.Constants.PREF_VIEW_HINT, R.string.hint_view_act);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_map, menu);
        return true;
    }

    /**
     * Fork out to either training or localization activity
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
        case R.id.action_new_session:
            intent = new Intent(this, TrainActivity.class);
            intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
            startActivityForResult(intent, 1);
            return true;
        case R.id.action_localize:
            intent = new Intent(this, LocalizeActivity.class);
            intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
            startActivity(intent);
            return true;
        //		case R.id.action_add_map_scale:
        //			intent = new Intent(this, MapScaleActivity.class);
        //			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
        //			startActivity(intent);
        //			return true;
        //		case R.id.action_lazy_train:
        //			intent = new Intent(this, AutomaticTrainActivity.class);
        //			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
        //			startActivity(intent);
        //			return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void updateMarkers() {
        mCachedMapData = Utils.gatherLocalizationData(getContentResolver(), mMapId);
        Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData, getApplicationContext(),
                mRelative);
        for (final PhotoMarker mrk : mrkrs) {
            mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PopupMenu popup = new PopupMenu(ViewMapActivity.this, mrk.marker);
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

            mrk.marker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    aparray = mCachedMapData.get(new Utils.TrainLocation(mrk.x, mrk.y));
                    mTextView.setText(String.valueOf(mrk.x) + " , " + String.valueOf(mrk.y) + "  " +
                            " size = " + aparray.size());
                    Log.d("qwer", "aparray size = " + aparray.size());
                    APValueAdapter adapter = new APValueAdapter(ViewMapActivity.this, aparray);

                    mListView.setAdapter(adapter);
                }
            });
        }
        mAttacher.replaceData(mrkrs);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            updateMarkers();
        }
    }

    private void onDelete(float x, float y) {
        String[] mSelectionArgs = {String.valueOf(x - 0.0001), String.valueOf(x + 0.0001), String
                .valueOf(y - 0.0001), String.valueOf(y + 0.0001)};
        getContentResolver().delete(DataProvider.READINGS_URI, "mapx>? and mapx<? and mapy>? and " +
                "" + "mapy<?", mSelectionArgs);
    }
}
