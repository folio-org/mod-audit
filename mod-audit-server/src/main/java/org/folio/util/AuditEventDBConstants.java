package org.folio.util;

public class AuditEventDBConstants {

  private AuditEventDBConstants() {}

  public static final String ID_FIELD = "id";

  public static final String ACTION_FIELD = "action";

  public static final String ORDER_ID_FIELD = "order_id";

  public static final String ORDER_LINE_ID_FIELD = "order_line_id";

  public static final String PIECE_ID_FIELD = "piece_id";

  public static final String USER_ID_FIELD = "user_id";

  public static final String EVENT_DATE_FIELD = "event_date";

  public static final String ACTION_DATE_FIELD = "action_date";

  public static final String MODIFIED_CONTENT_FIELD = "modified_content_snapshot";

  public static final String TOTAL_RECORDS_FIELD = "total_records";

  public static final String ORDER_BY_PATTERN = "ORDER BY %s %s";

  public static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";
}
