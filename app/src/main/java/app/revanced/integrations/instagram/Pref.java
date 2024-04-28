package app.revanced.integrations.instagram;

import android.util.Log;
import app.revanced.integrations.instagram.settings.Settings;

import java.util.*;

@SuppressWarnings("unused")
public class Pref {

    public static boolean blockTracker() {
        return Utils.getBooleanPerf(Settings.BLOCK_TRACKER);
    }

    public static boolean anonStoryView() {
        return Utils.getBooleanPerf(Settings.HIDE_STORY_VIEW);
    }

    public static boolean removedAds() {
        return Utils.getBooleanPerf(Settings.REMOVE_GENERAL_ADS);
    }

    public static boolean carousel2one() {
        return Utils.getBooleanPerf(Settings.FORCE_CAROUSEL_START);
    }

    public static boolean removeSuggReels() {
        return Utils.getBooleanPerf(Settings.REMOVE_SUGGESTED_USER);
    }

    public static boolean removeSuggUser() {
        return Utils.getBooleanPerf(Settings.REMOVE_SUGGESTED_USER);
    }

    public static boolean removeStoryBlock() {
        return Utils.getBooleanPerf(Settings.REMOVE_STORY_BLOCK);
    }

    public static boolean removeThreadsBlock() {
        return Utils.getBooleanPerf(Settings.REMOVE_SUGGESTED_THREADS);
    }
    public static boolean removeSuggPost() {
        return Utils.getBooleanPerf(Settings.REMOVE_SUGGESTED_POST);
    }

    //end
}