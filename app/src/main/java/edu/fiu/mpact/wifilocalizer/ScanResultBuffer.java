package edu.fiu.mpact.wifilocalizer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.wifi.ScanResult;

import org.javatuples.Triplet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanResultBuffer {
    protected final long mMapId;
    private final Map<float[], Deque<ContentValues>> mToCommit = new HashMap<>();
    private final Deque<Triplet<List<ScanResult>, float[], Long>> mStash = new ArrayDeque<>();

    public ScanResultBuffer(long mapId) {
        mMapId = mapId;
    }

    public int removeByCoordinate(float x, float y) {
        final int removed = ((Deque) mToCommit.remove(new float[] {x, y})).size();
        return removed;
    }

    public void clearStash() {
        mStash.clear();
    }

    public int stashScanResults(List<ScanResult> resultsToStash, float[] loc) {
        mStash.add(new Triplet<>(resultsToStash, loc, System.currentTimeMillis()));
        return resultsToStash.size();
    }

    public int saveStash() {
        int rowsInserted = 0;

        for (Triplet<List<ScanResult>, float[], Long> t : mStash) {
            rowsInserted += insertScanResults(t.getValue0(), t.getValue1(), t.getValue2());
        }

        return rowsInserted;
    }

    public ContentValues srToCv(ScanResult result, float[] loc, long time) {
        final ContentValues values = new ContentValues();
        values.put(Database.Readings.DATETIME, System.currentTimeMillis());
        values.put(Database.Readings.MAP_X, loc[0]);
        values.put(Database.Readings.MAP_Y, loc[1]);
        values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
        values.put(Database.Readings.AP_NAME, result.SSID);
        values.put(Database.Readings.MAC, result.BSSID);
        values.put(Database.Readings.MAP_ID, mMapId);
        values.put(Database.Readings.UPDATE_STATUS, 0);

        return values;
    }

    public int insertScanResults(List<ScanResult> results, float[] location, long time) {
        int rowsInserted = 0;

        final Deque<ContentValues> deque;
        if (mToCommit.containsKey(location)) {
            deque = mToCommit.get(location);
        } else {
            deque = new ArrayDeque<>();
        }

        for (ScanResult result : results) {
            deque.add(srToCv(result, location, time));
            rowsInserted++;
        }

        mToCommit.put(location, deque);
        return rowsInserted;
    }

    /**
     * Bulk insert contents of mCachedResults into Readings table.
     *
     * @param resolver ContentResolver to use to save
     * @return numnber of newly inserted rows
     */
    public int saveTrainingToDatabase(ContentResolver resolver) {
        final ContentValues[] TEMPLATE = new ContentValues[0];
        final ContentValues[] valuesToInsert = mToCommit.values().toArray(TEMPLATE);

        return resolver.bulkInsert(DataProvider.READINGS_URI, valuesToInsert);
    }
}
