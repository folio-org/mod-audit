package org.folio.builder.service;

import org.folio.TestSuite;
import org.folio.builder.LogRecordBuilderResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class LogRecordBuilderResolverTest extends BuilderTestBase {
  @ParameterizedTest
  @EnumSource(LogEventTypes.class)
  void testLogRecordBuilderResolver(LogEventTypes logEventType) {
    LogRecordBuilder builder = LogRecordBuilderResolver.getBuilder(logEventType.getValue(), new HashMap<>(),
      TestSuite.getVertx().getOrCreateContext());
    assertThat(builder, instanceOf(logEventType.getClazz()));
  }
}
