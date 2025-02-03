package org.folio.util;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DbUtils {
  private DbUtils() {
  }

  public static String formatDBTableName(String tenantId, String table) {
    return format("%s.%s", convertToPsqlStandard(tenantId), table);
  }

  public static Stream<Row> streamOf(RowSet<Row> rowSet) {
    Objects.requireNonNull(rowSet);
    Spliterator<Row> spliterator = Spliterators.spliterator(rowSet.iterator(), rowSet.rowCount(), Spliterator.ORDERED);
    return StreamSupport.stream(spliterator, false);
  }
}
