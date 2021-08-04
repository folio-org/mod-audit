package org.folio.builder.service;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_DELETED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_MODIFIED;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.PAYLOAD;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.builder.description.ManualBlockDescriptionBuilder;
import org.folio.rest.external.User;
import org.folio.rest.external.UserCollection;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.external.ManualBlock;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public class ManualBlockRecordBuilder extends LogRecordBuilder {
  public ManualBlockRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {

    String logEventType = getProperty(event, LOG_EVENT_TYPE);
    var block = getObjectProperty(event, PAYLOAD).mapTo(ManualBlock.class);

    String userId = block.getUserId();
    String sourceId = block.getMetadata()
      .getUpdatedByUserId();

    return getEntitiesByIds(USERS_URL, UserCollection.class, 2, 0, userId, sourceId).thenCompose(users -> {
      Map<String, User> usersGroupedById = StreamEx.of(users.getUsers()).collect(toMap(User::getId, identity()));
      LogRecord manualBlockLogRecord = buildManualBlockLogRecord(block, logEventType, userId, sourceId, usersGroupedById);
      return CompletableFuture.completedFuture(singletonList(manualBlockLogRecord));
    });
  }

  private LogRecord buildManualBlockLogRecord(ManualBlock block, String logEventType, String userId, String sourceId,
      Map<String, User> usersGroupedById) {
    return new LogRecord().withObject(LogRecord.Object.MANUAL_BLOCK)
      .withUserBarcode(ofNullable(usersGroupedById.get(userId)).flatMap(user -> of(user.getBarcode())).orElse(null))
      .withSource(getSource(logEventType, sourceId, usersGroupedById))
      .withAction(resolveLogRecordAction(logEventType))
      .withDate(new Date())
      .withDescription(new ManualBlockDescriptionBuilder().buildDescription(block))
      .withLinkToIds(new LinkToIds().withUserId(userId));
  }

  private String getSource(String logEventType, String sourceId, Map<String, User> usersGroupedById) {
    var user = usersGroupedById.get(sourceId);
    return isNull(user) || isNull(user.getPersonal()) ? null
        : buildPersonalName(user.getPersonal().getFirstName(), user.getPersonal().getLastName());
  }

  private LogRecord.Action resolveLogRecordAction(String logEventType) {
    if (MANUAL_BLOCK_CREATED.equals(logEventType)) {
      return LogRecord.Action.CREATED;
    } else if (MANUAL_BLOCK_MODIFIED.equals(logEventType)) {
      return LogRecord.Action.MODIFIED;
    } else if (MANUAL_BLOCK_DELETED.equals(logEventType)) {
      return LogRecord.Action.DELETED;
    } else {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }

}
