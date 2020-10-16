package org.folio;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.vertx.junit5.VertxExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestBase {
  public static final Map<String, String> headersMap = new HashMap<>();
  static {
    headersMap.put("X-Okapi-Tenant", "modaudittest");
    headersMap.put("X-Okapi-Permissions", "audit.all");
    headersMap.put("Content-Type", "application/json");
  }

  public static final String CHECK_IN_PAYLOAD_JSON = "payloads/check_in.json";
  public static final String CHECK_OUT_PAYLOAD_JSON = "payloads/check_out.json";
  public static final String MANUAL_BLOCK_CREATED_PAYLOAD_JSON = "payloads/manual_block_created.json";
  public static final String MANUAL_BLOCK_UPDATED_PAYLOAD_JSON = "payloads/manual_block_updated.json";
  public static final String MANUAL_BLOCK_DELETED_PAYLOAD_JSON = "payloads/manual_block_deleted.json";
  public static final String FEE_FINE_PAYLOAD_JSON = "payloads/fee_fine_billed.json";
  public static final String LOAN_PAYLOAD_JSON = "payloads/loan.json";
  public static final String NOTICE_PAYLOAD_JSON = "payloads/notice.json";

  public String getFile(String filename) {
    String value = "";
    try (InputStream inputStream = this.getClass()
      .getClassLoader()
      .getResourceAsStream(filename)) {
      if (inputStream != null) {
        value = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      value = "";
    }
    return value;
  }
}
