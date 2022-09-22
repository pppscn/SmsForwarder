package com.idormy.sms.forwarder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idormy.sms.forwarder.BuildConfig
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.dao.LogsDao
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.ext.Converters
import com.idormy.sms.forwarder.utils.DATABASE_NAME
import java.util.concurrent.Executors

@Database(
    entities = [Frpc::class, Logs::class, Rule::class, Sender::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun frpcDao(): FrpcDao
    abstract fun logsDao(): LogsDao
    abstract fun ruleDao(): RuleDao
    abstract fun senderDao(): SenderDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .allowMainThreadQueries() //TODO:允许主线程访问，后面再优化
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        //fillInDb(context.applicationContext)
                        db.execSQL(
                            """
INSERT INTO "Frpc" VALUES ('830b0a0e-c2b3-4f95-b3c9-55db12923d2e', '远程控制SmsForwarder', '[common]
#frps服务端公网IP
server_addr = 88.88.88.88
#frps服务端公网端口
server_port = 8888
#可选，建议启用
token = 88888888
#连接服务端的超时时间（增大时间避免frpc在网络未就绪的情况下启动失败）
dial_server_timeout = 60
#第一次登陆失败后是否退出
login_fail_exit = false

#[二选一即可]每台机器不可重复，通过 http://88.88.88.88:5000 访问
[SmsForwarder-TCP]
type = tcp
local_ip = 127.0.0.1
local_port = 5000
#只要修改下面这一行
remote_port = 5000

#[二选一即可]每台机器不可重复，通过 http://smsf.demo.com 访问
[SmsForwarder-HTTP]
type = http
local_ip = 127.0.0.1
local_port = 5000
#只要修改下面这一行
custom_domains = smsf.demo.com
', 0, '1651334400000')
""".trimIndent()
                        )
                    }
                })
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                )

            if (BuildConfig.DEBUG) {
                builder.setQueryCallback({ sqlQuery, bindArgs ->
                    println("SQL_QUERY: $sqlQuery\nBIND_ARGS: $bindArgs")
                }, Executors.newSingleThreadExecutor())
            }

            return builder.build()
        }

        //转发日志添加SIM卡槽信息
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table log add column sim_info TEXT ")
            }
        }

        //转发规则添加SIM卡槽信息
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column sim_slot TEXT NOT NULL DEFAULT 'ALL' ")
            }
        }

        //转发日志添加转发状态与返回信息
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table log add column forward_status INTEGER NOT NULL DEFAULT 1 ")
                database.execSQL("Alter table log add column forward_response TEXT NOT NULL DEFAULT 'ok' ")
            }
        }

        //转发规则添加规则自定义信息模板
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column sms_template TEXT NOT NULL DEFAULT '' ")
            }
        }

        //增加转发规则与日志的分类
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column type TEXT NOT NULL DEFAULT 'sms' ")
                database.execSQL("Alter table log add column type TEXT NOT NULL DEFAULT 'sms' ")
            }
        }

        //转发规则添加正则替换内容
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column regex_replace TEXT NOT NULL DEFAULT '' ")
            }
        }

        //更新日志表状态：0=失败，1=待处理，2=成功
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("update log set forward_status = 2 where forward_status = 1 ")
            }
        }

        //规则/通道状态：0=禁用，1=启用
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column status INTEGER NOT NULL DEFAULT 1 ")
                database.execSQL("update sender set status = 1 ")
            }
        }

        //从SQLite迁移到 Room
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
CREATE TABLE "Frpc" (
  "uid" TEXT NOT NULL,
  "name" TEXT NOT NULL,
  "config" TEXT NOT NULL,
  "autorun" INTEGER NOT NULL DEFAULT 0,
  "time" INTEGER NOT NULL,
  PRIMARY KEY ("uid")
)
""".trimIndent()
                )
                database.execSQL(
                    """
INSERT INTO "Frpc" VALUES ('830b0a0e-c2b3-4f95-b3c9-55db12923d2e', '远程控制SmsForwarder', '[common]
#frps服务端公网IP
server_addr = 88.88.88.88
#frps服务端公网端口
server_port = 8888
#可选，建议启用
token = 88888888
#连接服务端的超时时间（增大时间避免frpc在网络未就绪的情况下启动失败）
dial_server_timeout = 60
#第一次登陆失败后是否退出
login_fail_exit = false

#[二选一即可]每台机器不可重复，通过 http://88.88.88.88:5000 访问
[SmsForwarder-TCP]
type = tcp
local_ip = 127.0.0.1
local_port = 5000
#只要修改下面这一行
remote_port = 5000

#[二选一即可]每台机器不可重复，通过 http://smsf.demo.com 访问
[SmsForwarder-HTTP]
type = http
local_ip = 127.0.0.1
local_port = 5000
#只要修改下面这一行
custom_domains = smsf.demo.com
', 0, '1651334400000')
""".trimIndent()
                )

                database.execSQL("ALTER TABLE log RENAME TO old_log")
                database.execSQL(
                    """
CREATE TABLE "Logs" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" TEXT NOT NULL DEFAULT 'sms',
  "from" TEXT NOT NULL DEFAULT '',
  "content" TEXT NOT NULL DEFAULT '',
  "rule_id" INTEGER NOT NULL DEFAULT 0,
  "sim_info" TEXT NOT NULL DEFAULT '',
  "forward_status" INTEGER NOT NULL DEFAULT 1,
  "forward_response" TEXT NOT NULL DEFAULT '',
  "time" INTEGER NOT NULL,
  FOREIGN KEY ("rule_id") REFERENCES "Rule" ("id") ON DELETE CASCADE ON UPDATE CASCADE
)
""".trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX \"index_Log_id\" ON \"Logs\" ( \"id\" ASC)")
                database.execSQL("CREATE INDEX \"index_Log_rule_id\" ON \"Logs\" ( \"rule_id\" ASC)")
                database.execSQL("INSERT INTO Logs (id,type,`from`,content,sim_info,rule_id,forward_status,forward_response,time) SELECT _id,type,l_from,content,sim_info,rule_id,forward_status,forward_response,strftime('%s000',time) FROM old_log")
                database.execSQL("DROP TABLE old_log")

                database.execSQL("ALTER TABLE rule RENAME TO old_rule")
                database.execSQL(
                    """
CREATE TABLE "Rule" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" TEXT NOT NULL DEFAULT 'sms',
  "filed" TEXT NOT NULL DEFAULT 'transpond_all',
  "check" TEXT NOT NULL DEFAULT 'is',
  "value" TEXT NOT NULL DEFAULT '',
  "sender_id" INTEGER NOT NULL DEFAULT 0,
  "sms_template" TEXT NOT NULL DEFAULT '',
  "regex_replace" TEXT NOT NULL DEFAULT '',
  "sim_slot" TEXT NOT NULL DEFAULT 'ALL',
  "status" INTEGER NOT NULL DEFAULT 1,
  "time" INTEGER NOT NULL,
  FOREIGN KEY ("sender_id") REFERENCES "Sender" ("id") ON DELETE CASCADE ON UPDATE CASCADE
)
""".trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX \"index_Rule_id\" ON \"Rule\" ( \"id\" ASC)")
                database.execSQL("CREATE INDEX \"index_Rule_sender_id\" ON \"Rule\" ( \"sender_id\" ASC)")
                database.execSQL("INSERT INTO Rule (id,type,filed,`check`,value,sender_id,time,sms_template,regex_replace,status,sim_slot) SELECT _id,type,filed,tcheck,value,sender_id,strftime('%s000',time),sms_template,regex_replace,status,sim_slot FROM old_rule")
                database.execSQL("DROP TABLE old_rule")

                database.execSQL("ALTER TABLE sender RENAME TO old_sender")
                database.execSQL(
                    """
CREATE TABLE "Sender" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" INTEGER NOT NULL DEFAULT 1,
  "name" TEXT NOT NULL DEFAULT '',
  "json_setting" TEXT NOT NULL DEFAULT '',
  "status" INTEGER NOT NULL DEFAULT 1,
  "time" INTEGER NOT NULL
)
""".trimIndent()
                )
                database.execSQL("INSERT INTO Sender (id,name,status,type,json_setting,time) SELECT _id,name,status,type,json_setting,strftime('%s000',time) FROM old_sender")
                database.execSQL("DROP TABLE old_sender")
            }
        }

    }

}
