package edu.fiu.mpact.wifilocalizer;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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

import com.github.amlcurran.showcaseview.ShowcaseView;

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
public class ViewMapActivity extends AppCompatActivity {
    private long mMapId;
    private RelativeLayout mRelative;
    private PhotoViewAttacher mAttacher;
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

        final ShowcaseView.Builder builder = new ShowcaseView.Builder(this)
            .withMaterialShowcase()
            .setStyle(R.style.CustomShowcaseTheme2)
            .setContentTitle("View Map details here")
            .setContentText("Click on the train button to start a new training session")
            .hideOnTouchOutside();
        Utils.showHelpOnFirstRun(this, Utils.Constants.PREF_VIEW_HINT, builder);

        // Setup activity data
        mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
        mTextView = (TextView) findViewById(R.id.marker_location_text);
        mListView = (ListView) findViewById(R.id.marker_rss_list);

        // Get details of map
        final Uri queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId);
        final Cursor cursor = getContentResolver().query(queryUri, null, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, getString(R.string.toast_map_id_warning), Toast.LENGTH_LONG).show();
            if (cursor != null) cursor.close();
            finish();
            return;
        }
        final Uri mapUri = Uri.parse(cursor.getString(cursor.getColumnIndex(Database.Maps.DATA)));
        final String mapName = cursor.getString(cursor.getColumnIndex(Database.Maps.NAME));
        cursor.close();

        // Try to change action bar title
        final ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(mapName);

        // Set map image
        mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
        final ImageView mImageView = (ImageView) findViewById(R.id.img_map_preview);
        if (mImageView != null) mImageView.setImageURI(mapUri);
        else Log.e("onCreate", "Couldn't find ImageView...bad layout");
        mAttacher = new PhotoViewAttacher(mImageView, Utils.getImageSize(mapUri, getApplicationContext()));

        // Set markers
        setupMarkers();
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

                    final String displayText = String.format(Locale.US, "%f, %f size = %d", mrk.x, mrk.y, data.size());
                    mTextView.setText(displayText);
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
        case R.id.action_train:
            intent = new Intent(this, TrainActivity.class);
            intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
            startActivityForResult(intent, 1);
            return true;
        case R.id.action_localize:
            intent = new Intent(this, LocalizeActivity.class);
            intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
            startActivity(intent);
            return true;
        case R.id.action_export_data:
            exportData();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void exportData() {
        StringBuilder out = new StringBuilder();

        // Setup file name
        String mapName = "unknown-map";
        final Cursor nameCursor = getContentResolver().query(DataProvider.MAPS_URI,
            new String[] {Database.Maps.NAME},
            Database.Maps.ID + "=?",
            new String[] {Long.toString(mMapId)}, null);
        if (nameCursor != null && nameCursor.moveToFirst())
            mapName = nameCursor.getString(nameCursor.getColumnIndex(Database.Maps.NAME));
        if (nameCursor != null) nameCursor.close();

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
        final Cursor mapReadings = getContentResolver().query(DataProvider.READINGS_URI,
            selection, Database.Readings.MAP_ID + "=?", new String[] {Long.toString(mMapId)}, null);

        if (mapReadings == null) {
            Toast.makeText(this, "Couldn't query Readings table", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mapReadings.getColumnIndex(Database.Readings.MAP_X) == -1 ||
            mapReadings.getColumnIndex(Database.Readings.MAP_Y) == -1) {
            Toast.makeText(this, "No x/y column", Toast.LENGTH_SHORT).show();
            return;
        }

        // Write header
        out.append(TextUtils.join(",", selection));
        out.append("\n");

        // Write a csv row at a time
        while (mapReadings.moveToNext()) {
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
            out.append(row);
            out.append("\n");
        }
        mapReadings.close();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "WiFi Data for " + mapName);
        intent.putExtra(Intent.EXTRA_TEXT, out.toString());
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {});
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setupMarkers();
        }
    }

    private void onDelete(float x, float y) {
        // Offset by epsilon to account for floating point weirdness
        final double epsilon = 0.001;

        final String[] mSelectionArgs = {
            String.valueOf(x - epsilon), String.valueOf(x + epsilon),
            String.valueOf(y - epsilon), String.valueOf(y + epsilon)
        };

        final String sql = String.format("%s>? and %s<? and %s>? and %s<?",
            Database.Readings.MAP_X, Database.Readings.MAP_X,
            Database.Readings.MAP_Y, Database.Readings.MAP_Y
        );
        getContentResolver().delete(DataProvider.READINGS_URI, sql, mSelectionArgs);
    }
}
