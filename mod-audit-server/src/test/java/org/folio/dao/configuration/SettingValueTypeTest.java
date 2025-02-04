package org.folio.dao.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class SettingValueTypeTest {

  @ParameterizedTest
  @MethodSource("validEnumValues")
  void testFromValueWithValidValues(String value, SettingValueType expected) {
    assertEquals(expected, SettingValueType.fromValue(value));
  }

  @Test
  void testFromValueWithInvalidValue() {
    var exception = assertThrows(IllegalArgumentException.class, () -> {
      SettingValueType.fromValue("INVALID");
    });
    assertEquals("Value 'INVALID' is not a valid SettingValueType", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("validEnumValues")
  void testToString(String expected, SettingValueType type) {
    assertEquals(expected, type.toString());
  }

  @ParameterizedTest
  @MethodSource("validEnumValues")
  void testValue(String expected, SettingValueType type) {
    assertEquals(expected, type.value());
  }

  private static Stream<Arguments> validEnumValues() {
    return Stream.of(
      Arguments.of("STRING", SettingValueType.STRING),
      Arguments.of("INTEGER", SettingValueType.INTEGER),
      Arguments.of("BOOLEAN", SettingValueType.BOOLEAN)
    );
  }
}