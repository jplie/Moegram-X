/*
 * This file is created under moeGramX project development under GPLv3 license.
 * Copyright © Kira Roubin (jplie), 2024 (kirao@kiri.su)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package moe.kirao.mgx.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.drinkless.tdlib.TdApi;
import org.jetbrains.annotations.NotNull;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TGMessage;
import org.thunderdog.challegram.data.ThreadInfo;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.util.StringList;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import me.vkryl.core.StringUtils;
import me.vkryl.core.collection.IntList;
import me.vkryl.core.lambda.RunnableData;
import me.vkryl.td.MessageId;
import moe.kirao.mgx.utils.ChatUtils;
import moe.kirao.mgx.utils.SystemUtils;

public class MessageDetailsController extends RecyclerViewController<MessageDetailsController.Args> implements View.OnClickListener {
  public static final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
    private final ArrayList<String> skipFields = new ArrayList<>() {
      {
        add("outline");
        add("data");
        add("waveform");
        add("minithumbnail");
        add("id");
        add("uniqueId");
        add("remote");
      }
    };

    @Override
    public boolean shouldSkipField (FieldAttributes f) {
      return skipFields.contains(f.getName());
    }

    @Override
    public boolean shouldSkipClass (Class<?> clazz) {
      return false;
    }
  }).create();

  public static class Args {
    public final TdApi.Message msg;
    public final ThreadInfo messageThread;

    public Args (TGMessage msg, ThreadInfo messageThread) {
      this.msg = msg.getMessage();
      this.messageThread = messageThread;
    }
  }

  private final Args args;

  private static final int TRIM_MODE_NAME = 0;
  private static final int TRIM_MODE_USERNAME = 1;
  private static final int TRIM_MODE_UNCHANGED = 2;

  private static final String stopWord = "¯\\_(ツ)_/¯";

  private boolean resolvedLocally;

  public MessageDetailsController (Context context, Tdlib tdlib, Args args) {
    super(context, tdlib);
    this.args = args;
  }

  @Override
  public int getId () {
    return R.id.controller_details;
  }

  @Override
  protected int getMenuId () {
    return R.id.menu_btn_copy;
  }

  @Override
  public void fillMenuItems (int id, HeaderView header, LinearLayout menu) {
    if (id == R.id.menu_btn_copy) {
      header.addCopyButton(menu, this, ColorId.headerIcon);
    }
  }

  @Override
  public void onMenuItemPressed (int id, View view) {
    if (id == R.id.menu_btn_copy) {
      UI.copyText(gson.toJson(args.msg), R.string.CopiedText);
    }
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.MsgDetails);
  }

  /**
   * Convert date from UnixTimestamp and return (edit)date context string
   **/
  private String getDate (int unixt, boolean Edited) {
    return Edited ? Lang.getModifiedTimestamp(unixt, TimeUnit.SECONDS) : Lang.getRelativeTimestamp(unixt, TimeUnit.SECONDS);
  }

  /** @noinspection deprecation*/
  private void openPath (String path) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.putExtra(Intent.EXTRA_STREAM, SystemUtils.getUri(path));
    intent.setDataAndType(SystemUtils.getUri(path), U.resolveMimeType(path));
    context.startActivityForResult(Intent.createChooser(intent, null), 500);
  }

  private String getDocumentRes (String path) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(path, options);
    return options.outWidth + "x" + options.outHeight;
  }

  /** Strip unneeded symbols from given text and returns only requested info*/
  private String trimText (String info, int mode) {
    if (resolvedLocally) {
      String[] items = info.split("\n");
      switch (mode) {
        case TRIM_MODE_NAME:
          return items[1];
        case TRIM_MODE_USERNAME:
          return items.length != 2 ? items[2] : "";
        case TRIM_MODE_UNCHANGED:
          return info;
      }
    }

    String[] items = info.split("\n");
    var user = new TdApi.User();

    for (String line : items) {
      if (line.contains("\uD83D\uDC64")) {
        user.id = Long.parseLong(line.replaceAll("\\D+", ""));
      } else if (line.contains("\uD83D\uDC66\uD83C\uDFFB")) {
        user.firstName = line.replaceAll("^.+? ", "");
      } else if (line.contains("\u2063\uD83C\uDF10")) {
        line = line.replaceAll("^.+? ", "");
        user.usernames = new TdApi.Usernames(null, null, line);
      }
    }
    return mode == TRIM_MODE_NAME ? user.firstName : mode == TRIM_MODE_USERNAME
      ? user.usernames != null ? user.usernames.editableUsername : ""
      : String.join("\n", String.valueOf(user.id), user.firstName, user.usernames != null ? user.usernames.editableUsername : "");
  }

  private int getConstructor () {
    return args.msg.content.getConstructor();
  }

  private String getMsgContent (int constructor, String type) {
    TdApi.Message msg = args.msg;
    switch (constructor) {
      case TdApi.MessageText.CONSTRUCTOR:
        return type.equals("Text") ? ((TdApi.MessageText) msg.content).text.text : "";
      case TdApi.MessagePhoto.CONSTRUCTOR:
        TdApi.PhotoSize[] photoSizes = ((TdApi.MessagePhoto) msg.content).photo.sizes;
        TdApi.File photo = photoSizes[photoSizes.length - 1].photo;
        switch (type) {
          case "Text":
            return ((TdApi.MessagePhoto) msg.content).caption.text;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), photo.expectedSize);
          case "Mime":
            return U.resolveMimeType(photo.local.path);
          case "Path":
            return photo.local.path;
          case "Resolution":
            return photoSizes[photoSizes.length - 1].width + "x" + photoSizes[photoSizes.length - 1].height;
          default:
            return "";
        }
      case TdApi.MessageDocument.CONSTRUCTOR:
        TdApi.Document document = ((TdApi.MessageDocument) msg.content).document;
        switch (type) {
          case "Text":
            return ((TdApi.MessageDocument) msg.content).caption.text;
          case "Path":
            return document.document.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), document.document.size);
          case "Mime":
            return document.mimeType;
          case "Name":
            return document.fileName;
          case "Resolution":
            U.MediaMetadata metadata = U.getMediaMetadata(document.document.local.path);
            return metadata != null ? metadata.width + "x" + metadata.height : getDocumentRes(document.document.local.path);
          default:
            return "";
        }
      case TdApi.MessageVideo.CONSTRUCTOR:
        TdApi.Video video = ((TdApi.MessageVideo) msg.content).video;
        switch (type) {
          case "Text":
            return ((TdApi.MessageVideo) msg.content).caption.text;
          case "Path":
            return video.video.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), video.video.expectedSize);
          case "Mime":
            return video.mimeType;
          case "Name":
            return video.fileName;
          case "Resolution":
            return video.width + "x" + video.height;
          case "Duration":
            return DateUtils.formatElapsedTime(video.duration);
          case "Bitrate":
            U.MediaMetadata metadata = U.getMediaMetadata(video.video.local.path);
            return (metadata != null ? metadata.bitrate / 1000 : video.video.expectedSize / video.duration * 8 / 1000) + " Kbps";
          default:
            return "";
        }
      case TdApi.MessageSticker.CONSTRUCTOR:
        TdApi.Sticker sticker = ((TdApi.MessageSticker) msg.content).sticker;
        switch (type) {
          case "Path":
            return sticker.sticker.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), sticker.sticker.expectedSize);
          case "Mime":
            return U.resolveMimeType(sticker.sticker.local.path);
          case "Resolution":
            return sticker.width + "x" + sticker.height;
          case "Emoji":
            return sticker.emoji;
          case "packId":
            return String.valueOf(sticker.setId); // probably should be passed with getAuthorId()
          default:
            return "";
        }
      case TdApi.MessageAudio.CONSTRUCTOR:
        TdApi.Audio audio = ((TdApi.MessageAudio) msg.content).audio;
        switch (type) {
          case "Text":
            return ((TdApi.MessageAudio) msg.content).caption.text;
          case "Path":
            return audio.audio.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), audio.audio.expectedSize);
          case "Mime":
            return audio.mimeType;
          case "Name":
            return audio.fileName;
          case "SongName":
            return audio.title;
          case "Performer":
            return audio.performer;
          case "Duration":
            return DateUtils.formatElapsedTime(audio.duration);
          case "Bitrate":
            U.MediaMetadata metadata = U.getMediaMetadata(audio.audio.local.path);
            return (metadata != null ? metadata.bitrate / 1000 : audio.audio.expectedSize / audio.duration * 8 / 1000) + " Kbps";
          default:
            return "";
        }
      case TdApi.MessageAnimation.CONSTRUCTOR:
        TdApi.Animation animation = ((TdApi.MessageAnimation) msg.content).animation;
        switch (type) {
          case "Text":
            return ((TdApi.MessageAnimation) msg.content).caption.text;
          case "Path":
            return animation.animation.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), animation.animation.expectedSize);
          case "Mime":
            return animation.mimeType;
          case "Name":
            return animation.fileName;
          case "Duration":
            return DateUtils.formatElapsedTime(animation.duration);
          case "Resolution":
            return animation.width + "x" + animation.height;
          default:
            return "";
        }
      case TdApi.MessageVoiceNote.CONSTRUCTOR:
        TdApi.VoiceNote voiceNote = ((TdApi.MessageVoiceNote) msg.content).voiceNote;
        switch (type) {
          case "Text":
            return ((TdApi.MessageVoiceNote) msg.content).caption.text;
          case "Path":
            return voiceNote.voice.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), voiceNote.voice.expectedSize);
          case "Mime":
            return voiceNote.mimeType;
          case "Duration":
            return DateUtils.formatElapsedTime(voiceNote.duration);
          case "Bitrate":
            U.MediaMetadata metadata = U.getMediaMetadata(voiceNote.voice.local.path);
            return (metadata != null ? metadata.bitrate / 1000 : voiceNote.voice.expectedSize / voiceNote.duration * 8 / 1000) + " Kbps";
          default:
            return "";
        }
      case TdApi.MessageVideoNote.CONSTRUCTOR:
        TdApi.VideoNote videoNote = ((TdApi.MessageVideoNote) msg.content).videoNote;
        U.MediaMetadata metadata = U.getMediaMetadata(videoNote.video.local.path);
        switch (type) {
          case "Path":
            return videoNote.video.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), videoNote.video.expectedSize);
          case "Mime":
            return U.resolveMimeType(videoNote.video.local.path);
          case "Duration":
            return DateUtils.formatElapsedTime(videoNote.duration);
          case "Resolution":
            return metadata != null ? metadata.width + "x" + metadata.height : "";
          case "Bitrate":
            return (metadata != null ? metadata.bitrate / 1000 : videoNote.video.expectedSize / videoNote.duration * 8 / 1000) + " Kbps";
          default:
            return "";
        }
      case TdApi.MessageAnimatedEmoji.CONSTRUCTOR:
        TdApi.AnimatedEmoji customEmoji = ((TdApi.MessageAnimatedEmoji) msg.content).animatedEmoji;
        if (customEmoji.sticker == null) return "";
        switch (type) {
          case "Path":
            return customEmoji.sticker.sticker.local.path;
          case "Size":
            return Formatter.formatShortFileSize(UI.getContext(), customEmoji.sticker.sticker.expectedSize);
          case "Mime":
            return U.resolveMimeType(customEmoji.sticker.sticker.local.path);
          case "Resolution":
            return customEmoji.sticker.width + "x" + customEmoji.sticker.height;
          case "Emoji":
            return customEmoji.sticker.emoji;
          case "packId":
            return String.valueOf(customEmoji.sticker.setId); // probably should be passed with getAuthorId()
          default:
            return "";
        }
      case TdApi.MessageStory.CONSTRUCTOR:
        TdApi.MessageStory story = ((TdApi.MessageStory) msg.content);
        return type.equals("Id") ? String.valueOf(story.storyId) : "";
      default:
        return "";
    }
  }

  private long getAuthorId (boolean int32) {
    switch (getConstructor()) {
      case TdApi.MessageSticker.CONSTRUCTOR:
        TdApi.Sticker sticker = ((TdApi.MessageSticker) args.msg.content).sticker;
        return int32 ? sticker.setId >> 32 : 0x100000000L + (sticker.setId >> 32);
      case TdApi.MessageAnimatedEmoji.CONSTRUCTOR:
        TdApi.Sticker animatedEmoji = ((TdApi.MessageAnimatedEmoji) args.msg.content).animatedEmoji.sticker;
        return animatedEmoji != null ? int32 ? animatedEmoji.setId >> 32 : 0x100000000L + (animatedEmoji.setId >> 32) : 0;
      case TdApi.MessageStory.CONSTRUCTOR:
        TdApi.MessageStory story = ((TdApi.MessageStory) args.msg.content);
        return story.storySenderChatId;
    }
    return 0;
  }

  /**
   * Returns that specified item should or shouldn't displayed
   **/
  private boolean hasItems (String item) {
    switch (item) {
      case "edited":
        return args.msg.editDate != 0 && args.msg.editDate != args.msg.date;
      case "attachCaption":
        // Here we check if message contain text
        switch (getConstructor()) {
          case TdApi.MessagePhoto.CONSTRUCTOR:
            return !StringUtils.isEmpty(((TdApi.MessagePhoto) args.msg.content).caption.text);
          case TdApi.MessageDocument.CONSTRUCTOR:
            return !StringUtils.isEmpty(((TdApi.MessageDocument) args.msg.content).caption.text);
          case TdApi.MessageVideo.CONSTRUCTOR:
            return !StringUtils.isEmpty(((TdApi.MessageVideo) args.msg.content).caption.text);
          case TdApi.MessageAnimation.CONSTRUCTOR:
            return !StringUtils.isEmpty(((TdApi.MessageAnimation) args.msg.content).caption.text);
          case TdApi.MessageVoiceNote.CONSTRUCTOR:
            return !StringUtils.isEmpty(((TdApi.MessageVoiceNote) args.msg.content).caption.text);
          default:
            return false;
        }
      case "path":
        String path = getMsgContent(getConstructor(), "Path");
        return !StringUtils.isEmptyOrBlank(path);
      case "name":
        String name = getMsgContent(getConstructor(), "Name");
        return !StringUtils.isEmptyOrBlank(name);
      case "mime":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Mime"));
      case "signature":
        return !StringUtils.isEmptyOrBlank(args.msg.authorSignature);
      case "userChat":
        return !tdlib.isChannel(args.msg.senderId) && tdlib.senderUserId(args.msg) == args.msg.chatId; // Message sent to featured chat
      case "bitrate":
        String bitrate = getMsgContent(getConstructor(), "Bitrate");
        return !StringUtils.isEmptyOrBlank(bitrate);
      case "emoji":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Emoji"));
      case "author":
        return (getConstructor() == TdApi.MessageSticker.CONSTRUCTOR &&
          !getMsgContent(TdApi.MessageSticker.CONSTRUCTOR, "packId").equals("0")) || // Means that sticker does not belongs to pack
          getConstructor() == TdApi.MessageAnimatedEmoji.CONSTRUCTOR ||
          getConstructor() == TdApi.MessageStory.CONSTRUCTOR;
      case "duration":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Duration"));
      case "performer":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Performer"));
      case "songName":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "SongName"));
      case "resolution":
        String res = getMsgContent(getConstructor(), "Resolution");
        return !StringUtils.isEmpty(res) && !(res.equals("0x0") || res.equals("-1x-1"));
      case "size":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Size"));
      case "count":
        return !StringUtils.isEmptyOrBlank(getMsgContent(getConstructor(), "Id"));
    }
    return false;
  }

  private void fetchAuthor (RunnableData<String> after) {
    String localInfo = ChatUtils.resolveUserLocal(tdlib, getAuthorId(true));
    if (localInfo == null)
      localInfo = ChatUtils.resolveUserLocal(tdlib, getAuthorId(false));

    if (localInfo != null) {
      resolvedLocally = true;
      after.runWithData(localInfo);
    } else {
      ChatUtils.processAuthorRequest(tdlib, 189165596L, String.valueOf(getAuthorId(true)), info32 -> {
        if (!info32.equals(stopWord)) {
          after.runWithData(info32);
        } else if (getConstructor() != TdApi.MessageStory.CONSTRUCTOR) {
          ChatUtils.processAuthorRequest(tdlib, 189165596L, String.valueOf(getAuthorId(false)), info64 -> after.runWithData(!info64.equals(stopWord) ? info64 : null));
        }
      });
    }
  }

  private void openActions (@NonNull IntList ids, @NonNull StringList strings, @NonNull IntList icons, int buttonId, @StringRes int buttonStringId, int buttonIconId, CharSequence info, CharSequence text, @Nullable String data) {
    ids.append(R.id.btn_copyText);
    strings.append(R.string.Copy);
    icons.append(R.drawable.baseline_content_copy_24);

    if (buttonId != 0 && !(buttonId == R.id.btn_inlineOpen && StringUtils.isEmptyOrBlank(data))) {
      ids.append(buttonId);
      strings.append(buttonStringId);
      icons.append(buttonIconId);
    }

    showOptions(info, ids.get(), strings.get(), null, icons.get(), (itemView, id) -> {
      if (id == R.id.btn_copyText) {
        UI.copyText(text, R.string.CopiedText);
      } else if (id == R.id.btn_openGroupProfile) {
        tdlib.ui().openChatProfile(this, getConstructor() == TdApi.MessageStory.CONSTRUCTOR || resolvedLocally ?
          Long.parseLong(data) : args.msg.chatId, args.messageThread, new TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(itemView)));
      } else if (id == R.id.btn_openProfile) {
        tdlib.ui().openSenderProfile(this, args.msg.senderId, new TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(itemView)));
      } else if (id == R.id.btn_openPath) {
        openPath(data);
      } else if (id == R.id.btn_inlineOpen) {
        tdlib.ui().openUrl(this, tdlib.tMeUrl(data), null, null);
      }
      return true;
    });
  }

  @Override
  public void onClick (@NotNull View v) {
    int viewId = v.getId();
    IntList ids = new IntList(3);
    StringList strings = new StringList(3);
    IntList icons = new IntList(3);

    if (viewId == R.id.btn_chatIdDetails) {
      String username = !StringUtils.isEmpty(tdlib.chatUsername(args.msg.chatId)) ? '@' + tdlib.chatUsername(args.msg.chatId) : "";
      String title = tdlib.chatTitle(args.msg.chatId);
      String chatId = String.valueOf(args.msg.chatId);
      String senderInfo = tdlib.isSelfChat(args.msg.chatId) ? title : String.join("\n", chatId, title, username).trim();
      openActions(ids, strings, icons, R.id.btn_openGroupProfile, R.string.Open, R.drawable.baseline_group_24, senderInfo, senderInfo, chatId);
    } else if (viewId == R.id.btn_chatId) {
      String msgId = String.valueOf(MessageId.toServerMessageId(args.msg.id));
      openActions(ids, strings, icons, 0, 0, 0, msgId, msgId, null);
    } else if (viewId == R.id.btn_dateDetails) {
      String date = hasItems("edited") ?
        String.join("\n", Lang.getString(R.string.Sent) + " " + getDate(args.msg.date, false), Lang.getString(R.string.Edited) + " " + getDate(args.msg.editDate, false))
        : getDate(args.msg.date, false);
      openActions(ids, strings, icons, 0, 0, 0, date, date, null);
    } else if (viewId == R.id.btn_senderDetails) {
      TdApi.MessageSender sender = args.msg.senderId;
      String username = !StringUtils.isEmpty(tdlib.senderUsername(sender)) ? '@' + tdlib.senderUsername(sender) : "";
      String signature = args.msg.authorSignature;
      String name = hasItems("signature") ? signature : tdlib.senderName(sender);
      String userId = String.valueOf(tdlib.isChannel(sender) ? args.msg.chatId : tdlib.senderUserId(args.msg));
      String senderInfo = hasItems("signature") ? signature : String.join("\n", userId, name, username).trim();
      openActions(ids, strings, icons, R.id.btn_openProfile, R.string.Open, R.drawable.dot_baseline_acc_personal_24, senderInfo, senderInfo, userId);
    } else if (viewId == R.id.btn_authorDetails) {
      fetchAuthor(info -> runOnUiThreadOptional(() -> {
        if (!StringUtils.isEmpty(info)) {
          String username = trimText(info, TRIM_MODE_USERNAME);
          String text = trimText(info, TRIM_MODE_UNCHANGED).trim();
          boolean openById = resolvedLocally && getConstructor() != TdApi.MessageStory.CONSTRUCTOR;
          openActions(ids, strings, icons, openById ?
            R.id.btn_openGroupProfile : R.id.btn_inlineOpen, R.string.Open, R.drawable.dot_baseline_acc_personal_24, text, text, openById ?
            info.split("\n")[0].replaceAll("\\D+", "") : !StringUtils.isEmpty(username) ?
            username.replace("@", "") : null);
        } else {
          String authorId = String.join("\n", "int32: " + getAuthorId(true), "int64: " + getAuthorId(false));
          openActions(ids, strings, icons, R.id.btn_inlineOpen, R.string.Open, R.drawable.dot_baseline_acc_personal_24, authorId, authorId, null);
        }
      }));
    } else if (viewId == R.id.btn_filePath) {
      String path = getMsgContent(getConstructor(), "Path");
      openActions(ids, strings, icons, R.id.btn_openPath, R.string.Open, R.drawable.baseline_sd_storage_24, path, path, path);
    } else if (viewId == R.id.btn_size) {
      String size = getMsgContent(getConstructor(), "Size");
      openActions(ids, strings, icons, 0, 0, 0, size, size, null);
    } else if (viewId == R.id.btn_mime) {
      String mime = getMsgContent(getConstructor(), "Mime");
      openActions(ids, strings, icons, 0, 0, 0, mime, mime, null);
    } else if (viewId == R.id.btn_fileRes) {
      String resolution = getMsgContent(getConstructor(), "Resolution");
      openActions(ids, strings, icons, 0, 0, 0, resolution, resolution, null);
    } else if (viewId == R.id.btn_fileCaption) {
      String text = getMsgContent(getConstructor(), "Text");
      openActions(ids, strings, icons, 0, 0, 0, text, text, null);
    } else if (viewId == R.id.btn_fileName) {
      String name = getMsgContent(getConstructor(), "Name");
      openActions(ids, strings, icons, 0, 0, 0, name, name, null);
    } else if (viewId == R.id.btn_fileDuration) {
      String duration = getMsgContent(getConstructor(), "Duration");
      openActions(ids, strings, icons, 0, 0, 0, duration, duration, null);
    } else if (viewId == R.id.btn_audioPerformerDetails) {
      String performer = getMsgContent(getConstructor(), "Performer");
      openActions(ids, strings, icons, 0, 0, 0, performer, performer, null);
    } else if (viewId == R.id.btn_audioSongNameDetails) {
      String songName = getMsgContent(getConstructor(), "SongName");
      openActions(ids, strings, icons, 0, 0, 0, songName, songName, null);
    } else if (viewId == R.id.btn_stickerEmojiDetails) {
      String emoji = getMsgContent(getConstructor(), "Emoji");
      openActions(ids, strings, icons, 0, 0, 0, emoji, emoji, null);
    } else if (viewId == R.id.btn_mediaBitrate) {
      String bitrate = getMsgContent(getConstructor(), "Bitrate");
      openActions(ids, strings, icons, 0, 0, 0, bitrate, bitrate, null);
    } else if (viewId == R.id.btn_storyId) {
      String storyId = getMsgContent(getConstructor(), "Id");
      openActions(ids, strings, icons, 0, 0, 0, storyId, storyId, null);
    }
  }

  @Override
  protected void onCreateView (@NotNull Context context, @NotNull CustomRecyclerView recyclerView) {
    SettingsAdapter adapter = new SettingsAdapter(this) {
      @Override
      protected void setValuedSetting (@NotNull ListItem item, @NotNull SettingView view, boolean isUpdate) {
        view.setDrawModifier(item.getDrawModifier());
        int itemId = item.getId();
        if (itemId == R.id.btn_chatIdDetails) {
          view.setData(tdlib.chatTitle(args.msg.chatId));
        } else if (itemId == R.id.btn_chatId) {
          view.setData(String.valueOf(MessageId.toServerMessageId(args.msg.id)));
        } else if (itemId == R.id.btn_dateDetails) {
          view.setData(hasItems("edited") ? getDate(args.msg.editDate, true) : getDate(args.msg.date, false));
        } else if (itemId == R.id.btn_fileCaption) {
          view.setData(getMsgContent(getConstructor(), "Text"));
        } else if (itemId == R.id.btn_senderDetails) {
          TdApi.MessageSender sender = args.msg.senderId;
          view.setData(tdlib.isChannel(sender) ?
            hasItems("signature") ? args.msg.authorSignature : tdlib.senderName(sender) :
            tdlib.senderName(sender));
        } else if (itemId == R.id.btn_authorDetails) {
          fetchAuthor(info -> view.setData(!StringUtils.isEmpty(info) ? trimText(info, TRIM_MODE_NAME) : Lang.getString(R.string.PhoneNumberUnknown)));
        } else if (itemId == R.id.btn_filePath) {
          view.setData(R.string.Open);
        } else if (itemId == R.id.btn_size) {
          view.setData(getMsgContent(getConstructor(), "Size"));
        } else if (itemId == R.id.btn_mime) {
          view.setData(getMsgContent(getConstructor(), "Mime"));
        } else if (itemId == R.id.btn_fileRes) {
          view.setData(getMsgContent(getConstructor(), "Resolution"));
        } else if (itemId == R.id.btn_fileName) {
          view.setData(getMsgContent(getConstructor(), "Name"));
        } else if (itemId == R.id.btn_fileDuration) {
          view.setData(getMsgContent(getConstructor(), "Duration"));
        } else if (itemId == R.id.btn_audioPerformerDetails) {
          view.setData(getMsgContent(getConstructor(), "Performer"));
        } else if (itemId == R.id.btn_audioSongNameDetails) {
          view.setData(getMsgContent(getConstructor(), "SongName"));
        } else if (itemId == R.id.btn_stickerEmojiDetails) {
          view.setData(getMsgContent(getConstructor(), "Emoji"));
        } else if (itemId == R.id.btn_mediaBitrate) {
          view.setData(getMsgContent(getConstructor(), "Bitrate"));
        } else if (itemId == R.id.btn_storyId) {
          view.setData(getMsgContent(getConstructor(), "Id"));
        }
      }
    };

    ArrayList<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    switch (getConstructor()) {
      case TdApi.MessageSticker.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Sticker));
        break;
      case TdApi.MessagePhoto.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Photo));
        break;
      case TdApi.MessageDocument.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Document));
        break;
      case TdApi.MessageVideoNote.CONSTRUCTOR:
      case TdApi.MessageVideo.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Video));
        break;
      case TdApi.MessageAudio.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Audio));
        break;
      case TdApi.MessageAnimation.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Gif));
        break;
      case TdApi.MessageVoiceNote.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Voice));
        break;
      case TdApi.MessageAnimatedEmoji.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.AnimatedEmoji));
        break;
      case TdApi.MessageStory.CONSTRUCTOR:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.RightStories));
        break;
      default:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.Message));
        break;
    }
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));

    if (!hasItems("userChat") || tdlib.isSelfChat(args.msg.chatId)) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_chatIdDetails, R.drawable.baseline_chat_bubble_24, tdlib.isChannel(args.msg.senderId) ? R.string.Channel : tdlib.isUserChat(args.msg.chatId) ? R.string.Chat : R.string.Group));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_chatId, R.drawable.baseline_identifier_24, R.string.Message));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_dateDetails, R.drawable.baseline_date_range_24, hasItems("edited") ? Lang.getString(R.string.Date) + '*' : Lang.getString(R.string.Date)));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    if (!tdlib.isChannel(args.msg.senderId) || hasItems("signature")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_senderDetails, R.drawable.baseline_person_24, R.string.SenderId));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("mime")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_mime, R.drawable.baseline_extension_24, R.string.MimeType));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("resolution")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_fileRes, R.drawable.baseline_crop_original_24, R.string.FileRes));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (getConstructor() == TdApi.MessageText.CONSTRUCTOR || hasItems("attachCaption")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_fileCaption, R.drawable.baseline_format_text_24, R.string.Message));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("path")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_filePath, R.drawable.baseline_map_24, R.string.FilePath));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("size")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_size, R.drawable.baseline_sd_storage_24, R.string.FileSize));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("name")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_fileName, R.drawable.deproko_baseline_text_add_24, R.string.FileName));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("duration")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_fileDuration, R.drawable.baseline_access_time_24, R.string.Duration));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("bitrate")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_mediaBitrate, R.drawable.baseline_bar_chart_24, R.string.Bitrate));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("performer")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_audioPerformerDetails, R.drawable.baseline_person_24, R.string.FilePerformer));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("songName")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_audioSongNameDetails, R.drawable.baseline_music_note_24, R.string.FileSongName));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("author")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_authorDetails, R.drawable.baseline_info_24, R.string.SetId));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("count")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_storyId, R.drawable.baseline_book_24, R.string.StoryId));
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    }
    if (hasItems("emoji")) {
      items.add(new ListItem(ListItem.TYPE_INFO_SETTING, R.id.btn_stickerEmojiDetails, R.drawable.baseline_emoticon_24, R.string.EmojiHeader));
    }
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
  }
}