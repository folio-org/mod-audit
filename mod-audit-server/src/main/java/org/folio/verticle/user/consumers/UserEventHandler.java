package org.folio.verticle.user.consumers;

import static org.folio.util.user.UserEventType.UNKNOWN;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.user.UserEventService;
import org.folio.util.user.UserEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserEventHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventService userEventService;
  private final Vertx vertx;

  public UserEventHandler(Vertx vertx, UserEventService userEventService) {
    this.vertx = vertx;
    this.userEventService = userEventService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var kafkaHeaders = kafkaConsumerRecord.headers();
    var okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(UserEvent.class);
    event.setUserId(kafkaConsumerRecord.key());

    if (UNKNOWN == event.getType()) {
      LOGGER.debug("handle:: Event type not supported [eventId: {}, userId: {}]",
        event.getId(), event.getUserId());
      result.complete(event.getId());
      return result.future();
    }

    LOGGER.info("handle:: Starting processing of User event with id: {} for user id: {}", event.getId(), event.getUserId());
    userEventService.processEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("handle:: User event with id: {} has been processed for user id: {}", event.getId(), event.getUserId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate User event with id: {} for user id: {} received, skipped processing", event.getId(), event.getUserId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of User event with id: {} for user id: {} has been failed", event.getId(), event.getUserId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
