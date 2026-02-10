package org.folio.mapper.user;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Function;
import org.folio.dao.user.UserAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.services.diff.user.UserDiffCalculator;
import org.folio.util.user.UserEvent;
import org.folio.util.user.UserEventType;
import org.folio.util.user.UserUtils;
import org.springframework.stereotype.Component;

@Component
public class UserEventToEntityMapper implements Function<UserEvent, UserAuditEntity> {

  private final UserDiffCalculator userDiffCalculator;

  public UserEventToEntityMapper(UserDiffCalculator userDiffCalculator) {
    this.userDiffCalculator = userDiffCalculator;
  }

  @Override
  public UserAuditEntity apply(UserEvent event) {
    var performedByStr = UserUtils.extractPerformedBy(event);
    var performedBy = performedByStr != null ? UUID.fromString(performedByStr) : null;
    var diff = UserEventType.UPDATED.equals(event.getType())
               ? getDiff(event)
               : null;
    return new UserAuditEntity(
      UUID.fromString(event.getId()),
      new Timestamp(event.getTimestamp()),
      UUID.fromString(event.getUserId()),
      event.getType().name(),
      performedBy,
      diff
    );
  }

  private ChangeRecordDto getDiff(UserEvent event) {
    return userDiffCalculator.calculateDiff(event.getOldValue(), event.getNewValue());
  }
}
