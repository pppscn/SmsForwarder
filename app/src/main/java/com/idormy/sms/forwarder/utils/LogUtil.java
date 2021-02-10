package com.idormy.sms.forwarder.utils;

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

public class LogUtil {
    static String TAG = "LogUtil";
    static Boolean hasInit = false;

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

    public static long addLog(LogModel logModel) {
        Log.i(TAG, "addLog logModel: " + logModel);
        //不保存转发消息
        if (logModel == null) return 0;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LogTable.LogEntry.COLUMN_NAME_FROM, logModel.getFrom());
        values.put(LogTable.LogEntry.COLUMN_NAME_CONTENT, logModel.getContent());
        values.put(LogTable.LogEntry.COLUMN_NAME_RULE_ID, logModel.getRuleId());

        // Insert the new row, returning the primary key value of the new row

        return db.insert(LogTable.LogEntry.TABLE_NAME, null, values);
    }

    public static int delLog(Long id, String key) {
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
        String[] selectionArgs = selectionArgList.toArray(new String[selectionArgList.size()]);
        // Issue SQL statement.
        return db.delete(LogTable.LogEntry.TABLE_NAME, selection, selectionArgs);

    }

    public static List<LogVo> getLog(Long id, String key) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                LogTable.LogEntry.TABLE_NAME + "." + BaseColumns._ID,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FROM,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_TIME,
                LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_CONTENT,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_FILED,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_CHECK,
                RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_VALUE,
                SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_NAME,
                SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_TYPE
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
        String[] selectionArgs = selectionArgList.toArray(new String[selectionArgList.size()]);

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
                String itemfrom = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_FROM));
                String content = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_CONTENT));
                String time = cursor.getString(
                        cursor.getColumnIndexOrThrow(LogTable.LogEntry.TABLE_NAME + "." + LogTable.LogEntry.COLUMN_NAME_TIME));
                String ruleFiled = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_FILED));
                String ruleCheck = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_CHECK));
                String ruleValue = cursor.getString(
                        cursor.getColumnIndexOrThrow(RuleTable.RuleEntry.TABLE_NAME + "." + RuleTable.RuleEntry.COLUMN_NAME_VALUE));
                String senderName = cursor.getString(
                        cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_NAME));
                Integer senderType = cursor.getInt(
                        cursor.getColumnIndexOrThrow(SenderTable.SenderEntry.TABLE_NAME + "." + SenderTable.SenderEntry.COLUMN_NAME_TYPE));

                Log.d(TAG, "getLog: time" + time);
                String rule = RuleModel.getRuleMatch(ruleFiled, ruleCheck, ruleValue) + " 转发到 " + senderName;
//                String rule = time+" 转发到 "+senderName;
                int senderImageId = SenderModel.getImageId(senderType);
                LogVo logVo = new LogVo(itemfrom, content, time, rule, senderImageId);

                LogVos.add(logVo);
            } catch (Exception e) {
                Log.i(TAG, "getLog e:" + e.getMessage());
            }

        }


        cursor.close();

        return LogVos;
    }

}
