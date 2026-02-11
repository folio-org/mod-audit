package org.folio.services.diff.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.AdditionalCallNumber;
import org.folio.rest.external.HoldingsRecord;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class HoldingsDiffCalculatorTest {

  private final HoldingsDiffCalculator holdingsDiffCalculator = new HoldingsDiffCalculator();

  @Test
  void shouldReturnHoldingsResourceType() {
    assertThat(holdingsDiffCalculator.getResourceType()).isEqualTo(InventoryResourceType.HOLDINGS);
  }

  @Test
  void shouldInitializeInnerObjectFields() {
    // given
    var holdingsRecord = new HoldingsRecord();

    // when
    var result = holdingsDiffCalculator.access(holdingsRecord).get();

    // then
    assertThat(result.getTags()).isNotNull();
    assertThat(result.getReceivingHistory()).isNotNull();
  }

  @Test
  void shouldDetectFieldModifiedChange() {
    // given
    var oldHoldingsRecord = getMap(new HoldingsRecord().withId("1").withCallNumber("Call Number 1"));
    var newHoldingsRecord = getMap(new HoldingsRecord().withId("1").withCallNumber("Call Number 2"));

    // when
    var changeRecordDTO = holdingsDiffCalculator.calculateDiff(oldHoldingsRecord, newHoldingsRecord);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain call number field modified change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.modified("callNumber", "callNumber", "Call Number 1", "Call Number 2"));
  }

  @Test
  void shouldDetectNewAdditionalCallNumber() {
    List<AdditionalCallNumber> additionalCallNumberList = new ArrayList<>();
    AdditionalCallNumber additionalCallNumber = new AdditionalCallNumber().withCallNumber("123").withPrefix("A").withSuffix("Z");
    additionalCallNumberList.add(additionalCallNumber);
    var oldItem = getMap(new HoldingsRecord().withId("1").withAdditionalCallNumbers(additionalCallNumberList));
    AdditionalCallNumber additionalCallNumberNew = new AdditionalCallNumber().withCallNumber("456").withPrefix("A").withSuffix("Z");
    additionalCallNumberList.add(additionalCallNumberNew);
    var newItem = getMap(new HoldingsRecord().withId("1").withAdditionalCallNumbers(additionalCallNumberList));
    var changeRecordDTO = holdingsDiffCalculator.calculateDiff(oldItem, newItem);
    assertThat(changeRecordDTO.getCollectionChanges()).hasSize(1);
  }

  @Test
  void shouldDetectAdditionalCallNumberChange() {
    List<AdditionalCallNumber> additionalCallNumberList = new ArrayList<>();
    AdditionalCallNumber additionalCallNumber = new AdditionalCallNumber().withCallNumber("123").withPrefix("A").withSuffix("Z");
    additionalCallNumberList.add(additionalCallNumber);
    var oldItem = getMap(new HoldingsRecord().withId("1").withAdditionalCallNumbers(additionalCallNumberList));
    additionalCallNumberList.getFirst().setCallNumber("456");
    var newItem = getMap(new HoldingsRecord().withId("1").withAdditionalCallNumbers(additionalCallNumberList));
    var changeRecordDTO = holdingsDiffCalculator.calculateDiff(oldItem, newItem);
    assertThat(changeRecordDTO.getCollectionChanges()).hasSize(1);
  }

  private static Map<String, Object> getMap(HoldingsRecord obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}
