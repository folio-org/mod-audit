package org.folio.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.TestSuite.port;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.RmbVersion;

import io.restassured.http.Header;

public class TenantApiTestUtil {

  public static final String LOAD_SYNC_PARAMETER = "loadSync";
  private static final int TENANT_OP_WAITINGTIME = 60000;
  public static final Header X_OKAPI_URL = new Header("X-Okapi-Url", "http://localhost:" + port);
  public static final Header X_OKAPI_URL_TO = new Header("X-Okapi-Url-To", "http://localhost:" + port);

  public static final String CHECK_IN_PAYLOAD_JSON = "payloads/check_in.json";
  public static final String CHECK_IN_WITH_TIMEZONE_PAYLOAD_JSON = "payloads/check_in_with_timezone.json";
  public static final String CHECK_IN_WITH_BACKDATE_TIMEZONE_PAYLOAD_JSON = "payloads/check_in_with_backdate_timezone.json";
  public static final String CHECK_OUT_PAYLOAD_JSON = "payloads/check_out.json";
  public static final String CHECK_OUT_THROUGH_OVERRIDE_PAYLOAD_JSON = "payloads/check_out_through_override.json";

  public static final String MANUAL_BLOCK_CREATED_PAYLOAD_JSON = "payloads/manual_block_created.json";
  public static final String MANUAL_BLOCK_UPDATED_PAYLOAD_JSON = "payloads/manual_block_updated.json";
  public static final String MANUAL_BLOCK_DELETED_PAYLOAD_JSON = "payloads/manual_block_deleted.json";
  public static final String FEE_FINE_PAYLOAD_JSON = "payloads/fee_fine_billed.json";
  public static final String FEE_FINE_VIRTUAL_ITEM_PAYLOAD_JSON = "payloads/fee_fine_billed_automated_virtual_item.json";
  public static final String LOAN_PAYLOAD_JSON = "payloads/loan.json";
  public static final String LOAN_ANONYMIZE_PAYLOAD_JSON = "payloads/anonymize_loan.json";
  public static final String LOAN_AGE_TO_LOST_PAYLOAD_JSON = "payloads/loan_age_to_lost.json";
  public static final String LOAN_CHANGED_DUE_DATE_PAYLOAD_JSON = "payloads/loan_changed_due_date.json";
  public static final String LOAN_WRONG_ACTION_JSON = "payloads/loan_wrong_action.json";
  public static final String LOAN_EMPTY_ACTION_JSON = "payloads/loan_empty_action.json";
  public static final String NOTICE_PAYLOAD_JSON = "payloads/notice.json";
  public static final String NOTICE_ERROR_FULL_PAYLOAD_JSON = "payloads/notice_error_full.json";
  public static final String NOTICE_ERROR_MINIMAL_PAYLOAD_JSON = "payloads/notice_error_minimal.json";
  public static final String NOTICE_ERROR_NON_EXISTENT_USER_ID = "payloads/notice_error_non_existent_user_id.json";
  public static final String REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON =
    "payloads/request_created_through_override.json";

  public static final String REQUEST_CREATED_PAYLOAD_JSON = "payloads/request_created.json";
  public static final String REQUEST_EDITED_PAYLOAD_JSON = "payloads/request_edited.json";

  public static final String REQUEST_EDITED_PAYLOAD_WITH_NON_EMPTY_DATE_JSON = "payloads/request_edited_with_non_empty_date.json";
  public static final String REQUEST_MOVED_PAYLOAD_JSON = "payloads/request_moved.json";
  public static final String REQUEST_REORDERED_PAYLOAD_JSON = "payloads/request_reordered.json";
  public static final String REQUEST_CANCELLED_PAYLOAD_JSON = "payloads/request_cancelled.json";
  public static final String REQUEST_EXPIRED_PAYLOAD_JSON = "payloads/request_expired.json";
  public static final String REQUEST_PICKUP_EXPIRED_PAYLOAD_JSON = "payloads/request_pickup_expired.json";

  public static final String ANONYMIZE_CHECK_IN = "payloads/anonymize_check_in.json";
  public static final String ANONYMIZE_CHECK_OUT = "payloads/anonymize_check_out.json";
  public static final String ANONYMIZE_LOAN_CLOSED = "payloads/anonymize_loan_closed.json";

  public static final List<String> SAMPLES = Arrays.asList(CHECK_IN_PAYLOAD_JSON, CHECK_OUT_PAYLOAD_JSON, MANUAL_BLOCK_CREATED_PAYLOAD_JSON, MANUAL_BLOCK_UPDATED_PAYLOAD_JSON, MANUAL_BLOCK_DELETED_PAYLOAD_JSON,
    FEE_FINE_PAYLOAD_JSON, FEE_FINE_VIRTUAL_ITEM_PAYLOAD_JSON, LOAN_PAYLOAD_JSON, LOAN_AGE_TO_LOST_PAYLOAD_JSON, LOAN_WRONG_ACTION_JSON, LOAN_EMPTY_ACTION_JSON, NOTICE_PAYLOAD_JSON,
    REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON, REQUEST_CREATED_PAYLOAD_JSON, REQUEST_EDITED_PAYLOAD_JSON, REQUEST_MOVED_PAYLOAD_JSON,
    REQUEST_REORDERED_PAYLOAD_JSON, REQUEST_CANCELLED_PAYLOAD_JSON, REQUEST_EXPIRED_PAYLOAD_JSON, ANONYMIZE_CHECK_OUT, ANONYMIZE_CHECK_IN, ANONYMIZE_LOAN_CLOSED);

  private TenantApiTestUtil() {

  }

  public static TenantAttributes prepareTenantBody(Boolean isLoadSampleData, Boolean isLoadReferenceData) {
    TenantAttributes tenantAttributes = new TenantAttributes();

    String moduleId = String.format("%s-%s", ModuleName.getModuleName(), RmbVersion.getRmbVersion());
    List<Parameter> parameters = new ArrayList<>();
    parameters.add(new Parameter().withKey("loadReference")
      .withValue(isLoadReferenceData.toString()));
    parameters.add(new Parameter().withKey("loadSample")
      .withValue(isLoadSampleData.toString()));
    parameters.add(new Parameter().withKey(LOAD_SYNC_PARAMETER)
      .withValue("true"));

    tenantAttributes.withModuleTo(moduleId)
      .withParameters(parameters);

    return tenantAttributes;
  }

  public static TenantJob prepareTenant(Header tenantHeader, boolean isLoadSampleData, boolean isLoadReferenceData) {
    TenantAttributes tenantAttributes = prepareTenantBody(isLoadSampleData, isLoadReferenceData);
    return postTenant(tenantHeader, tenantAttributes);
  }

  public static TenantJob postTenant(Header tenantHeader, TenantAttributes tenantAttributes) {
    CompletableFuture<TenantJob> future = new CompletableFuture<>();
    TenantClient tClient = new TenantClient(X_OKAPI_URL.getValue(), tenantHeader.getValue(), null);
    try {
      tClient.postTenant(tenantAttributes, event -> {
        if (event.failed()) {
          future.completeExceptionally(event.cause());
        } else {
          TenantJob tenantJob = event.result()
            .bodyAsJson(TenantJob.class);
          tClient.getTenantByOperationId(tenantJob.getId(), TENANT_OP_WAITINGTIME, result -> {
            if (result.failed()) {
              future.completeExceptionally(result.cause());
            } else {
              future.complete(tenantJob);
            }
          });
        }
      });
      return future.get(120, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }

  public static void deleteTenantAndPurgeTables(Header tenantHeader) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    TenantClient tClient = new TenantClient(X_OKAPI_URL_TO.getValue(), tenantHeader.getValue(), null);
    TenantAttributes tenantAttributes = prepareTenantBody(false, false).withPurge(true);
    try {
      tClient.postTenant(tenantAttributes, event -> {
        if (event.failed()) {
          future.completeExceptionally(event.cause());
        } else {
          future.complete(null);
        }
      });
      future.get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail(e);
    }
  }

  public static void deleteTenant(TenantJob tenantJob, Header tenantHeader) {
    TenantClient tenantClient = new TenantClient(X_OKAPI_URL_TO.getValue(), tenantHeader.getValue(), null);

    if (tenantJob != null) {
      CompletableFuture<Void> completableFuture = new CompletableFuture<>();
      tenantClient.deleteTenantByOperationId(tenantJob.getId(), event -> {
        if (event.failed()) {
          completableFuture.completeExceptionally(event.cause());
        } else {
          completableFuture.complete(null);
        }
      });
      try {
        completableFuture.get(60, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        fail(e);
      }

    }

  }

  private JsonObject getSampleAsJson(String fullPath) throws IOException {
    try (InputStream resourceAsStream = TenantApiTestUtil.class.getClassLoader().getResourceAsStream(fullPath)) {
      if (resourceAsStream != null) {
        return new JsonObject(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
      }
    }
    throw new IOException("Error loading sample file");
  }

  public static String getFile(String filename) {
    String value = EMPTY;
    try (InputStream inputStream = TenantApiTestUtil.class.getClassLoader()
      .getResourceAsStream(filename)) {
      if (inputStream != null) {
        value = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      value = EMPTY;
    }
    return value;
  }
}
