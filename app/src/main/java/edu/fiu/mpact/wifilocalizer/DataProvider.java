package edu.fiu.mpact.wifilocalizer;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;


@SuppressWarnings("ConstantConditions")
public class DataProvider extends ContentProvider {
    /**
     * This is the base string that all URIs must start as.
     */
    public static final String AUTHORITY = "edu.fiu.mpact.wifilocalizer.DataProvider";
    public static final Uri MAPS_URI = Uri.parse("content://" + AUTHORITY + "/" + Database.Maps.TABLE_NAME);
    public static final Uri META_URI = Uri.parse("content://" + AUTHORITY + "/" + Database.Meta.TABLE_NAME);
    public static final Uri READINGS_URI = Uri.parse("content://" + AUTHORITY + "/" + Database.Readings.TABLE_NAME);
    public static final Uri PROBES_URI = Uri.parse("content://" + AUTHORITY + "/" + Database.Probes.TABLE_NAME);
    public static final int MAPS = 1; // id for all getting all maps
    public static final String MAPS_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + Database.Maps.TABLE_NAME;
    public static final int MAPS_ID = 2; // id for a *single* map by its ID
    public static final String MAPS_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + Database.Maps.TABLE_NAME;
    public static final int META = 3;
    public static final String META_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + Database.Meta.TABLE_NAME;
    public static final int META_ID = 4;
    public static final String META_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + Database.Meta.TABLE_NAME;
    public static final int READINGS = 5;
    public static final String READINGS_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + Database.Readings.TABLE_NAME;
    public static final int READINGS_ID = 6;
    public static final String READINGS_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + Database.Readings.TABLE_NAME;
    public static final int PROBES = 7;
    public static final String PROBES_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + Database.Probes.TABLE_NAME;
    public static final int PROBES_ID = 8;
    public static final String PROBES_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + Database.Probes.TABLE_NAME;
    // match URIs to their constants
    private static final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mMatcher.addURI(AUTHORITY, Database.Maps.TABLE_NAME, MAPS);
        mMatcher.addURI(AUTHORITY, Database.Maps.TABLE_NAME + "/#", MAPS_ID);

        mMatcher.addURI(AUTHORITY, Database.Meta.TABLE_NAME, META);
        mMatcher.addURI(AUTHORITY, Database.Meta.TABLE_NAME + "/#", META_ID);
        mMatcher.addURI(AUTHORITY, Database.Readings.TABLE_NAME, READINGS);
        mMatcher.addURI(AUTHORITY, Database.Readings.TABLE_NAME + "/#", READINGS_ID);
        mMatcher.addURI(AUTHORITY, Database.Probes.TABLE_NAME, PROBES);
        mMatcher.addURI(AUTHORITY, Database.Probes.TABLE_NAME + "/#", PROBES_ID);
    }

    private Database mDb;

    @Override
    public boolean onCreate() {
        mDb = new Database(getContext());
        return true;
    }

    // ***********************************************************************

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (mMatcher.match(uri)) {
        case MAPS:
            queryBuilder.setTables(Database.Maps.TABLE_NAME);
            break;
        case MAPS_ID:
            queryBuilder.setTables(Database.Maps.TABLE_NAME);
            queryBuilder.appendWhere(Database.Maps.ID + "=" + uri.getLastPathSegment());
            break;
        case META:
            queryBuilder.setTables(Database.Meta.TABLE_NAME);
            break;
        case META_ID:
            queryBuilder.setTables(Database.Meta.TABLE_NAME);
            queryBuilder.appendWhere(Database.Meta.ID + "=" + uri.getLastPathSegment());
            break;
        case READINGS:
            queryBuilder.setTables(Database.Readings.TABLE_NAME);
            break;
        case READINGS_ID:
            queryBuilder.setTables(Database.Readings.TABLE_NAME);
            queryBuilder.appendWhere(Database.Readings.ID + "=" + uri.getLastPathSegment());
            break;
        case PROBES:
            queryBuilder.setTables(Database.Probes.TABLE_NAME);
            break;
        case PROBES_ID:
            queryBuilder.setTables(Database.Probes.TABLE_NAME);
            queryBuilder.appendWhere(Database.Probes.ID + "=" + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unmatchable URI " + uri);
        }

        final Cursor cursor = queryBuilder.query(mDb.getReadableDatabase(), projection,
                selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int numRows;
        String id;

        switch (mMatcher.match(uri)) {
        case MAPS:
            numRows = mDb.getWritableDatabase().delete(Database.Maps.TABLE_NAME, selection,
                    selectionArgs);
            break;
        case MAPS_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().delete(Database.Maps.TABLE_NAME, Database
                        .Maps.ID + "=" + id, null);
            else numRows = mDb.getWritableDatabase().delete(Database.Maps.TABLE_NAME, selection +
                    " and " + Database.Maps.ID + "=" + id, selectionArgs);
            break;
        case META:
            numRows = mDb.getWritableDatabase().delete(Database.Meta.TABLE_NAME, selection,
                    selectionArgs);
            break;
        case META_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().delete(Database.Meta.TABLE_NAME, Database
                        .Meta.ID + "=" + id, null);
            else numRows = mDb.getWritableDatabase().delete(Database.Meta.TABLE_NAME, selection +
                    " and " + Database.Meta.ID + "=" + id, selectionArgs);
            break;
        case READINGS:
            numRows = mDb.getWritableDatabase().delete(Database.Readings.TABLE_NAME, selection,
                    selectionArgs);
            break;
        case READINGS_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().delete(Database.Readings.TABLE_NAME, Database
                        .Readings.ID + "=" + id, null);
            else
                numRows = mDb.getWritableDatabase().delete(Database.Readings.TABLE_NAME,
                        selection + " and " + Database.Readings.ID + "=" + id, selectionArgs);
            break;
        case PROBES:
            numRows = mDb.getWritableDatabase().delete(Database.Probes.TABLE_NAME, selection,
                    selectionArgs);
            break;
        case PROBES_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().delete(Database.Probes.TABLE_NAME, Database
                        .Probes.ID + "=" + id, null);
            else numRows = mDb.getWritableDatabase().delete(Database.Probes.TABLE_NAME, selection +
                    " and " + Database.Probes.ID + "=" + id, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unmatchable URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mMatcher.match(uri)) {
        case MAPS:
            return MAPS_TYPE;
        case MAPS_ID:
            return MAPS_ID_TYPE;
        case META:
            return META_TYPE;
        case META_ID:
            return META_ID_TYPE;
        case READINGS:
            return READINGS_TYPE;
        case READINGS_ID:
            return READINGS_ID_TYPE;
        case PROBES:
            return PROBES_TYPE;
        case PROBES_ID:
            return PROBES_ID_TYPE;
        default:
            return null;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri ret;
        long rowId;

        switch (mMatcher.match(uri)) {
        case MAPS:
        case MAPS_ID:
            rowId = mDb.getWritableDatabase().insert(Database.Maps.TABLE_NAME, null, values);
            ret = ContentUris.withAppendedId(MAPS_URI, rowId);
            break;
        case READINGS:
        case READINGS_ID:
            rowId = mDb.getWritableDatabase().insert(Database.Readings.TABLE_NAME, null, values);
            ret = ContentUris.withAppendedId(READINGS_URI, rowId);
            break;
        case PROBES:
        case PROBES_ID:
            rowId = mDb.getWritableDatabase().insert(Database.Probes.TABLE_NAME, null, values);
            ret = ContentUris.withAppendedId(PROBES_URI, rowId);
            break;
        default:
            throw new IllegalArgumentException("Unmatchable URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ret;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int rows;
        final SQLiteDatabase db = mDb.getWritableDatabase();

        switch (mMatcher.match(uri)) {
        case READINGS:
            rows = 0;
            db.beginTransaction();

            try {
                for (ContentValues value : values) {
                    final long rowId = db.insertOrThrow(Database.Readings.TABLE_NAME, null, value);
                    if (rowId != -1) rows++;
                }

                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.w("bulkInsert", "failed to insert row at row #" + rows);
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return rows;
        case META:
            rows = 0;
            db.beginTransaction();

            try {
                for (ContentValues value : values) {
                    final long rowId = db.insertOrThrow(Database.Meta.TABLE_NAME, null, value);
                    if (rowId != -1) rows++;
                }

                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.w("bulkInsert", "failed to insert row at row #" + rows);
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return rows;
        case PROBES:
            rows = 0;
            db.beginTransaction();

            try {
                for (ContentValues value : values) {
                    final long rowId = db.insertOrThrow(Database.Probes.TABLE_NAME, null, value);
                    if (rowId != -1) rows++;
                }

                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.w("bulkInsert", "failed to insert row at row #" + rows);
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return rows;
        default:
            return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int numRows;
        String id;

        switch (mMatcher.match(uri)) {
        case MAPS:
            numRows = mDb.getWritableDatabase().update(Database.Maps.TABLE_NAME, values,
                    selection, selectionArgs);
            break;
        case MAPS_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().update(Database.Maps.TABLE_NAME, values,
                        Database.Maps.ID + "=" + id, null);
            else
                numRows = mDb.getWritableDatabase().update(Database.Maps.TABLE_NAME, values,
                        Database.Maps.ID + "=" + id + " and " + selection, selectionArgs);
            break;
        case READINGS:
            numRows = mDb.getWritableDatabase().update(Database.Readings.TABLE_NAME, values,
                    selection, selectionArgs);
            break;
        case READINGS_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().update(Database.Readings.TABLE_NAME, values,
                        Database.Readings.ID + "=" + id, null);
            else
                numRows = mDb.getWritableDatabase().update(Database.Readings.TABLE_NAME, values,
                        Database.Readings.ID + "=" + id + " and " + selection, selectionArgs);
            break;
        case PROBES:
            numRows = mDb.getWritableDatabase().update(Database.Probes.TABLE_NAME, values,
                    selection, selectionArgs);
            break;
        case PROBES_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection))
                numRows = mDb.getWritableDatabase().update(Database.Probes.TABLE_NAME, values,
                        Database.Probes.ID + "=" + id, null);
            else
                numRows = mDb.getWritableDatabase().update(Database.Probes.TABLE_NAME, values,
                        Database.Probes.ID + "=" + id + " and " + selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unmatchable URI " + uri);
        }

        return numRows;
    }
}
