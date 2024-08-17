package moe.kirao.mgx;

import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.drinkmore.Tracer;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.tool.UI;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.vkryl.core.reference.ReferenceList;
import me.vkryl.leveldb.LevelDB;

public class MoexConfig {

  private static final int VERSION = 1;
  private static final AtomicBoolean hasInstance = new AtomicBoolean(false);
  private static volatile MoexConfig instance;
  private final LevelDB config;
  private static final String KEY_VERSION = "version";

  public static final String KEY_DISABLE_CAMERA_BUTTON = "disable_camera_button";
  public static final String KEY_DISABLE_RECORD_BUTTON = "disable_record_button";
  public static final String KEY_DISABLE_COMMANDS_BUTTON = "disable_commands_button";
  public static final String KEY_HIDE_STICKER_TIMESTAMP = "hide_sticker_timestamp";
  public static final String KEY_ENABLE_FEATURES_BUTTON = "enable_features_button";
  public static final String KEY_HIDE_PHONE_NUMBER = "hide_phone_number";
  public static final String KEY_SHOW_ID_PROFILE = "show_id_profile";
  public static final String KEY_DISABLE_SEND_AS_BUTTON = "disable_send_as_button";
  public static final String KEY_ROUNDED_STICKERS = "rounded_stickers";
  public static final String KEY_INCREASE_RECENTS_COUNT = "increase_recents_count";
  public static final String KEY_HIDE_MESSAGES_BADGE = "hide_messages_badge";
  public static final String KEY_CHANGE_SIZE_LIMIT = "change_size_limit";
  public static final String KEY_REMEMBER_SEND_OPTIONS = "remember_send_options";
  public static final String KEY_REMEMBER_SEND_OPTIONS_AUTHOR = "remember_send_options_author";
  public static final String KEY_REMEMBER_SEND_OPTIONS_CAPTIONS = "remember_send_options_captions";
  public static final String KEY_REMEMBER_SEND_OPTIONS_SOUND = "remember_send_options_sound";
  public static final String KEY_SQUARE_AVATAR = "square_avatar";
  public static final String KEY_BLUR_DRAWER = "blur_drawer";
  public static final String KEY_CHANGE_HEADER_TEXT = "change_header_text";
  public static final String KEY_TYPING_INSTEAD_CHOOSING = "typing_instead_choosing";
  public static final String KEY_DISABLE_REACTIONS = "disable_reactions";
  public static final String KEY_HIDE_BOTTOM_BAR = "hide_bottom_bar";
  public static final String KEY_DARKEN_DRAWER = "darken_drawer";

  public static final int SIZE_LIMIT_800 = 0;
  public static final int SIZE_LIMIT_1280 = 1;
  public static final int SIZE_LIMIT_2560 = 2;
  public static final int HEADER_TEXT_CHATS = 0;
  public static final int HEADER_TEXT_MOEX = 1;
  public static final int HEADER_TEXT_USERNAME = 2;
  public static final int HEADER_TEXT_NAME = 3;

  public static boolean disableCameraButton = instance().getBoolean(KEY_DISABLE_CAMERA_BUTTON, false);
  public static boolean disableRecordButton = instance().getBoolean(KEY_DISABLE_RECORD_BUTTON, false);
  public static boolean disableCommandsButton = instance().getBoolean(KEY_DISABLE_COMMANDS_BUTTON, false);
  public static boolean disableSendAsButton = instance().getBoolean(KEY_DISABLE_SEND_AS_BUTTON, false);
  public static boolean hideStickerTimestamp = instance().getBoolean(KEY_HIDE_STICKER_TIMESTAMP, false);
  public static boolean enableTestFeatures = instance().getBoolean(KEY_ENABLE_FEATURES_BUTTON, false);
  public static boolean hidePhoneNumber = instance().getBoolean(KEY_HIDE_PHONE_NUMBER, false);
  public static boolean showId = instance().getBoolean(KEY_SHOW_ID_PROFILE, false);
  public static boolean roundedStickers = instance().getBoolean(KEY_ROUNDED_STICKERS, false);
  public static boolean increaseRecents = instance().getBoolean(KEY_INCREASE_RECENTS_COUNT, false);
  public static boolean hideMessagesBadge = instance().getBoolean(KEY_HIDE_MESSAGES_BADGE, false);
  public static boolean rememberOptions = instance().getBoolean(KEY_REMEMBER_SEND_OPTIONS, false);
  public static boolean squareAvatar = instance().getBoolean(KEY_SQUARE_AVATAR, false);
  public static boolean blurDrawer = instance().getBoolean(KEY_BLUR_DRAWER, false);
  public static boolean typingInsteadChoosing = instance().getBoolean(KEY_TYPING_INSTEAD_CHOOSING, true);
  public static boolean disableReactions = instance().getBoolean(KEY_DISABLE_REACTIONS, false);
  public static boolean hideBottomBar = instance().getBoolean(KEY_HIDE_BOTTOM_BAR, false);
  public static boolean darkenDrawer = instance().getBoolean(KEY_DARKEN_DRAWER, false);

  private MoexConfig () {
    File configDir = new File(UI.getAppContext().getFilesDir(), "moexconf");
    if (!configDir.exists() && !configDir.mkdir()) {
      throw new IllegalStateException("Unable to create working directory");
    }
    long ms = SystemClock.uptimeMillis();
    config = new LevelDB(new File(configDir, "db").getPath(), true, new LevelDB.ErrorHandler() {
      @Override public boolean onFatalError (LevelDB levelDB, Throwable error) {
        Tracer.onDatabaseError(error);
        return true;
      }

      @Override public void onError (LevelDB levelDB, String message, @Nullable Throwable error) {
        // Cannot use custom Log, since settings are not yet loaded
        android.util.Log.e(Log.LOG_TAG, message, error);
      }
    });
    int configVersion = 0;
    try {
      configVersion = Math.max(0, config.tryGetInt(KEY_VERSION));
    } catch (FileNotFoundException ignored) {
    }
    if (configVersion > VERSION) {
      Log.e("Downgrading database version: %d -> %d", configVersion, VERSION);
      config.putInt(KEY_VERSION, VERSION);
    }
    for (int version = configVersion + 1; version <= VERSION; version++) {
      SharedPreferences.Editor editor = config.edit();
      editor.putInt(KEY_VERSION, version);
      editor.apply();
    }
    Log.i("Opened database in %dms", SystemClock.uptimeMillis() - ms);
  }

  public static MoexConfig instance () {
    if (instance == null) {
      synchronized (MoexConfig.class) {
        if (instance == null) {
          if (hasInstance.getAndSet(true)) throw new AssertionError();
          instance = new MoexConfig();
        }
      }
    }
    return instance;
  }

  public LevelDB edit () {
    return config.edit();
  }

  public void remove (String key) {
    config.remove(key);
  }

  public void putLong (String key, long value) {
    config.putLong(key, value);
  }

  public long getLong (String key, long defValue) {
    return config.getLong(key, defValue);
  }

  public void putLongArray (String key, long[] value) {
    config.putLongArray(key, value);
  }

  public long[] getLongArray (String key) {
    return config.getLongArray(key);
  }

  public void putInt (String key, int value) {
    config.putInt(key, value);
  }

  public int getInt (String key, int defValue) {
    return config.getInt(key, defValue);
  }

  public void putFloat (String key, float value) {
    config.putFloat(key, value);
  }

  public void getFloat (String key, float defValue) {
    config.getFloat(key, defValue);
  }

  public void putBoolean (String key, boolean value) {
    config.putBoolean(key, value);
  }

  public boolean getBoolean (String key, boolean defValue) {
    return config.getBoolean(key, defValue);
  }

  public void putString (String key, @NonNull String value) {
    config.putString(key, value);
  }

  public String getString (String key, String defValue) {
    return config.getString(key, defValue);
  }

  public boolean containsKey (String key) {
    return config.contains(key);
  }

  public LevelDB config () {
    return config;
  }

  public interface SettingsChangeListener {
    void onSettingsChanged (String key, Object newSettings, Object oldSettings);
  }

  private ReferenceList<SettingsChangeListener> settingsListeners;

  public void addSettingsListener (SettingsChangeListener listener) {
    if (settingsListeners == null)
      settingsListeners = new ReferenceList<>();
    settingsListeners.add(listener);
  }

  public void removeSettingsListener (SettingsChangeListener listener) {
    if (settingsListeners != null) {
      settingsListeners.remove(listener);
    }
  }

  private void notifyClientListeners (String key, Object newSettings, Object oldSettings) {
    if (settingsListeners != null) {
      for (SettingsChangeListener listener : settingsListeners) {
        listener.onSettingsChanged(key, newSettings, oldSettings);
      }
    }
  }

  public void toggleDisableCameraButton () {
    notifyClientListeners(KEY_DISABLE_CAMERA_BUTTON, !disableCameraButton, disableCameraButton);
    putBoolean(KEY_DISABLE_CAMERA_BUTTON, disableCameraButton ^= true);
  }

  public void toggleDisableRecordButton () {
    notifyClientListeners(KEY_DISABLE_RECORD_BUTTON, !disableRecordButton, disableRecordButton);
    putBoolean(KEY_DISABLE_RECORD_BUTTON, disableRecordButton ^= true);
  }

  public void toggleDisableCommandsButton () {
    notifyClientListeners(KEY_DISABLE_COMMANDS_BUTTON, !disableCommandsButton, disableCommandsButton);
    putBoolean(KEY_DISABLE_COMMANDS_BUTTON, disableCommandsButton ^= true);
  }

  public void toggleDisableStickerTimestamp () {
    putBoolean(KEY_HIDE_STICKER_TIMESTAMP, hideStickerTimestamp ^= true);
  }

  public void toggleEnableFeaturesButton () {
    putBoolean(KEY_ENABLE_FEATURES_BUTTON, enableTestFeatures ^= true);
  }

  public void toggleHidePhoneNumber () {
    putBoolean(KEY_HIDE_PHONE_NUMBER, hidePhoneNumber ^= true);
    notifyClientListeners(KEY_HIDE_PHONE_NUMBER, !hidePhoneNumber, hidePhoneNumber);
  }

  public void toggleShowIdProfile () {
    putBoolean(KEY_SHOW_ID_PROFILE, showId ^= true);
  }

  public void toggleDisableSendAsButton () {
    notifyClientListeners(KEY_DISABLE_SEND_AS_BUTTON, !disableSendAsButton, disableSendAsButton);
    putBoolean(KEY_DISABLE_SEND_AS_BUTTON, disableSendAsButton ^= true);
  }

  public void toggleRoundedStickers () {
    putBoolean(KEY_ROUNDED_STICKERS, roundedStickers ^= true);
  }

  public void toggleIncreaseRecents () {
    notifyClientListeners(KEY_INCREASE_RECENTS_COUNT, !increaseRecents, increaseRecents);
    putBoolean(KEY_INCREASE_RECENTS_COUNT, increaseRecents ^= true);
  }

  public void toggleHideMessagesBadge () {
    putBoolean(KEY_HIDE_MESSAGES_BADGE, hideMessagesBadge ^= true);
  }

  public int getSizeLimit () {
    return getInt(KEY_CHANGE_SIZE_LIMIT, SIZE_LIMIT_1280);
  }

  public void setSizeLimit (int size) {
    if (size == SIZE_LIMIT_1280) {
      remove(KEY_CHANGE_SIZE_LIMIT);
    } else {
      putInt(KEY_CHANGE_SIZE_LIMIT, size);
    }
  }

  public void toggleRememberSendOptions () {
    putBoolean(KEY_REMEMBER_SEND_OPTIONS, rememberOptions ^= true);
  }

  public void SendAsCopy (boolean state) {
    putBoolean(KEY_REMEMBER_SEND_OPTIONS_AUTHOR, state);
  }

  public Boolean getAuthorState () {
    return getBoolean(KEY_REMEMBER_SEND_OPTIONS_AUTHOR, false);
  }

  public void SendWithoutCaption (boolean state) {
    putBoolean(KEY_REMEMBER_SEND_OPTIONS_CAPTIONS, state);
  }

  public Boolean getCaptionState () {
    return getBoolean(KEY_REMEMBER_SEND_OPTIONS_CAPTIONS, false);
  }

  public void SendSilent (boolean state) {
    putBoolean(KEY_REMEMBER_SEND_OPTIONS_SOUND, state);
  }

  public Boolean getSilentState () {
    return getBoolean(KEY_REMEMBER_SEND_OPTIONS_SOUND, false);
  }

  public void toggleSquareAvatar () {
    putBoolean(KEY_SQUARE_AVATAR, squareAvatar ^= true);
  }

  public void toggleBlurDrawer () {
    putBoolean(KEY_BLUR_DRAWER, blurDrawer ^= true);
    notifyClientListeners(KEY_BLUR_DRAWER, !blurDrawer, blurDrawer);
  }

  public int getHeaderText () {
    return getInt(KEY_CHANGE_HEADER_TEXT, HEADER_TEXT_MOEX);
  }

  public void setHeaderText (int mode) {
    int oldState = getHeaderText();
    if (mode == HEADER_TEXT_MOEX) {
      remove(KEY_CHANGE_HEADER_TEXT);
    } else {
      putInt(KEY_CHANGE_HEADER_TEXT, mode);
    }
    notifyClientListeners(KEY_CHANGE_HEADER_TEXT, mode, oldState);
  }

  public void toggleTypingInsteadChoosing () {
    putBoolean(KEY_TYPING_INSTEAD_CHOOSING, typingInsteadChoosing ^= true);
  }

  public void toggleDisableReactions () {
    putBoolean(KEY_DISABLE_REACTIONS, disableReactions ^= true);
  }

  public void toggleHideBottomBar () {
    putBoolean(KEY_HIDE_BOTTOM_BAR, hideBottomBar ^= true);
  }

  public void toggleDarkenDrawer () {
    putBoolean(KEY_DARKEN_DRAWER, darkenDrawer ^= true);
  }
}
