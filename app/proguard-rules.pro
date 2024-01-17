#=========================================基础不变的混淆配置=========================================##
#指定代码的压缩级别
-optimizationpasses 5
#包名不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
#-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共的库的类的成员
#-dontskipnonpubliclibraryclassmembers
#优化  不优化输入的类文件
-dontoptimize
#预校验
#-dontpreverify
#混淆时是否记录日志
-verbose
# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#保护注解
-keepattributes *Annotation*
#忽略警告
-ignorewarnings

##记录生成的日志数据,gradle build时在本项目根目录输出##
#apk 包内所有 class 的内部结构
#-dump class_files.txt
#未混淆的类和成员
-printseeds seeds.txt
#列出从 apk 中删除的代码
-printusage unused.txt
#混淆前后的映射
-printmapping mapping.txt
# 并保留源文件名为"Proguard"字符串，而非原始的类名 并保留行号
-keepattributes SourceFile,LineNumberTable
########记录生成的日志数据，gradle build时 在本项目根目录输出-end#####

#需要保留的东西
# 保持哪些类不被混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.**
#-keep public class com.android.vending.licensing.ILicensingService

#如果有引用v4包可以添加下面这行
#-keep public class * extends android.support.v4.app.Fragment

##########JS接口类不混淆，否则执行不了
-dontwarn com.android.JsInterface.**
-keep class com.android.JsInterface.** {*; }

#极光推送和百度lbs android sdk一起使用proguard 混淆的问题#http的类被混淆后，导致apk定位失败，保持apache 的http类不被混淆就好了
-dontwarn org.apache.**
-keep class org.apache.**{ *; }

-keep public class * extends android.view.View {
  public <init>(android.content.Context);
  public <init>(android.content.Context, android.util.AttributeSet);
  public <init>(android.content.Context, android.util.AttributeSet, int);
  public void set*(...);
 }

#保持 native 方法不被混淆
-keepclasseswithmembernames class * {
  native <methods>;
}

#保持自定义控件类不被混淆
-keepclasseswithmembers class * {
  public <init>(android.content.Context, android.util.AttributeSet);
}

#保持自定义控件类不被混淆
-keepclassmembers class * extends android.app.Activity {
  public void *(android.view.View);
}

#保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#保持 Serializable 不被混淆
-keepnames class * implements java.io.Serializable

#保持 Serializable 不被混淆并且enum 类也不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#保持枚举 enum 类不被混淆 如果混淆报错，建议直接使用上面的 -keepclassmembers class * implements java.io.Serializable即可
-keepclassmembers enum * {
      public static **[] values();
      public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
      public void *ButtonClicked(android.view.View);
}

#不混淆资源类
-keep class **.R$* {*;}

#===================================混淆保护自己项目的部分代码以及引用的第三方jar包library=============================#######
#如果引用了v4或者v7包
-dontwarn android.support.**


# AndroidX 防止混淆
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# zxing
-dontwarn com.google.zxing.**
-keep class com.google.zxing.**{*;}

#SignalR推送
-keep class microsoft.aspnet.signalr.** { *; }

# 极光推送混淆
#-dontoptimize
#-dontpreverify
#-dontwarn cn.jpush.**
#-keep class cn.jpush.** { *; }
#-dontwarn cn.jiguang.**
#-keep class cn.jiguang.** { *; }

# 数据库框架OrmLite
-keepattributes *DatabaseField*
-keepattributes *DatabaseTable*
-keepattributes *SerializedName*
-keep class com.j256.**
-keepclassmembers class com.j256.** { *; }
-keep enum com.j256.**
-keepclassmembers enum com.j256.** { *; }
-keep interface com.j256.**
-keepclassmembers interface com.j256.** { *; }

#XHttp2
-keep class com.xuexiang.xhttp2.model.** { *; }
-keep class com.xuexiang.xhttp2.cache.model.** { *; }
-keep class com.xuexiang.xhttp2.cache.stategy.**{*;}
-keep class com.xuexiang.xhttp2.annotation.** { *; }

#okhttp
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**
-dontwarn javax.annotation.**

#如果用到Gson解析包的，直接添加下面这几行就能成功混淆，不然会报错
-keepattributes Signature
-keep class com.google.gson.stream.** { *; }
-keepattributes EnclosingMethod
-keep class org.xz_sale.entity.**{*;}
-keep class com.google.gson.** {*;}
-keep class com.google.**{*;}
#-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# RxJava RxAndroid
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode producerNode;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode consumerNode;
#}

-dontwarn okio.**
-dontwarn javax.annotation.**

# fastjson
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.** { *; }
-keepattributes Signature

# xpage
-keep class com.xuexiang.xpage.annotation.** { *; }
-keep class com.xuexiang.xpage.config.** { *; }

# xaop
-keep @com.xuexiang.xaop.annotation.* class * {*;}
-keep @org.aspectj.lang.annotation.* class * {*;}
-keep class * {
    @com.xuexiang.xaop.annotation.* <fields>;
    @org.aspectj.lang.annotation.* <fields>;
}
-keepclassmembers class * {
    @com.xuexiang.xaop.annotation.* <methods>;
    @org.aspectj.lang.annotation.* <methods>;
}

# xrouter
-keep public class com.xuexiang.xrouter.routes.**{*;}
-keep class * implements com.xuexiang.xrouter.facade.template.ISyringe{*;}
# 如果使用了 byType 的方式获取 Service，需添加下面规则，保护接口
-keep interface * implements com.xuexiang.xrouter.facade.template.IProvider
# 如果使用了 单类注入，即不定义接口实现 IProvider，需添加下面规则，保护实现
-keep class * implements com.xuexiang.xrouter.facade.template.IProvider

# xupdate
-keep class com.xuexiang.xupdate.entity.** { *; }

# xvideo
-keep class com.xuexiang.xvideo.jniinterface.** { *; }

# xipc
-keep @com.xuexiang.xipc.annotation.* class * {*;}
-keep class * {
    @com.xuexiang.xipc.annotation.* <fields>;
}
-keepclassmembers class * {
    @com.xuexiang.xipc.annotation.* <methods>;
}

# umeng统计
-keep class com.umeng.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.xuexiang.xui.widget.edittext.materialedittext.** { *; }

# Android Keep Alive(安卓保活)，Cactus 集成双进程前台服务，JobScheduler，onePix(一像素)，WorkManager，无声音乐
-keep class com.gyf.cactus.entity.* {*;}

# 排除实体类
-keep class com.idormy.sms.forwarder.core.http.entity.** {*;}
-keep class com.idormy.sms.forwarder.database.entity.** {*;}
-keep class com.idormy.sms.forwarder.entity.** {*;}
-keep class com.idormy.sms.forwarder.server.model.** {*;}

# javax.mail
-dontwarn com.sun.**
-dontwarn javax.mail.**
-dontwarn javax.activation.**
-keep class com.sun.** { *;}
-keep class javax.mail.** { *;}
-keep class javax.activation.** { *;}
-keep class com.smailnet.emailkit.** { *;}
-keep class com.idormy.sms.forwarder.utils.mail.** {*;}
-keep class com.gitee.xuankaicat.kmnkt.** {*;}
-keep class org.eclipse.paho.client.** {*;}

-keep public class com.xuexiang.xrouter.routes.**{*;}
-keep class * implements com.xuexiang.xrouter.facade.template.ISyringe{*;}
# 如果使用了 byType 的方式获取 Service，需添加下面规则，保护接口
-keep interface * implements com.xuexiang.xrouter.facade.template.IProvider
# 如果使用了 单类注入，即不定义接口实现 IProvider，需添加下面规则，保护实现
-keep class * implements com.xuexiang.xrouter.facade.template.IProvider

-dontwarn com.alipay.sdk.**
-dontwarn com.android.org.conscrypt.**
-dontwarn java.awt.image.**
-dontwarn javax.lang.model.**
-dontwarn javax.naming.**
-dontwarn javax.naming.directory.**

# This is generated automatically by the Android Gradle plugin.
-dontwarn org.joda.convert.**
-dontwarn org.slf4j.impl.**

# MultiLanguages
-keep class com.hjq.language.** {*;}

# crontab解析
-keep class gatewayapps.crondroid.** { *; }
-keep class net.redhogs.cronparser.** { *; }
