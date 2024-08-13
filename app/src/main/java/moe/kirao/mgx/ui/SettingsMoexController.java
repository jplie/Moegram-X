package moe.kirao.mgx.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.SettingsWrapBuilder;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;

import moe.kirao.mgx.MoexConfig;

public class SettingsMoexController extends RecyclerViewController<SettingsMoexController.Args> implements View.OnClickListener, View.OnLongClickListener {
  public SettingsMoexController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override public int getId () {
    return R.id.controller_moexSettings;
  }

  public static final int CATEGORY_MAIN = 0;
  public static final int CATEGORY_GENERAL = 1;
  public static final int CATEGORY_INTERFACE = 2;
  public static final int CATEGORY_CHATS = 3;

  public static class Args {
    private final int category;

    public Args (int category) {
      this.category = category;
    }
  }

  private int category = CATEGORY_MAIN;

  @Override
  public void setArguments (Args args) {
    super.setArguments(args);
    this.category = args.category;
  }

  @Override public CharSequence getName () {
    return category == CATEGORY_MAIN ? Lang.getString(R.string.MoexSettings) : category == CATEGORY_CHATS ? Lang.getString(R.string.Chats) : category == CATEGORY_INTERFACE ? Lang.getString(R.string.InterfaceMoexSettings) : Lang.getString(R.string.GeneralMoexSettings);
  }

  private SettingsAdapter adapter;

  @Override public void onClick (View v) {
    int viewId = v.getId();
    SettingsMoexController c = new SettingsMoexController(context, tdlib);
    if (viewId == R.id.btn_GeneralMoexSettings) {
      c.setArguments(new SettingsMoexController.Args(SettingsMoexController.CATEGORY_GENERAL));
      navigateTo(c);
    } else if (viewId == R.id.btn_InterfaceMoexSettings) {
      c.setArguments(new SettingsMoexController.Args(SettingsMoexController.CATEGORY_INTERFACE));
      navigateTo(c);
    } else if (viewId == R.id.btn_ChatsMoexSettings) {
      c.setArguments(new SettingsMoexController.Args(SettingsMoexController.CATEGORY_CHATS));
      navigateTo(c);
    } else if (viewId == R.id.btn_moexCrowdinLink) {
      tdlib.ui().openUrl(this, Lang.getString(R.string.MoexCrowdinLink), new TdlibUi.UrlOpenParameters());
    } else if (viewId == R.id.btn_moexChatLink) {
      tdlib.ui().openUrl(this, Lang.getString(R.string.MoexChatLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    } else if (viewId == R.id.btn_moexChannelLink) {
      tdlib.ui().openUrl(this, Lang.getString(R.string.MoexChannelLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    } else if (viewId == R.id.btn_moexSourceLink) {
      tdlib.ui().openUrl(this, Lang.getString(R.string.MoexSourceLink), new TdlibUi.UrlOpenParameters());
    } else if (viewId == R.id.btn_build) {
      UI.showToast(R.string.cuteToast, Toast.LENGTH_SHORT);
    } else if (viewId == R.id.btn_hidePhone) {
      MoexConfig.instance().toggleHidePhoneNumber();
      adapter.updateValuedSettingById(R.id.btn_hidePhone);
    } else if (viewId == R.id.btn_enableFeaturesButton) {
      MoexConfig.instance().toggleEnableFeaturesButton();
      adapter.updateValuedSettingById(R.id.btn_enableFeaturesButton);
    } else if (viewId == R.id.btn_showIdProfile) {
      MoexConfig.instance().toggleShowIdProfile();
      adapter.updateValuedSettingById(R.id.btn_showIdProfile);
    } else if (viewId == R.id.btn_hideMessagesBadge) {
      MoexConfig.instance().toggleHideMessagesBadge();
      adapter.updateValuedSettingById(R.id.btn_hideMessagesBadge);
    } else if (viewId == R.id.btn_changeSizeLimit) {
      showChangeSizeLimit();
    } else if (viewId == R.id.btn_squareAvatar) {
      MoexConfig.instance().toggleSquareAvatar();
      adapter.updateValuedSettingById(R.id.btn_squareAvatar);
    } else if (viewId == R.id.btn_blurDrawer) {
      MoexConfig.instance().toggleBlurDrawer();
      adapter.updateValuedSettingById(R.id.btn_blurDrawer);
    } else if (viewId == R.id.btn_headerText) {
      showHeaderTextOptions();
    } else if (viewId == R.id.btn_disableReactions) {
      MoexConfig.instance().toggleDisableReactions();
      adapter.updateValuedSettingById(R.id.btn_disableReactions);
    } else if (viewId == R.id.btn_hideMessagePanelButtons) {
      showHideMessagePanelOptions();
    } else if (viewId == R.id.btn_hideBottomBar) {
      MoexConfig.instance().toggleHideBottomBar();
      adapter.updateValuedSettingById(R.id.btn_hideBottomBar);
    } else if (viewId == R.id.btn_disableStickerTimestamp) {
      MoexConfig.instance().toggleDisableStickerTimestamp();
      adapter.updateValuedSettingById(R.id.btn_disableStickerTimestamp);
    } else if (viewId == R.id.btn_roundedStickers) {
      MoexConfig.instance().toggleRoundedStickers();
      adapter.updateValuedSettingById(R.id.btn_roundedStickers);
    } else if (viewId == R.id.btn_IncreaseRecents) {
      MoexConfig.instance().toggleIncreaseRecents();
      adapter.updateValuedSettingById(R.id.btn_IncreaseRecents);
    } else if (viewId == R.id.btn_rememberOptions) {
      MoexConfig.instance().toggleRememberSendOptions();
      adapter.updateValuedSettingById(R.id.btn_rememberOptions);
    } else if (viewId == R.id.btn_typingInstead) {
      MoexConfig.instance().toggleTypingInsteadChoosing();
      adapter.updateValuedSettingById(R.id.btn_typingInstead);
    } else if (viewId == R.id.btn_darkenDrawer) {
      MoexConfig.instance().toggleDarkenDrawer();
      adapter.updateValuedSettingById(R.id.btn_darkenDrawer);
    }
  }

  @Override
  public boolean onLongClick (View v) {
    if (v.getId() == R.id.btn_build) {
      UI.copyText(String.join("\n", Lang.getStringSecure(R.string.MoexVer), U.getUsefulMetadata(tdlib)), R.string.CopiedText);
    }
    return false;
  }

  private void showChangeSizeLimit () {
    int sizeLimitOption = MoexConfig.instance().getSizeLimit();
    showSettings(new SettingsWrapBuilder(R.id.btn_changeSizeLimit).setRawItems(new ListItem[] {
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_sizeLimit800, 0, R.string.px800, R.id.btn_changeSizeLimit, sizeLimitOption == MoexConfig.SIZE_LIMIT_800),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_sizeLimit1280, 0, R.string.px1280, R.id.btn_changeSizeLimit, sizeLimitOption == MoexConfig.SIZE_LIMIT_1280),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_sizeLimit2560, 0, R.string.px2560, R.id.btn_changeSizeLimit, sizeLimitOption == MoexConfig.SIZE_LIMIT_2560),
    }).addHeaderItem(Lang.getMarkdownString(this, R.string.SizeLimitDesc)).setIntDelegate((id, result) -> {
      int sizeOption;
      int sizeLimit = result.get(R.id.btn_changeSizeLimit);
      if (sizeLimit == R.id.btn_sizeLimit800) {
        sizeOption = MoexConfig.SIZE_LIMIT_800;
      } else if (sizeLimit == R.id.btn_sizeLimit1280) {
        sizeOption = MoexConfig.SIZE_LIMIT_1280;
      } else {
        sizeOption = MoexConfig.SIZE_LIMIT_2560;
      }
      MoexConfig.instance().setSizeLimit(sizeOption);
      adapter.updateValuedSettingById(R.id.btn_changeSizeLimit);
    }));
  }

  private void showHideMessagePanelOptions () {
    showSettings(new SettingsWrapBuilder(R.id.btn_hideMessagePanelButtons).addHeaderItem(
      new ListItem(ListItem.TYPE_INFO, R.id.text_title, 0, R.string.HideCameraButtonInfo, false)).setRawItems(
      new ListItem[] {
        new ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_disableCameraButton, 0, R.string.DisableCameraButton, MoexConfig.disableCameraButton),
        new ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_disableCommandsButton, 0, R.string.DisableCommandsButton, MoexConfig.disableCommandsButton),
        new ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_disableRecordButton, 0, R.string.DisableRecordButton, MoexConfig.disableRecordButton),
        new ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_disableSendAsButton, 0, R.string.DisableSendAsButton, MoexConfig.disableSendAsButton)
      }).setIntDelegate((id, result) -> {
      if (MoexConfig.disableCameraButton == (result.get(R.id.btn_disableCameraButton) == 0)) {
        MoexConfig.instance().toggleDisableCameraButton();
      }
      if (MoexConfig.disableCommandsButton == (result.get(R.id.btn_disableCommandsButton) == 0)) {
        MoexConfig.instance().toggleDisableCommandsButton();
      }
      if (MoexConfig.disableRecordButton == (result.get(R.id.btn_disableRecordButton) == 0)) {
        MoexConfig.instance().toggleDisableRecordButton();
      }
      if (MoexConfig.disableSendAsButton == (result.get(R.id.btn_disableSendAsButton) == 0)) {
        MoexConfig.instance().toggleDisableSendAsButton();
      }
      adapter.updateValuedSettingById(R.id.btn_hideMessagePanelButtons);
    }));
  }

  private void showHeaderTextOptions () {
    int headerTextOption = MoexConfig.instance().getHeaderText();
    showSettings(new SettingsWrapBuilder(R.id.btn_headerText).setRawItems(new ListItem[] {
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_headerTextChats, 0, R.string.Chats, R.id.btn_headerText, headerTextOption == MoexConfig.HEADER_TEXT_CHATS),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_headerTextMoex, 0, R.string.moexHeaderClient, R.id.btn_headerText, headerTextOption == MoexConfig.HEADER_TEXT_MOEX),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_headerTextUsername, 0, R.string.Username, R.id.btn_headerText, headerTextOption == MoexConfig.HEADER_TEXT_USERNAME),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_headerTextName, 0, R.string.login_FirstName, R.id.btn_headerText, headerTextOption == MoexConfig.HEADER_TEXT_NAME),
    }).setIntDelegate((id, result) -> {
      int defaultOption;
      int headerText = result.get(R.id.btn_headerText);
      if (headerText == R.id.btn_headerTextChats) {
        defaultOption = MoexConfig.HEADER_TEXT_CHATS;
      } else if (headerText == R.id.btn_headerTextName) {
        defaultOption = MoexConfig.HEADER_TEXT_NAME;
      } else if (headerText == R.id.btn_headerTextUsername) {
        defaultOption = MoexConfig.HEADER_TEXT_USERNAME;
      } else {
        defaultOption = MoexConfig.HEADER_TEXT_MOEX;
      }
      MoexConfig.instance().setHeaderText(defaultOption);
      adapter.updateValuedSettingById(R.id.btn_headerText);
    }));
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        view.setDrawModifier(item.getDrawModifier());
        int itemId = item.getId();
        if (itemId == R.id.btn_moexCrowdinLink) {
          view.setData(R.string.MoexCrowdinText);
        } else if (itemId == R.id.btn_moexChatLink) {
          view.setData(R.string.moexChat);
        } else if (itemId == R.id.btn_moexChannelLink) {
          view.setData(R.string.moexChannel);
        } else if (itemId == R.id.btn_moexSourceLink) {
          view.setData(R.string.moexGithub);
        } else if (itemId == R.id.btn_hidePhone) {
          view.getToggler().setRadioEnabled(MoexConfig.hidePhoneNumber, isUpdate);
        } else if (itemId == R.id.btn_enableFeaturesButton) {
          view.getToggler().setRadioEnabled(MoexConfig.enableTestFeatures, isUpdate);
        } else if (itemId == R.id.btn_showIdProfile) {
          view.getToggler().setRadioEnabled(MoexConfig.showId, isUpdate);
        } else if (itemId == R.id.btn_hideMessagesBadge) {
          view.getToggler().setRadioEnabled(MoexConfig.hideMessagesBadge, isUpdate);
        } else if (itemId == R.id.btn_changeSizeLimit) {
          int size = MoexConfig.instance().getSizeLimit();
          switch (size) {
            case MoexConfig.SIZE_LIMIT_800:
              view.setData(R.string.px800);
              break;
            case MoexConfig.SIZE_LIMIT_1280:
              view.setData(R.string.px1280);
              break;
            case MoexConfig.SIZE_LIMIT_2560:
              view.setData(R.string.px2560);
              break;
          }
        } else if (itemId == R.id.btn_disableCameraButton) {
          view.getToggler().setRadioEnabled(MoexConfig.disableCameraButton, isUpdate);
        } else if (itemId == R.id.btn_disableRecordButton) {
          view.getToggler().setRadioEnabled(MoexConfig.disableRecordButton, isUpdate);
        } else if (itemId == R.id.btn_disableCommandsButton) {
          view.getToggler().setRadioEnabled(MoexConfig.disableCommandsButton, isUpdate);
        } else if (itemId == R.id.btn_disableSendAsButton) {
          view.getToggler().setRadioEnabled(MoexConfig.disableSendAsButton, isUpdate);
        } else if (itemId == R.id.btn_squareAvatar) {
          view.getToggler().setRadioEnabled(MoexConfig.squareAvatar, isUpdate);
        } else if (itemId == R.id.btn_blurDrawer) {
          view.getToggler().setRadioEnabled(MoexConfig.blurDrawer, isUpdate);
        } else if (itemId == R.id.btn_headerText) {
          int mode = MoexConfig.instance().getHeaderText();
          switch (mode) {
            case MoexConfig.HEADER_TEXT_CHATS:
              view.setData(R.string.Chats);
              break;
            case MoexConfig.HEADER_TEXT_MOEX:
              view.setData(R.string.moexHeaderClient);
              break;
            case MoexConfig.HEADER_TEXT_USERNAME:
              view.setData(R.string.Username);
              break;
            case MoexConfig.HEADER_TEXT_NAME:
              view.setData(R.string.login_FirstName);
              break;
          }
        } else if (itemId == R.id.btn_disableReactions) {
          view.getToggler().setRadioEnabled(MoexConfig.disableReactions, isUpdate);
        } else if (itemId == R.id.btn_hideMessagePanelButtons) {
          StringBuilder b = new StringBuilder();
          if (MoexConfig.disableCameraButton) {
            b.append(Lang.getString(R.string.DisableCameraButton));
          }
          if (MoexConfig.disableRecordButton) {
            if (b.length() > 0) {
              b.append(Lang.getConcatSeparator());
            }
            b.append(Lang.getString(R.string.DisableRecordButton));
          }
          if (MoexConfig.disableCommandsButton) {
            if (b.length() > 0) {
              b.append(Lang.getConcatSeparator());
            }
            b.append(Lang.getString(R.string.DisableCommandsButton));
          }
          if (MoexConfig.disableSendAsButton) {
            if (b.length() > 0) {
              b.append(Lang.getConcatSeparator());
            }
            b.append(Lang.getString(R.string.DisableSendAsButton));
          }
          if (b.length() == 0) {
            b.append(Lang.getString(R.string.BlockedNone));
          }
          view.setData(b.toString());
        } else if (itemId == R.id.btn_hideBottomBar) {
          view.getToggler().setRadioEnabled(MoexConfig.hideBottomBar, isUpdate);
        } else if (itemId == R.id.btn_disableStickerTimestamp) {
          view.getToggler().setRadioEnabled(MoexConfig.hideStickerTimestamp, isUpdate);
        } else if (itemId == R.id.btn_roundedStickers) {
          view.getToggler().setRadioEnabled(MoexConfig.roundedStickers, isUpdate);
        } else if (itemId == R.id.btn_IncreaseRecents) {
          view.getToggler().setRadioEnabled(MoexConfig.increaseRecents, isUpdate);
        } else if (itemId == R.id.btn_rememberOptions) {
          view.getToggler().setRadioEnabled(MoexConfig.rememberOptions, isUpdate);
        } else if (itemId == R.id.btn_typingInstead) {
          view.getToggler().setRadioEnabled(MoexConfig.typingInsteadChoosing, isUpdate);
        } else if (itemId == R.id.btn_darkenDrawer) {
          view.getToggler().setRadioEnabled(MoexConfig.darkenDrawer, isUpdate);
        }
      }
    };

    ArrayList<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    switch (category) {
      default:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexAbout));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.MoexAboutText), false));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexCategories));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_GeneralMoexSettings, R.drawable.baseline_settings_24, R.string.GeneralMoexSettings));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_InterfaceMoexSettings, R.drawable.baseline_extension_24, R.string.InterfaceMoexSettings));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_ChatsMoexSettings, R.drawable.baseline_chat_bubble_24, R.string.ChatsMoexSettings));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexLinks));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_moexCrowdinLink, R.drawable.baseline_translate_24, R.string.Translate));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_moexChatLink, R.drawable.outline_forum_24, R.string.MoexChatText));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_moexChannelLink, R.drawable.baseline_link_24, R.string.MoexChannelText));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR));
        items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_moexSourceLink, R.drawable.baseline_code_24, R.string.MoexSourceText));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_BUILD_NO, R.id.btn_build, 0, R.string.MoexVer, false));
        break;
      case CATEGORY_GENERAL:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.GeneralMoexSettings));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_hideMessagesBadge, 0, R.string.hideMessagesBadge));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ProfileOptions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_showIdProfile, 0, R.string.showIdProfile));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ExperimentalOptions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_changeSizeLimit, 0, R.string.changeSizeLimit));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
        items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.changeSizeLimitInfo), false));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_enableFeaturesButton, 0, R.string.EnableFeatures));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
        items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.FeaturesButtonInfo), false));
        break;
      case CATEGORY_INTERFACE:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.DrawerOptions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_blurDrawer, 0, R.string.MoexBlurDrawer));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_darkenDrawer, 0, R.string.MoexDarkenDrawer));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_hidePhone, 0, R.string.hidePhoneNumber));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexChatsHeader));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_headerText, 0, R.string.changeHeaderText));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_squareAvatar, 0, R.string.SquareAvatar));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_disableReactions, 0, R.string.DisableReactions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexHideButtons));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_hideMessagePanelButtons, 0, R.string.HideMessagePanelButtons));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_hideBottomBar, 0, R.string.HideBottomBar));
        break;
      case CATEGORY_CHATS:
        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.MoexStickersCount));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_disableStickerTimestamp, 0, R.string.DisableStickerTimestamp));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_roundedStickers, 0, R.string.RoundedStickers));
        items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_IncreaseRecents, 0, R.string.IncreaseRecents));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

        items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ActivityOptions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_rememberOptions, 0, R.string.RememberOptions));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
        items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.RememberOptionsInfo), false));
        items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
        items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_typingInstead, 0, R.string.TypingInstead));
        items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
        items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.TypingInsteadInfo), false));
        break;
    }
    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);
  }
}
