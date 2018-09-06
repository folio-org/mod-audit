package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.okapi.common.ErrorType.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.folio.okapi.common.ExtendedAsyncResult;
import org.folio.okapi.common.Failure;
import org.folio.okapi.common.Success;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Audit;
import org.folio.rest.jaxrs.model.AuditCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.resource.AuditDataResource;
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
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;
import org.z3950.zing.cql.cql2pgjson.SchemaException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AuditDataResourceImpl implements AuditDataResource {

  public static final String API_CXT = "/audit-data";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Messages messages = Messages.getInstance();

  private final String DB_TAB_AUDIT = "audit_data";
  private final String DB_TAB_AUDIT_ID = "_id";
  private final String JSON_SCHEMA_AUDIT = "ramls/audit.json";
  private String SCHEMA_AUDIT = null;

  private void initCQLValidation() {
    try {
      SCHEMA_AUDIT = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(JSON_SCHEMA_AUDIT), "UTF-8");
    } catch (Exception e) {
      logger.error("unable to load schema - " + JSON_SCHEMA_AUDIT + ", validation of query fields will not be active",
          e);
    }
  }

  public AuditDataResourceImpl() {
    if (SCHEMA_AUDIT == null) {
      initCQLValidation();
    }
  }

  private CQLWrapper getCQL(String query, int limit, int offset, String schema)
      throws IOException, FieldException, SchemaException {
    CQL2PgJSON cql2pgJson = null;
    if (schema != null) {
      cql2pgJson = new CQL2PgJSON(DB_TAB_AUDIT + ".jsonb", schema);
    } else {
      cql2pgJson = new CQL2PgJSON(DB_TAB_AUDIT + ".jsonb");
    }
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

  @SuppressWarnings("unchecked")
  @Override
  @Validate
  public void getAuditData(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    logger.debug("Getting Audit data " + offset + "+" + limit + " q=" + query);

    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset, SCHEMA_AUDIT);
    } catch (Exception e) {
      ValidationHelper.handleError(e, asyncResultHandler);
      return;
    }

    getClient(okapiHeaders, vertxContext).get(DB_TAB_AUDIT, Audit.class, new String[] { "*" }, cql, true, false,
        reply -> {
          if (reply.succeeded()) {
            AuditCollection auditCollection = new AuditCollection();
            auditCollection.setAudit((List<Audit>) reply.result().getResults());
            Integer totalRecords = reply.result().getResultInfo().getTotalRecords();
            auditCollection.setTotalRecords(totalRecords);
            asyncResultHandler.handle(succeededFuture(GetAuditDataResponse.withJsonOK(auditCollection)));
          } else {
            ValidationHelper.handleError(reply.cause(), asyncResultHandler);
          }
        });
  }

  @Override
  @Validate
  public void postAuditData(String lang, Audit audit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    logger.debug("Save Audit record " + audit);

    String id = audit.getId();
    if (id == null || id.isEmpty()) {
      audit.setId(UUID.randomUUID().toString());
      logger.info("Added id to Audit record " + audit);
    }
    getClient(okapiHeaders, vertxContext).save(DB_TAB_AUDIT, id, audit, reply -> {
      if (reply.succeeded()) {
        Object ret = reply.result();
        audit.setId((String) ret);
        OutStream stream = new OutStream();
        stream.setData(audit);
        asyncResultHandler.handle(succeededFuture(PostAuditDataResponse.withJsonCreated(API_CXT + "/" + ret, stream)));
      } else {
        ValidationHelper.handleError(reply.cause(), asyncResultHandler);
      }
    });
  }

  @Override
  @Validate
  public void getAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    logger.debug("Get Audit record by " + id);

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        asyncResultHandler.handle(succeededFuture(GetAuditDataByIdResponse.withJsonOK(res.result())));
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
              .handle(succeededFuture(GetAuditDataByIdResponse.withPlainNotFound(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
              .handle(succeededFuture(GetAuditDataByIdResponse.withPlainBadRequest(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(GetAuditDataByIdResponse.withPlainInternalServerError(msg)));
        }
      }
    });
  }

  @Override
  @Validate
  public void putAuditDataById(String id, String lang, Audit audit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    logger.warn("Update Audit record by " + id);

    if (audit.getId() == null) {
      audit.setId(id);
    }
    if (!id.equals(audit.getId())) {
      Errors err = ValidationHelper.createValidationErrorMessage("id", audit.getId(), "Can not change Id");
      asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.withJsonUnprocessableEntity(err)));
    }

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        getClient(okapiHeaders, vertxContext).update(DB_TAB_AUDIT, audit, id, reply -> {
          if (reply.succeeded()) {
            if (reply.result().getUpdated() == 0) {
              asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse
                  .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.NoRecordsUpdated))));
            } else {
              asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.withNoContent()));
            }
          } else {
            ValidationHelper.handleError(reply.cause(), asyncResultHandler);
          }
        });
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
              .handle(succeededFuture(PutAuditDataByIdResponse.withPlainNotFound(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
              .handle(succeededFuture(PutAuditDataByIdResponse.withPlainBadRequest(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(PutAuditDataByIdResponse.withPlainInternalServerError(msg)));
        }
      }
    });

  }

  @Override
  @Validate
  public void deleteAuditDataById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    logger.warn("Delte Audit record " + id);

    getOneAudit(id, okapiHeaders, vertxContext, res -> {
      if (res.succeeded()) {
        getClient(okapiHeaders, vertxContext).delete(DB_TAB_AUDIT, id, reply -> {
          if (reply.succeeded()) {
            if (reply.result().getUpdated() == 1) {
              asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.withNoContent()));
            } else {
              logger.error(messages.getMessage(lang, MessageConsts.DeletedCountError, 1, reply.result().getUpdated()));
              asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.withPlainNotFound(
                  messages.getMessage(lang, MessageConsts.DeletedCountError, 1, reply.result().getUpdated()))));
            }
          } else {
            ValidationHelper.handleError(reply.cause(), asyncResultHandler);
          }
        });
      } else {
        switch (res.getType()) {
        case NOT_FOUND:
          asyncResultHandler
              .handle(succeededFuture(DeleteAuditDataByIdResponse.withPlainNotFound(res.cause().getMessage())));
          break;
        case USER:
          asyncResultHandler
              .handle(succeededFuture(DeleteAuditDataByIdResponse.withPlainBadRequest(res.cause().getMessage())));
          break;
        default:
          String msg = res.cause().getMessage();
          if (msg.isEmpty()) {
            msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          }
          asyncResultHandler.handle(succeededFuture(DeleteAuditDataByIdResponse.withPlainInternalServerError(msg)));
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
        new Criteria().addField(DB_TAB_AUDIT_ID).setJSONB(false).setOperation("=").setValue("'" + id + "'"));

    getClient(okapiHeaders, vertxContext).get(DB_TAB_AUDIT, Audit.class, c, true, reply -> {
      if (reply.succeeded()) {
        @SuppressWarnings("unchecked")
        List<Audit> audits = (List<Audit>) reply.result().getResults();
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
