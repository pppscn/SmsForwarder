package com.idormy.sms.forwarder.model;

import android.provider.BaseColumns;

public final class LogTable {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private LogTable() {
    }

    /* Inner class that defines the table contents */
    public static class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "log";
        public static final String COLUMN_NAME_FROM = "l_from";
        public static final String COLUMN_NAME_CONTENT = "content";
        public static final String COLUMN_NAME_RULE_ID = "rule_id";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_SIM_INFO = "sim_info";
        public static final String COLUMN_NAME_FORWARD_STATUS = "forward_status";
        public static final String COLUMN_NAME_FORWARD_RESPONSE = "forward_response";
    }
}
