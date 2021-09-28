package com.idormy.sms.forwarder.utils;

import android.text.TextUtils;

import java.io.IOException;

/**
 * 使用方法:
 * OSUtils.ROM_TYPE romType = OSUtils.getRomType();
 * 可能您需要对其他的ROM进行区分，那么只需三步：
 * 一：使用BuildProperties获取到所有的key,遍历获取到所有的value(getProperty),或者直接找到build.prop文件。
 * 二：找到定制ROM特征的标识（key/value）
 * 三：增加ROM_TYPE枚举类型，getRomType方法加入识别比对即可
 * 作者：YouAreMyShine
 * 链接：https://www.jianshu.com/p/bb1f765a425f
 */
public class OSUtils {
    /**
     * MIUI ROM标识
     * <p>
     * "ro.miui.ui.version.code" -> "5"
     * <p>
     * "ro.miui.ui.version.name" -> "V7"
     * <p>
     * "ro.miui.has_handy_mode_sf" -> "1"
     * <p>
     * "ro.miui.has_real_blur" -> "1"
     * <p>
     * <p>
     * <p>
     * Flyme ROM标识
     * <p>
     * "ro.build.user" -> "flyme"
     * <p>
     * "persist.sys.use.flyme.icon" -> "true"
     * <p>
     * "ro.flyme.published" -> "true"
     * <p>
     * "ro.build.display.id" -> "Flyme OS 5.1.2.0U"
     * <p>
     * "ro.meizu.setupwizard.flyme" -> "true"
     * <p>
     * <p>
     * <p>
     * EMUI ROM标识
     * <p>
     * "ro.build.version.emui" -> "EmotionUI_1.6"
     */

    //MIUI标识
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    //EMUI标识
    private static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";
    //Flyme标识
    private static final String KEY_FLYME_ID_FLAG_KEY = "ro.build.display.id";
    private static final String KEY_FLYME_ID_FLAG_VALUE_KEYWORD = "Flyme";
    private static final String KEY_FLYME_ICON_FLAG = "persist.sys.use.flyme.icon";
    private static final String KEY_FLYME_SETUP_FLAG = "ro.meizu.setupwizard.flyme";
    private static final String KEY_FLYME_PUBLISH_FLAG = "ro.flyme.published";

    /**
     * 获取ROM类型，MIUI_ROM, *FLYME_ROM,    * EMUI_ROM,    * OTHER_ROM
     *
     * @return ROM_TYPE ROM类型的枚举
     */
    public static ROM_TYPE getRomType() {
        ROM_TYPE rom_type = ROM_TYPE.OTHER_ROM;
        try {
            BuildProperties buildProperties = BuildProperties.getInstance();
            if (buildProperties.containsKey(KEY_EMUI_VERSION_CODE)) {
                return ROM_TYPE.EMUI_ROM;
            }
            if (buildProperties.containsKey(KEY_MIUI_VERSION_CODE) || buildProperties.containsKey(KEY_MIUI_VERSION_NAME)) {
                return ROM_TYPE.MIUI_ROM;
            }
            if (buildProperties.containsKey(KEY_FLYME_ICON_FLAG) || buildProperties.containsKey(KEY_FLYME_SETUP_FLAG) || buildProperties.containsKey(KEY_FLYME_PUBLISH_FLAG)) {
                return ROM_TYPE.FLYME_ROM;
            }
            if (buildProperties.containsKey(KEY_FLYME_ID_FLAG_KEY)) {
                String romName = buildProperties.getProperty(KEY_FLYME_ID_FLAG_KEY);
                if (!TextUtils.isEmpty(romName) && romName.contains(KEY_FLYME_ID_FLAG_VALUE_KEYWORD)) {
                    return ROM_TYPE.FLYME_ROM;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rom_type;
    }

    public enum ROM_TYPE {
        MIUI_ROM,
        FLYME_ROM,
        EMUI_ROM,
        OTHER_ROM
    }
}
