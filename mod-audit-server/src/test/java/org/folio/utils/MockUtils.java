package org.folio.utils;

import static org.mockito.Mockito.lenient;

import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
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
}
