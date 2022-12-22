package org.folio.util;

import org.apache.commons.lang3.StringUtils;

public class OrderAuditEventDBConstants {

  private OrderAuditEventDBConstants() {}

  public static final String ID_FIELD = "id";

  public static final String ACTION_FIELD = "action";

  public static final String ORDER_ID_FIELD = "order_id";

  public static final String ORDER_LINE_ID_FIELD = "order_line_id";

  public static final String USER_ID_FIELD = "user_id";

  public static final String EVENT_DATE_FIELD = "event_date";

  public static final String ACTION_DATE_FIELD = "action_date";

  public static final String MODIFIED_CONTENT_FIELD = "modified_content_snapshot";

  public static final String TOTAL_RECORDS_FIELD = "total_records";

  public static final String SORTABLE_FIELD = "actionDate";

  public static final String ORDER_BY_PATTERN = " order by action_date";

  public static final String LIMIT_AND_OFFSET_PATTERN = " LIMIT $2 OFFSET $3";

  public static StringBuilder prepareQuery(String query, String orderBy, String limit, String sortBy) {
    StringBuilder queryBuilder = new StringBuilder(query);
    if (StringUtils.isNotEmpty(sortBy) && sortBy.equals(SORTABLE_FIELD)) {
      queryBuilder = queryBuilder.append(orderBy);
    }

    if (!StringUtils.contains(queryBuilder, limit)) {
      queryBuilder = queryBuilder.append(limit);
    }

    return queryBuilder;
  }

}
