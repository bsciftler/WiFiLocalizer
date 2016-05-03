package edu.fiu.mpact.wifilocalizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.swagger.client.model.Reading;


public class Database extends SQLiteOpenHelper {
    protected static final String DB_NAME = "LocalizationData.db";
    protected static final int DB_VERSION = 5;
    public static final String[] TABLES = {Maps.TABLE_NAME, Readings.TABLE_NAME, Meta.TABLE_NAME,
        Probes.TABLE_NAME};
    private static final String[] SCHEMAS = {Maps.SCHEMA, Readings.SCHEMA, Meta.SCHEMA, Probes
        .SCHEMA};


    public static class Maps {
        public static final String TABLE_NAME = "Maps";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String DATE_ADDED = "date_added";
        public static final String DATA = "data";
        private static final String ID_COLUMN = ID + " INTEGER PRIMARY KEY AUTOINCREMENT";
        private static final String NAME_COLUMN = NAME + " TEXT NOT NULL";
        private static final String DATE_ADDED_COLUMN = DATE_ADDED + " INTEGER NOT NULL";
        private static final String DATA_COLUMN = DATA + " TEXT NOT NULL";
        private static final String SCHEMA = generateSchema(TABLE_NAME, ID_COLUMN, NAME_COLUMN,
            DATE_ADDED_COLUMN, DATA_COLUMN);
    }


    public static class Meta {
        public static final String TABLE_NAME = "Meta";
        public static final String ID = "_id";
        public static final String MAC = "mac";
        private static final String ID_COLUMN = ID + " INTEGER PRIMARY KEY";
        private static final String MAC_COLUMN = MAC + " TEXT_NOT NULL";
        private static final String SCHEMA = generateSchema(TABLE_NAME, ID_COLUMN, MAC_COLUMN);
    }


    public static class Readings {
        public static final String TABLE_NAME = "Readings";
        public static final String ID = "_id";
        public static final String DATETIME = "datetime";
        public static final String MAP_X = "mapx";
        public static final String MAP_Y = "mapy";
        public static final String SIGNAL_STRENGTH = "rss";
        public static final String AP_NAME = "ap_name";
        public static final String MAC = "mac";
        public static final String MAP_ID = "map";
        public static final String UPDATE_STATUS = "up_status";
        private static final String ID_COLUMN = ID + " INTEGER PRIMARY KEY AUTOINCREMENT";
        private static final String DATETIME_COLUMN = DATETIME + " INTEGER NOT NULL";
        private static final String MAP_X_COLUMN = MAP_X + " FLOAT";
        private static final String MAP_Y_COLUMN = MAP_Y + " FLOAT";
        private static final String SIGNAL_STRENGTH_COLUMN = SIGNAL_STRENGTH + " INTEGER NOT NULL";
        private static final String AP_NAME_COLUMN = AP_NAME + " TEXT NOT NULL";
        private static final String MAC_COLUMN = MAC + " TEXT NOT NULL";
        private static final String MAP_ID_COLUMN = MAP_ID + " INTEGER NOT NULL";
        private static final String UPDATE_STATUS_COLUMN = UPDATE_STATUS + " INTEGER NOT NULL";
        private static final String MAP_ID_FOREIGN_COLUMN = generateForeignKeyColumn(MAP_ID, Maps
            .TABLE_NAME, Maps.ID);
        private static final String SCHEMA = generateSchema(TABLE_NAME, ID_COLUMN,
            DATETIME_COLUMN, MAP_X_COLUMN, MAP_Y_COLUMN, SIGNAL_STRENGTH_COLUMN,
            AP_NAME_COLUMN, MAC_COLUMN, MAP_ID_COLUMN, UPDATE_STATUS_COLUMN,
            MAP_ID_FOREIGN_COLUMN);
    }


    public static class Probes {
        public static final String TABLE_NAME = "Probes";
        public static final String ID = "_id";
        public static final String MAP_X = "mapx";
        public static final String MAP_Y = "mapy";
        public static final String SIGNAL_STRENGTH = "rss";
        public static final String FINGERPRINT = "fingerp";
        public static final String MAP_ID = "map";
        private static final String ID_COLUMN = ID + " INTEGER PRIMARY KEY";
        private static final String MAP_X_COLUMN = MAP_X + " FLOAT";
        private static final String MAP_Y_COLUMN = MAP_Y + " FLOAT";
        private static final String SIGNAL_STRENGTH_COLUMN = SIGNAL_STRENGTH + " INTEGER NOT NULL";
        private static final String FINGERPRINT_COLUMN = FINGERPRINT + " TEXT";
        private static final String MAP_ID_COLUMN = MAP_ID + " INTEGER NOT NULL";
        private static final String MAP_ID_FOREIGN_COLUMN = generateForeignKeyColumn(MAP_ID, Maps
            .TABLE_NAME, Maps.ID);
        private static final String SCHEMA = generateSchema(TABLE_NAME, ID_COLUMN,
            MAP_X_COLUMN, MAP_Y_COLUMN, SIGNAL_STRENGTH_COLUMN,
            FINGERPRINT_COLUMN, MAP_ID_COLUMN, MAP_ID_FOREIGN_COLUMN);
    }

    // ***********************************************************************

    public Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Database.onCreate", "*** Creating Tables ***");

        for (final String schema : SCHEMAS) {
            Log.d("Database.onCreate", "Command = " + schema);
            db.execSQL(schema);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("Database.onUpgrade", "Upgrading database from " + oldVersion + " to " + newVersion);

        Log.w("Database.onUpgrade", "Dropping and recreating tables!");
        for (final String tableName : TABLES)
            db.execSQL("DROP TABLE IF EXISTS " + tableName);

        onCreate(db);
    }

    protected static String generateForeignKeyColumn(String fk, String refTable, String refColumn) {
        final String format = "FOREIGN KEY(%s) REFERENCES %s(%s)";
        return String.format(Locale.US, format, fk, refTable, refColumn);
    }

    protected static String generateSchema(String tableName, String... columnDefs) {
        final StringBuilder ret = new StringBuilder();
        // Build beginning of CREATE statement
        ret.append("CREATE TABLE IF NOT EXISTS ");
        ret.append(tableName);
        ret.append('(');

        // Build columns of table
        for (int i = 0; i < columnDefs.length - 1; i++) {
            ret.append(columnDefs[i]);
            ret.append(',');
        }
        if (columnDefs.length > 0) ret.append(columnDefs[columnDefs.length - 1]);

        // Build end
        ret.append(')');
        ret.append(';');
        return ret.toString();
    }

    public Cursor getNonUploadedReadings() {
        return getReadableDatabase().query(Readings.TABLE_NAME, null, Readings.UPDATE_STATUS + "=?",
            new String[] {"0"}, null, null, null);
    }

    public List<Reading> readingsCursorToJson(Cursor cursor) {
        final List<Reading> readingList = new ArrayList<>();

        while (cursor.moveToNext()) {
            final Reading reading = new Reading();
            reading.setMapId((int)cursor.getLong(cursor.getColumnIndex(Readings.ID)));
            reading.setTimestamp();

            cv.put("datetime", cursor.getLong(cursor.getColumnIndex(Readings.DATETIME)));
            cv.put("mapx", cursor.getFloat(cursor.getColumnIndex(Readings.MAP_X)));
            cv.put("mapy", cursor.getFloat(cursor.getColumnIndex(Readings.MAP_Y)));
            cv.put("rss", cursor.getLong(cursor.getColumnIndex(Readings.SIGNAL_STRENGTH)));
            cv.put("ap_name", cursor.getString(cursor.getColumnIndex(Readings.AP_NAME)));
            cv.put("mac", cursor.getString(cursor.getColumnIndex(Readings.MAC)));
            cv.put("map", cursor.getLong(cursor.getColumnIndex(Readings.MAP_ID)));
            cv.put("sdk", Build.VERSION.SDK_INT);
            cv.put("manufacturer", Build.MANUFACTURER);
            cv.put("model", Build.MODEL);
            readingList.add(cv);
        }

        return readingList;
    }

    public void updateSyncStatus(String id, String status) {
        Log.d("updateSyncStatus", "id = " + id + " status = " + status);

        final ContentValues values = new ContentValues();
        values.put(Readings.UPDATE_STATUS, status);

        getWritableDatabase().update(Readings.TABLE_NAME, values, Readings.ID_COLUMN + "=?",
            new String[] {id});
    }
}
