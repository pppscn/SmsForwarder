package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.SenderTable;
import com.idormy.sms.forwarder.utils.DbHelper;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SynchronizeOnNonFinalField", "UnusedReturnValue", "unused"})
public class SenderUtil {
    static final String TAG = "SenderUtil";
    static Boolean hasInit = false;

    @SuppressLint("StaticFieldLeak")
    static Context context;
    static DbHelper dbHelper;
    static SQLiteDatabase db;

    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
            dbHelper = new DbHelper(context);
            // Gets the data repository in write mode
            db = dbHelper.getReadableDatabase();
        }

    }

    public static long addSender(SenderModel senderModel) {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SenderTable.SenderEntry.COLUMN_NAME_NAME, senderModel.getName());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_TYPE, senderModel.getType());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_STATUS, senderModel.getStatus());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING, senderModel.getJsonSetting());

        // Insert the new row, returning the primary key value of the new row

        return db.insert(SenderTable.SenderEntry.TABLE_NAME, null, values);
    }

    public static long updateSender(SenderModel senderModel) {
        if (senderModel == null) return 0;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SenderTable.SenderEntry.COLUMN_NAME_NAME, senderModel.getName());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_TYPE, senderModel.getType());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_STATUS, senderModel.getStatus());
        values.put(SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING, senderModel.getJsonSetting());

        String selection = SenderTable.SenderEntry._ID + " = ? ";
        String[] whereArgs = {String.valueOf(senderModel.getId())};

        return db.update(SenderTable.SenderEntry.TABLE_NAME, values, selection, whereArgs);
    }

    public static int delSender(Long id) {
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + SenderTable.SenderEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));

        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);
        // Issue SQL statement.
        return db.delete(SenderTable.SenderEntry.TABLE_NAME, selection, selectionArgs);

    }

    public static List<SenderModel> getSender(Long id, String key) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                BaseColumns._ID,
                SenderTable.SenderEntry.COLUMN_NAME_NAME,
                SenderTable.SenderEntry.COLUMN_NAME_TYPE,
                SenderTable.SenderEntry.COLUMN_NAME_STATUS,
                SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING,
                SenderTable.SenderEntry.COLUMN_NAME_TIME
        };
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + SenderTable.SenderEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));
        }

        if (key != null) {
            // Define 'where' part of query.
            selection = " and (" + SenderTable.SenderEntry.COLUMN_NAME_NAME + " LIKE ? or " + SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING + " LIKE ? ) ";
            // Specify arguments in placeholder order.
            selectionArgList.add(key);
            selectionArgList.add(key);
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                SenderTable.SenderEntry._ID + " DESC";

        Cursor cursor = db.query(
                SenderTable.SenderEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        List<SenderModel> tSenders = new ArrayList<>();
        while (cursor.moveToNext()) {

            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry._ID));
            String itemName = cursor.getString(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_NAME));
            int itemStatus = cursor.getInt(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_STATUS));
            int itemType = cursor.getInt(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_TYPE));
            String itemJsonSetting = cursor.getString(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING));
            long itemTime = cursor.getLong(
                    cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_TIME));
            Log.d(TAG, "getSender: itemId" + itemId);

            SenderModel senderModel = new SenderModel();
            senderModel.setId(itemId);
            senderModel.setName(itemName);
            senderModel.setStatus(itemStatus);
            senderModel.setType(itemType);
            senderModel.setJsonSetting(itemJsonSetting);
            senderModel.setTime(itemTime);

            tSenders.add(senderModel);
        }
        cursor.close();
        return tSenders;
    }

    public static int countSender(String key) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
        };
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();

        if (key != null) {
            // Define 'where' part of query.
            selection = " and (" + SenderTable.SenderEntry.COLUMN_NAME_NAME + " LIKE ? or " + SenderTable.SenderEntry.COLUMN_NAME_JSON_SETTING + " LIKE ? ) ";
            // Specify arguments in placeholder order.
            selectionArgList.add(key);
            selectionArgList.add(key);
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        // How you want the results sorted in the resulting Cursor

        Cursor cursor = db.query(
                SenderTable.SenderEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

}
