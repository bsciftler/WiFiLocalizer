package edu.fiu.mpact.wifilocalizer;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import uk.co.senab.photoview.PhotoMarker;

public class Utils {
    public final class Constants {
        // Fully qualified project name for use in intent data passing
        // This doesn't have to actually be correct, but try to be.
        private static final String PKG = "edu.fiu.mpact.wifilocalizer";
        public static final String MAP_ID_EXTRA = PKG + ".map_id";

        // Shared preferences file
        public static final String PREF_FILE = "SharedHintsPreferences";
        // Unique keys to address the hint boxes
        public static final String PREF_HIDE_ALL_HINTS = "hint0";
        public static final String PREF_MAIN_HINT = "hint1";
        public static final String PREF_VIEW_HINT = "hint2";
        public static final String PREF_LOCALIZE_HINT = "hint3";
        public static final String PREF_TRAIN_HINT = "hint4";
    }

    /**
     * @param context
     * @param key
     * @param res
     * @return the value of the preference
     */
    public static boolean createHintIfNeeded(Context context, String key, int res) {
        final SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_FILE, Context
                .MODE_PRIVATE);

        // Check if we've already bugged about this
        if (prefs.getBoolean(key, false)) {
            return false;
        } else if (prefs.getBoolean(Constants.PREF_HIDE_ALL_HINTS, false)) {
            return true;
        }

        // http://stackoverflow.com/a/9763836/1832800

        final View checkBoxView = View.inflate(context, R.layout.checkbox_dialog, null);
        final CheckBox box = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSharedPreference(prefs, Constants.PREF_HIDE_ALL_HINTS, isChecked);
            }
        });
        box.setText(R.string.dont_show_hints);
        buildDialog(context, res).setView(checkBoxView).setIcon(R.drawable.ic_launcher).show();

        // Mark down this dialog as shown
        setSharedPreference(prefs, key, true);

        return true;
    }

    public static AlertDialog.Builder buildDialog(Context context, int res) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        return dialog.setPositiveButton(android.R.string.yes, null).setMessage(res);
    }

    public static void setSharedPreference(SharedPreferences prefs, String key, boolean value) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static Uri resourceToUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID));
    }

    // ***********************************************************************

    public static class EncTrainDistMatchPair {
        public TrainLocation trainLocation;
        public BigInteger dist;
        public int matches;

        public EncTrainDistMatchPair(TrainLocation t, BigInteger d, int m) {
            trainLocation = t;
            dist = d;
            matches = m;
        }
    }

    public static class EncTrainDistPair {
        public TrainLocation trainLocation;
        public BigInteger dist;

        public EncTrainDistPair(TrainLocation t, BigInteger d) {
            trainLocation = t;
            dist = d;
        }
    }

    public static class TrainDistPair implements Comparable<TrainDistPair> {
        public TrainLocation trainLocation;
        public double dist;

        public TrainDistPair(TrainLocation t, double d) {
            trainLocation = t;
            dist = d;
        }

        @Override
        public int compareTo(TrainDistPair another) {
            return dist < another.dist ? -1 : dist > another.dist ? 1 : 0;
        }
    }

    public static class Coord {
        public float mX, mY;
        public int mRssi = -1;

        public Coord(float x, float y, int rssi) {
            this(x, y);
            mRssi = rssi;
        }

        public Coord(float x, float y) {
            mX = x;
            mY = y;
        }
    }

    public static class TrainLocation extends Coord {
        public TrainLocation(float x, float y) {
            super(x, y);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof TrainLocation)) return false;
            if (obj == this) return true;

            TrainLocation t = (TrainLocation) obj;

            return this.mX == t.mX && this.mY == t.mY;
        }

        public int hashCode() {
            int hash = 3;

            hash = 7 * hash + (int) this.mX;
            return hash;
        }
    }

    public static class APValue {
        public String mBssid = "";
        public int mRssi = -1; //received signal strength indicator

        public APValue(String bssid, int rssi) {
            mBssid = bssid;
            mRssi = rssi;
        }
    }

    public static class EncAPValue {
        public String mBssid;
        public BigInteger mRssi;

        public EncAPValue(String bssid, BigInteger rssi) {
            mBssid = bssid;
            mRssi = rssi;
        }
    }

    public static class PineAPResponse {
        private int count;
        private String[] results;
    }

    // ************************************************************************

    public static int[] getImageSize(Uri uri, Context c) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //BitmapFactory.decodeFile(uri.getPath(), options);

        BitmapFactory.decodeResource(c.getResources(), R.drawable.ec_1, options);


        return new int[]{options.outWidth, options.outHeight};
    }

    /**
     * Creates a new ImageView instance associated as a child of layout.
     *
     * @param context which context to associate with ImageView...can just use
     *                `getApplicationContext()` or `this`
     * @param layout  parent layout
     * @return view with image resource set to a drawable
     */
    private static ImageView createNewMarker(Context context, RelativeLayout layout, int resId) {
        final ImageView ret = new ImageView(context);
        final int markerSize = context.getResources().getInteger(R.integer.map_marker_size) * 2;

        ret.setImageResource(resId);
        layout.addView(ret, new LayoutParams(markerSize, markerSize));

        return ret;
    }

    public static PhotoMarker createNewMarker(Context context, RelativeLayout wrapper, float x,
                                              float y, int resId) {
        return new PhotoMarker(createNewMarker(context, wrapper, resId), x, y, context
                .getResources().getInteger(R.integer.map_marker_size));
    }

    public static PhotoMarker createNewMarker(Context context, RelativeLayout wrapper, float x,
                                              float y) {
        return createNewMarker(context, wrapper, x, y, R.drawable.x);
    }

    public static Deque<PhotoMarker> generateMarkers(Deque<Coord> coordsToDraw, Context context,
                                                     RelativeLayout wrapper) {
        final Deque<PhotoMarker> data = new LinkedList<PhotoMarker>();
        final Set<Coord> points = new HashSet<Coord>();

        for (Coord tmpCoord : coordsToDraw) {
            if (!points.contains(tmpCoord)) {
                points.add(tmpCoord);
                PhotoMarker toAdd = createNewMarker(context, wrapper, tmpCoord.mX, tmpCoord.mY);
                data.add(toAdd);
            }
        }

        return data;
    }

    /**
     * Gather (x,y) points we have samples for in this map.
     */
    public static Deque<PhotoMarker> gatherSamples(ContentResolver cr, Context context,
                                                   RelativeLayout wrapper, long mapId) {
        // Get cursor into readings table
        // FIXME find out if possible to do a SQL DISTINCT query...
        final Cursor cursor = cr.query(DataProvider.READINGS_URI, new String[]{Database.Readings
                .MAP_X, Database.Readings.MAP_Y}, Database.Readings.MAP_ID + "=?", new
                String[]{Long.toString(mapId)}, null);

        // For readability, store these as local constants
        final int xColumn = cursor.getColumnIndex(Database.Readings.MAP_X);
        final int yColumn = cursor.getColumnIndex(Database.Readings.MAP_Y);

        // Load all the APs into our storage set
        final Deque<Coord> coordsToDraw = new LinkedList<Coord>();
        while (cursor.moveToNext()) {
            if (cursor.isNull(xColumn) || cursor.isNull(yColumn)) continue;

            coordsToDraw.add(new Coord(cursor.getFloat(xColumn), cursor.getFloat(yColumn)));
        }
        cursor.close();

        return generateMarkers(coordsToDraw, context, wrapper);
    }

    public static Deque<PhotoMarker> generateMarkers(Map<TrainLocation, ArrayList<APValue>>
                                                             coordsToDraw, Context context,
                                                     RelativeLayout wrapper) {
        return generateMarkers(new LinkedList<Coord>(coordsToDraw.keySet()), context, wrapper);
    }

    public static HashSet<String> gatherMetaMacs(ContentResolver cr) {
        HashSet<String> asdf = new HashSet<>();
        final Cursor cursor = cr.query(DataProvider.META_URI, null, null, null, null);
        final int macColumn = cursor.getColumnIndex(Database.Meta.MAC);
        while (cursor.moveToNext()) {
            if (cursor.isNull(macColumn)) continue;
            asdf.add(cursor.getString(macColumn));
        }
        cursor.close();
        System.out.println("gathermetamacs size = " + asdf.size());
        return asdf;
    }

    public static Map<TrainLocation, ArrayList<APValue>> gatherLocalizationData(ContentResolver
                                                                                        cr, long
            mapId) {
        final Cursor cursor = cr.query(DataProvider.READINGS_URI, new String[]{Database.Readings
                .MAP_X, Database.Readings.MAP_Y, Database.Readings.MAC, Database.Readings
                .SIGNAL_STRENGTH}, Database.Readings.MAP_ID + "=?", new String[]{Long.toString
                (mapId)}, null);

        // For readability, store these as local constants
        final int xColumn = cursor.getColumnIndex(Database.Readings.MAP_X);
        final int yColumn = cursor.getColumnIndex(Database.Readings.MAP_Y);
        final int bssidColumn = cursor.getColumnIndex(Database.Readings.MAC);
        final int rssiColumn = cursor.getColumnIndex(Database.Readings.SIGNAL_STRENGTH);

        // Cache results of cursor result in a more convenient data structure
        final Map<TrainLocation, ArrayList<APValue>> data = new HashMap<TrainLocation,
                ArrayList<APValue>>();
        int j = 0;
        int i = 0;
        while (cursor.moveToNext()) {
            if (cursor.isNull(xColumn) || cursor.isNull(yColumn)) continue;


            TrainLocation loc = new TrainLocation(cursor.getFloat(xColumn), cursor.getFloat
                    (yColumn));
            //Log.d("x and y", cursor.getFloat(xColumn) + " " + cursor.getFloat(yColumn));

            APValue ap = new APValue(cursor.getString(bssidColumn), cursor.getInt(rssiColumn));
            //Log.d("x and y of AP", cursor.getString(bssidColumn) + " " + cursor.getFloat
            // (rssiColumn));


            if (data.containsKey(loc)) {
                data.get(loc).add(ap);
                j++;
            } else {
                ArrayList<APValue> new_ = new ArrayList<APValue>();
                new_.add(ap);
                data.put(loc, new_);
            }

            //Log.d("CURSOR", i + "");
            i++;
        }
        cursor.close();
        //Log.d("TOTAL IN IF: ", " " + j);

        return data;
    }

    private static Map<TrainLocation, ArrayList<APValue>> gatherFileData(long mapId) {


        return null;

    }


    //	public static int[] getImageSize(Drawable image) {
    //		int iHeight = image.getIntrinsicHeight();
    //		int iWidth = image.getIntrinsicWidth();
    //		return new int[] {iWidth , iHeight  };
    //	}

}
