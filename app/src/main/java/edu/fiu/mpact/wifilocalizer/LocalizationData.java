package edu.fiu.mpact.wifilocalizer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.RelativeLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.senab.photoview.PhotoMarker;


public class LocalizationData {
    private static final String[] PROJECTION = {
            Database.Readings.MAP_X, Database.Readings.MAP_Y,
            Database.Readings.MAC, Database.Readings.SIGNAL_STRENGTH};
    private static final String SELECTION = Database.Readings.MAP_ID + "=?";

    private Map<Location, Deque<AccessPoint>> mData = new HashMap<>();

    // ***********************************************************************

    /**
     * Load in data from file.
     *
     * @param inStream opened InputStream to the file to read
     * @param mapId    filter to only rows that have this mapId
     */
    LocalizationData(InputStream inStream, long mapId) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] lineList = line.split("\\|");

                if (Long.parseLong(lineList[8]) == mapId) {
                    Location loc = new Location(Float.valueOf(lineList[3]), Float.valueOf(lineList[4]));
                    AccessPoint ap = new AccessPoint(lineList[7], Integer.parseInt(lineList[5]));

                    if (mData.containsKey(loc)) {
                        mData.get(loc).add(ap);
                    } else {
                        Deque<AccessPoint> toAdd = new ArrayDeque<>();
                        toAdd.add(ap);
                        mData.put(loc, toAdd);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("loadFileData", "failed to read line", e);
        }
    }

    /**
     * Load in data from local application database.
     *
     * @param cr    ContentResolver to query
     * @param mapId filter to only rows that have this mapId
     */
    LocalizationData(ContentResolver cr, long mapId) {
        final Cursor cursor = cr.query(DataProvider.READINGS_URI,
                PROJECTION, SELECTION, new String[] {Long.toString(mapId)}, null);
        if (cursor == null) return;

        final int xColumn = cursor.getColumnIndex(Database.Readings.MAP_X);
        final int yColumn = cursor.getColumnIndex(Database.Readings.MAP_Y);
        final int bssidColumn = cursor.getColumnIndex(Database.Readings.MAC);
        final int rssiColumn = cursor.getColumnIndex(Database.Readings.SIGNAL_STRENGTH);

        while (cursor.moveToNext()) {
            if (cursor.isNull(xColumn) || cursor.isNull(yColumn)) continue;

            final Location loc = new Location(cursor.getFloat(xColumn), cursor.getFloat(yColumn));
            final AccessPoint ap = new AccessPoint(cursor.getString(bssidColumn), cursor.getInt(rssiColumn));

            if (mData.containsKey(loc)) {
                mData.get(loc).add(ap);
            } else {
                Deque<AccessPoint> toAdd = new ArrayDeque<>();
                toAdd.add(ap);
                mData.put(loc, toAdd);
            }
        }

        cursor.close();
    }

    public Deque<PhotoMarker> generateMarkers(Context context, RelativeLayout wrapper) {
        final Deque<Location> coordsToDraw = new ArrayDeque<>(mData.keySet());
        final Deque<PhotoMarker> data = new ArrayDeque<>();
        final Set<Location> points = new HashSet<>();

        for (Location tmpCoord : coordsToDraw) {
            if (!points.contains(tmpCoord)) {
                points.add(tmpCoord);
                data.add(Utils.createNewMarker(context, wrapper, tmpCoord.mX, tmpCoord.mY));
            }
        }

        return data;
    }

    public int numLocations() {
        return mData.size();
    }

    public void removeLocation(Location loc) {
        mData.remove(loc);
    }

    public Set<Location> getLocations() {
        return mData.keySet();
    }

    public Deque<AccessPoint> getAccessPoints(Location loc) {
        return mData.get(loc);
    }

    public static Set<String> getAccessPointBssids(Deque<AccessPoint> aps) {
        final Set<String> bssids = new HashSet<>(aps.size());
        for (AccessPoint ap : aps) bssids.add(ap.mBssid);
        return bssids;
    }

    public static Set<ScanResult> getCommonBssids(Set<String> bssidsOriginal, List<ScanResult> results) {
        final Set<String> bssids = new HashSet<>(bssidsOriginal);

        final Set<ScanResult> ret = new HashSet<>(results.size());
        for (ScanResult sr : results)
            if (bssids.contains(sr.BSSID))
                ret.add(sr);

        return ret;
    }

    // ***********************************************************************


    /**
     * We use this container class as the key for our data store, so we need to override .equals()
     * and .hashCode()
     */
    public static class Location {
        public float mX, mY;

        Location(float x, float y) {
            mX = x;
            mY = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Location)) return false;
            else if (obj == this) return true;

            Location t = (Location) obj;
            return this.mX == t.mX && this.mY == t.mY;
        }

        @Override
        public int hashCode() {
            int hash = 3;

            hash = 7 * hash + (int) this.mX;
            hash = 7 * hash + (int) this.mY;

            return hash;
        }
    }


    public static class AccessPoint {
        public String mBssid = "";
        public int mRssi = -1;

        public AccessPoint(String bssid, int rssi) {
            mBssid = bssid;
            mRssi = rssi;
        }
    }
}
