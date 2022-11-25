package org.folio.verticle.acquisition;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.*;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.processing.events.utils.PomReaderUtil;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.util.OrderEventTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class OrderEventConsumerVerticle extends AbstractVerticle {

  private static final GlobalLoadSensor globalLoadSensor = new GlobalLoadSensor();

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  @Qualifier("newKafkaConfig")
  private KafkaConfig kafkaConfig;

  @Value("${orders.kafka.OrderConsumer.loadLimit:5}")
  private int loadLimit;

  @Autowired
  @Qualifier("OrderEventHandler")
  private AsyncRecordHandler<String, String> OrderEventHandler;

  @Autowired
  @Qualifier("OrderEventErrorHandler")
  private ProcessRecordErrorHandler<String, String> OrderEventErrorHandler;


  //TODO: should be changed to the real value
  public List<String> getEvents() {
    return List.of(OrderEventTypes.ORDER_TYPE_CHANGED.value());
  }

  @Override
  public void start(Promise<Void> startPromise) {

    LOGGER.debug("OrderEventConsumerVerticle :: start");

    List<Future<Void>> futures = new ArrayList<>();

    getEvents().forEach(event -> {
      SubscriptionDefinition subscriptionDefinition = KafkaTopicNameHelper
        .createSubscriptionDefinition(kafkaConfig.getEnvId(),
          KafkaTopicNameHelper.getDefaultNameSpace(),
          event);
      KafkaConsumerWrapper<String, String> consumerWrapper = KafkaConsumerWrapper.<String, String>builder()
        .context(context)
        .vertx(vertx)
        .kafkaConfig(kafkaConfig)
        .loadLimit(loadLimit)
        .globalLoadSensor(globalLoadSensor)
        .subscriptionDefinition(subscriptionDefinition)
        .processRecordErrorHandler(getErrorHandler())
        .build();

      futures.add(consumerWrapper.start(getHandler(),
        constructModuleName() + "_" + getClass().getSimpleName()));
    });

    GenericCompositeFuture.all(futures).onComplete(ar -> startPromise.complete());
  }

  public static String constructModuleName() {
    return PomReaderUtil.INSTANCE.constructModuleVersionAndVersion(ModuleName.getModuleName(),
      ModuleName.getModuleVersion());
  }

  public AsyncRecordHandler<String, String> getHandler() {
    return this.OrderEventHandler;
  }

  public ProcessRecordErrorHandler<String, String> getErrorHandler() {
    return this.OrderEventErrorHandler;
  }
}
