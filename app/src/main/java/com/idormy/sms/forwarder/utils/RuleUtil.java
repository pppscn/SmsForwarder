package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.RuleTable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "SynchronizeOnNonFinalField"})
public class RuleUtil {
    static final String TAG = "RuleUtil";
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

    public static long addRule(RuleModel ruleModel) {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RuleTable.RuleEntry.COLUMN_NAME_FILED, ruleModel.getFiled());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_CHECK, ruleModel.getCheck());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_VALUE, ruleModel.getValue());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_SENDER_ID, ruleModel.getSenderId());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT, ruleModel.getSimSlot());

        // Insert the new row, returning the primary key value of the new row

        return db.insert(RuleTable.RuleEntry.TABLE_NAME, null, values);
    }

    public static long updateRule(RuleModel ruleModel) {
        if (ruleModel == null) return 0;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RuleTable.RuleEntry.COLUMN_NAME_FILED, ruleModel.getFiled());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_CHECK, ruleModel.getCheck());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_VALUE, ruleModel.getValue());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_SENDER_ID, ruleModel.getSenderId());
        values.put(RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT, ruleModel.getSimSlot());

        String selection = RuleTable.RuleEntry._ID + " = ? ";
        String[] whereArgs = {String.valueOf(ruleModel.getId())};

        return db.update(RuleTable.RuleEntry.TABLE_NAME, values, selection, whereArgs);
    }

    public static int delRule(Long id) {
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + RuleTable.RuleEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));

        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);
        // Issue SQL statement.
        return db.delete(RuleTable.RuleEntry.TABLE_NAME, selection, selectionArgs);

    }

    public static List<RuleModel> getRule(Long id, String key) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                BaseColumns._ID,
                RuleTable.RuleEntry.COLUMN_NAME_FILED,
                RuleTable.RuleEntry.COLUMN_NAME_CHECK,
                RuleTable.RuleEntry.COLUMN_NAME_VALUE,
                RuleTable.RuleEntry.COLUMN_NAME_SENDER_ID,
                RuleTable.RuleEntry.COLUMN_NAME_TIME,
                RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT,
        };
        // Define 'where' part of query.
        String selection = " 1 = 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + RuleTable.RuleEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));
        }

        if (key != null) {
            // Define 'where' part of query.
            if (key.equals("SIM1") || key.equals("SIM2")) {
                selection += " and " + RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT + " IN ( 'ALL', ? ) ";
            } else {
                selection += " and " + RuleTable.RuleEntry.COLUMN_NAME_VALUE + " LIKE ? ";
            }
            // Specify arguments in placeholder order.
            selectionArgList.add(key);
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RuleTable.RuleEntry._ID + " DESC";

        Cursor cursor = db.query(
                RuleTable.RuleEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        List<RuleModel> tRules = new ArrayList<>();
        while (cursor.moveToNext()) {

            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry._ID));
            String itemFiled = cursor.getString(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_FILED));
            String itemCheck = cursor.getString(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_CHECK));
            String itemValue = cursor.getString(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_VALUE));
            long itemSenderId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_SENDER_ID));
            long itemTime = cursor.getLong(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_TIME));
            String itemSimSlot = cursor.getString(
                    cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT));

            Log.d(TAG, "getRule: itemId" + itemId);
            RuleModel ruleModel = new RuleModel();
            ruleModel.setId(itemId);
            ruleModel.setFiled(itemFiled);
            ruleModel.setCheck(itemCheck);
            ruleModel.setValue(itemValue);
            ruleModel.setSenderId(itemSenderId);
            ruleModel.setTime(itemTime);
            ruleModel.setSimSlot(itemSimSlot);

            tRules.add(ruleModel);
        }
        cursor.close();
        return tRules;
    }

}
