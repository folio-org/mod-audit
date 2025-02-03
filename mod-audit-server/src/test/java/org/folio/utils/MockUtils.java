package org.folio.utils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mockito.stubbing.Stubber;

@UtilityClass
public class MockUtils {

  public static Stubber mockPostgresExecutionSuccess(int futureIndex) {
    return lenient().doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      var future = (Promise<RowSet<Row>>) invocation.getArgument(futureIndex);
      future.complete();
      return null;
    });
  }

  public static Stubber mockPostgresExecutionSuccess(int futureIndex, RowSet<Row> rowSet) {
    return lenient().doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      var future = (Promise<RowSet<Row>>) invocation.getArgument(futureIndex);
      future.complete(rowSet);
      return null;
    });
  }

  public static RowSet<Row> mockRowSet(Row row) {
    var rowSet = mock(RowSet.class);
    var iterator = mockRowIterator(row);
    when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(rowSet.rowCount()).thenReturn(1);
    return rowSet;
  }

  public static RowIterator<Row> mockRowIterator(Row row) {
    var iterator = List.of(row).iterator();
    return new RowIterator<>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Row next() {
        return iterator.next();
      }
    };
  }
}
