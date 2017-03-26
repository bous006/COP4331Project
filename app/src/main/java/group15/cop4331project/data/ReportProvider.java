package group15.cop4331project.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import group15.cop4331project.data.ReportContract.ReportEntry;
/**
 * Created by bous006 on 3/4/2017.
 */

public class ReportProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = ReportProvider.class.getSimpleName();

    //might not need this and instead just use REPORTS (WHERE id = userID) or something
    /** URI matcher code for the content URI for "My Reports" the reports table */
    private static final int MY_REPORTS = 100;

    /** URI matcher code for the content URI for the reports table */
    private static final int REPORTS = 100;

    /** URI matcher code for the content URI for a single report in the reports table */
    private static final int REPORT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    // Static initializer. This is run the first time anything is called from this class.
    static {
        //this is used to provide access to MULTIPLE rows of the reports table
        sUriMatcher.addURI(ReportContract.CONTENT_AUTHORITY, ReportContract.PATH_REPORTS, REPORTS);

        //this is used to provide access to ONE row of the reports table
        sUriMatcher.addURI(ReportContract.CONTENT_AUTHORITY, ReportContract.PATH_REPORTS + "/#", REPORT_ID);
    }

    /** This is the database helper */
    private ReportDbHelper mDbHelper;


    @Override
    public boolean onCreate() {
        mDbHelper = new ReportDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        //Get a readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        //This cursor will hold the result of the query
        Cursor cursor;

        //figure out if the uri matcher can match the uri to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORTS:
                // For the REPORTS code , query the reports table directly with the given
                // arguments. This cursor could contain multiple rows of the reports table
                cursor = database.query(ReportContract.ReportEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case REPORT_ID:
                // For the REPORT_IF code we need to extract the ID that is asked for and
                // here is an explanation that i found online
                /**
                 * // the selection will be "_id=?" and the selection argument will be a
                 * // String array containing the actual ID of 3 in this case.
                 * //
                 * // For every "?" in the selection, we need to have an element in the selection
                 * // arguments that will fill in the "?". Since we have 1 question mark in the
                 * // selection, we have 1 String in the selection arguments' String array.
                 */
                selection = ReportContract.ReportEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(ReportEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORTS:
                return ReportEntry.CONTENT_LIST_TYPE;
            case REPORT_ID:
                return ReportEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORTS:
                return insertReport(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertReport(Uri uri, ContentValues values) {

        //Check that the name is not null
        String name = values.getAsString(ReportContract.ReportEntry.COLUMN_REPORT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Report requires a name");
        }

        // Check that the gender is valid
        Integer type = values.getAsInteger(ReportEntry.COLUMN_REPORT_TYPE);
        if (type == null || !ReportEntry.isValidType(type)) {
            throw new IllegalArgumentException("Report requires valid type");
        }

        // Check that the gender is valid
        Long date = values.getAsLong(ReportEntry.COLUMN_REPORT_DATE);
        if (date == null) {
            throw new IllegalArgumentException("Report requires valid type");
        }

        //Check that the description is not null
        String description = values.getAsString(ReportContract.ReportEntry.COLUMN_REPORT_DESCRIPTION);
        if (description == null) {
            throw new IllegalArgumentException("Report requires a description");
        }

        // Get a writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //insert the new report into the database with the given values
        long id = database.insert(ReportContract.ReportEntry.TABLE_NAME, null, values);

        //if the ID is -1, then the insertion failed so we should log an error
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the report content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORTS:
                //delete all rows that match teh selection and selectionargs
                rowsDeleted = database.delete(ReportContract.ReportEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case REPORT_ID:
                //delete a single row given by teh ID in the URI
                selection = ReportContract.ReportEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ReportContract.ReportEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        //if any rows were deleted then notify all listeners of such
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case REPORTS:
                return updateReport(uri, contentValues, selection, selectionArgs);
            case REPORT_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ReportContract.ReportEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateReport(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateReport(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        //Check that the name is not null
        String name = values.getAsString(ReportContract.ReportEntry.COLUMN_REPORT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Report requires a name");
        }

        // check that the type value is valid.
        if (values.containsKey(ReportEntry.COLUMN_REPORT_TYPE)) {
            Integer type = values.getAsInteger(ReportEntry.COLUMN_REPORT_TYPE);
            if (type == null || !ReportEntry.isValidType(type)) {
                throw new IllegalArgumentException("Report requires valid type");
            }
        }

        // Check that the gender is valid
        Long date = values.getAsLong(ReportEntry.COLUMN_REPORT_DATE);
        if (date == null) {
            throw new IllegalArgumentException("Report requires valid type");
        }

        //Check that the description is not null
        String description = values.getAsString(ReportContract.ReportEntry.COLUMN_REPORT_DESCRIPTION);
        if (description == null) {
            throw new IllegalArgumentException("Report requires a description");
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ReportContract.ReportEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }
}
