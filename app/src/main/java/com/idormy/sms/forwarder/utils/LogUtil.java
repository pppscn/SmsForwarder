package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.idormy.sms.forwarder.model.LogModel;
import com.idormy.sms.forwarder.model.LogTable;
import com.idormy.sms.forwarder.model.RuleModel;
import com.idormy.sms.forwarder.model.RuleTable;
import com.idormy.sms.forwarder.model.SenderModel;
import com.idormy.sms.forwarder.model.SenderTable;
import com.idormy.sms.forwarder.model.vo.LogVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class LogUtil {
    static final String TAG = "LogUtil";
    static Boolean hasInit = false;

    @SuppressLint("StaticFieldLeak")
    static Context context;
    static DbHelper dbHelper;
    static SQLiteDatabase db;

    public static void init(Context context1) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
            dbHelper = new DbHelper(context);
            // Gets the data repository in write mode
            db = dbHelper.getReadableDatabase();
        }

    }

    public static long addLog(LogModel logModel) {
        Log.i(TAG, "addLog logModel: " + logModel);
        //不保存转发消息
        if (logModel == null) return 0;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LogTable.LogEntry.COLUMN_NAME_FROM, logModel.getFrom());
        values.put(LogTable.LogEntry.COLUMN_NAME_CONTENT, logModel.getContent());
        values.put(LogTable.LogEntry.COLUMN_NAME_SIM_INFO, logModel.getSimInfo());
        values.put(LogTable.LogEntry.COLUMN_NAME_RULE_ID, logModel.getRuleId());

        // Insert the new row, returning the primary key value of the new row

        return db.insert(LogTable.LogEntry.TABLE_NAME, null, values);
    }

    public static void delLog(Long id, String key) {
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + LogTable.LogEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));
        }

        if (key != null) {
            // Define 'where' part of query.
            selection = " and (" + LogTable.LogEntry.COLUMN_NAME_FROM + " LIKE ? or " + LogTable.LogEntry.COLUMN_NAME_CONTENT + " LIKE ? ) ";
            // Specify arguments in placeholder order.
            selectionArgList.add(key);
            selectionArgList.add(key);
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);
        // Issue SQL statement.
        db.delete(LogTable.LogEntry.TABLE_NAME, selection, selectionArgs);

    }

    public static int updateLog(Long id, int forward_status, String forward_response) {
        if (id == null || id <= 0) return 0;

        String selection = LogTable.LogEntry._ID + " = ? ";
        List<String> selectionArgList = new ArrayList<>();
        selectionArgList.add(String.valueOf(id));

        ContentValues values = new ContentValues();
        values.put(LogTable.LogEntry.COLUMN_NAME_FORWARD_STATUS, forward_status);
        values.put(LogTable.LogEntry.COLUMN_NAME_FORWARD_RESPONSE, forward_response);

        String[] selectionArgs = selectionArgList.toArray(new String[0]);
        return db.update(LogTable.LogEntry.TABLE_NAME, values, selection, selectionArgs);

    }

    public static List<LogVo> getLog(Long id, String key) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                LogTable.LogEntry.TABLE_NAME + "." + BaseColumns._ID + " AS " + BaseColumns._ID,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FROM + " AS " + LogTable.LogEntry.COLUMN_NAME_FROM,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_TIME + " AS " + LogTable.LogEntry.COLUMN_NAME_TIME,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_CONTENT + " AS " + LogTable.LogEntry.COLUMN_NAME_CONTENT,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_SIM_INFO + " AS " + LogTable.LogEntry.COLUMN_NAME_SIM_INFO,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FORWARD_STATUS + " AS " + LogTable.LogEntry.COLUMN_NAME_FORWARD_STATUS,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FORWARD_RESPONSE + " AS " + LogTable.LogEntry.COLUMN_NAME_FORWARD_RESPONSE,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_FILED + " AS " + RuleTable.RuleEntry.COLUMN_NAME_FILED,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_CHECK + " AS " + RuleTable.RuleEntry.COLUMN_NAME_CHECK,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_VALUE + " AS " + RuleTable.RuleEntry.COLUMN_NAME_VALUE,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT + " AS " + RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT,
                SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_NAME + " AS " + SenderTable.SenderEntry.COLUMN_NAME_NAME,
                SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_TYPE + " AS " + SenderTable.SenderEntry.COLUMN_NAME_TYPE
        };
        // Define 'where' part of query.
        String selection = " 1 ";
        // Specify arguments in placeholder order.
        List<String> selectionArgList = new ArrayList<>();
        if (id != null) {
            // Define 'where' part of query.
            selection += " and " + LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry._ID + " = ? ";
            // Specify arguments in placeholder order.
            selectionArgList.add(String.valueOf(id));
        }

        if (key != null) {
            // Define 'where' part of query.
            selection = " and (" + LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FROM + " LIKE ? or " + LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_CONTENT + " LIKE ? ) ";
            // Specify arguments in placeholder order.
            selectionArgList.add(key);
            selectionArgList.add(key);
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry._ID + " DESC";

        Cursor cursor = db.query(
                // The table to query
                LogTable.LogEntry.TABLE_NAME
                        + " LEFT JOIN " + RuleTable.RuleEntry.TABLE_NAME + " ON " + LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_RULE_ID + "=" + RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry._ID
                        + " LEFT JOIN " + SenderTable.SenderEntry.TABLE_NAME + " ON " + SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry._ID + "=" + RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_SENDER_ID,
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );


        Log.d(TAG, "getLog: " + db.getPath());
        List<LogVo> LogVos = new ArrayList<>();

        Log.d(TAG, "getLog: itemId cursor" + Arrays.toString(cursor.getColumnNames()));
        while (cursor.moveToNext()) {
            try {
                Long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(BaseColumns._ID));
                String itemFrom = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_FROM));
                String content = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_CONTENT));
                String simInfo = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_SIM_INFO));
                String time = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_TIME));
                int forwardStatus = cursor.getInt(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_FORWARD_STATUS));
                String forwardResponse = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.COLUMN_NAME_FORWARD_RESPONSE));
                String ruleFiled = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_FILED));
                String ruleCheck = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_CHECK));
                String ruleValue = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_VALUE));
                String ruleSimSlot = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.COLUMN_NAME_SIM_SLOT));
                String senderName = cursor.getString(
                        cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_NAME));
                int senderType = cursor.getInt(
                        cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.COLUMN_NAME_TYPE));

                String rule = RuleModel.getRuleMatch(ruleFiled, ruleCheck, ruleValue, ruleSimSlot);
                if (senderName != null) rule += senderName.trim();

                int senderImageId = SenderModel.getImageId(senderType);
                LogVo logVo = new LogVo(itemId, itemFrom, content, simInfo, time, rule, senderImageId, forwardStatus, forwardResponse);
                LogVos.add(logVo);
            } catch (Exception e) {
                Log.e(TAG, "getLog e:" + e.getMessage());
            }

        }

        cursor.close();

        return LogVos;
    }

}
