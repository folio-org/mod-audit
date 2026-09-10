package org.folio.verticle.logrecord.consumers;

import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.services.logrecord.LogRecordService;
import org.folio.util.KafkaUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LogRecordEventHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final LogRecordService logRecordService;
  private final Vertx vertx;

  public LogRecordEventHandler(Vertx vertx, LogRecordService logRecordService) {
    this.vertx = vertx;
    this.logRecordService = logRecordService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var recordKey = kafkaConsumerRecord.key();

    JsonObject payload;
    String logEventType;
    try {
      payload = new JsonObject(kafkaConsumerRecord.value());
      logEventType = payload.getString(LOG_EVENT_TYPE.value());
    } catch (Exception e) {
      LOGGER.warn("handle:: Failed to parse LOG_RECORD event [key: {}] due to: {}", recordKey, e.getMessage());
      result.complete(recordKey);
      return result.future();
    }

    var okapiHeaders = KafkaUtils.getOkapiHeaders(kafkaConsumerRecord);
    var vertxContext = vertx.getOrCreateContext();

    LOGGER.info("handle:: Starting processing of LOG_RECORD event [key: {}, logEventType: {}]", recordKey, logEventType);
    logRecordService.processLogRecord(logEventType, payload, okapiHeaders, vertxContext)
      .onSuccess(ar -> {
        LOGGER.info("handle:: LOG_RECORD event [key: {}, logEventType: {}] has been processed", recordKey, logEventType);
        result.complete(recordKey);
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.warn("handle:: Duplicate LOG_RECORD event [key: {}, logEventType: {}] received, skipped processing",
            recordKey, logEventType);
          result.complete(recordKey);
        } else {
          LOGGER.error("Processing of LOG_RECORD event [key: {}, logEventType: {}] has failed", recordKey, logEventType, e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
