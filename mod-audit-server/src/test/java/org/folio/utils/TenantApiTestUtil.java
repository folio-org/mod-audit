package org.folio.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.TestSuite.port;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.tools.PomReader;

import io.restassured.http.Header;

public class TenantApiTestUtil {

  public static final String LOAD_SYNC_PARAMETER = "loadSync";
  private static final int TENANT_OP_WAITINGTIME = 60000;
  public static final Header X_OKAPI_URL = new Header("X-Okapi-Url", "http://localhost:" + port);

  public static final String CHECK_IN_PAYLOAD_JSON = "payloads/check_in.json";
  public static final String CHECK_OUT_PAYLOAD_JSON = "payloads/check_out.json";

  public static final String MANUAL_BLOCK_CREATED_PAYLOAD_JSON = "payloads/manual_block_created.json";
  public static final String MANUAL_BLOCK_UPDATED_PAYLOAD_JSON = "payloads/manual_block_updated.json";
  public static final String MANUAL_BLOCK_DELETED_PAYLOAD_JSON = "payloads/manual_block_deleted.json";
  public static final String FEE_FINE_PAYLOAD_JSON = "payloads/fee_fine_billed.json";
  public static final String LOAN_PAYLOAD_JSON = "payloads/loan.json";
  public static final String LOAN_ANONYMIZE_PAYLOAD_JSON = "payloads/loan_anonymize.json";
  public static final String LOAN_AGE_TO_LOST_PAYLOAD_JSON = "payloads/loan_age_to_lost.json";
  public static final String LOAN_WRONG_ACTION_JSON = "payloads/loan_wrong_action.json";
  public static final String NOTICE_PAYLOAD_JSON = "payloads/notice.json";

  public static final String REQUEST_CREATED_PAYLOAD_JSON = "payloads/request_created.json";
  public static final String REQUEST_EDITED_PAYLOAD_JSON = "payloads/request_edited.json";
  public static final String REQUEST_MOVED_PAYLOAD_JSON = "payloads/request_moved.json";
  public static final String REQUEST_REORDERED_PAYLOAD_JSON = "payloads/request_reordered.json";
  public static final String REQUEST_CANCELLED_PAYLOAD_JSON = "payloads/request_cancelled.json";
  public static final String REQUEST_EXPIRED_PAYLOAD_JSON = "payloads/request_expired.json";

  private TenantApiTestUtil() {

  }

  public static TenantAttributes prepareTenantBody(Boolean isLoadSampleData, Boolean isLoadReferenceData) {
    TenantAttributes tenantAttributes = new TenantAttributes();

    String moduleId = String.format("%s-%s", PomReader.INSTANCE.getModuleName(), PomReader.INSTANCE.getVersion());
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
      return future.get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }

  public static void deleteTenantAndPurgeTables(Header tenantHeader) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    TenantClient tClient = new TenantClient(X_OKAPI_URL.getValue(), tenantHeader.getValue(), null);
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
