package org.folio;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.vertx.junit5.VertxExtension;
import org.apache.commons.io.IOUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.junit.jupiter.api.extension.ExtendWith;



@ExtendWith(VertxExtension.class)
public class TestBase {
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

  public String getFile(String filename) {
    String value = EMPTY;
    try (InputStream inputStream = this.getClass()
      .getClassLoader()
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
