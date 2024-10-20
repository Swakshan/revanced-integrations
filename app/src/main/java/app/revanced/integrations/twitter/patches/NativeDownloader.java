package app.revanced.integrations.twitter.patches;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;
import app.revanced.integrations.twitter.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NativeDownloader {
    public static String downloadString() {
        return Utils.strRes("piko_pref_native_downloader_alert_title");
    }

    private static String getExtension(String typ) {
        if (typ.equals("video/mp4")) {
            return "mp4";
        }
        if (typ.equals("video/webm")) {
            return "webm";
        }
        if (typ.equals("application/x-mpegURL")) {
            return "m3u8";
        }
        return "jpg";
    }

    private static ArrayList<HashMap<String, String>> getMediaData(Object yghObj) {
        ArrayList<HashMap<String, String>> mediaData = new ArrayList<>();

        Class<?> yghClazz = yghObj.getClass();
        Class<?> superClass = yghClazz.getSuperclass();

        try {
            Object superClassInstance = superClass.cast(yghObj);
            List<?> list;

            Field mediaListField = null;
            for (Field field : superClass.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    mediaListField = field;
                    mediaListField.setAccessible(true);
                    break;
                }
            }

            assert mediaListField != null;
            list = (List<?>) mediaListField.get(superClassInstance);

            assert list != null;
            if (list.isEmpty()) {
                return mediaData;
            }

            Class<?> itemClass = list.get(0).getClass();

            Field imageUrlField = null;
            Method getVideoDataMethod = null;
            Field videoField = null;
            Field videoCDCField = null;

            for (Object item : list) {
                Method[] itemMethods = itemClass.getDeclaredMethods();

                if (getVideoDataMethod == null) {
                    getVideoDataMethod = itemMethods[itemMethods.length - 1];
                }
                Object videoData = getVideoDataMethod.invoke(item);

                HashMap<String, String> data = new HashMap<>();

                if (videoData != null) {
                    Class<?> videoDataClass = videoData.getClass();

                    // setting fields
                    if (videoField == null) {
                        int c = 1;
                        for (Field field : videoDataClass.getDeclaredFields()) {
                            if (String.class.isAssignableFrom(field.getType())) {
                                if (c == 1) {
                                    videoField = field;
                                    videoField.setAccessible(true);
                                } else if (c == 2) {
                                    videoCDCField = field;
                                    videoCDCField.setAccessible(true);
                                    break;
                                }

                                c++;
                            }
                        }
                    }

                    String mediaUrl = (String) videoField.get(videoData);
                    String c = (String) videoCDCField.get(videoData);

                    String ext = getExtension(c);

                    data.put("type", Utils.strRes("drafts_empty_video"));
                    data.put("ext", ext);
                    data.put("url", mediaUrl);
                    mediaData.add(data);
                } else {
                    if (imageUrlField == null) {
                        for (Field field : itemClass.getDeclaredFields()) {
                            if (String.class.isAssignableFrom(field.getType())) {
                                imageUrlField = field;
                                imageUrlField.setAccessible(true);
                                break;
                            }
                        }
                    }

                    assert imageUrlField != null;
                    String mediaUrl = (String) imageUrlField.get(item);
                    String ext = "jpg";

                    data.put("type", Utils.strRes("drafts_empty_photo"));
                    data.put("ext", ext);
                    data.put("url", mediaUrl + "?name=4096x4096&format=jpg");

                    mediaData.add(data);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mediaData;
    }

    private static void alertBox(Context ctx, String filename, ArrayList<HashMap<String, String>> mediaData) throws NoSuchFieldException, IllegalAccessException {
        LinearLayout ln = new LinearLayout(ctx);
        ln.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(Utils.strRes("piko_pref_native_downloader_alert_title"));

        int n = mediaData.size();
        String[] choices = new String[n];
        for (int i = 0; i < n; i++) {
            HashMap<String, String> hashMap = mediaData.get(i);
            String typ = hashMap.get("type");
            choices[i] = "• " + typ + " " + (i + 1);
        }

        builder.setItems(choices, (dialogInterface, which) -> {
            HashMap<String, String> media = mediaData.get(which);

            Utils.toast(Utils.strRes("download_started"));
            Utils.downloadFile(media.get("url"), filename + (which + 1), media.get("ext"));
        });

        builder.setNegativeButton(Utils.strRes("piko_pref_native_downloader_download_all"), (dialogInterface, index) -> {
            Utils.toast(Utils.strRes("download_started"));

            int i = 1;
            for (HashMap<String, String> media : mediaData) {
                Utils.downloadFile(media.get("url"), filename + i, media.get("ext"));
                i++;
            }
            dialogInterface.dismiss();
        });

        builder.show();
    }

    // downloader(Landroid/content/Context;Ljava/lang/Object;)V
    public static void downloader(Context activity, Object tweet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Class<?> tweetClass = tweet.getClass();

        Method getIdMethod = tweetClass.getDeclaredMethod("getId");
        Long id = (Long) getIdMethod.invoke(tweet);
        assert id != null;

        /*
          Check how many versions this naive implementation lasts. checked until
          versions 10.48 and the method names were the same.
         */
        Method getUserNameMethod = tweetClass.getDeclaredMethod("r");
        Method getMediaMethod = tweetClass.getDeclaredMethod("b");

        String username = (String) getUserNameMethod.invoke(tweet);
        if (username == null) {
            Utils.toast("username is not found");
        }
        assert username != null;
        Object obj = getMediaMethod.invoke(tweet);
        ArrayList<HashMap<String, String>> media = getMediaData(obj);

        assert media != null;
        if (media.isEmpty()) {
            Utils.toast(Utils.strRes("piko_pref_native_downloader_no_media"));
            return;
        }

        if (media.size() == 1) {
            HashMap<String, String> item = media.get(0);

            Utils.toast(Utils.strRes("download_started"));
            Utils.downloadFile(item.get("url"), username + "_" + id, item.get("ext"));
            return;
        }

        alertBox(activity, username + "_" + id + "-", media);
    }
}