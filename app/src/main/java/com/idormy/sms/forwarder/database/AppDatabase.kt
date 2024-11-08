package com.idormy.sms.forwarder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.dao.LogsDao
import com.idormy.sms.forwarder.database.dao.MsgDao
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.dao.TaskDao
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.database.entity.LogsDetail
import com.idormy.sms.forwarder.database.entity.Msg
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.database.ext.ConvertersDate
import com.idormy.sms.forwarder.utils.DATABASE_NAME
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TAG_LIST

@Database(
    entities = [Frpc::class, Msg::class, Logs::class, Rule::class, Sender::class, Task::class],
    views = [LogsDetail::class],
    version = 20,
    exportSchema = false
)
@TypeConverters(ConvertersDate::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun frpcDao(): FrpcDao
    abstract fun msgDao(): MsgDao
    abstract fun logsDao(): LogsDao
    abstract fun ruleDao(): RuleDao
    abstract fun senderDao(): SenderDao
    abstract fun taskDao(): TaskDao

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
                context.applicationContext, AppDatabase::class.java, DATABASE_NAME
            ).allowMainThreadQueries() //TODO:允许主线程访问，后面再优化
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
#只要修改下面这一行（frps所在服务器必须暴露的公网端口）
remote_port = 5000

#[二选一即可]每台机器不可重复，通过 http://smsf.demo.com 访问
[SmsForwarder-HTTP]
type = http
local_ip = 127.0.0.1
local_port = 5000
#只要修改下面这一行（在frps端将域名反代到vhost_http_port）
custom_domains = smsf.demo.com
', 0, '1651334400000')
""".trimIndent()
                        )
                    }
                }).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                )

            /*if (BuildConfig.DEBUG) {
                builder.setQueryCallback({ sqlQuery, bindArgs ->
                    println("SQL_QUERY: $sqlQuery\nBIND_ARGS: $bindArgs")
                }, Executors.newSingleThreadExecutor())
            }*/

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
INSERT INTO "Frpc" VALUES ('830b0a0e-c2b3-4f95-b3c9-55db12923d2e', '远程控制SmsForwarder', '
#frps服务端公网IP
serverAddr = "88.88.88.88"
#frps服务端公网端口
serverPort = 8888
#连接服务端的超时时间（增大时间避免frpc在网络未就绪的情况下启动失败）
transport.dialServerTimeout = 60
#第一次登陆失败后是否退出
loginFailExit = false
#可选，建议启用
auth.method = "token"
auth.token = "88888888"

#[二选一即可]每台机器的 name 和 remotePort 不可重复，通过 http://88.88.88.88:5000 访问
[[proxies]]
#同一个frps下，多台设备的 name 不可重复
name = "SmsForwarder-TCP-001"
type = "tcp"
localIP = "127.0.0.1"
localPort = 5000
#只要修改下面这一行（frps所在服务器必须暴露且防火墙放行的公网端口，同一个frps下不可重复）
remotePort = 5000

#[二选一即可]每台机器的 name 和 customDomains 不可重复，通过 http://smsf.demo.com 访问
[[proxies]]
#同一个frps下，多台设备的 name 不可重复
name = "SmsForwarder-HTTP-001"
type = "http"
localPort = 5000
#只要修改下面这一行（在frps端将域名反代到vhost_http_port）
customDomains = ["smsf.demo.com"]

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

        //转发日志添加SIM卡槽ID
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table Logs add column sub_id INTEGER NOT NULL DEFAULT 0")
            }
        }

        //单个转发规则可绑定多个发送通道
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table Logs add column sender_id INTEGER NOT NULL DEFAULT 0")
                database.execSQL("Update Logs Set sender_id = (Select sender_id from Rule where Logs.rule_id = Rule.id)")
                database.execSQL("Alter table Rule add column sender_list TEXT NOT NULL DEFAULT ''")
                database.execSQL("Update Rule set sender_list = sender_id")
                database.execSQL("CREATE INDEX \"index_Rule_sender_ids\" ON \"Rule\" ( \"sender_list\" ASC)")
                //删除字段：sender_id
                /*database.execSQL("Create table Rule_t as Select id,type,filed,check,value,sender_list,sms_template,regex_replace,sim_slot,status,time from Rule where 1 = 1")
                database.execSQL("Drop table Rule")
                database.execSQL("Alter table Rule_t rename to Rule")
                database.execSQL("CREATE UNIQUE INDEX \"index_Rule_id\" ON \"Rule\" ( \"id\" ASC)")*/
            }
        }

        //转发规则添加发送通道逻辑
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table Rule add column sender_logic TEXT NOT NULL DEFAULT 'ALL'")
            }
        }

        //分割Logs表
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //database.execSQL("Create table Msg as Select id,type,`from`,content,(case when sim_info like 'SIM1%' then '0' when sim_info like 'SIM2%' then '1' else '-1' end) as sim_slot,sim_info,sub_id,time from Logs where 1 = 1")
                database.execSQL(
                    """
CREATE TABLE "Msg" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" TEXT NOT NULL DEFAULT 'sms',
  "from" TEXT NOT NULL DEFAULT '',
  "content" TEXT NOT NULL DEFAULT '',
  "sim_slot" INTEGER NOT NULL DEFAULT -1,
  "sim_info" TEXT NOT NULL DEFAULT '',
  "sub_id" INTEGER NOT NULL DEFAULT 0,
  "time" INTEGER NOT NULL
)
""".trimIndent()
                )
                database.execSQL("INSERT INTO Msg (id,type,`from`,content,sim_slot,sim_info,sub_id,time) Select id,type,`from`,content,(case when sim_info like 'SIM1%' then '0' when sim_info like 'SIM2%' then '1' else '-1' end) as sim_slot,sim_info,sub_id,time from Logs where 1 = 1")
                database.execSQL("CREATE UNIQUE INDEX \"index_Msg_id\" ON \"Msg\" ( \"id\" ASC)")
                database.execSQL("ALTER TABLE Logs RENAME TO Logs_old")
                //database.execSQL("Create table Logs_new as Select id,id as msg_id,rule_id,sender_id,forward_status,forward_response,time from Logs where 1 = 1")
                database.execSQL(
                    """
CREATE TABLE "Logs" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" TEXT NOT NULL DEFAULT 'sms',
  "msg_id" INTEGER NOT NULL DEFAULT 0,
  "rule_id" INTEGER NOT NULL DEFAULT 0,
  "sender_id" INTEGER NOT NULL DEFAULT 0,
  "forward_status" INTEGER NOT NULL DEFAULT 1,
  "forward_response" TEXT NOT NULL DEFAULT '',
  "time" INTEGER NOT NULL,
  FOREIGN KEY ("msg_id") REFERENCES "Msg" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY ("rule_id") REFERENCES "Rule" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY ("sender_id") REFERENCES "Sender" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);
""".trimIndent()
                )
                database.execSQL("INSERT INTO Logs (id,type,msg_id,rule_id,sender_id,forward_status,forward_response,time) SELECT id,type,id as msg_id,rule_id,sender_id,forward_status,forward_response,time FROM Logs_old")
                database.execSQL("DROP TABLE Logs_old")
                database.execSQL("CREATE UNIQUE INDEX \"index_Logs_id\" ON \"Logs\" ( \"id\" ASC)")
                database.execSQL("CREATE INDEX \"index_Logs_msg_id\" ON \"Logs\" ( \"msg_id\" ASC)")
                database.execSQL("CREATE INDEX \"index_Logs_rule_id\" ON \"Logs\" ( \"rule_id\" ASC)")
                database.execSQL("CREATE INDEX \"index_Logs_sender_id\" ON \"Logs\" ( \"sender_id\" ASC)")
            }
        }

        // 定义数据库迁移配置
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 这里新建一个视图（视图名称要用两个半角的间隔号括起来）
                database.execSQL("CREATE VIEW `LogsDetail` AS SELECT LOGS.id,LOGS.type,LOGS.msg_id,LOGS.rule_id,LOGS.sender_id,LOGS.forward_status,LOGS.forward_response,LOGS.TIME,Rule.filed AS rule_filed,Rule.`check` AS rule_check,Rule.value AS rule_value,Rule.sim_slot AS rule_sim_slot,Sender.type AS sender_type,Sender.NAME AS sender_name FROM LOGS  LEFT JOIN Rule ON LOGS.rule_id = Rule.id LEFT JOIN Sender ON LOGS.sender_id = Sender.id")
            }
        }

        //免打扰(禁用转发)时间段
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column silent_period_start INTEGER NOT NULL DEFAULT 0 ")
                database.execSQL("Alter table rule add column silent_period_end INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table Msg add column call_type INTEGER NOT NULL DEFAULT 0")
            }
        }

        //自动化任务
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
CREATE TABLE "Task" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" INTEGER NOT NULL DEFAULT 1,
  "name" TEXT NOT NULL DEFAULT '',
  "description" TEXT NOT NULL DEFAULT '',
  "conditions" TEXT NOT NULL DEFAULT '',
  "actions" TEXT NOT NULL DEFAULT '',
  "last_exec_time" INTEGER NOT NULL,
  "next_exec_time" INTEGER NOT NULL,
  "status" INTEGER NOT NULL DEFAULT 1
)
""".trimIndent()
                )
            }
        }

        //自定义模板可用变量统一成英文标签
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //替换自定义模板标签
                var smsTemplate = SettingUtils.smsTemplate
                //替换Rule.sms_template中的标签
                var ruleColumnCN = "sms_template"
                var ruleColumnTW = "sms_template"
                //替换Sender.json_setting中的标签
                var senderColumnCN = "json_setting"
                var senderColumnTW = "json_setting"

                for (i in TAG_LIST.indices) {
                    val tagCN = TAG_LIST[i]["zh_CN"].toString()
                    val tagTW = TAG_LIST[i]["zh_TW"].toString()
                    val tagEN = TAG_LIST[i]["en"].toString()
                    smsTemplate = smsTemplate.replace(tagCN, tagEN)
                    ruleColumnCN = "REPLACE($ruleColumnCN, '$tagCN', '$tagEN')"
                    ruleColumnTW = "REPLACE($ruleColumnTW, '$tagTW', '$tagEN')"
                    senderColumnCN = "REPLACE($senderColumnCN, '$tagCN', '$tagEN')"
                    senderColumnTW = "REPLACE($senderColumnTW, '$tagTW', '$tagEN')"
                }

                database.execSQL("UPDATE Rule SET sms_template = $ruleColumnCN WHERE sms_template != ''")
                database.execSQL("UPDATE Rule SET sms_template = $ruleColumnTW WHERE sms_template != ''")

                database.execSQL("UPDATE Sender SET json_setting = $senderColumnCN WHERE type NOT IN (4, 5, 6, 7, 8, 14)")
                database.execSQL("UPDATE Sender SET json_setting = $senderColumnTW WHERE type NOT IN (4, 5, 6, 7, 8, 14)")

                SettingUtils.smsTemplate = smsTemplate
            }
        }

        //免打扰星期段
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table rule add column silent_day_of_week TEXT NOT NULL DEFAULT '' ")
            }
        }

    }

}
