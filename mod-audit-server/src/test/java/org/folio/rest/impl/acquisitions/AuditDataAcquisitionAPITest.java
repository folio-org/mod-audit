package org.folio.rest.impl.acquisitions;

import io.restassured.http.Header;
import org.folio.rest.impl.ApiTestBase;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuditDataAcquisitionAPITest extends ApiTestBase {

  public static final Header TENANT = new Header("X-Okapi-Tenant", "modaudittest");

  protected static final Header PERMS = new Header("X-Okapi-Permissions", "audit.all");

  protected static final Header CONTENT_TYPE = new Header("Content-Type", "application/json");

  protected static final String INVALID_ID = "646ea52c-2c65-4d28-9a8f-e0d200fd6b00";

  protected static final String ID = "646ea52c-2c65-4d28-9a8f-e7d236fd6b09";

  protected static final String ACQ_AUDIT_ORDER_PATH = "/audit-data/acquisition/order/";

  @Test
  void shouldReturnJobExecutionOnGetById() {
    given().header(CONTENT_TYPE).get("/admin/health").then().log().all().statusCode(200);

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ INVALID_ID).then().log().all().statusCode(404)
      .body(containsString("OrderAuditEvent with id \'"+INVALID_ID+ "\' was not found"));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ID).then().log().all().statusCode(200)
      .body(containsString(ID));

    given().header(CONTENT_TYPE).header(TENANT).header(PERMS).get(ACQ_AUDIT_ORDER_PATH+ ID + 123).then().log().all().statusCode(500)
      .body(containsString("Internal Server Error"));
  }
}
