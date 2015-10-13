package edu.fiu.mpact.wifilocalizer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.wifi.ScanResult;

import org.javatuples.Quartet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ScanResultBuffer {
    protected final long mMapId;
    private final Deque<Quartet<List<ScanResult>, float[], Long, Utils.PineappleResponse>> mStash
            = new ArrayDeque<>();
    private Deque<ContentValues> mToCommit = new ArrayDeque<>();
    private Deque<ContentValues> mProbesToCommit = new ArrayDeque<>();

    public ScanResultBuffer(long mapId) {
        mMapId = mapId;
    }

    public int removeByCoordinate(float x, float y) {
        final Deque<ContentValues> commitCopy = new ArrayDeque<>(mToCommit.size());

        for (ContentValues values : mToCommit) {
            if (values.getAsFloat(Database.Readings.MAP_X) != x && values.getAsFloat(Database
                    .Readings.MAP_Y) != y) {
                commitCopy.add(values);
            }
        }

        final int diff = mToCommit.size() - commitCopy.size();
        mToCommit = commitCopy;

        return diff;
    }

    public void clearStash() {
        mStash.clear();
    }

    public int stashScanResults(List<ScanResult> resultsToStash, float[] loc, Utils
            .PineappleResponse response) {
        mStash.add(new Quartet<>(resultsToStash, loc, System.currentTimeMillis(), response));

        return resultsToStash.size();
    }

    public int saveStash() {
        int rowsInserted = 0;

        for (Quartet<List<ScanResult>, float[], Long, Utils.PineappleResponse> t : mStash) {
            rowsInserted += insertScanResults(t.getValue0(), t.getValue1(), t.getValue2());
            insertProbeResults(t.getValue1(), t.getValue3());
        }

        return rowsInserted;
    }

    private ContentValues srToCv(ScanResult result, float[] loc, long time) {
        final ContentValues values = new ContentValues();
        values.put(Database.Readings.DATETIME, time);
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

        for (ScanResult result : results) {
            mToCommit.add(srToCv(result, location, time));
            rowsInserted++;
        }

        return rowsInserted;
    }

    public int insertProbeResults(float[] location, Utils.PineappleResponse response) {
        if (response == null)
            return 0;

        int rowsInserted = 0;

        for (String[] s : response.getData()) {
            final ContentValues values = new ContentValues();
            values.put(Database.Probes.MAP_X, location[0]);
            values.put(Database.Probes.MAP_Y, location[1]);
            values.put(Database.Probes.SIGNAL_STRENGTH, s[0]);
            values.put(Database.Probes.FINGERPRINT, s[1]);
            values.put(Database.Probes.MAP_ID, mMapId);
            mProbesToCommit.add(values);

            rowsInserted++;
        }

        return rowsInserted;
    }

    /**
     * Bulk insert contents of mCachedResults into Readings table.
     *
     * @param resolver ContentResolver to use to save
     * @return number of newly inserted rows
     */
    public int saveTrainingToDatabase(ContentResolver resolver) {
        return resolver.bulkInsert(DataProvider.READINGS_URI, mToCommit.toArray(new
                ContentValues[]{}));
    }
}
