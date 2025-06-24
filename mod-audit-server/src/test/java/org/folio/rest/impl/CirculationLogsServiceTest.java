package org.folio.rest.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CirculationLogsServiceTest {

  @ParameterizedTest
  @MethodSource("getCQLAndFieldsData")
  void test_checkCqlFieldsAndSortConditions(String cqlQuery, String fieldName, List<String> negativeFieldNames, String sortFieldName, Boolean expectedResult) {
    Boolean isConditionExist = CirculationLogsService.checkCqlFieldsAndSortConditions(cqlQuery, fieldName, negativeFieldNames, sortFieldName);
    assertThat(isConditionExist, equalTo(expectedResult));
  }

  private static Stream<Arguments> getCQLAndFieldsData() {
    return Stream.of(
      // Scenario 1: Simple query with only required field
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", "items", null, null, true),

      // Scenario 2: Query with missing required field
      Arguments.of("(userBarcode=\"USER123\")", "items", null, null, false),

      // Scenario 3: Complex query with date range, multiple actions, and valid sort
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\")) sortby date/sort.descending",
        "items", List.of("description", "userBarcode"), "date", true),

      // Scenario 4: Query with negative field present (userBarcode)
      Arguments.of("(items=\"ITEM_BARCODE_153009\" and userBarcode==\"*USER_BARCODE_153009*\") sortby date/sort.descending",
        "items", List.of("description", "userBarcode"), "date", false),

      // Scenario 5: Query with negative field present (description)
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", "items",
        List.of("description", "userBarcode"), "date", false),

      // Scenario 6: Query with invalid sort field
      Arguments.of("(items=\"ITEM_123\") sortby userBarcode/sort.descending", "items", null, "date", false),

      // Scenario 7: Query with null negative fields but valid sort
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", null,
        "date", true),

      // Scenario 8: Query with all null parameters
      Arguments.of("(items=\"ITEM_123\")", null, null, null, true),

      // Scenario 9: Query with empty lists
      Arguments.of("(items=\"ITEM_123\")", "", Collections.emptyList(), "", true),

      // Scenario 10: Empty query string
      Arguments.of("", "items", List.of("description"), "date", false),

      // Scenario 11: Complex query with multiple actions and valid sort
      Arguments.of("(items=\"ITEM_123\" and action==(\"Created\" or \"Updated\")) sortby date/sort.descending",
        "items", List.of("description"), "date", true),

      // Scenario 12: Query without sort, testing field presence
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\"))",
        "items", List.of("description", "userBarcode"), "date", false),

      // Scenario 13: Query with null sort fields
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", null,
        null, true),
      // Scenario 14: More scenarios
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\"))", "items", List.of("description", "userBarcode"), "date", false),
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", true),
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\")) sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", true),
      Arguments.of("(items=\"ITEM_BARCODE_153009\" and userBarcode==\"*USER_BARCODE_153009*\") sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", false),
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", false),
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\" and action==(\"Send\")) sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", List.of("description", "userBarcode"), "date", true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", null, "date", true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", null, "action", false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", "items", null, null, true),
      Arguments.of("(items=\"ITEM_123\") sortby userBarcode/sort.descending", "items", null, "date", false),
      Arguments.of("(items=\"ITEM_123\")", null, null, null, true),
      Arguments.of("(items=\"ITEM_123\")", "", Collections.emptyList(), "", true),
      Arguments.of("", "items",  List.of("description"), "date", false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", "items",  null, null, true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", "items",  null, null, true)
    );
  }
}