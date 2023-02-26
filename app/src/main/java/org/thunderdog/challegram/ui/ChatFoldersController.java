package org.thunderdog.challegram.ui;

import static org.thunderdog.challegram.telegram.TdlibSettingsManager.CHAT_FOLDER_STYLE_LABEL_AND_ICON;
import static org.thunderdog.challegram.telegram.TdlibSettingsManager.CHAT_FOLDER_STYLE_ICON_ONLY;
import static org.thunderdog.challegram.telegram.TdlibSettingsManager.CHAT_FOLDER_STYLE_LABEL_ONLY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.drinkless.td.libcore.telegram.TdApi;
import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.component.user.RemoveHelper;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.emoji.Emoji;
import org.thunderdog.challegram.navigation.SettingsWrapBuilder;
import org.thunderdog.challegram.telegram.ChatFiltersListener;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.theme.ThemeColorId;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.Strings;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.util.AdapterSubListUpdateCallback;
import org.thunderdog.challegram.util.DrawModifier;
import org.thunderdog.challegram.util.ListItemDiffUtilCallback;
import org.thunderdog.challegram.util.text.Text;
import org.thunderdog.challegram.v.CustomRecyclerView;
import org.thunderdog.challegram.widget.NonMaterialButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.ArrayUtils;
import me.vkryl.core.ColorUtils;
import me.vkryl.core.MathUtils;
import me.vkryl.core.collection.IntList;
import me.vkryl.core.lambda.RunnableBool;

public class ChatFoldersController extends RecyclerViewController<Void> implements View.OnClickListener, View.OnLongClickListener, ChatFiltersListener {
  private static final long MAIN_CHAT_FILTER_ID = Long.MIN_VALUE;
  private static final long ARCHIVE_CHAT_FILTER_ID = Long.MIN_VALUE + 1;

  private static final int TYPE_CHAT_FILTER = 0;
  private static final int TYPE_RECOMMENDED_CHAT_FILTER = 1;

  private final @IdRes int chatFiltersPreviousItemId = ViewCompat.generateViewId();
  private final @IdRes int recommendedChatFiltersPreviousItemId = ViewCompat.generateViewId();

  private int chatFilterGroupItemCount, recommendedChatFilterGroupItemCount;
  private boolean recommendedChatFiltersInitialized;

  private @Nullable TdApi.RecommendedChatFilter[] recommendedChatFilters;

  private SettingsAdapter adapter;
  private ItemTouchHelper itemTouchHelper;

  public ChatFoldersController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_chatFolders;
  }

  @Override
  public boolean needAsynchronousAnimation () {
    return !recommendedChatFiltersInitialized;
  }

  @Override
  public long getAsynchronousAnimationTimeout (boolean fastAnimation) {
    return 500l;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.ChatFolders);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    itemTouchHelper = RemoveHelper.attach(recyclerView, new ItemTouchHelperCallback());

    TdApi.ChatFilterInfo[] chatFilters = tdlib.chatFilterInfos();
    int mainChatListPosition = tdlib.mainChatListPosition();
    int archiveChatListPosition = tdlib.settings().archiveChatListPosition();
    List<ListItem> chatFilterItemList = buildChatFilterItemList(chatFilters, mainChatListPosition, archiveChatListPosition);
    chatFilterGroupItemCount = chatFilterItemList.size();

    ArrayList<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.ChatFoldersSettings));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_chatFolderStyle, 0, R.string.ChatFoldersAppearance));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_countMutedChats, 0, R.string.CountMutedChats));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM, chatFiltersPreviousItemId));

    items.addAll(chatFilterItemList);
    items.add(new ListItem(ListItem.TYPE_PADDING).setHeight(Screen.dp(12f)));

    adapter = new SettingsAdapter(this) {
      @SuppressLint("ClickableViewAccessibility")
      @Override
      protected SettingHolder initCustom (ViewGroup parent, int customViewType) {
        switch (customViewType) {
          case TYPE_CHAT_FILTER: {
            SettingView settingView = new SettingView(parent.getContext(), tdlib);
            settingView.setType(SettingView.TYPE_SETTING);
            settingView.addToggler();
            settingView.forcePadding(0, Screen.dp(66f));
            settingView.setOnTouchListener(new ChatFilterOnTouchListener());
            settingView.setOnClickListener(ChatFoldersController.this);
            settingView.setOnLongClickListener(ChatFoldersController.this);
            settingView.getToggler().setOnClickListener(v -> {
              UI.forceVibrate(v, false);
              ListItem item = (ListItem) settingView.getTag();
              boolean enabled = settingView.getToggler().toggle(true);
              settingView.setVisuallyEnabled(enabled, true);
              settingView.setIconColorId(enabled ? R.id.theme_color_icon : R.id.theme_color_iconLight);
              if (isMainChatFilter(item)) {
                tdlib.settings().setMainChatListEnabled(enabled);
              } else if (isArchiveChatFilter(item)) {
                tdlib.settings().setArchiveChatListEnabled(enabled);
              } else if (isChatFilter(item)) {
                tdlib.settings().setChatFilterEnabled(item.getIntValue(), enabled);
              } else {
                throw new IllegalArgumentException();
              }
            });
            addThemeInvalidateListener(settingView);
            return new SettingHolder(settingView);
          }
          case TYPE_RECOMMENDED_CHAT_FILTER:
            SettingView settingView = new SettingView(parent.getContext(), tdlib);
            settingView.setType(SettingView.TYPE_INFO_COMPACT);
            settingView.setSwapDataAndName();
            settingView.setOnClickListener(ChatFoldersController.this);
            addThemeInvalidateListener(settingView);

            FrameLayout.LayoutParams params = FrameLayoutFix.newParams(Screen.dp(29f), Screen.dp(28f), (Lang.rtl() ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
            params.leftMargin = params.rightMargin = Screen.dp(17f);
            NonMaterialButton button = new NonMaterialButton(parent.getContext()) {
              @Override
              protected void onSizeChanged (int width, int height, int oldWidth, int oldHeight) {
                settingView.forcePadding(0, Math.max(0, width + params.leftMargin + params.rightMargin - Screen.dp(17f)));
              }
            };
            button.setId(R.id.btn_double);
            button.setLayoutParams(params);
            button.setText(R.string.PlusSign);
            button.setOnClickListener(ChatFoldersController.this);
            settingView.addView(button);

            return new SettingHolder(settingView);
        }
        throw new IllegalArgumentException("customViewType=" + customViewType);
      }

      @Override
      protected void modifyCustom (SettingHolder holder, int position, ListItem item, int customViewType, View view, boolean isUpdate) {
        if (customViewType == TYPE_CHAT_FILTER) {
          SettingView settingView = (SettingView) holder.itemView;
          settingView.setIcon(item.getIconResource());
          settingView.setName(item.getString());
          settingView.setTextColorId(item.getTextColorId(ThemeColorId.NONE));
          settingView.setIgnoreEnabled(true);
          settingView.setEnabled(true);
          settingView.setDrawModifier(item.getDrawModifier());

          boolean isEnabled;
          if (isMainChatFilter(item)) {
            isEnabled = tdlib.settings().isMainChatListEnabled();
            settingView.setClickable(false);
            settingView.setLongClickable(false);
          } else if (isArchiveChatFilter(item)) {
            isEnabled = tdlib.settings().isArchiveChatListEnabled();
            settingView.setClickable(false);
            settingView.setLongClickable(false);
          } else if (isChatFilter(item)) {
            isEnabled = tdlib.settings().isChatFilterEnabled(item.getIntValue());
            settingView.setClickable(true);
            settingView.setLongClickable(true);
          } else {
            throw new IllegalArgumentException();
          }
          settingView.setVisuallyEnabled(isEnabled, false);
          settingView.getToggler().setRadioEnabled(isEnabled, false);
          settingView.setIconColorId(isEnabled ? R.id.theme_color_icon : R.id.theme_color_iconLight);
        } else if (customViewType == TYPE_RECOMMENDED_CHAT_FILTER) {
          SettingView settingView = (SettingView) holder.itemView;
          settingView.setIcon(item.getIconResource());
          settingView.setName(item.getString());
          settingView.setData(item.getStringValue());
          settingView.setTextColorId(item.getTextColorId(ThemeColorId.NONE));
          settingView.setEnabled(true);
          View button = settingView.findViewById(R.id.btn_double);
          button.setEnabled(true);
          button.setTag(item.getData());
        } else {
          throw new IllegalArgumentException("customViewType=" + customViewType);
        }
      }

      @SuppressLint("ClickableViewAccessibility")
      @Override
      protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_createNewFolder) {
          boolean canCreateChatFilter = canCreateChatFilter();
          view.setIgnoreEnabled(true);
          view.setVisuallyEnabled(canCreateChatFilter, isUpdate);
          view.setIconColorId(canCreateChatFilter ? R.id.theme_color_inlineIcon : R.id.theme_color_iconLight);
        } else {
          view.setIgnoreEnabled(false);
          view.setEnabledAnimated(true, isUpdate);
          view.setIconColorId(ThemeColorId.NONE);
        }
        if (item.getId() == R.id.btn_chatFolderStyle) {
          int positionRes;
          if (tdlib.settings().displayFoldersAtTop()) {
            positionRes = R.string.ChatFoldersPositionTop;
          } else {
            positionRes = R.string.ChatFoldersPositionBottom;
          }
          int styleRes;
          switch (tdlib.settings().chatFolderStyle()) {
            case CHAT_FOLDER_STYLE_LABEL_AND_ICON:
              styleRes = R.string.LabelAndIcon;
              break;
            case CHAT_FOLDER_STYLE_ICON_ONLY:
              styleRes = R.string.IconOnly;
              break;
            default:
            case CHAT_FOLDER_STYLE_LABEL_ONLY:
              styleRes = R.string.LabelOnly;
              break;
          }
          view.setData(Lang.getString(R.string.format_chatFoldersPositionAndStyle, Lang.getString(positionRes), Lang.getString(styleRes)));
        } else if (item.getId() == R.id.btn_countMutedChats) {
          view.getToggler().setRadioEnabled(tdlib.settings().shouldCountMutedChats(), isUpdate);
        }
      }
    };
    adapter.setItems(items, false);
    recyclerView.setAdapter(adapter);

    tdlib.listeners().subscribeToChatFiltersUpdates(this);
    updateRecommendedChatFilters();
  }

  @Override
  public void destroy () {
    super.destroy();
    tdlib.listeners().unsubscribeFromChatFiltersUpdates(this);
  }

  @Override
  public boolean saveInstanceState (Bundle outState, String keyPrefix) {
    super.saveInstanceState(outState, keyPrefix);
    return true;
  }

  @Override
  public boolean restoreInstanceState (Bundle in, String keyPrefix) {
    super.restoreInstanceState(in, keyPrefix);
    return true;
  }

  private boolean shouldUpdateRecommendedChatFilters = false;

  @Override
  protected void onFocusStateChanged () {
    if (isFocused()) {
      if (shouldUpdateRecommendedChatFilters) {
        shouldUpdateRecommendedChatFilters = false;
        updateRecommendedChatFilters();
      }
    } else {
      shouldUpdateRecommendedChatFilters = true;
    }
  }

  @Override
  public void onChatFiltersChanged (TdApi.ChatFilterInfo[] chatFilters, int mainChatListPosition) {
    runOnUiThreadOptional(() -> {
      adapter.updateValuedSettingById(R.id.btn_createNewFolder);
      updateChatFilters(chatFilters, mainChatListPosition, tdlib.settings().archiveChatListPosition());
      if (isFocused()) {
        tdlib.ui().postDelayed(() -> {
          if (!isDestroyed() && isFocused()) {
            updateRecommendedChatFilters();
          }
        }, /* ¯\_(ツ)_/¯ */ 500L);
      }
    });
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_createNewFolder) {
      if (canCreateChatFilter()) {
        navigateTo(EditChatFolderController.newFolder(context, tdlib));
      } else {
        showChatFilterLimitReached(v);
      }
    } else if (v.getId() == R.id.chatFilter) {
      ListItem item = (ListItem) v.getTag();
      if (isMainChatFilter(item) || isArchiveChatFilter(item)) {
        return;
      }
      editChatFilter((TdApi.ChatFilterInfo) item.getData());
    } else if (v.getId() == R.id.recommendedChatFilter) {
      if (canCreateChatFilter()) {
        ListItem item = (ListItem) v.getTag();
        TdApi.ChatFilter chatFilter = (TdApi.ChatFilter) item.getData();
        chatFilter.iconName = tdlib.chatFilterIconName(chatFilter);
        navigateTo(EditChatFolderController.newFolder(context, tdlib, chatFilter));
      } else {
        showChatFilterLimitReached(v);
      }
    } else if (v.getId() == R.id.btn_double) {
      Object tag = v.getTag();
      if (tag instanceof TdApi.ChatFilter) {
        if (canCreateChatFilter()) {
          v.setEnabled(false);
          TdApi.ChatFilter chatFilter = (TdApi.ChatFilter) tag;
          WeakReference<View> viewRef = new WeakReference<>(v);
          createChatFilter(chatFilter, (ok) -> {
            if (ok) {
              removeRecommendedChatFilter(chatFilter);
            } else {
              View view = viewRef.get();
              if (view != null && view.getTag() == tag) {
                view.setEnabled(true);
              }
            }
          });
        } else {
          showChatFilterLimitReached(v);
        }
      }
    } else if (v.getId() == R.id.btn_chatFolderStyle) {
      int chatFolderStyle = tdlib.settings().chatFolderStyle();
      ListItem[] items = new ListItem[] {
        new ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_displayFoldersAtTop, 0, R.string.DisplayFoldersAtTheTop, tdlib.settings().displayFoldersAtTop()),
        new ListItem(ListItem.TYPE_SHADOW_BOTTOM).setTextColorId(R.id.theme_color_background),
        new ListItem(ListItem.TYPE_SHADOW_TOP).setTextColorId(R.id.theme_color_background),
        new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_labelOnly, 0, R.string.LabelOnly, R.id.btn_chatFolderStyle, chatFolderStyle == CHAT_FOLDER_STYLE_LABEL_ONLY),
        new ListItem(ListItem.TYPE_SEPARATOR_FULL),
        new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_iconOnly, 0, R.string.IconOnly, R.id.btn_chatFolderStyle, chatFolderStyle == CHAT_FOLDER_STYLE_ICON_ONLY),
        new ListItem(ListItem.TYPE_SEPARATOR_FULL),
        new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_labelAndIcon, 0, R.string.LabelAndIcon, R.id.btn_chatFolderStyle, chatFolderStyle == CHAT_FOLDER_STYLE_LABEL_AND_ICON),
      };
      SettingsWrapBuilder settings = new SettingsWrapBuilder(R.id.btn_chatFolderStyle)
        .setRawItems(items)
        .setNeedSeparators(false)
        .setIntDelegate((id, result) -> {
          int selection = result.get(R.id.btn_chatFolderStyle);
          int style;
          if (selection == R.id.btn_iconOnly) {
            style = CHAT_FOLDER_STYLE_ICON_ONLY;
          } else if (selection == R.id.btn_labelAndIcon) {
            style = CHAT_FOLDER_STYLE_LABEL_AND_ICON;
          } else {
            style = CHAT_FOLDER_STYLE_LABEL_ONLY;
          }
          boolean displayFoldersAtTop = result.get(R.id.btn_displayFoldersAtTop) != 0;
          tdlib.settings().setChatFolderStyle(style);
          tdlib.settings().setDisplayFoldersAtTop(displayFoldersAtTop);
          adapter.updateValuedSettingById(R.id.btn_chatFolderStyle);
        });
      showSettings(settings);
    } else if (v.getId() == R.id.btn_countMutedChats) {
      tdlib.settings().setShouldCountMutedChats(adapter.toggleView(v));
    }
  }

  @Override
  public boolean onLongClick (View v) {
    if (v.getId() == R.id.chatFilter) {
      ListItem item = (ListItem) v.getTag();
      if (isMainChatFilter(item) || isArchiveChatFilter(item)) {
        return false;
      }
      showChatFilterOptions((TdApi.ChatFilterInfo) item.getData());
      return true;
    }
    return false;
  }

  private void startDrag (RecyclerView.ViewHolder viewHolder) {
    if (viewHolder == null)
      return;
    ListItem listItem = (ListItem) viewHolder.itemView.getTag();
    if (isMainChatFilter(listItem) && !tdlib.hasPremium()) {
      UI.forceVibrateError(viewHolder.itemView);
      CharSequence markdown = Lang.getString(R.string.PremiumRequiredMoveFolder, Lang.getString(R.string.CategoryMain));
      context()
        .tooltipManager()
        .builder(viewHolder.itemView)
        .icon(R.drawable.msg_folder_reorder)
        .controller(this)
        .show(tdlib, Strings.buildMarkdown(this, markdown))
        .hideDelayed();
      return;
    }
    itemTouchHelper.startDrag(viewHolder);
  }

  private void showChatFilterLimitReached (View view) {
    UI.forceVibrateError(view);
    if (tdlib.hasPremium()) {
      showTooltip(view, Lang.getMarkdownString(this, R.string.ChatFolderLimitReached, tdlib.chatFilterCountMax()));
    } else {
      Object viewTag = view.getTag();
      WeakReference<View> viewRef = new WeakReference<>(view);
      tdlib.send(new TdApi.GetPremiumLimit(new TdApi.PremiumLimitTypeChatFilterCount()), (result) -> runOnUiThreadOptional(() -> {
        View v = viewRef.get();
        if (v == null || !ViewCompat.isAttachedToWindow(v) || viewTag != v.getTag())
          return;
        CharSequence text;
        if (result.getConstructor() == TdApi.PremiumLimit.CONSTRUCTOR) {
          TdApi.PremiumLimit premiumLimit = (TdApi.PremiumLimit) result;
          text = Lang.getMarkdownString(this, R.string.PremiumRequiredCreateFolder, premiumLimit.defaultValue, premiumLimit.premiumValue);
        } else {
          text = Lang.getMarkdownString(this, R.string.ChatFolderLimitReached, tdlib.chatFilterCountMax());
        }
        showTooltip(v, text);
      }));
    }
  }

  private void showTooltip (View view, CharSequence text) {
    context()
      .tooltipManager()
      .builder(view)
      .controller(this)
      .show(tdlib, text)
      .hideDelayed(3500, TimeUnit.MILLISECONDS);
  }

  private void showChatFilterOptions (TdApi.ChatFilterInfo chatFilterInfo) {
    Options options = new Options.Builder()
      .info(chatFilterInfo.title)
      .item(new OptionItem(R.id.btn_edit, Lang.getString(R.string.EditFolder), OPTION_COLOR_NORMAL, R.drawable.baseline_edit_24))
      .item(new OptionItem(R.id.btn_delete, Lang.getString(R.string.RemoveFolder), OPTION_COLOR_RED, R.drawable.baseline_delete_24))
      .build();
    showOptions(options, (optionItemView, id) -> {
      if (id == R.id.btn_edit) {
        editChatFilter(chatFilterInfo);
      } else if (id == R.id.btn_delete) {
        showRemoveFolderConfirm(chatFilterInfo.id);
      }
      return true;
    });
  }

  private void showRemoveFolderConfirm (int chatFilterId) {
    showConfirm(Lang.getString(R.string.RemoveFolderConfirm), Lang.getString(R.string.Remove), R.drawable.baseline_delete_24, OPTION_COLOR_RED, () -> {
      deleteChatFilter(chatFilterId);
    });
  }

  private void editChatFilter (TdApi.ChatFilterInfo chatFilterInfo) {
    tdlib.send(new TdApi.GetChatFilter(chatFilterInfo.id), (result) -> runOnUiThreadOptional(() -> {
      switch (result.getConstructor()) {
        case TdApi.ChatFilter.CONSTRUCTOR:
          TdApi.ChatFilter chatFilter = (TdApi.ChatFilter) result;
          EditChatFolderController controller = new EditChatFolderController(context, tdlib);
          controller.setArguments(new EditChatFolderController.Arguments(chatFilterInfo.id, chatFilter));
          navigateTo(controller);
          break;
        case TdApi.Error.CONSTRUCTOR:
          UI.showError(result);
          break;
        default:
          Log.unexpectedTdlibResponse(result, TdApi.GetChatFilter.class, TdApi.ChatFilter.class, TdApi.Error.class);
          break;
      }
    }));
  }

  private void createChatFilter (TdApi.ChatFilter chatFilter, RunnableBool after) {
    tdlib.send(new TdApi.CreateChatFilter(chatFilter), (result) -> runOnUiThreadOptional(() -> {
      switch (result.getConstructor()) {
        case TdApi.ChatFilterInfo.CONSTRUCTOR:
          after.runWithBool(true);
          break;
        case TdApi.Error.CONSTRUCTOR:
          after.runWithBool(false);
          UI.showError(result);
          break;
      }
    }));
  }

  private void deleteChatFilter (int chatFilterId) {
    int position = -1;
    TdApi.ChatFilterInfo[] chatFilters = tdlib.chatFilterInfos();
    for (int index = 0; index < chatFilters.length; index++) {
      TdApi.ChatFilterInfo chatFilter = chatFilters[index];
      if (chatFilter.id == chatFilterId) {
        position = index;
        break;
      }
    }
    if (position != -1) {
      int archiveChatListPosition = tdlib.settings().archiveChatListPosition();
      if (position >= tdlib.mainChatListPosition()) position++;
      if (position >= archiveChatListPosition) position++;
      boolean affectsArchiveChatListPosition = position < archiveChatListPosition && archiveChatListPosition < chatFilters.length + 2;
      tdlib.send(new TdApi.DeleteChatFilter(chatFilterId), tdlib.okHandler(() -> {
        if (affectsArchiveChatListPosition && archiveChatListPosition == tdlib.settings().archiveChatListPosition()) {
          tdlib.settings().setArchiveChatListPosition(archiveChatListPosition - 1);
          if (!isDestroyed()) {
            updateChatFilters();
          }
        }
      }));
    }
  }

  private void reorderChatFilters () {
    int firstIndex = indexOfFirstChatFilter();
    int lastIndex = indexOfLastChatFilter();
    if (firstIndex == RecyclerView.NO_POSITION || lastIndex == RecyclerView.NO_POSITION)
      return;
    int mainChatListPosition = 0;
    int archiveChatListPosition = 0;
    IntList chatFilterIds = new IntList(tdlib.chatFilterInfos().length);
    int filterPosition = 0;
    for (int index = firstIndex; index <= lastIndex; index++) {
      ListItem item = adapter.getItem(index);
      if (item == null) {
        updateChatFilters();
        return;
      }
      if (isChatFilter(item)) {
        if (isMainChatFilter(item)) {
          mainChatListPosition = filterPosition;
        } else if (isArchiveChatFilter(item)) {
          archiveChatListPosition = filterPosition;
        } else {
          chatFilterIds.append(item.getIntValue());
        }
        filterPosition++;
      }
    }
    if (mainChatListPosition > archiveChatListPosition) {
      mainChatListPosition--;
    }
    if (archiveChatListPosition > chatFilterIds.size()) {
      archiveChatListPosition = Integer.MAX_VALUE;
    }
    if (mainChatListPosition != 0 && !tdlib.hasPremium()) {
      updateChatFilters();
      return;
    }
    tdlib.settings().setArchiveChatListPosition(archiveChatListPosition);
    if (chatFilterIds.size() > 0) {
      tdlib.send(new TdApi.ReorderChatFilters(chatFilterIds.get(), mainChatListPosition), (result) -> {
        if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
          UI.showError(result);
          runOnUiThreadOptional(this::updateChatFilters);
        }
      });
    }
  }

  private boolean isChatFilter (ListItem item) {
    return item.getId() == R.id.chatFilter;
  }

  private boolean isMainChatFilter (ListItem item) {
    return isChatFilter(item) && item.getLongId() == MAIN_CHAT_FILTER_ID;
  }

  private boolean isArchiveChatFilter (ListItem item) {
    return isChatFilter(item) && item.getLongId() == ARCHIVE_CHAT_FILTER_ID;
  }

  private boolean canMoveChatFilter (ListItem item) {
    return isChatFilter(item) && (tdlib.hasPremium() || !isMainChatFilter(item));
  }

  private int indexOfFirstChatFilter () {
    int index = indexOfChatFilterGroup();
    return index == RecyclerView.NO_POSITION ? RecyclerView.NO_POSITION : index + 2 /* header, shadowTop */;
  }

  private int indexOfLastChatFilter () {
    int index = indexOfChatFilterGroup();
    return index == RecyclerView.NO_POSITION ? RecyclerView.NO_POSITION : index + chatFilterGroupItemCount - 2 /* shadowBottom, separator */;
  }

  private int indexOfChatFilterGroup () {
    int index = adapter.indexOfViewById(chatFiltersPreviousItemId);
    return index == RecyclerView.NO_POSITION ? RecyclerView.NO_POSITION : index + 1;
  }

  private int indexOfRecommendedChatFilterGroup () {
    int index = adapter.indexOfViewById(recommendedChatFiltersPreviousItemId);
    return index == RecyclerView.NO_POSITION ? RecyclerView.NO_POSITION : index + 1;
  }

  private List<ListItem> buildChatFilterItemList (TdApi.ChatFilterInfo[] chatFilters, int mainChatListPosition, int archiveChatListPosition) {
    List<ListItem> itemList = new ArrayList<>(chatFilters.length + 6);
    itemList.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ChatFolders));
    itemList.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    int chatFilterCount = chatFilters.length + 2; /* All Chats, Archived */
    int chatFilterIndex = 0;
    mainChatListPosition = MathUtils.clamp(mainChatListPosition, 0, chatFilters.length);
    archiveChatListPosition = MathUtils.clamp(archiveChatListPosition, 0, chatFilterCount - 1);
    if (mainChatListPosition == archiveChatListPosition) {
      mainChatListPosition++;
    }
    for (int position = 0; position < chatFilterCount; position++) {
      if (position == mainChatListPosition) {
        itemList.add(mainChatFilterItem());
      } else if (position == archiveChatListPosition) {
        itemList.add(archiveChatFilterItem());
      } else if (chatFilterIndex < chatFilters.length) {
        TdApi.ChatFilterInfo chatFilter = chatFilters[chatFilterIndex++];
        itemList.add(chatFilterItem(chatFilter));
      } else if (BuildConfig.DEBUG) {
        throw new RuntimeException();
      }
    }
    itemList.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_createNewFolder, R.drawable.baseline_create_new_folder_24, R.string.CreateNewFolder).setTextColorId(R.id.theme_color_inlineText));
    itemList.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    itemList.add(new ListItem(ListItem.TYPE_DESCRIPTION, recommendedChatFiltersPreviousItemId, 0, R.string.ChatFoldersInfo));
    return itemList;
  }

  private List<ListItem> buildRecommendedChatFilterItemList (TdApi.RecommendedChatFilter[] recommendedChatFilters) {
    if (recommendedChatFilters.length == 0) {
      return Collections.emptyList();
    }
    List<ListItem> itemList = new ArrayList<>(recommendedChatFilters.length * 2 - 1 + 3);
    itemList.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.RecommendedFolders));
    itemList.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    for (int index = 0; index < recommendedChatFilters.length; index++) {
      if (index > 0) {
        itemList.add(new ListItem(ListItem.TYPE_SEPARATOR));
      }
      itemList.add(recommendedChatFilterItem(recommendedChatFilters[index]));
    }
    itemList.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    return itemList;
  }

  private ListItem mainChatFilterItem () {
    ListItem item = new ListItem(ListItem.TYPE_CUSTOM - TYPE_CHAT_FILTER, R.id.chatFilter);
    item.setString(R.string.CategoryMain);
    item.setLongId(MAIN_CHAT_FILTER_ID);
    item.setIconRes(tdlib.hasPremium() ? R.drawable.baseline_drag_handle_24 : R.drawable.deproko_baseline_lock_24);
    return item;
  }

  private ListItem archiveChatFilterItem () {
    ListItem item = new ListItem(ListItem.TYPE_CUSTOM - TYPE_CHAT_FILTER, R.id.chatFilter);
    item.setString(R.string.CategoryArchive);
    item.setLongId(ARCHIVE_CHAT_FILTER_ID);
    item.setIconRes(R.drawable.baseline_drag_handle_24);
    item.setDrawModifier(new LocalFolderBadge());
    return item;
  }

  private ListItem chatFilterItem (TdApi.ChatFilterInfo chatFilterInfo) {
    ListItem item = new ListItem(ListItem.TYPE_CUSTOM - TYPE_CHAT_FILTER, R.id.chatFilter, R.drawable.baseline_drag_handle_24, Emoji.instance().replaceEmoji(chatFilterInfo.title));
    item.setIntValue(chatFilterInfo.id);
    item.setLongId(chatFilterInfo.id);
    item.setData(chatFilterInfo);
    return item;
  }

  private ListItem recommendedChatFilterItem (TdApi.RecommendedChatFilter recommendedChatFilter) {
    ListItem item = new ListItem(ListItem.TYPE_CUSTOM - TYPE_RECOMMENDED_CHAT_FILTER, R.id.recommendedChatFilter);
    item.setData(recommendedChatFilter.filter);
    item.setString(recommendedChatFilter.filter.title);
    item.setStringValue(recommendedChatFilter.description);
    item.setIconRes(tdlib.chatFilterIcon(recommendedChatFilter.filter, R.drawable.baseline_folder_24));
    return item;
  }

  private boolean canCreateChatFilter () {
    return tdlib.chatFilterCount() < tdlib.chatFilterCountMax();
  }

  private void updateChatFilters () {
    updateChatFilters(tdlib.chatFilterInfos(), tdlib.mainChatListPosition(), tdlib.settings().archiveChatListPosition());
  }

  private void updateChatFilters (TdApi.ChatFilterInfo[] chatFilters, int mainChatListPosition, int archiveChatListPosition) {
    int fromIndex = indexOfChatFilterGroup();
    if (fromIndex == RecyclerView.NO_POSITION)
      return;
    List<ListItem> subList = adapter.getItems().subList(fromIndex, fromIndex + chatFilterGroupItemCount);
    List<ListItem> newList = buildChatFilterItemList(chatFilters, mainChatListPosition, archiveChatListPosition);
    chatFilterGroupItemCount = newList.size();
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(chatFiltersDiff(subList, newList));
    subList.clear();
    subList.addAll(newList);
    diffResult.dispatchUpdatesTo(new AdapterSubListUpdateCallback(adapter, fromIndex));
  }

  private void updateRecommendedChatFilters () {
    tdlib.send(new TdApi.GetRecommendedChatFilters(), (result) -> {
      runOnUiThreadOptional(() -> {
        if (result.getConstructor() == TdApi.RecommendedChatFilters.CONSTRUCTOR) {
          updateRecommendedChatFilters(((TdApi.RecommendedChatFilters) result).chatFilters);
        }
        if (!recommendedChatFiltersInitialized) {
          recommendedChatFiltersInitialized = true;
          executeScheduledAnimation();
        }
      });
    });
  }

  private void updateRecommendedChatFilters (TdApi.RecommendedChatFilter[] chatFilters) {
    int fromIndex = indexOfRecommendedChatFilterGroup();
    if (fromIndex == RecyclerView.NO_POSITION)
      return;
    List<ListItem> subList = adapter.getItems().subList(fromIndex, fromIndex + recommendedChatFilterGroupItemCount);
    List<ListItem> newList = buildRecommendedChatFilterItemList(chatFilters);
    if (subList.isEmpty() && newList.isEmpty()) {
      return;
    }
    recommendedChatFilters = chatFilters;
    recommendedChatFilterGroupItemCount = newList.size();
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(recommendedChatFiltersDiff(subList, newList));
    subList.clear();
    subList.addAll(newList);
    diffResult.dispatchUpdatesTo(new AdapterSubListUpdateCallback(adapter, fromIndex));
  }

  private void removeRecommendedChatFilter (TdApi.ChatFilter chatFilter) {
    if (recommendedChatFilters == null || recommendedChatFilters.length == 0)
      return;
    int indexToRemove = -1;
    for (int i = 0; i < recommendedChatFilters.length; i++) {
      if (chatFilter == recommendedChatFilters[i].filter) {
        indexToRemove = i;
        break;
      }
    }
    if (indexToRemove != -1) {
      TdApi.RecommendedChatFilter[] chatFilters = new TdApi.RecommendedChatFilter[recommendedChatFilters.length - 1];
      if (chatFilters.length > 0) {
        ArrayUtils.removeElement(recommendedChatFilters, indexToRemove, chatFilters);
      }
      updateRecommendedChatFilters(chatFilters);
    }
  }

  private static DiffUtil.Callback chatFiltersDiff (List<ListItem> oldList, List<ListItem> newList) {
    return new ListItemDiffUtilCallback(oldList, newList) {
      @Override
      public boolean areItemsTheSame (ListItem oldItem, ListItem newItem) {
        return oldItem.getViewType() == newItem.getViewType() &&
          oldItem.getId() == newItem.getId() &&
          oldItem.getLongId() == newItem.getLongId();
      }

      @Override
      public boolean areContentsTheSame (ListItem oldItem, ListItem newItem) {
        return Objects.equals(oldItem.getString(), newItem.getString());
      }
    };
  }

  private static DiffUtil.Callback recommendedChatFiltersDiff (List<ListItem> oldList, List<ListItem> newList) {
    return new ListItemDiffUtilCallback(oldList, newList) {
      @Override
      public boolean areItemsTheSame (ListItem oldItem, ListItem newItem) {
        if (oldItem.getViewType() == newItem.getViewType() && oldItem.getId() == newItem.getId()) {
          if (oldItem.getId() == R.id.recommendedChatFilter) {
            return Objects.equals(oldItem.getString(), newItem.getString());
          }
          return true;
        }
        return false;
      }

      @Override
      public boolean areContentsTheSame (ListItem oldItem, ListItem newItem) {
        if (oldItem.getId() == R.id.recommendedChatFilter) {
          return oldItem.getIconResource() == newItem.getIconResource() &&
            Objects.equals(oldItem.getString(), newItem.getString()) &&
            Objects.equals(oldItem.getStringValue(), newItem.getStringValue());
        }
        return Objects.equals(oldItem.getString(), newItem.getString());
      }
    };
  }

  private class ItemTouchHelperCallback implements RemoveHelper.ExtendedCallback {
    @Override
    public boolean isLongPressDragEnabled () {
      return false;
    }

    @Override
    public int makeDragFlags (RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      ListItem item = (ListItem) viewHolder.itemView.getTag();
      return isChatFilter(item) ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
    }

    @Override
    public boolean canRemove (RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int position) {
      ListItem item = (ListItem) viewHolder.itemView.getTag();
      return isChatFilter(item) && !isMainChatFilter(item) && !isArchiveChatFilter(item);
    }

    @Override
    public void onRemove (RecyclerView.ViewHolder viewHolder) {
      ListItem item = (ListItem) viewHolder.itemView.getTag();
      showRemoveFolderConfirm(item.getIntValue());
    }

    @Override
    public boolean onMove (@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
      int sourcePosition = source.getAbsoluteAdapterPosition();
      int targetPosition = target.getAbsoluteAdapterPosition();
      if (sourcePosition == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) {
        return false;
      }
      int firstChatFilterIndex = indexOfFirstChatFilter();
      int lastChatFilterIndex = indexOfLastChatFilter();
      if (firstChatFilterIndex == RecyclerView.NO_POSITION || lastChatFilterIndex == RecyclerView.NO_POSITION) {
        return false;
      }
      if (targetPosition < firstChatFilterIndex || targetPosition > lastChatFilterIndex) {
        return false;
      }
      adapter.moveItem(sourcePosition, targetPosition, /* notify */ true);
      return true;
    }

    @Override
    public boolean canDropOver (@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
      ListItem sourceItem = (ListItem) source.itemView.getTag();
      ListItem targetItem = (ListItem) target.itemView.getTag();
      return isChatFilter(sourceItem) && isChatFilter(targetItem) && (canMoveChatFilter(targetItem) || BuildConfig.DEBUG && isArchiveChatFilter(sourceItem));
    }

    @Override
    public void onCompleteMovement (int fromPosition, int toPosition) {
      reorderChatFilters();
    }
  }

  private class ChatFilterOnTouchListener implements View.OnTouchListener {
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch (View view, MotionEvent event) {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        float paddingStart = ((SettingView) view).getMeasuredNameStart();
        boolean shouldStartDrag = Lang.rtl() ? event.getX() > view.getWidth() - paddingStart : event.getX() < paddingStart;
        if (shouldStartDrag) {
          startDrag(getRecyclerView().getChildViewHolder(view));
        }
      }
      return false;
    }
  }

  private static class LocalFolderBadge implements DrawModifier {
    private final Text text;

    public LocalFolderBadge () {
      text = new Text.Builder(Lang.getString(R.string.LocalFolderBadge), Integer.MAX_VALUE, Paints.robotoStyleProvider(12f), Theme::textDecentColor)
        .allBold()
        .singleLine()
        .noClickable()
        .ignoreNewLines()
        .ignoreContinuousNewLines()
        .noSpacing()
        .build();
    }

    @Override
    public void afterDraw (View view, Canvas c) {
      SettingView settingView = (SettingView) view;
      float centerY = view.getHeight() / 2 + Screen.dp(.8f);
      int startX = (int) (settingView.getMeasuredNameStart() + settingView.getMeasuredNameWidth()) + Screen.dp(8f) + Screen.dp(6f);
      int startY = Math.round(centerY) - text.getLineCenterY();
      float alpha = 0.7f + settingView.getVisuallyEnabledFactor() * 0.3f;
      text.draw(c, startX, startY, null, alpha);

      int strokeColor = ColorUtils.alphaColor(alpha, Theme.textDecentColor());
      Paint.FontMetricsInt fontMetrics = Paints.getFontMetricsInt(Paints.getTextPaint16());
      float height = fontMetrics.descent - fontMetrics.ascent - Screen.dp(2f);

      RectF rect = Paints.getRectF(startX - Screen.dp(6f), centerY - height / 2f, startX + text.getWidth() + Screen.dp(6f), centerY + height / 2f);
      float radius = Screen.dp(4f);
      c.drawRoundRect(rect, radius, radius, Paints.strokeSmallPaint(strokeColor));
    }

    @Override
    public int getWidth () {
      return Screen.dp(8f) + text.getWidth() + Screen.dp(6f) * 2;
    }
  }
}