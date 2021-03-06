package org.folio.builder.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import java.util.HashMap;

import org.folio.TestSuite;
import org.folio.builder.LogRecordBuilderResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class LogRecordBuilderResolverTest extends BuilderTestBase {
  @ParameterizedTest
  @EnumSource(LogEventTypes.class)
  void testLogRecordBuilderResolver(LogEventTypes logEventType) {
    LogRecordBuilder builder = LogRecordBuilderResolver.getBuilder(logEventType.getValue(), new HashMap<>(),
      TestSuite.getVertx().getOrCreateContext());
    assertThat(builder, instanceOf(logEventType.getClazz()));
  }
}
