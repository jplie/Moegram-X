package org.thunderdog.challegram.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.drinkless.td.libcore.telegram.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.attach.CustomItemAnimator;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.component.user.RemoveHelper;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.AvatarPlaceholder;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.data.TGFoundChat;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.support.RippleSupport;
import org.thunderdog.challegram.support.ViewSupport;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Drawables;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.tool.Views;
import org.thunderdog.challegram.util.AdapterSubListUpdateCallback;
import org.thunderdog.challegram.util.ListItemDiffUtilCallback;
import org.thunderdog.challegram.v.CustomRecyclerView;
import org.thunderdog.challegram.widget.BetterChatView;
import org.thunderdog.challegram.widget.MaterialEditTextGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.ArrayUtils;
import me.vkryl.core.StringUtils;

public class EditChatFolderController extends RecyclerViewController<EditChatFolderController.Arguments> implements View.OnClickListener, SettingsAdapter.TextChangeListener, SelectChatsController.Delegate {

  private static final int NO_CHAT_FILTER_ID = 0;
  private static final int COLLAPSED_CHAT_COUNT = 3;
  private static final int MAX_CHAT_FILTER_TITLE_LENGTH = 12;
  private static final TdApi.ChatFilter EMPTY_CHAT_FILTER = TD.newChatFilter();
  private static final ArrayList<ListItem> TEMP_ITEM_LIST = new ArrayList<>(0);

  public static class Arguments {
    private final int chatFilterId;
    private final String chatFilterName;
    private final @Nullable TdApi.ChatFilter chatFilter;

    public static Arguments newFolder () {
      return new Arguments(NO_CHAT_FILTER_ID, (TdApi.ChatFilter) null);
    }

    public static Arguments newFolder (@Nullable TdApi.ChatFilter chatFilter) {
      return new Arguments(NO_CHAT_FILTER_ID, chatFilter);
    }

    public Arguments (TdApi.ChatFilterInfo chatFilterInfo) {
      this(chatFilterInfo.id, chatFilterInfo.title);
    }

    public Arguments (int chatFilterId, @Nullable TdApi.ChatFilter chatFilter) {
      this(chatFilterId, chatFilter != null ? chatFilter.title : "", chatFilter);
    }

    public Arguments (int chatFilterId, String chatFilterName) {
      this(chatFilterId, chatFilterName, null);
    }

    private Arguments (int chatFilterId, String chatFilterName, @Nullable TdApi.ChatFilter chatFilter) {
      this.chatFilter = chatFilter;
      this.chatFilterId = chatFilterId;
      this.chatFilterName = chatFilterName;
    }
  }

  public static EditChatFolderController newFolder (Context context, Tdlib tdlib) {
    EditChatFolderController controller = new EditChatFolderController(context, tdlib);
    controller.setArguments(Arguments.newFolder());
    return controller;
  }

  public static EditChatFolderController newFolder (Context context, Tdlib tdlib, TdApi.ChatFilter chatFilter) {
    EditChatFolderController controller = new EditChatFolderController(context, tdlib);
    controller.setArguments(Arguments.newFolder(chatFilter));
    return controller;
  }

  @SuppressWarnings("FieldCanBeLocal")
  private final @IdRes int includedChatsPreviousItemId = R.id.btn_folderIncludeChats;
  @SuppressWarnings("FieldCanBeLocal")
  private final @IdRes int excludedChatsPreviousItemId = R.id.btn_folderExcludeChats;
  private final @IdRes int includedChatsNextItemId = ViewCompat.generateViewId();
  private final @IdRes int excludedChatsNextItemId = ViewCompat.generateViewId();

  private boolean showAllIncludedChats;
  private boolean showAllExcludedChats;

  private SettingsAdapter adapter;
  private ListItem input;

  private int chatFilterId;
  private TdApi.ChatFilter originChatFilter;
  private TdApi.ChatFilter editedChatFilter;

  public EditChatFolderController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public boolean needAsynchronousAnimation () {
    return originChatFilter == null && chatFilterId != NO_CHAT_FILTER_ID;
  }

  @Override
  public long getAsynchronousAnimationTimeout (boolean fastAnimation) {
    return 500l;
  }

  @Override
  public int getId () {
    return R.id.controller_editChatFolders;
  }

  @Override
  public CharSequence getName () {
    Arguments arguments = getArgumentsStrict();
    return chatFilterId != NO_CHAT_FILTER_ID ? arguments.chatFilterName : Lang.getString(R.string.NewFolder);
  }

  @Override
  public void setArguments (Arguments args) {
    super.setArguments(args);
    this.chatFilterId = args.chatFilterId;
    this.originChatFilter = args.chatFilter;
    this.editedChatFilter = args.chatFilter != null ? TD.copyOf(args.chatFilter) : TD.newChatFilter(args.chatFilterName);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    ArrayList<ListItem> items = new ArrayList<>();
    items.add(new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.FolderName));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(input = new ListItem(ListItem.TYPE_CUSTOM_SINGLE, R.id.input).setStringValue(editedChatFilter.title));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    items.add(new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.FolderIncludedChats));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_folderIncludeChats, R.drawable.baseline_add_24, R.string.FolderActionIncludeChats).setTextColorId(R.id.theme_color_inlineText));
    fillIncludedChats(editedChatFilter, items);
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM, includedChatsNextItemId));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.FolderIncludedChatsInfo)));

    items.add(new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.FolderExcludedChats));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_folderExcludeChats, R.drawable.baseline_add_24, R.string.FolderActionExcludeChats).setTextColorId(R.id.theme_color_inlineText));
    fillExcludedChats(editedChatFilter, items);
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM, excludedChatsNextItemId));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.FolderExcludedChatsInfo)));

    if (chatFilterId != NO_CHAT_FILTER_ID) {
      items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
      items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_removeFolder, R.drawable.baseline_folder_delete_24, R.string.RemoveFolder).setTextColorId(R.id.theme_color_textNegative));
      items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
      items.add(new ListItem(ListItem.TYPE_PADDING).setHeight(Screen.dp(12f)));
    }

    adapter = new Adapter(this);
    adapter.setLockFocusOn(this, /* showAlways */ StringUtils.isEmpty(editedChatFilter.title));
    adapter.setTextChangeListener(this);
    adapter.setItems(items, false);
    CustomItemAnimator itemAnimator = new CustomItemAnimator(AnimatorUtils.DECELERATE_INTERPOLATOR, 180l);
    itemAnimator.setSupportsChangeAnimations(false);
    recyclerView.setItemAnimator(itemAnimator);
    recyclerView.setAdapter(adapter);
    RemoveHelper.attach(recyclerView, new RemoveHelperCallback());

    if (originChatFilter == null && chatFilterId != NO_CHAT_FILTER_ID) {
      loadChatFilter();
    }
  }

  @Override
  public boolean saveInstanceState (Bundle outState, String keyPrefix) {
    super.saveInstanceState(outState, keyPrefix);
    Arguments arguments = getArgumentsStrict();
    outState.putInt(keyPrefix + "_chatFilterId", arguments.chatFilterId);
    outState.putString(keyPrefix + "_chatFilterName", arguments.chatFilterName);
    TD.saveChatFilter(outState, keyPrefix + "_originChatFilter", originChatFilter);
    TD.saveChatFilter(outState, keyPrefix + "_editedChatFilter", editedChatFilter);
    outState.putBoolean(keyPrefix + "_showAllIncludedChats", showAllIncludedChats);
    outState.putBoolean(keyPrefix + "_showAllExcludedChats", showAllExcludedChats);
    return true;
  }

  @Override
  public boolean restoreInstanceState (Bundle in, String keyPrefix) {
    super.restoreInstanceState(in, keyPrefix);
    int chatFilterId = in.getInt(keyPrefix + "_chatFilterId", NO_CHAT_FILTER_ID);
    String chatFilterName = in.getString(keyPrefix + "_chatFilterName");
    TdApi.ChatFilter originChatFilter = TD.restoreChatFilter(in, keyPrefix + "_originChatFilter");
    TdApi.ChatFilter editedChatFilter = TD.restoreChatFilter(in, keyPrefix + "_editedChatFilter");
    if (chatFilterName != null && editedChatFilter != null) {
      super.setArguments(new Arguments(chatFilterId, chatFilterName, originChatFilter));
      this.chatFilterId = chatFilterId;
      this.originChatFilter = originChatFilter;
      this.editedChatFilter = editedChatFilter;
      this.showAllIncludedChats = in.getBoolean(keyPrefix + "_showAllIncludedChats");
      this.showAllExcludedChats = in.getBoolean(keyPrefix + "_showAllExcludedChats");
      return true;
    }
    return false;
  }

  @Override
  protected int getMenuId () {
    return R.id.menu_done;
  }

  @Override
  public void fillMenuItems (int id, HeaderView header, LinearLayout menu) {
    if (id == R.id.menu_done) {
      header.addDoneButton(menu, this).setVisibility(canSaveChanges() ? View.VISIBLE : View.GONE);
    }
  }

  @Override
  public void onMenuItemPressed (int id, View view) {
    if (id == R.id.menu_btn_done) {
      saveChanges();
    }
  }

  @Override
  public void onClick (View v) {
    int id = v.getId();
    if (id == R.id.btn_folderIncludeChats) {
      SelectChatsController selectChats = new SelectChatsController(context, tdlib);
      selectChats.setArguments(SelectChatsController.Arguments.includedChats(this, chatFilterId, editedChatFilter));
      navigateTo(selectChats);
    } else if (id == R.id.btn_folderExcludeChats) {
      SelectChatsController selectChats = new SelectChatsController(context, tdlib);
      selectChats.setArguments(SelectChatsController.Arguments.excludedChats(this, chatFilterId, editedChatFilter));
      navigateTo(selectChats);
    } else if (id == R.id.btn_showAdvanced) {
      ListItem item = (ListItem) v.getTag();
      if (item.getBoolValue()) {
        if (!showAllIncludedChats) {
          showAllIncludedChats = true;
          updateIncludedChats();
        }
      } else {
        if (!showAllExcludedChats) {
          showAllExcludedChats = true;
          updateExcludedChats();
        }
      }
    } else if (id == R.id.btn_removeFolder) {
      showRemoveFolderConfirm();
    } else if (id == R.id.chat || ArrayUtils.contains(TD.CHAT_TYPES, id)) {
      int position = getRecyclerView().getChildAdapterPosition(v);
      ListItem item = (ListItem) v.getTag();
      showRemoveConditionConfirm(position, item);
    }
  }

  @Override
  public boolean onBackPressed (boolean fromTop) {
    if (hasChanges()) {
      showUnsavedChangesPromptBeforeLeaving(/* onConfirm */ null);
      return true;
    }
    return super.onBackPressed(fromTop);
  }

  @Override
  public void onBlur () {
    super.onBlur();
    adapter.setLockFocusOn(this, false);
    setLockFocusView(getLockFocusView(), false);
  }

  @Override
  public void onPrepareToShow () {
    super.onPrepareToShow();
    updateMenuButton();
  }

  @Override
  public void onTextChanged (int id, ListItem item, MaterialEditTextGroup v, String text) {
    editedChatFilter.title = text;
    updateMenuButton();
  }

  private void fillIncludedChats (TdApi.ChatFilter chatFilter, List<ListItem> outList) {
    int chatTypeCount = TD.countIncludedChatTypes(chatFilter);
    int chatCount = chatFilter.pinnedChatIds.length + chatFilter.includedChatIds.length;
    int visibleChatCount = showAllIncludedChats || (chatCount <= COLLAPSED_CHAT_COUNT + 1) ? chatCount : COLLAPSED_CHAT_COUNT;
    int moreCount = chatCount - visibleChatCount;
    int itemCount = (chatTypeCount + visibleChatCount) * 2 + (moreCount > 0 ? 2 : 0);
    if (itemCount == 0)
      return;
    ArrayUtils.ensureCapacity(outList, itemCount);
    for (int includedChatType : TD.includedChatTypes(chatFilter)) {
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setIntValue(includedChatType));
      outList.add(chatTypeItem(includedChatType));
    }
    int count = 0;
    for (long pinnedChatId : chatFilter.pinnedChatIds) {
      if (count++ >= visibleChatCount)
        break;
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setLongValue(pinnedChatId));
      outList.add(chatItem(pinnedChatId).setBoolValue(true /* included chat */));
    }
    for (long includedChatId : chatFilter.includedChatIds) {
      if (count++ >= visibleChatCount)
        break;
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setLongValue(includedChatId));
      outList.add(chatItem(includedChatId).setBoolValue(true /* included chat */));
    }
    if (moreCount > 0) {
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setIntValue(R.id.btn_showAdvanced));
      outList.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_showAdvanced, R.drawable.baseline_direction_arrow_down_24, Lang.plural(R.string.ChatsXShowMore, moreCount)).setBoolValue(true /* included chats */));
    }
  }

  private void fillExcludedChats (TdApi.ChatFilter chatFilter, List<ListItem> outList) {
    int chatTypeCount = TD.countExcludedChatTypes(chatFilter);
    int chatCount = chatFilter.excludedChatIds.length;
    int visibleChatCount = showAllExcludedChats || (chatCount <= COLLAPSED_CHAT_COUNT + 1) ? chatCount : COLLAPSED_CHAT_COUNT;
    int moreCount = chatCount - visibleChatCount;
    int itemCount = (chatTypeCount + visibleChatCount) * 2 + (moreCount > 0 ? 2 : 0);
    if (itemCount == 0)
      return;
    ArrayUtils.ensureCapacity(outList, itemCount);
    for (int excludedChatType : TD.excludedChatTypes(chatFilter)) {
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setIntValue(excludedChatType));
      outList.add(chatTypeItem(excludedChatType));
    }
    for (int index = 0; index < visibleChatCount; index++) {
      long excludedChatId = chatFilter.excludedChatIds[index];
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setLongValue(excludedChatId));
      outList.add(chatItem(excludedChatId).setBoolValue(false /* excluded chat */));
    }
    if (moreCount > 0) {
      outList.add(new ListItem(ListItem.TYPE_SEPARATOR).setIntValue(R.id.btn_showAdvanced));
      outList.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_showAdvanced, R.drawable.baseline_direction_arrow_down_24, Lang.plural(R.string.ChatsXShowMore, moreCount)).setBoolValue(false /* excluded chats */));
    }
  }

  private ListItem chatItem (long chatId) {
    TGFoundChat foundChat = new TGFoundChat(tdlib, null, chatId, true);
    foundChat.setNoUnread();
    return new ListItem(ListItem.TYPE_CHAT_BETTER, R.id.chat).setData(foundChat).setLongId(chatId);
  }

  private ListItem chatTypeItem (@IdRes int id) {
    return new ListItem(ListItem.TYPE_CHAT_BETTER, id, TD.chatTypeIcon24(id), TD.chatTypeName(id)).setIntValue(TD.chatTypeColor(id));
  }

  private void loadChatFilter () {
    tdlib.send(new TdApi.GetChatFilter(chatFilterId), (result) -> runOnUiThreadOptional(() -> {
      switch (result.getConstructor()) {
        case TdApi.ChatFilter.CONSTRUCTOR:
          updateChatFilter((TdApi.ChatFilter) result);
          break;
        case TdApi.Error.CONSTRUCTOR:
          UI.showError(result);
          break;
      }
    }));
  }

  private void updateChatFilter (TdApi.ChatFilter chatFilter) {
    this.editedChatFilter = chatFilter;
    updateMenuButton();
    updateIncludedChats();
    updateExcludedChats();
  }

  private void updateIncludedChats () {
    int previousItemIndex = adapter.indexOfViewById(includedChatsPreviousItemId);
    int nextItemIndex = adapter.indexOfViewById(includedChatsNextItemId);
    if (previousItemIndex == -1 || nextItemIndex == -1)
      return;
    int firstItemIndex = previousItemIndex + 1;
    TEMP_ITEM_LIST.clear();
    fillIncludedChats(editedChatFilter, TEMP_ITEM_LIST);
    if (firstItemIndex < nextItemIndex) {
      List<ListItem> oldList = adapter.getItems().subList(firstItemIndex, nextItemIndex);
      DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtilCallback(oldList, TEMP_ITEM_LIST));
      oldList.clear();
      oldList.addAll(TEMP_ITEM_LIST);
      diffResult.dispatchUpdatesTo(new AdapterSubListUpdateCallback(adapter, firstItemIndex));
    } else if (TEMP_ITEM_LIST.size() > 0) {
      adapter.addItems(firstItemIndex, TEMP_ITEM_LIST.toArray(new ListItem[0]));
    }
    TEMP_ITEM_LIST.clear();
  }

  private void updateExcludedChats () {
    int previousItemIndex = adapter.indexOfViewById(excludedChatsPreviousItemId);
    int nextItemIndex = adapter.indexOfViewById(excludedChatsNextItemId);
    if (previousItemIndex == -1 || nextItemIndex == -1)
      return;
    int firstItemIndex = previousItemIndex + 1;
    TEMP_ITEM_LIST.clear();
    fillExcludedChats(editedChatFilter, TEMP_ITEM_LIST);
    if (firstItemIndex < nextItemIndex) {
      List<ListItem> oldList = adapter.getItems().subList(firstItemIndex, nextItemIndex);
      DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtilCallback(oldList, TEMP_ITEM_LIST));
      oldList.clear();
      oldList.addAll(TEMP_ITEM_LIST);
      diffResult.dispatchUpdatesTo(new AdapterSubListUpdateCallback(adapter, firstItemIndex));
    } else if (TEMP_ITEM_LIST.size() > 0) {
      adapter.addItems(firstItemIndex, TEMP_ITEM_LIST.toArray(new ListItem[0]));
    }
    TEMP_ITEM_LIST.clear();
  }

  private void updateFolderName () {
    if (StringUtils.isEmpty(editedChatFilter.title) && editedChatFilter.pinnedChatIds.length == 0 && editedChatFilter.includedChatIds.length == 0) {
      int[] includedChatTypes = TD.includedChatTypes(editedChatFilter);
      if (includedChatTypes.length == 1) {
        int includedChatType = includedChatTypes[0];
        String chatTypeName = Lang.getString(TD.chatTypeName(includedChatType));
        boolean hasChanges = false;
        if (input.setStringValueIfChanged(chatTypeName)) {
          editedChatFilter.title = chatTypeName;
          hasChanges = true;
        }
        if (StringUtils.isEmpty(editedChatFilter.iconName)) {
          String chatTypeIconName = TD.chatTypeIconName(includedChatType);
          if (!StringUtils.isEmpty(chatTypeIconName)) {
            editedChatFilter.iconName = chatTypeIconName;
            hasChanges = true;
          }
        }
        if (hasChanges) {
          adapter.updateSimpleItemById(input.getId());
        }
      }
    }
  }

  @Override
  public void onSelectedChatsChanged (int mode, Set<Long> chatIds, Set<Integer> chatTypes) {
    if (mode == SelectChatsController.MODE_FOLDER_INCLUDE_CHATS) {
      TD.updateIncludedChats(editedChatFilter, originChatFilter, chatIds);
      TD.updateIncludedChatTypes(editedChatFilter, chatTypes);
    } else if (mode == SelectChatsController.MODE_FOLDER_EXCLUDE_CHATS) {
      TD.updateExcludedChats(editedChatFilter, chatIds);
      TD.updateExcludedChatTypes(editedChatFilter, chatTypes);
    } else {
      throw new UnsupportedOperationException();
    }
    updateFolderName();
    updateChatFilter(editedChatFilter);
  }

  private void showRemoveConditionConfirm (int position, ListItem item) {
    boolean inclusion = item.getBoolValue();
    CharSequence title;
    if (item.getId() == R.id.chat) {
      title = ((TGFoundChat) item.getData()).getFullTitle();
    } else {
      title = item.getString();
    }
    CharSequence info = Lang.getStringBold(inclusion ? R.string.FolderRemoveInclusionConfirm : R.string.FolderRemoveExclusionConfirm, title);
    showConfirm(info, Lang.getString(R.string.Remove), R.drawable.baseline_delete_24, OPTION_COLOR_RED, () -> {
      int index = adapter.getItem(position) == item ? position : adapter.indexOfView(item);
      if (index != RecyclerView.NO_POSITION) {
        adapter.removeRange(index - 1, 2); /* separator, condition */
      }
      if (item.getId() == R.id.chat) {
        long chatId = item.getLongId();
        if (inclusion) {
          editedChatFilter.pinnedChatIds = ArrayUtils.removeElement(editedChatFilter.pinnedChatIds, ArrayUtils.indexOf(editedChatFilter.pinnedChatIds, chatId));
          editedChatFilter.includedChatIds = ArrayUtils.removeElement(editedChatFilter.includedChatIds, ArrayUtils.indexOf(editedChatFilter.includedChatIds, chatId));
        } else {
          editedChatFilter.excludedChatIds = ArrayUtils.removeElement(editedChatFilter.excludedChatIds, ArrayUtils.indexOf(editedChatFilter.excludedChatIds, chatId));
        }
      } else if (item.getId() == R.id.chatType_contact) {
        editedChatFilter.includeContacts = false;
      } else if (item.getId() == R.id.chatType_nonContact) {
        editedChatFilter.includeNonContacts = false;
      } else if (item.getId() == R.id.chatType_group) {
        editedChatFilter.includeGroups = false;
      } else if (item.getId() == R.id.chatType_channel) {
        editedChatFilter.includeChannels = false;
      } else if (item.getId() == R.id.chatType_bot) {
        editedChatFilter.includeBots = false;
      } else if (item.getId() == R.id.chatType_muted) {
        editedChatFilter.excludeMuted = false;
      } else if (item.getId() == R.id.chatType_read) {
        editedChatFilter.excludeRead = false;
      } else if (item.getId() == R.id.chatType_archived) {
        editedChatFilter.excludeArchived = false;
      }
      updateFolderName();
      updateMenuButton();
    });
  }

  private void showRemoveFolderConfirm () {
    showConfirm(Lang.getString(R.string.RemoveFolderConfirm), Lang.getString(R.string.Remove), R.drawable.baseline_delete_24, OPTION_COLOR_RED, () -> {
      deleteChatFilter(chatFilterId);
    });
  }

  private boolean hasChanges () {
    TdApi.ChatFilter originChatFilter = this.originChatFilter != null ? this.originChatFilter : EMPTY_CHAT_FILTER;
    TdApi.ChatFilter editedChatFilter = this.editedChatFilter != null ? this.editedChatFilter : EMPTY_CHAT_FILTER;
    return !TD.contentEquals(originChatFilter, editedChatFilter);
  }

  private boolean canSaveChanges () {
    String title = editedChatFilter.title.trim();
    if (StringUtils.isEmpty(title)) {
      return false;
    }
    int codePointCount = Character.codePointCount(title, 0, title.length());
    if (codePointCount > MAX_CHAT_FILTER_TITLE_LENGTH) {
      return false;
    }
    return (editedChatFilter.includeContacts || editedChatFilter.includeNonContacts || editedChatFilter.includeGroups || editedChatFilter.includeChannels || editedChatFilter.includeBots || editedChatFilter.pinnedChatIds.length > 0 || editedChatFilter.includedChatIds.length > 0) &&
      (chatFilterId == NO_CHAT_FILTER_ID || hasChanges());
  }

  private void saveChanges () {
    if (chatFilterId != NO_CHAT_FILTER_ID) {
      editChatFilter(chatFilterId, TD.copyOf(editedChatFilter));
    } else {
      createChatFilter(TD.copyOf(editedChatFilter));
    }
  }

  private void createChatFilter (TdApi.ChatFilter chatFilter) {
    tdlib.send(new TdApi.CreateChatFilter(chatFilter), tdlib.resultHandler(TdApi.ChatFilterInfo.class, this::closeSelf));
  }

  private void editChatFilter (int chatFilterId, TdApi.ChatFilter chatFilter) {
    tdlib.send(new TdApi.EditChatFilter(chatFilterId, chatFilter), tdlib.resultHandler(TdApi.ChatFilterInfo.class, this::closeSelf));
  }

  private void deleteChatFilter (int chatFilterId) {
    tdlib.send(new TdApi.DeleteChatFilter(chatFilterId), tdlib.okHandler(this::closeSelf));
  }

  private void closeSelf () {
    if (!isDestroyed()) {
      navigateBack();
    }
  }

  private void updateMenuButton () {
    if (headerView != null) {
      headerView.updateButton(getMenuId(), R.id.menu_btn_done, canSaveChanges() ? View.VISIBLE : View.GONE, 0);
    }
  }

  private class Adapter extends SettingsAdapter {
    public Adapter (ViewController<?> context) {
      super(context);
    }
    @Override
    protected void setChatData (ListItem item, int position, BetterChatView chatView) {
      if (item.getId() == R.id.chat) {
        chatView.setNoSubtitle(false);
        chatView.setChat((TGFoundChat) item.getData());
        chatView.setAllowMaximizePreview(false);
      } else {
        chatView.setTitle(item.getString());
        chatView.setSubtitle(null);
        chatView.setNoSubtitle(true);
        chatView.setAvatar(null, new AvatarPlaceholder.Metadata(item.getIntValue(), item.getIconResource()));
        chatView.clearPreviewChat();
      }
    }

    @Override
    protected SettingHolder initCustom (ViewGroup parent) {
      FrameLayoutFix frameLayout = new FrameLayoutFix(parent.getContext());
      frameLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, Screen.dp(57f)));
      ViewSupport.setThemedBackground(frameLayout, R.id.theme_color_filling, EditChatFolderController.this);

      MaterialEditTextGroup editText = new MaterialEditTextGroup(parent.getContext(), false);
      editText.setId(android.R.id.input);
      editText.applyRtl(Lang.rtl());
      editText.addThemeListeners(EditChatFolderController.this);
      editText.setTextListener(this);
      editText.setFocusListener(this);
      editText.addLengthCounter(true);
      editText.setMaxLength(MAX_CHAT_FILTER_TITLE_LENGTH);
      editText.getEditText().setLineDisabled(true);
      editText.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

      FrameLayout.LayoutParams editTextParams = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
      editTextParams.leftMargin = Screen.dp(16f);
      editTextParams.rightMargin = Screen.dp(57f);
      editTextParams.bottomMargin = Screen.dp(8f);
      frameLayout.addView(editText, editTextParams);

      ImageView imageView = new ImageView(parent.getContext());
      imageView.setId(android.R.id.icon);
      imageView.setScaleType(ImageView.ScaleType.CENTER);
      imageView.setColorFilter(Theme.getColor(R.id.theme_color_icon));
      addThemeFilterListener(imageView, R.id.theme_color_icon);
      RippleSupport.setTransparentSelector(imageView);
      Views.setClickable(imageView);
      imageView.setOnClickListener(v -> showIconSelector());

      FrameLayout.LayoutParams imageViewParams = new FrameLayout.LayoutParams(Screen.dp(57f), Screen.dp(57f), Gravity.CENTER_VERTICAL | Gravity.RIGHT);
      frameLayout.addView(imageView, imageViewParams);

      setLockFocusView(editText.getEditText());

      SettingHolder holder = new SettingHolder(frameLayout);
      holder.setIsRecyclable(false);
      return holder;
    }

    @Override
    protected void setCustom (ListItem item, SettingHolder holder, int position) {
      MaterialEditTextGroup editText = holder.itemView.findViewById(android.R.id.input);
      editText.applyRtl(Lang.rtl());
      editText.setEmptyHint(R.string.FolderNameHint);
      editText.setText(item.getStringValue());

      ImageView imageView = holder.itemView.findViewById(android.R.id.icon);
      int iconResource = TD.iconByName(editedChatFilter.iconName, R.drawable.baseline_folder_24);
      imageView.setImageDrawable(Drawables.get(imageView.getResources(), iconResource));
    }

    @Override
    protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
      if (item.getId() == R.id.btn_folderIncludeChats || item.getId() == R.id.btn_folderExcludeChats) {
        view.setIconColorId(R.id.theme_color_inlineIcon);
      } else if (item.getId() == R.id.btn_removeFolder) {
        view.setIconColorId(R.id.theme_color_iconNegative);
      } else {
        view.setIconColorId(0 /* theme_color_icon */);
      }
    }
  }

  private static class DiffUtilCallback extends ListItemDiffUtilCallback {
    public DiffUtilCallback (List<ListItem> oldList, List<ListItem> newList) {
      super(oldList, newList);
    }

    @Override
    public boolean areItemsTheSame (ListItem oldItem, ListItem newItem) {
      if (oldItem.getViewType() != newItem.getViewType() || oldItem.getId() != newItem.getId())
        return false;
      if (oldItem.getId() == R.id.chat) {
        return oldItem.getLongId() == newItem.getLongId();
      }
      if (oldItem.getViewType() == ListItem.TYPE_SEPARATOR)
        return oldItem.getIntValue() == newItem.getIntValue() && oldItem.getLongValue() == newItem.getLongValue();
      return true;
    }

    @Override
    public boolean areContentsTheSame (ListItem oldItem, ListItem newItem) {
      return false;
    }
  }

  private class RemoveHelperCallback implements RemoveHelper.Callback {
    @Override
    public boolean canRemove (RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int position) {
      return viewHolder.getItemViewType() == ListItem.TYPE_CHAT_BETTER;
    }

    @Override
    public void onRemove (RecyclerView.ViewHolder viewHolder) {
      ListItem item = (ListItem) viewHolder.itemView.getTag();
      int position = viewHolder.getAbsoluteAdapterPosition();
      showRemoveConditionConfirm(position, item);
    }
  }

  private void showIconSelector () {
    ChatFolderIconSelector.show(this, iconName -> {
      if (!ObjectsCompat.equals(editedChatFilter.iconName, iconName)) {
        editedChatFilter.iconName = iconName;
        adapter.updateSimpleItemById(input.getId());
        updateMenuButton();
      }
    });
  }
}
