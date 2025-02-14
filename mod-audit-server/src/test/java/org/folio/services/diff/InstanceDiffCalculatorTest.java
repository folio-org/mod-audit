package org.folio.services.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.Instance;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class InstanceDiffCalculatorTest {

  private final InstanceDiffCalculator instanceDiffCalculator = new InstanceDiffCalculator();

  @Test
  void shouldReturnInstanceResourceType() {
    assertThat(instanceDiffCalculator.getResourceType()).isEqualTo(InventoryResourceType.INSTANCE);
  }

  @Test
  void shouldInitializeInnerObjectFields() {
    // given
    var instance = new Instance();

    // when
    var result = instanceDiffCalculator.access(instance).get();

    // then
    assertThat(result.getDates()).isNotNull();
    assertThat(result.getTags()).isNotNull();
  }

  @Test
  void shouldDetectFieldModifiedChange() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withTitle("Title 1"));
    var newInstance = getMap(new Instance().withId("1").withTitle("Title 2"));

    // when
    var changeRecordDTO = instanceDiffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain title field modified change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.modified("title", "title", "Title 1", "Title 2"));
  }

  private static Map<String, Object> getMap(Instance obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}