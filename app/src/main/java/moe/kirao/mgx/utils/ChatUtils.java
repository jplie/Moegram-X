package moe.kirao.mgx.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

import me.vkryl.core.StringUtils;
import me.vkryl.core.lambda.RunnableData;
import me.vkryl.td.Td;

public class ChatUtils {
  @Nullable
  public static String resolveUserLocal(@NonNull Tdlib tdlib, long userId) {
    TdApi.MessageSender sender = tdlib.sender(userId);
    if (tdlib.senderName(sender).startsWith("User#")) return null;
    if (!StringUtils.isEmptyOrBlank(tdlib.senderUsername(sender))) {
      return String.join("\n", String.valueOf(userId), tdlib.senderName(sender), '@' + tdlib.senderUsername(sender));
    } else {
      return String.join("\n", String.valueOf(userId), tdlib.senderName(sender));
    }
  }

  public static void processAuthorRequest (@NonNull Tdlib tdlib, long botUserId, String query, RunnableData<String> after) {
    tdlib.client().send(new TdApi.GetInlineQueryResults(botUserId, tdlib.selfChatId(), null, query, null), article -> {
      if (article.getConstructor() == TdApi.InlineQueryResults.CONSTRUCTOR) {
        TdApi.InlineQueryResult result = ((TdApi.InlineQueryResults) article).results[0];
        if (result.getConstructor() == TdApi.InlineQueryResultArticle.CONSTRUCTOR) {
          long queryId = ((TdApi.InlineQueryResults) article).inlineQueryId;
          String resultId = ((TdApi.InlineQueryResultArticle) result).id;
          tdlib.client().send(new TdApi.SendInlineQueryResultMessage(tdlib.selfChatId(), 0, null, Td.newSendOptions(true), queryId, resultId, false), newMsg -> {
            if (newMsg.getConstructor() != TdApi.Message.CONSTRUCTOR) return;
            tdlib.deleteMessages(tdlib.selfChatId(), new long[] {((TdApi.Message) newMsg).id}, true);
            after.runWithData(((TdApi.MessageText) ((TdApi.Message) newMsg).content).text.text);
          });
        }
      }
    });
  }
}