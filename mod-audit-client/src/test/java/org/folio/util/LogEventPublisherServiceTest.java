package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.util.PubSubLogPublisherUtil.EVENT_TYPE;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.acq.model.LogEventPayload;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventDescriptor;
import org.folio.rest.jaxrs.model.PublisherDescriptor;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LogEventPublisherServiceTest extends AbstractRestTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogEventPublisherServiceTest.class);
  public static final int TIMEOUT = 5;

  @Test
  public void testPublishEvent() throws InterruptedException, ExecutionException, TimeoutException {

    LOGGER.info("=== Test: publishing log record event ===");

    LogEventPayload expected = new LogEventPayload().withLoggedObjectType(LogEventPayload.LoggedObjectType.LOAN)
      .withRequest("{request: body}")
      .withResponse("{id:" + UUID.randomUUID().toString() + "}");

    PubSubLogPublisherUtil.sendEventWithPayload(expected, new OkapiConnectionParams(okapiHeaders, Vertx.vertx().getOrCreateContext().owner())).get(TIMEOUT, TimeUnit.SECONDS);

    verify(1, postRequestedFor(urlEqualTo(PUBSUB_PUBLISH_URL)));
    LoggedRequest event = WireMock.findAll(postRequestedFor(urlEqualTo(PUBSUB_PUBLISH_URL))).get(0);
    LogEventPayload actual = new JsonObject(new JsonObject(event.getBodyAsString()).mapTo(Event.class).getEventPayload()).mapTo(LogEventPayload.class);
    assertThat(expected, is(actual));

  }

  @Test
  public void testModuleRegistration() throws Exception {

    LOGGER.info("=== Test: module registration ===");

    PubSubLogPublisherUtil.registerLogEventPublisher(okapiHeaders, Vertx.vertx().getOrCreateContext().owner()).get(TIMEOUT, TimeUnit.SECONDS);

    LoggedRequest loggedRequest1 = WireMock.findAll(postRequestedFor(urlPathEqualTo(PUBSUB_EVENT_TYPES))).get(0);
    EventDescriptor eventDescriptor = new JsonObject(loggedRequest1.getBodyAsString()).mapTo(EventDescriptor.class);
    assertThat(eventDescriptor.getEventType(), is(EVENT_TYPE));

    LoggedRequest loggedRequest2 = WireMock.findAll(postRequestedFor(urlPathEqualTo(PUBSUB_EVENT_TYPES_DECLARE_PUBLISHER_URL))).get(0);
    PublisherDescriptor publisherDescriptor = new JsonObject(loggedRequest2.getBodyAsString()).mapTo(PublisherDescriptor.class);
    assertThat(publisherDescriptor.getModuleId(), is(PubSubClientUtils.constructModuleName()));
    assertThat(publisherDescriptor.getEventDescriptors(), hasSize(1));
    assertThat(publisherDescriptor.getEventDescriptors().get(0).getEventType(), is(EVENT_TYPE));
  }
}
