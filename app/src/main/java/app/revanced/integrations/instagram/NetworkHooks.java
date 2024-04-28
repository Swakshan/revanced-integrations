package app.revanced.integrations.instagram;

import android.app.*;
import android.content.*;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.*;
import com.facebook.proxygen.*;
import java.io.*;
//import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.*;
import android.app.Application;
import app.revanced.integrations.instagram.settings.Settings;
import app.revanced.integrations.instagram.Pref;

public class NetworkHooks extends Application {

  public static final String TAG = Settings.LOG_NAME;

  private static Field bufferStreamField, readBufferField;

  private static void logSuperclasses(Class cl, int depth) {
    if (cl == null) return;
    String d = "";
    for (int i = 0; i < depth; i++) d += "-";
    Log.v(TAG, d + " " + cl.getName());
    Class sup = cl.getSuperclass();
    logSuperclasses(sup, depth + 1);
  }
  
  public static int nativeReadBufferRead( NativeReadBuffer buffer, byte[] data, int offset, int count) throws IOException {
    if (isModifiableRequest(buffer)) {
      maybeReadAndModifyResponse(buffer);
      if (buffer.modifiedResponse != null) {
        if (buffer.modifiedResponseOffset >= buffer.modifiedResponse.length) {
          Log.i(TAG, "modified response eof");
          return 0;
        } else {
          Log.i(TAG,"serving modified response: " +buffer.modifiedResponseOffset +"/" +buffer.modifiedResponse.length);
          int dstLen = Math.min(  count,  buffer.modifiedResponse.length - buffer.modifiedResponseOffset);
          System.arraycopy(  buffer.modifiedResponse,  buffer.modifiedResponseOffset,  data,  offset,  dstLen);
          buffer.modifiedResponseOffset += dstLen;
          return dstLen;
        }
      }
    }

    int res = buffer._read(data, offset, count);
    return res;
  }

  public static int nativeReadBufferSize(NativeReadBuffer buffer) {
    if (isModifiableRequest(buffer)) {
      try {
        maybeReadAndModifyResponse(buffer);
      } catch (IOException x) {
        Log.w(TAG, x);
      }
    }
    if (buffer.modifiedResponse != null) {
      int r = buffer.modifiedResponse.length - buffer.modifiedResponseOffset;
      Log.d(TAG, "returning size for modified response: " + r);
      return r;
    }
    int ret = buffer._size();
    return ret;
  }



  public static void jniHandlerSendHeaders(JniHandler handler,HttpUriRequest httpUriRequest) throws IOException {
    jniHandlerSendRequest(handler, httpUriRequest, null, 0, 0);
  }

//Block req
  public static void jniHandlerSendRequest(JniHandler handler,HttpUriRequest httpUriRequest,byte[] bArr,int i,int i2) throws IOException {
    URI uri = null;
    Object respHandler = null;
    try {
      uri =(URI) httpUriRequest.getClass().getMethod("getURI").invoke(httpUriRequest);
      if (readBufferField == null) {
        Field[] flds = handler.mResponseHandler.getClass().getDeclaredFields();
        Log.i(TAG, handler.mResponseHandler.getClass().getName());
        logSuperclasses(handler.mResponseHandler.getClass().getSuperclass(), 1);
        for (Field fld : flds) {
          Log.d(  TAG,  fld.toString() + " = " + fld.get(handler.mResponseHandler));
          if (fld.getType().getName().equals("com.facebook.proxygen.ReadBuffer")) {
            fld.setAccessible(true);
            readBufferField = fld;
            break;
          }
        }
      }
      NativeReadBuffer buf = (NativeReadBuffer) readBufferField.get(handler.mResponseHandler);
      buf.requestURI = uri;
    } catch (Exception x) {
      Log.w(TAG, x);
    }
    String host  = uri.getHost();
    String path = uri.getPath();

    if (
            (
                    (
                      ( host.equals("graph.instagram.com") && path.equals("/logging_client_events")) ||
                      ( host.equals("graph.facebook.com") && path.endsWith("/activities")) ||
                      path.contains("/ads/")
                    )&& Pref.blockTracker()
            )||
            (path.contains("/api/v2/media/seen/") && Pref.anonStoryView())
    ) {
      throw new IOException("blocked tracking request");
    }
  }

//initial url filter
  public static boolean isModifiableRequest(NativeReadBuffer buffer) {

    String host = buffer.requestURI.getHost();
    String path = buffer.requestURI.getPath();
    return  (!(path.contains("/api/v1/users/") && path.contains("/info_stream/")))  &&
    (
      buffer.requestURI != null &&
      host.equals("i.instagram.com") &&
      (
        path.equals("/api/v1/feed/timeline/") ||
        path.equals("/api/v1/feed/injected_reels_media/") ||
        path.equals("/api/v1/ads/graphql/") ||
        path.equals("/api/v1/discover/chaining_experience_feed/") ||
        path.equals("/api/v1/discover/topical_explore/")
      )
    );
  }

  //read response
  public static void maybeReadAndModifyResponse(NativeReadBuffer buffer)throws IOException {
    if (buffer.modifiedResponse == null) {
      ByteArrayOutputStream origResponse = new ByteArrayOutputStream();
      byte[] buf = new byte[4096];
      int read;
      String respStr;
      while (true) {
        int size = buffer._size();
        while (size > 0) {
          read = buffer._read(buf, 0, 4096);
          size -= read;
          Log.d(TAG, "reading original response: " + read + ", " + size + " left");
          if (read > 0) origResponse.write(buf, 0, read);
          if (size == 0) {
            size = buffer._size();
            Log.d(TAG, "read done, next size is " + size);
          }
        }

        Log.d(TAG, "read: DONE " + buffer.requestURI);
        respStr = new String(origResponse.toByteArray(), "UTF-8");
        if (respStr.endsWith("}")) {
          break;
        }
        Log.i(TAG, "Incomplete response, waiting for more data");
        try {
          Thread.sleep(50);
        } catch (Exception x) {}
      }
      JSONObject json;
      String uriPath = buffer.requestURI.getPath();
      try {
        json = new JSONObject(respStr);
      } catch (Exception x) {
        Log.w(TAG, "still incomplete response", x);
        return;
      }

      try {
        
        if (uriPath.equals("/api/v1/feed/timeline/")) {
          respStr = modifyFeedResponse(json,"feed_items");
        } else if (uriPath.equals("/api/v1/discover/chaining_experience_feed/")) {
          respStr = modifyFeedResponse(json,"items"); 
        } else if (uriPath.equals("/api/v1/discover/topical_explore/")) {
          respStr= modifyExplorerResponse(json);
        }else if (uriPath.equals("/api/v1/feed/injected_reels_media/")) {
          respStr= modifyInjectedStoriesResponse(json);
        }

        buffer.modifiedResponse = str2Bytes(respStr);

        buffer.modifiedResponseOffset = 0;
      } catch (Exception x) {
        Log.e(TAG, "error modifying response", x);
        buffer.modifiedResponse = origResponse.toByteArray();
        buffer.modifiedResponseOffset = 0;
      }
    }
  }


// my functions
  private static String modifyFeedResponse(JSONObject r,String node) throws JSONException, UnsupportedEncodingException {
    JSONArray feed = r.optJSONArray(node);
    JSONArray rebuiltFeed = new JSONArray();
    if (feed != null) {
      for (int i = 0; i < feed.length(); i++) {
        JSONObject post = feed.getJSONObject(i);
        JSONObject media = post.optJSONObject("media_or_ad");
        if (media != null) {
          if (media.has("injected") && Pref.removedAds()) { // it's a fancy name for ads
            Log.i(TAG,"Removing sponsor post by " +media.getJSONObject("user").getString("username"));
            continue;
          }
          if (media.has("carousel_media") && Pref.carousel2one()) {
            JSONArray carousel = media.getJSONArray("carousel_media");
            String firstId = carousel.getJSONObject(0).getString("id").split("_")[0];
            if (!firstId.equals(media.optString("main_feed_carousel_starting_media_id"))) {
              Log.i(TAG,"Forcing post by " +media.getJSONObject("user").getString("username") +" to first carousel media");
              media.put("main_feed_carousel_starting_media_id", firstId);
            }
          }
        } else if (post.has("suggested_users") && Pref.removeSuggUser()) {
          Log.i(TAG, "Removing suggested users block");
          continue;
        } else if (post.has("stories_netego") && Pref.removeStoryBlock()) {
          Log.i(TAG, "Removing stories block");
          continue;
        }else if (post.has("clips_netego") && Pref.removeSuggReels()) {
          Log.i(TAG, "Removing suggested reels");
          continue;
        }else if (post.has("bloks_netego") && Pref.removeThreadsBlock()) {
          Log.i(TAG, "Removing suggested reels");
          continue;
        }
        else if (post.has("explore_story") && Pref.removeSuggPost()) {
          media = post.optJSONObject("explore_story").optJSONObject("media_or_ad");
          Log.i(TAG, "Removing suggested post by "+ media.getJSONObject("user").getString("username"));
          continue;
        }
        rebuiltFeed.put(post);
      }
      r.put(node, rebuiltFeed);
    }
    return r.toString();
  }

  private static String modifyExplorerResponse(JSONObject r)throws JSONException, UnsupportedEncodingException {
    JSONArray rebuiltFeed = new JSONArray();
    JSONArray items = r.optJSONArray("sectional_items");
    
    if (items != null) {
      for (int i = 0; i < items.length(); i++) {
        JSONObject post = items.getJSONObject(i);
        String feed_type = post.getString("feed_type");
        if(feed_type.equals("media_or_ad")){
          JSONObject layout_content = post.getJSONObject("layout_content");
          JSONObject media = layout_content.getJSONObject("two_by_two_ad_item").getJSONObject("media_or_ad");
          if(media.has("injected") && Pref.removedAds()){
            layout_content.remove("two_by_two_ad_item");
            post.put("layout_content",layout_content);
            Log.i(TAG,"Removing ad by " +media.getJSONObject("user").getString("username"));
          }
        }
       
        rebuiltFeed.put(post);
      }
      r.put("sectional_items", rebuiltFeed);
    }
    return r.toString();
  }

  private static String modifyInjectedStoriesResponse(JSONObject r)throws JSONException, UnsupportedEncodingException {
    r.put("reels", new JSONObject());
    return r.toString();
  }

  //others
  private static byte[] str2Bytes(String r)
    throws UnsupportedEncodingException {
    return r.getBytes("UTF-8");
  }

}
