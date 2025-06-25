package org.folio.rest.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CirculationLogsServiceTest {

  @ParameterizedTest
  @MethodSource("getCQLAndFieldsData")
  void test_validateCqlForDisableIndexScan(String cqlQuery, Boolean expectedResult) {
    Boolean isConditionExist = CirculationLogsService.validateCqlForDisableIndexScan(cqlQuery);
    assertThat(isConditionExist, equalTo(expectedResult));
  }

  private static Stream<Arguments> getCQLAndFieldsData() {
    return Stream.of(
      // Scenario 1: Simple query with only required field
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", false),

      // Scenario 2: Query with missing required field
      Arguments.of("(userBarcode=\"USER123\")", false),

      // Scenario 3: Complex query with date range, multiple actions, and valid sort
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\")) sortby date/sort.descending", true),

      // Scenario 4: Query with negative field present (userBarcode)
      Arguments.of("(items=\"ITEM_BARCODE_153009\" and userBarcode==\"*USER_BARCODE_153009*\") sortby date/sort.descending", false),

      // Scenario 5: Query with negative field present (description)
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", false),

      // Scenario 6: Query with invalid sort field
      Arguments.of("(items=\"ITEM_123\") sortby userBarcode/sort.descending", false),

      // Scenario 7: Empty query string
      Arguments.of("", false),

      // Scenario 8: Complex query with multiple actions and valid sort
      Arguments.of("(items=\"ITEM_123\" and action==(\"Created\" or \"Updated\")) sortby date/sort.descending", true),

      // Scenario 0: Query with date range, items, and multiple actions without sort
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\"))", false),

      // Scenario 10: Query with single action sort fields
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", true),

      // Scenario 11: with date and items fields along with a valid sort condition
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", true),

      // Scenario 12: Query with description and items fields along with a valid sort condition
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\" and action==(\"Send\")) sortby date/sort.descending", false)
    );
  }
}