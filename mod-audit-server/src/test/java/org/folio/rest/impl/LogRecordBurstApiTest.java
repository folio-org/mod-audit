package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.folio.utils.TenantApiTestUtil.CHECK_OUT_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.TestSuite;
import org.folio.utils.KafkaTestProducerUtil;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

/**
 * Burst/backpressure regression test for MODAUD-315: publishes a burst of LOG_RECORD Kafka messages
 * that exceeds {@code LogRecordConsumersVerticle}'s configured {@code loadLimit}, verifying the
 * consumer's pause/resume backpressure recovers and every message is eventually processed within a
 * bounded time (instead of, e.g., stalling or dropping records under load).
 *
 * <p>Kept in its own top-level test class (own tenant, own {@code circulation_logs} rows, isolated by
 * {@code ApiTestBase}'s per-nested-class tenant install/purge) rather than folded into
 * {@code CirculationLogsImplApiTest}: that class asserts exact {@code totalRecords} counts across
 * several tests, so adding 60+ extra rows to its shared tenant would require every count assertion in
 * that file to account for the burst, coupling two independent concerns.
 */
public class LogRecordBurstApiTest extends ApiTestBase {

  // LogRecordConsumersVerticle runs with 2 consumer instances, each with loadLimit=10 by default
  // (InitAPIs' audit.log-record.kafka.consumer.* properties), sharing one GlobalLoadSensor. 60 is
  // comfortably above that combined capacity, guaranteeing the burst drives at least one pause/resume
  // cycle, while staying fast enough for routine test runs.
  private static final int BURST_MESSAGE_COUNT = 60;
  // Plain numeric and hyphen-free so CQL's default tokenization treats it as a single exact-match
  // term, consistent with every other userBarcode value used across the existing sample payloads.
  private static final String BURST_USER_BARCODE = "88888888888888";
  private static final Duration BURST_PROCESSING_TIMEOUT = Duration.ofSeconds(60);

  private final Logger logger = LogManager.getLogger();

  @Test
  void burstOfLogRecordsShouldAllBeProcessedUnderBackpressure() {
    logger.info("Burst/backpressure regression test (MODAUD-315): publishing {} LOG_RECORD messages rapidly",
      BURST_MESSAGE_COUNT);

    // "requests" is stripped so CheckOutRecordBuilder emits exactly one LogRecord (the loan
    // check-out) per message, keeping the persisted-record count 1:1 with BURST_MESSAGE_COUNT.
    var basePayload = new JsonObject(getFile(CHECK_OUT_PAYLOAD_JSON)).put("requests", List.of());
    var burstPayloads = IntStream.range(0, BURST_MESSAGE_COUNT)
      .mapToObj(i -> basePayload.copy()
        .put("loanId", UUID.randomUUID().toString())
        .put("userBarcode", BURST_USER_BARCODE)
        .encode())
      .toList();

    var start = Instant.now();
    KafkaTestProducerUtil.publishLogRecordEvents(TestSuite.getVertx(), TENANT.getValue(), burstPayloads);

    var query = String.format("?query=(userBarcode=%s)", BURST_USER_BARCODE);
    await().atMost(BURST_PROCESSING_TIMEOUT)
      .untilAsserted(() -> given().headers(headers()).get(CIRCULATION_LOGS_ENDPOINT + query)
        .then().statusCode(200)
        .assertThat().body("totalRecords", equalTo(BURST_MESSAGE_COUNT)));
    var elapsed = Duration.between(start, Instant.now());

    logger.info("burstOfLogRecordsShouldAllBeProcessedUnderBackpressure:: all {} burst records processed in {}",
      BURST_MESSAGE_COUNT, elapsed);
    assertTrue(elapsed.compareTo(BURST_PROCESSING_TIMEOUT) < 0,
      "Burst of " + BURST_MESSAGE_COUNT + " LOG_RECORD messages took " + elapsed
        + " to fully process, exceeding the bounded-processing-time budget of " + BURST_PROCESSING_TIMEOUT
        + "; this indicates the loadLimit-based backpressure (pause/resume) is not recovering as expected.");
  }
}
