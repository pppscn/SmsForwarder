package com.idormy.sms.forwarder.model;

import android.provider.BaseColumns;

public final class SenderTable {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SenderTable() {
    }

    /* Inner class that defines the table contents */
    public static class SenderEntry implements BaseColumns {
        public static final String TABLE_NAME = "sender";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_JSON_SETTING = "json_setting";
        public static final String COLUMN_NAME_TIME = "time";
    }
}
