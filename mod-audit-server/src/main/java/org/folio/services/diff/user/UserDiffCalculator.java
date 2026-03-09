package org.folio.services.diff.user;

import java.util.function.Supplier;
import org.folio.rest.external.CustomFields;
import org.javers.core.diff.changetype.map.MapChange;
import org.folio.rest.external.Metadata;
import org.folio.rest.external.Personal__1;
import org.folio.rest.external.Tags__3;
import org.folio.rest.external.User;
import org.folio.services.diff.DiffCalculator;
import org.springframework.stereotype.Component;

@Component
public class UserDiffCalculator extends DiffCalculator<User> {

  @Override
  protected Supplier<User> access(User value) {
    return () -> {
      if (value.getPersonal() == null) {
        value.setPersonal(new Personal__1());
      }
      if (value.getMetadata() == null) {
        value.setMetadata(new Metadata());
      }
      if (value.getTags() == null) {
        value.setTags(new Tags__3());
      }
      if (value.getCustomFields() == null) {
        value.setCustomFields(new CustomFields());
      }
      return value;
    };
  }

  @Override
  protected boolean shouldProcessMapChange(MapChange mapChange) {
    return mapChange.getPropertyNameWithPath().startsWith("customFields.");
  }

  @Override
  protected Class<User> getType() {
    return User.class;
  }
}
