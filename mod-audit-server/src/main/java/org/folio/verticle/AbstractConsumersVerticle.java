package org.folio.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.GlobalLoadSensor;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.KafkaConsumerWrapper;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.processing.events.utils.PomReaderUtil;
import org.folio.rest.tools.utils.ModuleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConsumersVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final GlobalLoadSensor globalLoadSensor = new GlobalLoadSensor();

  @Autowired
  private KafkaConfig kafkaConfig;

  @Value("${srm.kafka.DataImportConsumer.loadLimit:5}")
  private int loadLimit;

  private final List<KafkaConsumerWrapper<String, String>> consumerWrappers = new ArrayList<>();

  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.info("start:: Starting {} verticle", getClass().getSimpleName());
    List<Future<Void>> futures = new ArrayList<>();

    getEvents().forEach(event -> {
      SubscriptionDefinition subscriptionDefinition = subscriptionDefinition(event, kafkaConfig);
      KafkaConsumerWrapper<String, String> consumerWrapper = KafkaConsumerWrapper.<String, String>builder()
        .context(context)
        .vertx(vertx)
        .kafkaConfig(kafkaConfig)
        .loadLimit(loadLimit)
        .globalLoadSensor(globalLoadSensor)
        .subscriptionDefinition(subscriptionDefinition)
        .build();

      consumerWrappers.add(consumerWrapper);

      futures.add(consumerWrapper.start(getHandler(),
        constructModuleName() + "_" + getClass().getSimpleName()));
    });

    GenericCompositeFuture.all(futures).onComplete(ar -> startPromise.complete());
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    LOGGER.info("stop:: Stopping {} verticle", getClass().getSimpleName());
    List<Future<Void>> futures = new ArrayList<>();
    consumerWrappers.forEach(consumerWrapper -> futures.add(consumerWrapper.stop()));
    GenericCompositeFuture.all(futures).onComplete(ar -> stopPromise.complete());
  }

  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfiguration) {
    return KafkaTopicNameHelper
      .createSubscriptionDefinition(kafkaConfiguration.getEnvId(),
        KafkaTopicNameHelper.getDefaultNameSpace(),
        event);
  }

  private String constructModuleName() {
    return PomReaderUtil.INSTANCE.constructModuleVersionAndVersion(ModuleName.getModuleName(),
      ModuleName.getModuleVersion());
  }

  /**
   * Events that consumer subscribed to.
   *
   * @return list of events
   */
  public abstract List<String> getEvents();

  /**
   * Handler that will be invoked when kafka messages comes to processing.
   *
   * @return handler to porcess kafka message
   */
  public abstract AsyncRecordHandler<String, String> getHandler();
}
