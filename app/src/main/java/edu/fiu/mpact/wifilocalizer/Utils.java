package edu.fiu.mpact.wifilocalizer;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.math.BigInteger;
import java.util.HashSet;
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

        public static final String PINEAPPLE_URL = "http://172.16.42.1";
        public static final String PINEAPPLE_SCRAPER_PORT = "8000";
        public static final String PINEAPPLE_SERVER_URL = PINEAPPLE_URL + ":" + PINEAPPLE_SCRAPER_PORT;

        public static final String METADATA_URL = "http://eic15.eng.fiu.edu/wifiloc/getmeta.php";
        public static final String POINTS_URL = "http://eic15.eng.fiu.edu/wifiloc/getpoints.php";
        public static final String DELETE_URL = "http://eic15.eng.fiu.edu/wifiloc/deletereading.php";
        public static final String INSERT_URL = "http://eic15.eng.fiu.edu/wifiloc/insertreading.php";
    }

    /**
     * Create hint dialogs with an optional checkbox to hide all. If the box has already been
     * shown once or the user has opted to not receive hints, we will not show. The method does
     * call the .show() method of the dialog.
     *
     * @param context context of the running activity
     * @param key     the SharedPreferences key of the boolean setting to check
     * @param res     the resource id of the hint string if
     * @return the value of the preference given by key
     */
    public static boolean showHelpOnFirstRun(Context context, String key, int res) {
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
        public LocalizationData.Location trainLocation;
        public BigInteger dist;
        public int matches;

        public EncTrainDistMatchPair(LocalizationData.Location t, BigInteger d, int m) {
            trainLocation = t;
            dist = d;
            matches = m;
        }
    }


    public static class EncTrainDistPair {
        public LocalizationData.Location trainLocation;
        public BigInteger dist;

        public EncTrainDistPair(LocalizationData.Location t, BigInteger d) {
            trainLocation = t;
            dist = d;
        }
    }


    public static class TrainDistPair implements Comparable<TrainDistPair> {
        public LocalizationData.Location trainLocation;
        public double dist;

        public TrainDistPair(LocalizationData.Location t, double d) {
            trainLocation = t;
            dist = d;
        }

        @Override
        public int compareTo(@NonNull TrainDistPair another) {
            return dist < another.dist ? -1 : dist > another.dist ? 1 : 0;
        }
    }


    public class PineappleResponse {
        public int count = 0;
        public String[] results;

        PineappleResponse() {
        }

        public String[] parse(String s) {
            final String[] split = s.split(" ", 2);
            return new String[] {split[0], split[1]};
        }

        public String[][] getData() {
            String[][] ret = new String[results.length][];

            for (int i = 0; i < ret.length; i++) {
                ret[i] = parse(results[i]);
            }

            return ret;
        }
    }

    // ************************************************************************

    /**
     * Returns a pair of (width, height) representing the dimensions of an image. Right now just
     * assumes all images are the same size as R.drawable.ec_1
     *
     * @param unused unused
     * @param c any context to give to BitmapFactory.decodeResource
     * @return the pair of pixel width x height
     */
    @SuppressWarnings("unused")
    public static int[] getImageSize(Uri unused, Context c) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(c.getResources(), R.drawable.ec_1, options);

        return new int[] {options.outWidth, options.outHeight};
    }

    /**
     * Creates a new ImageView instance associated as a child of layout.
     *
     * @param context which context to associate with ImageView
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

    public static PhotoMarker createNewMarker(Context context, RelativeLayout wrapper, float x, float y, int resId) {
        return new PhotoMarker(createNewMarker(context, wrapper, resId), x, y, context
                .getResources().getInteger(R.integer.map_marker_size));
    }

    public static PhotoMarker createNewMarker(Context context, RelativeLayout wrapper, float x, float y) {
        return createNewMarker(context, wrapper, x, y, R.drawable.x);
    }

    @NonNull
    public static Set<String> gatherMetaMacs(ContentResolver cr) {
        final Set<String> macs = new HashSet<>();

        final Cursor cursor = cr.query(DataProvider.META_URI, null, null, null, null);
        if (cursor == null) return macs;
        final int macColumn = cursor.getColumnIndex(Database.Meta.MAC);

        while (cursor.moveToNext()) {
            if (cursor.isNull(macColumn)) {
                continue;
            }

            macs.add(cursor.getString(macColumn));
        }
        cursor.close();

        return macs;
    }
}
