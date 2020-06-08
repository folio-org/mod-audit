package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.okapi.common.ErrorType.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.okapi.common.ExtendedAsyncResult;
import org.folio.okapi.common.Failure;
import org.folio.okapi.common.Success;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Audit;
import org.folio.rest.jaxrs.model.AuditCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.resource.AuditData;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.OutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.tools.utils.ValidationHelper;
import org.folio.cql2pgjson.CQL2PgJSON;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AuditDataImpl implements AuditData {

  protected static final String API_CXT = "/audit-data";
  protected static final String DB_TAB_AUDIT = "audit_data";
  protected static final String DB_TAB_AUDIT_ID = "id";
  protected static final String JSON_SCHEMA_AUDIT = "ramls/audit.json";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Messages messages = Messages.getInstance();
  private String auditSchema = null;

  private void initCQLValidation() {
    try {
      auditSchema = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(JSON_SCHEMA_AUDIT)), "UTF-8");
    } catch (Exception e) {
      logger.error("unable to load schema - " + JSON_SCHEMA_AUDIT + ", validation of query fields will not be active",
        e);
    }
  }

  public AuditDataImpl() {
      initCQLValidation();
  }

  private CQLWrapper getCQL(String query, int limit, int offset)
    throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(DB_TAB_AUDIT + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getAuditData(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logger.debug("Getting Audit data " + offset + "+" + limit + " q=" + query);

    CQLWrapper cql;
    try {
      cql = getCQL(query, limit, offset);
    } catch (Exception e) {
      ValidationHelper.handleError(e, asyncResultHandler);
      return;
    }

    getClient(okapiHeaders, vertxContext).get(DB_TAB_AUDIT, Audit.class, new String[] { "*" }, cql, true, false,
      reply -> {
        if (reply.succeeded()) {
          AuditCollection auditCollection = new AuditCollection();
          auditCollection.setAudit(reply.result().getResults());
          Integer totalRecords = reply.result().getResultInfo().getTotalRecords();
          auditCollection.setTotalRecords(totalRecords);
          asyncResultHandler
            .handle(succeededFuture(GetAuditDataResponse.respond200WithApplicationJson(auditCollection)));
        } else {
          ValidationHelper.handleError(reply.cause(), asyncResultHandler);
        }
      });
  }

  @Override
  @Validate
  public void postAuditData(String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logger.debug("Save Audit record " + audit);

    String id = audit.getId();
    if (id == null || id.isEmpty()) {
      audit.setId(UUID.randomUUID().toString());
    }
    getClient(okapiHeaders, vertxContext).save(DB_TAB_AUDIT, id, audit, reply -> {
      if (reply.succeeded()) {
        String ret = reply.result();
        audit.setId(ret);
        OutStream stream = new OutStream();
        stream.setData(audit);
        asyncResultHandler.handle(succeededFuture(PostAuditDataResponse.respond201WithApplicationJson(stream,
          PostAuditDataResponse.headersFor201().withLocation(API_CXT + "/" + ret))));
      } else {
        ValidationHelper.handleError(reply.cause(), asyncResultHandler);
      }
    });
  }

  @Override
  @Validate
  public void getAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logger.debug("Get Audit record by " + id);

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        asyncResultHandler
          .handle(succeededFuture(GetAuditDataByIdResponse.respond200WithApplicationJson(res.result())));
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
            .handle(succeededFuture(GetAuditDataByIdResponse.respond404WithTextPlain(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
            .handle(succeededFuture(GetAuditDataByIdResponse.respond400WithTextPlain(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(GetAuditDataByIdResponse.respond500WithTextPlain(msg)));
        }
      }
    });
  }

  @Override
  @Validate
  public void putAuditDataById(String id, String lang, Audit audit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logger.warn("Update Audit record by " + id);

    if (audit.getId() == null) {
      audit.setId(id);
    }
    if (!id.equals(audit.getId())) {
      Errors err = ValidationHelper.createValidationErrorMessage("id", audit.getId(), "Can not change Id");
      asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.respond422WithApplicationJson(err)));
    }

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        getClient(okapiHeaders, vertxContext).update(DB_TAB_AUDIT, audit, id, reply -> {
          if (reply.succeeded()) {
            if (reply.result().rowCount() == 0) {
              asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse
                .respond500WithTextPlain(messages.getMessage(lang, MessageConsts.NoRecordsUpdated))));
            } else {
              asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.respond204()));
            }
          } else {
            ValidationHelper.handleError(reply.cause(), asyncResultHandler);
          }
        });
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
            .handle(succeededFuture(PutAuditDataByIdResponse.respond404WithTextPlain(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
            .handle(succeededFuture(PutAuditDataByIdResponse.respond400WithTextPlain(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.respond500WithTextPlain(msg)));
        }
      }
    });

  }

  @Override
  @Validate
  public void deleteAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logger.warn("Delte Audit record " + id);

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        getClient(okapiHeaders, vertxContext).delete(DB_TAB_AUDIT, id, reply -> {
          if (reply.succeeded()) {
            if (reply.result().rowCount() == 1) {
              asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.respond204()));
            } else {
              logger.error(messages.getMessage(lang, MessageConsts.DeletedCountError, 1, reply.result().rowCount()));
              asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.respond404WithTextPlain(
                messages.getMessage(lang, MessageConsts.DeletedCountError, 1, reply.result().rowCount()))));
            }
          } else {
            ValidationHelper.handleError(reply.cause(), asyncResultHandler);
          }
        });
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
            .handle(succeededFuture(DeleteAuditDataByIdResponse.respond404WithTextPlain(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
            .handle(succeededFuture(DeleteAuditDataByIdResponse.respond400WithTextPlain(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.respond500WithTextPlain(msg)));
        }
      }
    });

  }

  private PostgresClient getClient(Map<String, String> okapiHeaders, Context vertxContext) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    return PostgresClient.getInstance(vertxContext.owner(), tenantId);
  }

  private void getOneAudit(String id, Map<String, String> okapiHeaders, Context vertxContext,
    Handler<ExtendedAsyncResult<Audit>> resp) {

    Criterion c = new Criterion(
      new Criteria().addField(DB_TAB_AUDIT_ID).setJSONB(false).setOperation("=").setVal("'" + id + "'"));

    getClient(okapiHeaders, vertxContext).get(DB_TAB_AUDIT, Audit.class, c, true, reply -> {
      if (reply.succeeded()) {
        List<Audit> audits = reply.result().getResults();
        if (audits.isEmpty()) {
          resp.handle(new Failure<>(NOT_FOUND, "Audit " + id + " not found"));
        } else {
          resp.handle(new Success<>(audits.get(0)));
        }
      } else {
        String error = PgExceptionUtil.badRequestMessage(reply.cause());
        if (error == null) {
          resp.handle(new Failure<>(INTERNAL, ""));
        } else {
          resp.handle(new Failure<>(USER, error));
        }
      }
    });
  }

}
