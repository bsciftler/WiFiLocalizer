package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

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
    private LocalizationData mCachedMapData;


    public class APValueAdapter extends ArrayAdapter<LocalizationData.AccessPoint> {
        public APValueAdapter(Context context, Deque<LocalizationData.AccessPoint> aps) {
            super(context, 0, aps.toArray(new LocalizationData.AccessPoint[aps.size()]));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            LocalizationData.AccessPoint ap = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.marker_list_item, parent, false);
            }
            // Lookup view for data population
            TextView libssid = (TextView) convertView.findViewById(R.id.li_marker_bssid);
            TextView lirssi = (TextView) convertView.findViewById(R.id.li_marker_rssi);
            // Populate the data into the template view using the data object
            libssid.setText(ap.mBssid);
            lirssi.setText(String.valueOf(ap.mRssi));
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_map);

        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mTextView = (TextView) findViewById(R.id.marker_location_text);
        mListView = (ListView) findViewById(R.id.marker_rss_list);

        // Get URI for map image.
        // The cursor that handles the list of sessions is created in
        // SessionListFragment.java
        final Uri queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId);
        final Cursor cursor = getContentResolver().query(queryUri, null, null, null, null);
        if (!cursor.moveToFirst()) {
            Toast.makeText(this, getString(R.string.toast_map_id_warning), Toast.LENGTH_LONG).show();
            cursor.close();
            finish();
            return;
        }
        final Uri mapUri = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        cursor.close();

        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
        mImageView = (ImageView) findViewById(R.id.img_map_preview);
        mImageView.setImageURI(mapUri);
        mAttacher = new PhotoViewAttacher(mImageView, Utils.getImageSize(mapUri, getApplicationContext()));

        setupMarkers();

        Utils.createHintIfNeeded(this, Utils.Constants.PREF_VIEW_HINT, R.string.hint_view_act);
    }

    private void setupMarkers() {
        mCachedMapData = new LocalizationData(getContentResolver(), mMapId);
        final Deque<PhotoMarker> mrkrs = mCachedMapData.generateMarkers(getApplicationContext(), mRelative);

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
                    Deque<LocalizationData.AccessPoint> data =
                            mCachedMapData.getAccessPoints(new LocalizationData.Location(mrk.x, mrk.y));

                    mTextView.setText(String.valueOf(mrk.x) + " , " + String.valueOf(mrk.y) + "  " +
                            " size = " + data.size());
                    mListView.setAdapter(new APValueAdapter(ViewMapActivity.this, data));
                }
            });
        }

        mAttacher.replaceData(mrkrs);
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
        case R.id.action_export_csv:
            exportCsv();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void exportCsv() {
        // Setup file name
        String mapName = "unknown-map";
        final String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        final Cursor nameCursor = getContentResolver().query(DataProvider.MAPS_URI,
                new String[] {Database.Maps.NAME},
                Database.Maps.ID + "=?",
                new String[] {Long.toString(mMapId)}, null);
        if (nameCursor.moveToFirst())
            mapName = nameCursor.getString(nameCursor.getColumnIndex(Database.Maps.NAME));
        nameCursor.close();

        // Create file
        final Uri uri;
        final PrintWriter file;
        final String filename = String.format(Locale.US, "%s-%s", mapName, date);
        try {
            uri = Uri.fromFile(File.createTempFile(filename, ".csv", Environment.getExternalStorageDirectory()));
            file = new PrintWriter(new File(uri.getPath()));
        } catch (IOException e) {
            Toast.makeText(this, "Couldn't create CSV file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Select all semi-useful rows
        final String[] selection = {
                Database.Readings.ID,
                Database.Readings.DATETIME,
                Database.Readings.MAP_X,
                Database.Readings.MAP_Y,
                Database.Readings.SIGNAL_STRENGTH,
                Database.Readings.AP_NAME,
                Database.Readings.MAC,
                Database.Readings.UPDATE_STATUS
        };
        Cursor mapReadings = getContentResolver().query(DataProvider.READINGS_URI,
                selection,
                Database.Readings.MAP_ID + "=?",
                new String[] {Long.toString(mMapId)}, null);

        if (mapReadings == null) {
            Toast.makeText(this, "Couldn't query Readings table", Toast.LENGTH_SHORT).show();
            file.close();
            return;
        }

        // Write header
        file.write(TextUtils.join(",", selection) + "\n");

        // Write a csv row at a time
        while (mapReadings.moveToNext()) {
            // TODO mapx and mapy might trigger null pointer exceptions
            final String row = TextUtils.join(",", new String[] {
                    Integer.toString(mapReadings.getInt(mapReadings.getColumnIndex(Database.Readings.ID))),
                    Long.toString(mapReadings.getLong(mapReadings.getColumnIndex(Database.Readings.DATETIME))),
                    Float.toString(mapReadings.getFloat(mapReadings.getColumnIndex(Database.Readings.MAP_X))),
                    Float.toString(mapReadings.getFloat(mapReadings.getColumnIndex(Database.Readings.MAP_Y))),
                    Integer.toString(mapReadings.getInt(mapReadings.getColumnIndex(Database.Readings.SIGNAL_STRENGTH))),
                    mapReadings.getString(mapReadings.getColumnIndex(Database.Readings.AP_NAME)),
                    mapReadings.getString(mapReadings.getColumnIndex(Database.Readings.MAC)),
                    Integer.toString(mapReadings.getInt(mapReadings.getColumnIndex(Database.Readings.UPDATE_STATUS)))
            });
            file.write(row + "\n");
        }
        mapReadings.close();
        file.close();
        Toast.makeText(this, "Wrote " + uri.getPath(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setupMarkers();
        }
    }

    private void onDelete(float x, float y) {
        String[] mSelectionArgs = {
                String.valueOf(x - 0.0001), String.valueOf(x + 0.0001),
                String.valueOf(y - 0.0001), String.valueOf(y + 0.0001)
        };

        getContentResolver().delete(DataProvider.READINGS_URI,
                "mapx>? and mapx<? and mapy>? and mapy<?", mSelectionArgs);
    }
}
