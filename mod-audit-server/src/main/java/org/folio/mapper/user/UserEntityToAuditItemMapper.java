package org.folio.mapper.user;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.folio.dao.user.UserAuditEntity;
import org.folio.mapper.DiffMapper;
import org.folio.rest.jaxrs.model.UserAuditItem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEntityToAuditItemMapper implements Function<UserAuditEntity, UserAuditItem> {

  private final DiffMapper diffMapper;

  @Override
  public UserAuditItem apply(UserAuditEntity auditEntity) {
    var item = new UserAuditItem()
      .withEventId(auditEntity.eventId().toString())
      .withEventTs(auditEntity.eventDate().getTime())
      .withEventDate(auditEntity.eventDate())
      .withUserId(auditEntity.userId().toString())
      .withAction(auditEntity.action())
      .withDiff(diffMapper.map(auditEntity.diff()));
    if (auditEntity.performedBy() != null) {
      item.withPerformedBy(auditEntity.performedBy().toString());
    }
    return item;
  }
}
