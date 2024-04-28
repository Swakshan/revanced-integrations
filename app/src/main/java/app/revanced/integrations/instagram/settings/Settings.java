package app.revanced.integrations.instagram.settings;

import app.revanced.integrations.shared.settings.BaseSettings;
import app.revanced.integrations.shared.settings.BooleanSetting;
import app.revanced.integrations.shared.settings.StringSetting;

public class Settings extends BaseSettings {
    public static final String SHARED_PREF_NAME = "piko_settings";
    public static final String LOG_NAME = "piko";

    public static final BooleanSetting MISC_DEV_MODE = new BooleanSetting("misc_dev_mode", true);

    public static final BooleanSetting BLOCK_TRACKER = new BooleanSetting("block_tracker", true);
    public static final BooleanSetting MISC_FONT = new BooleanSetting("misc_font", false);

    public static final BooleanSetting HIDE_STORY_VIEW = new BooleanSetting("hide_story_view", true);

    public static final BooleanSetting REMOVE_GENERAL_ADS = new BooleanSetting("REMOVE_GENERAL_ADS", true);
    public static final BooleanSetting FORCE_CAROUSEL_START = new BooleanSetting("FORCE_CAROUSEL_START", true);

    public static final BooleanSetting REMOVE_SUGGESTED_USER = new BooleanSetting("REMOVE_SUGGESTED_USER", true);
    public static final BooleanSetting REMOVE_STORY_BLOCK = new BooleanSetting("REMOVE_STORY_BLOCK", true);
    public static final BooleanSetting REMOVE_SUGGESTED_REELS = new BooleanSetting("REMOVE_SUGGESTED_REELS", true);
    public static final BooleanSetting REMOVE_SUGGESTED_THREADS = new BooleanSetting("REMOVE_SUGGESTED_THREADS", true);
    public static final BooleanSetting REMOVE_SUGGESTED_POST = new BooleanSetting("REMOVE_SUGGESTED_POST", true);

}
