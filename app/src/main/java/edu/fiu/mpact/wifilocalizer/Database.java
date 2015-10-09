package edu.fiu.mpact.wifilocalizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Locale;

public class Database extends SQLiteOpenHelper {
    private static Database mInstance = null;

    protected static final String DB_NAME = "LocalizationData.db";
    protected static final int DB_VERSION = 4;

    public static final String[] TABLES = {Maps.TABLE_NAME, Readings.TABLE_NAME, Meta.TABLE_NAME};
    private static final String[] SCHEMAS = {Maps.SCHEMA, Readings.SCHEMA, Meta.SCHEMA};

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

    public static Database getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new Database(ctx.getApplicationContext());
        }
        return mInstance;
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

    public String composeJSONfromSQLite() {
        ArrayList<ContentValues> wordList;
        wordList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM Readings where up_status = 0";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        //Cursor cursor = database.query(Readings.TABLE_NAME,null,selectQuery,null,null,null,null);
        if (cursor.moveToFirst()) {
            do {
                ContentValues cv = new ContentValues();
                cv.put("id", cursor.getLong(cursor.getColumnIndex(Readings.ID)));
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
                wordList.add(cv);
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();
        Gson gson = new GsonBuilder().create();
        //Use GSON to serialize Array List to JSON
        return gson.toJson(wordList);
    }

    public int dbSyncCount() {
        String selectQuery = "SELECT  * FROM Readings where up_status = 0";
        SQLiteDatabase database = this.getWritableDatabase();

        Cursor cursor = database.rawQuery(selectQuery, null);
        final int count = cursor.getCount();
        cursor.close();

        database.close();
        return count;
    }


    // need to fix id stuff
    public void updateSyncStatus(String id, String status) {
        Log.d("updateSyncStatus", "id = " + id + " status = " + status);
        SQLiteDatabase database = this.getWritableDatabase();
        String updateQuery = "Update Readings set up_status = " + Integer.valueOf(status) + " " +
                "where _id=" + Integer.valueOf(id);
        Log.d("query", updateQuery);
        database.execSQL(updateQuery);
        database.close();
    }


    // this is for databasemanager
    public ArrayList<Cursor> getData(String maxQuery) {
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[]{"mesage"};
        //an array list of cursor to save three cursors ec_1 has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<>(2);
        MatrixCursor Cursor2 = new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try {
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[]{"Success"});

            alc.set(1, Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0, c);
                c.moveToFirst();

                return alc;
            }
            return alc;
        } catch (SQLException sqlEx) {
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the
            // arraylist
            Cursor2.addRow(new Object[]{"" + sqlEx.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        } catch (Exception ex) {

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the
            // arraylist
            Cursor2.addRow(new Object[]{"" + ex.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        }
    }
}
