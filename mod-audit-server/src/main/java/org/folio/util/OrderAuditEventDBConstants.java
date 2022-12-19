package org.folio.util;

public class OrderAuditEventDBConstants {

  private OrderAuditEventDBConstants() {}

  public static final String ID_FIELD = "id";

  public static final String ACTION_FIELD = "action";

  public static final String ORDER_ID_FIELD = "order_id";

  public static final String USER_ID_FIELD = "user_id";

  public static final String EVENT_DATE_FIELD = "event_date";

  public static final String ACTION_DATE_FIELD = "action_date";

  public static final String MODIFIED_CONTENT_FIELD = "modified_content_snapshot";

  public static final String TOTAL_RECORDS_FIELD = "total_records";

  public static final String TABLE_NAME = "acquisition_order_log";

  public static final String GET_BY_ORDER_ID_SQL = "SELECT *, (SELECT count(*) AS total_records FROM %s WHERE order_id = $1)  FROM %s WHERE order_id = $1 LIMIT $2 OFFSET $3";

  public static final String INSERT_SQL = "INSERT INTO %s (id, action, order_id, user_id, user_name, event_date, action_date, modified_content_snapshot) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

}
