package org.folio.services.diff.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.CustomFields;
import org.folio.rest.external.Personal__1;
import org.folio.rest.external.User;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class UserDiffCalculatorTest {

  private UserDiffCalculator userDiffCalculator;

  @BeforeEach
  void setUp() {
    userDiffCalculator = new UserDiffCalculator();
  }

  @Test
  void shouldDetectUsernameChange() {
    var oldUser = getMap(new User().withId("1").withUsername("oldUser"));
    var newUser = getMap(new User().withId("1").withUsername("newUser"));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.modified("username", "username", "oldUser", "newUser"));
  }

  @Test
  void shouldDetectPersonalInfoChange() {
    var oldUser = getMap(new User().withId("1").withPersonal(new Personal__1().withFirstName("John")));
    var newUser = getMap(new User().withId("1").withPersonal(new Personal__1().withFirstName("Jane")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.modified("firstName", "personal.firstName", "John", "Jane"));
  }

  @Test
  void shouldDetectPronounsChange() {
    var oldUser = getMap(new User().withId("1").withPersonal(new Personal__1().withPronouns("she/her")));
    var newUser = getMap(new User().withId("1").withPersonal(new Personal__1().withPronouns("they/them")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.modified("pronouns", "personal.pronouns", "she/her", "they/them"));
  }

  @Test
  void shouldDetectProfilePictureLinkChange() {
    var oldUser = getMap(new User().withId("1")
      .withPersonal(new Personal__1().withProfilePictureLink(URI.create("https://example.com/old.png"))));
    var newUser = getMap(new User().withId("1")
      .withPersonal(new Personal__1().withProfilePictureLink(URI.create("https://example.com/new.png"))));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.modified(
        "profilePictureLink", "personal.profilePictureLink",
        "https://example.com/old.png", "https://example.com/new.png"));
  }

  @Test
  void shouldDetectPreferredEmailCommunicationChange() {
    var oldUser = getMap(new User().withId("1"));
    var newUser = getMap(new User().withId("1"));
    newUser.put("preferredEmailCommunication", List.of("Support"));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getCollectionChanges())
      .hasSize(1)
      .extracting(CollectionChangeDto::getFullPath)
      .containsExactly("preferredEmailCommunication");
  }

  @Test
  void shouldHandleNullNestedObjects() {
    var oldUser = getMap(new User().withId("1"));
    var newUser = getMap(new User().withId("1").withUsername("newUser"));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.added("username", "username", "newUser"));
  }

  @Test
  void shouldReturnNullWhenNoDifference() {
    var userData = getMap(new User().withId("1").withUsername("sameUser"));

    var diff = userDiffCalculator.calculateDiff(userData, userData);

    assertThat(diff).isNull();
  }

  @Test
  void shouldDetectCustomFieldChanges() {
    var oldUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "42")));
    var newUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "meaning")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.modified("reasonForLife", "customFields.reasonForLife", "42", "meaning"));
  }

  @Test
  void shouldDetectAddedCustomField() {
    var oldUser = getMap(new User().withId("1"));
    var newUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "42")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.added("reasonForLife", "customFields.reasonForLife", "42"));
  }

  @Test
  void shouldDetectRemovedCustomField() {
    var oldUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "42")));
    var newUser = getMap(new User().withId("1"));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(1)
      .containsExactly(FieldChangeDto.removed("reasonForLife", "customFields.reasonForLife", "42"));
  }

  @Test
  void shouldDetectMultipleCustomFieldChanges() {
    var oldUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields()
        .withAdditionalProperty("reasonForLife", "42")
        .withAdditionalProperty("toBeRemoved", "gone")));
    var newUser = getMap(new User().withId("1")
      .withCustomFields(new CustomFields()
        .withAdditionalProperty("reasonForLife", "meaning")
        .withAdditionalProperty("brandNew", "hello")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(3)
      .containsExactlyInAnyOrder(
        FieldChangeDto.modified("reasonForLife", "customFields.reasonForLife", "42", "meaning"),
        FieldChangeDto.removed("toBeRemoved", "customFields.toBeRemoved", "gone"),
        FieldChangeDto.added("brandNew", "customFields.brandNew", "hello"));
  }

  @Test
  void shouldDetectCustomFieldAndRegularFieldChanges() {
    var oldUser = getMap(new User().withId("1").withUsername("oldUser")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "42")));
    var newUser = getMap(new User().withId("1").withUsername("newUser")
      .withCustomFields(new CustomFields().withAdditionalProperty("reasonForLife", "meaning")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getFieldChanges())
      .hasSize(2)
      .containsExactlyInAnyOrder(
        FieldChangeDto.modified("username", "username", "oldUser", "newUser"),
        FieldChangeDto.modified("reasonForLife", "customFields.reasonForLife", "42", "meaning"));
  }

  @Test
  void shouldIgnoreAdditionalPropertiesOnUser() {
    var oldUser = getMap(new User().withId("1").withAdditionalProperty("junk", "old"));
    var newUser = getMap(new User().withId("1").withAdditionalProperty("junk", "new"));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff).isNull();
  }

  @Test
  void shouldPopulateFullPathOnCollectionChanges() {
    var oldUser = getMap(new User().withId("1").withProxyFor(List.of("user-1")));
    var newUser = getMap(new User().withId("1").withProxyFor(List.of("user-2")));

    var diff = userDiffCalculator.calculateDiff(oldUser, newUser);

    assertThat(diff.getCollectionChanges())
      .hasSize(1)
      .extracting(CollectionChangeDto::getFullPath)
      .containsExactly("proxyFor");
  }

  private static Map<String, Object> getMap(User obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}
