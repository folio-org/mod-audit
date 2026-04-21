package org.folio.utils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mockito.stubbing.Stubber;

@UtilityClass
public class MockUtils {

  public static Stubber mockPostgresHandlerSuccess(int handlerIndex) {
    return lenient().doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<RowSet<Row>>> handler = invocation.getArgument(handlerIndex);
      handler.handle(Future.succeededFuture());
      return null;
    });
  }

  public static Stubber mockPostgresHandlerSuccess(int handlerIndex, RowSet<Row> rowSet) {
    return lenient().doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Handler<AsyncResult<RowSet<Row>>> handler = invocation.getArgument(handlerIndex);
      handler.handle(Future.succeededFuture(rowSet));
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
