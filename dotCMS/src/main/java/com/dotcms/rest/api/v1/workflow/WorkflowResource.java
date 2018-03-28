package com.dotcms.rest.api.v1.workflow;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;

import static com.dotcms.exception.ExceptionUtil.*;
import static com.dotcms.rest.ResponseEntityView.OK;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.workflow.form.*;
import com.dotcms.workflow.helper.WorkflowHelper;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.NotAllowedUserWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.Beta;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import static com.dotcms.util.CollectionsUtils.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("serial")
@Beta /* Non Official released */
@Path("/v1/workflow")
public class WorkflowResource {


    private final WorkflowHelper   workflowHelper;
    private final ContentHelper    contentHelper;
    private final WebResource      webResource;
    private final WorkflowAPI      workflowAPI;
    private final ResponseUtil     responseUtil;
    private final ContentletAPI    contentletAPI;
    private final PermissionAPI    permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;



    /**
     * Default constructor.
     */
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(),
                ContentHelper.getInstance(),
                APILocator.getWorkflowAPI(),
                APILocator.getContentletAPI(),
                ResponseUtil.INSTANCE,
                APILocator.getPermissionAPI(),
                WorkflowImportExportUtil.getInstance(),
                new WebResource());
    }

    @VisibleForTesting
        WorkflowResource(final WorkflowHelper workflowHelper,
                               final ContentHelper    contentHelper,
                               final WorkflowAPI      workflowAPI,
                               final ContentletAPI    contentletAPI,
                               final ResponseUtil     responseUtil,
                               final PermissionAPI    permissionAPI,
                               final WorkflowImportExportUtil workflowImportExportUtil,
                               final WebResource webResource) {

        this.workflowHelper           = workflowHelper;
        this.contentHelper            = contentHelper;
        this.webResource              = webResource;
        this.responseUtil             = responseUtil;
        this.workflowAPI              = workflowAPI;
        this.permissionAPI            = permissionAPI;
        this.contentletAPI            = contentletAPI;
        this.workflowImportExportUtil = workflowImportExportUtil;

    }

    private Response createUnAuthorizedResponse (final Exception e) {

        SecurityLogger.logInfo(this.getClass(), e.getMessage());
        return ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
    }

    private Response mapExceptionResponse(final Exception e){

        if(causedBy(e, SECURITY_EXCEPTIONS)){
            return this.createUnAuthorizedResponse(e);
        }

        if(causedBy(e, NOT_FOUND_EXCEPTIONS)){
            return ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        }

        if(causedBy(e, IllegalArgumentException.class)){
            return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        }

        return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Returns all schemes non-archived associated to a content type. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param contentTypeId String content type id to get the schemes associated to it.
     * @return Response
     */
    @GET
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSchemes(@Context final HttpServletRequest request,
                                      @QueryParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId);
            List<WorkflowScheme> schemes = (null != contentTypeId) ?
                    this.workflowHelper.findSchemesByContentType
                            (contentTypeId, initDataObject.getUser()) :
                    this.workflowHelper.findSchemes();

            return Response.ok(new ResponseEntityView(schemes)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findSchemes exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findSchemes.

    /**
     * Returns all schemes for the content type and include schemes non-archive . 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/schemes/schemescontenttypes/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAllSchemesAndSchemesByContentType(@Context            final HttpServletRequest request,
                                                                @PathParam("contentTypeId")      final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        try {

            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId
                            + " and including All Schemes");
            final List<WorkflowScheme> schemes = this.workflowHelper.findSchemes();
            final List<WorkflowScheme> contentTypeSchemes = this.workflowHelper.findSchemesByContentType
                    (contentTypeId, initDataObject.getUser());

            return Response.ok(new ResponseEntityView(new SchemesAndSchemesContentTypeView(schemes, contentTypeSchemes))).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),"Exception on findAllSchemesAndSchemesByContentType exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);

        }
    } // findAllSchemesAndSchemesByContentType.

    /**
     * Return Steps associated to the scheme, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findStepsByScheme(@Context final HttpServletRequest request,
                                            @PathParam("schemeId") final String schemeId) {

        this.webResource.init
                (null, true, request, true, null);

        try {
            Logger.debug(this, "Getting the workflow steps for the scheme: " + schemeId);
            final List<WorkflowStep> steps = this.workflowHelper.findSteps(schemeId);
            return Response.ok(new ResponseEntityView(steps)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findAllSchemesAndSchemesByContentType exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);

        }
    } // findSteps.

    /**
     * Finds the available actions for an inode
     * @param request HttpServletRequest
     * @param inode String
     * @return Response
     */
    @GET
    @Path("/contentlet/{inode}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableActions(@Context final HttpServletRequest request,
                                               @PathParam("inode") final String inode) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, "Getting the available actions for the contentlet inode: " + inode);
            final List<WorkflowAction> actions = this.workflowHelper.findAvailableActions(inode, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findStepsByScheme, contentlet inode: " + inode +
                            ", exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findAvailableActions.

    /**
     * Returns a single action, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAction(@Context final HttpServletRequest request,
                                     @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, "Finding the workflow action " + actionId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (Exception e) {
            return mapExceptionResponse(e);
        }

    } // findAction.

    /**
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionByStep(@Context final HttpServletRequest request,
                                           @PathParam("stepId")   final String stepId,
                                           @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, "Getting the workflow action " + actionId + " for the step: " + stepId);
            WorkflowAction action = this.workflowAPI.findAction(actionId, stepId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (Exception e) {
            return mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a collection of actions associated to the step, if step does not have any will returns 200 and an empty list.
     * 401 if the user does not have permission.
     * 404 if the stepId does not exists.
     * @param request  HttpServletRequest
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByStep(@Context final HttpServletRequest request,
                                            @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        final User user = initDataObject.getUser();
        try {
            Logger.debug(this, "Getting the workflow actions for the step: " + stepId);
            final List<WorkflowAction> actions = this.workflowHelper.findActions(stepId, user);
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a set of actions associated to the schemeId
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByScheme(@Context final HttpServletRequest request,
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            final List<WorkflowAction> actions = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findActionsByScheme.

    /**
     * Saves an action, by default the action is associated to the schema, however if the stepId is set will be automatically associated to the step too.
     * @param request               HttpServletRequest
     * @param workflowActionForm    WorkflowActionForm
     * @return Response
     */
    @POST
    @Path("/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response save(@Context final HttpServletRequest request,
                               final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        WorkflowAction newAction;

        try {

            Logger.debug(this, "Saving new workflow action: " + workflowActionForm.getActionName());
            newAction = this.workflowHelper.save(workflowActionForm, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(newAction)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        }  catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // save

    @PUT
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateAction(@Context final HttpServletRequest request,
                                       @PathParam("actionId") final String actionId,
                                       final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Response response;
        try {
            Logger.debug(this, "Updating action with id: " + actionId);
            final WorkflowAction workflowAction = this.workflowHelper.save( actionId, workflowActionForm, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(workflowAction)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on updateAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {
            Logger.error(this.getClass(),
                    "DoesNotExistException on updateAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on updateAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // deleteAction

    /**
     * Saves an action into a step
     * @param request                   HttpServletRequest
     * @param workflowActionStepForm    WorkflowActionStepForm
     * @return Response
     */
    @POST
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveActionToStep(@Context final HttpServletRequest request,
                                           @PathParam("stepId")   final String stepId,
                                           final WorkflowActionStepForm workflowActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + stepId);
            this.workflowHelper.saveActionToStep(new WorkflowActionStepBean.Builder().stepId(stepId)
                    .actionId(workflowActionStepForm.getActionId()).build(), initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (DotSecurityException | NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "Exception on saveActionToStep, workflowActionForm: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on saveActionToStep, workflowActionForm: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // saveAction

    /**
     * Deletes a step
     * @param request                   HttpServletRequest
     * @param stepId                   String
     * @return Response
     */
    @DELETE
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteStep(@Context final HttpServletRequest request,
                                     @PathParam("stepId") final String stepId) {

        this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Deleting the step: " + stepId);
            this.workflowHelper.deleteStep(stepId);
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // deleteStep

    /**
     * Deletes an action associated to the step
     * @param request                   HttpServletRequest
     * @param stepId                   String
     * @return Response
     */
    @DELETE
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @PathParam("actionId") final String actionId,
                                       @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Deleting the action: " + actionId + " for the step: " + stepId);
            this.workflowHelper.deleteAction(actionId, stepId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on deleteAction, action: " + actionId
                            + ", stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteAction, action: " + actionId
                            + ", stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId
                            + " stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // deleteAction

    /**
     * Deletes an action associated to the scheme and all references into steps
     * @param request                   HttpServletRequest
     * @param actionId                  String
     * @return Response
     */
    @DELETE
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Deleting the action: " + actionId);
            this.workflowHelper.deleteAction(actionId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // deleteAction

    /**
     * Change the order of the steps in a scheme
     * @param request                           HttpServletRequest
     * @param stepId                            String stepid to reorder
     * @param order                             int    order for the step
     * @return Response
     */
    @PUT
    @Path("/reorder/step/{stepId}/order/{order}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response reorderStep(@Context final HttpServletRequest request,
                                        @PathParam("stepId")   final String stepId, 
                                        @PathParam("order")    final int order) {
        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Doing reordering of step: " + stepId + ", order: " + order);
            this.workflowHelper.reorderStep(stepId, order, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on reorderStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on reorderStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on reorderStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    } // reorderStep


    @PUT
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateStep(@Context final HttpServletRequest request,
                                     @NotNull @PathParam("stepId") final String stepId,
                                     final WorkflowStepUpdateForm stepForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Logger.debug(this, "updating step for scheme with stepId: " + stepId);
        Response response;
        try {
            final WorkflowStep step = this.workflowHelper.updateStep(stepId, stepForm, initDataObject.getUser());
            response = Response.ok(new ResponseEntityView(step)).build();
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on updateStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {
            Logger.error(this.getClass(),
                    "DoesNotExistException on updateStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on updateStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException) ?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    } // reorderStep

    @POST
    @Path("/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addStep(@Context final HttpServletRequest request,
                                  final WorkflowStepAddForm newStepForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final String schemeId = newStepForm.getSchemeId();
        Logger.debug(this, "updating step for scheme with schemeId: " + schemeId);
        Response response;
        try {
            final WorkflowStep step = this.workflowHelper.addStep(newStepForm, initDataObject.getUser());
            response = Response.ok(new ResponseEntityView(step)).build();
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on addStep, stepId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {
            Logger.error(this.getClass(),
                    "DoesNotExistException on addStep, stepId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on addStep, stepId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException) ?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


    @GET
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findStepById(@Context final HttpServletRequest request,
                                       @NotNull @PathParam("stepId") final String stepId) {
        this.webResource.init(null, true, request, true, null);
        Logger.debug(this, "finding step by id stepId: " + stepId);
        try {
            final WorkflowStep step = this.workflowHelper.findStepById(stepId);
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (Exception e) {
            return mapExceptionResponse(e);
        }
    }


    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response fire(@Context final HttpServletRequest request,
                               @QueryParam("inode")            final String inode,
                               @NotNull @PathParam("actionId") final String actionId,
                               final FireActionForm fireActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Firing workflow action: " + actionId +
                            ", inode: " + inode);

            final Contentlet contentlet = (UtilMethods.isSet(inode))?
                        this.contentletAPI.find(inode, initDataObject.getUser(), false):
                        this.populateContentlet(fireActionForm, initDataObject.getUser());

            if (null != contentlet && null != fireActionForm) {
                contentlet.setStringProperty("wfPublishDate", fireActionForm.getPublishDate());
                contentlet.setStringProperty("wfPublishTime", fireActionForm.getPublishTime());
                contentlet.setStringProperty("wfExpireDate", fireActionForm.getExpireDate());
                contentlet.setStringProperty("wfExpireTime", fireActionForm.getExpireTime());
                contentlet.setStringProperty("wfNeverExpire", fireActionForm.getNeverExpire());
                contentlet.setStringProperty("whereToSend", fireActionForm.getWhereToSend());
                contentlet.setStringProperty("forcePush", fireActionForm.getForcePush());
            }

            response = (null == contentlet || contentlet.getMap().isEmpty())?
                        ExceptionMapperUtil.createResponse
                                (null, LanguageUtil.get("contentlet-was-not-found"), Response.Status.NOT_FOUND):

                        Response.ok(new ResponseEntityView(
                                this.workflowAPI.fireContentWorkflow(contentlet,
                                    new ContentletDependencies.Builder()
                                        .respectAnonymousPermissions(PageMode.get(request).respectAnonPerms)
                                        .workflowActionId(actionId)
                                        .workflowActionComments((null != fireActionForm)?fireActionForm.getComments():null)
                                        .workflowAssignKey((null != fireActionForm)?fireActionForm.getAssign():null)
                                        .modUser(initDataObject.getUser()).build())
                                )
                        ).build(); // 200
        } catch (DotSecurityException | ForbiddenException e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);
            SecurityLogger.logInfo(this.getClass(), e.getMessage());
            response =
                    ExceptionMapperUtil.createResponse(e, Response.Status.FORBIDDEN);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // fire.

    private Contentlet populateContentlet(final FireActionForm fireActionForm, final User user) throws DotSecurityException {

        final Contentlet contentlet = this.contentHelper.populateContentletFromMap
                (new Contentlet(), fireActionForm.getContentletFormData());

        final Supplier<String> errorMessageSupplier = () -> {

            String message = "no-permissions-contenttype";

            try {
                message =LanguageUtil.get(user.getLocale(),
                        message, user.getUserId(), contentlet.getContentType().id());
            } catch (LanguageException e) {
                throw new ForbiddenException(message);
            }

            return message;
        };

        try {
            if (!this.permissionAPI.doesUserHavePermission(contentlet.getContentType(), PermissionAPI.PERMISSION_READ, user, false)) {
                throw new DotSecurityException(errorMessageSupplier.get());
            }
        } catch (DotDataException e) {
            throw new DotSecurityException(errorMessageSupplier.get(), e);
        }

        return contentlet;
    }

    /**
     * Change the order of an action associated to the step
     * @param request                           HttpServletRequest
     * @param workflowReorderActionStepForm     WorkflowReorderBean
     * @return Response
     */
    @PUT
    @Path("/reorder/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response reorderAction(@Context final HttpServletRequest request,
                                        @PathParam("stepId")   final String stepId,
                                        @PathParam("actionId") final String actionId,
                                        final WorkflowReorderWorkflowActionStepForm workflowReorderActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Doing reordering of: " + workflowReorderActionStepForm);
            this.workflowHelper.reorderAction(
                    new WorkflowReorderBean.Builder().stepId(stepId).actionId(actionId)
                            .order(workflowReorderActionStepForm.getOrder()).build(),
                    initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (NotAllowedUserWorkflowException e) {
            Logger.error(this.getClass(),
                    "NotAllowedUserWorkflowException on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // reorderAction

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions)
     * in addition the permission (who can use) will be also returned.
     * @param request  HttpServletRequest
     * @return Response
     */
    @POST
    @Path("/schemes/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response importScheme(@Context final HttpServletRequest request,
                                       final WorkflowSchemeImportExportObjectForm workflowSchemeImportExportObjectForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        Locale                           locale;

        try {

            Logger.debug(this, "Importing the workflow schemes");
            this.workflowHelper.importScheme (workflowSchemeImportExportObjectForm.getWorkflowExportObject(),
                            workflowSchemeImportExportObjectForm.getPermissions(),
                            initDataObject.getUser());
            response     = Response.ok(new ResponseEntityView("OK")).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "Exception on importScheme, Error importing schemes, some objects could not exists", e);
            locale   = LocaleUtil.getLocale(request);
            response = this.responseUtil.getErrorResponse(request, Response.Status.NOT_FOUND,
                    locale, initDataObject.getUser().getUserId(), "Workflow-import-fail");
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on importScheme, schemeId, exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // importScheme.

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions)
     * in addition the permission (who can use) will be also returned.
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/export")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response exportScheme(@Context final HttpServletRequest request,
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        WorkflowSchemeImportExportObject exportObject;
        List<Permission>                 permissions;
        WorkflowScheme                   scheme;
        Locale                           locale;

        try {

            Logger.debug(this, "Exporting the workflow scheme: " + schemeId);
            scheme       = this.workflowAPI.findScheme(schemeId);
            exportObject = this.workflowImportExportUtil.buildExportObject(Arrays.asList(scheme));
            permissions  = this.workflowHelper.getActionsPermissions(exportObject.getActions());
            response     = Response.ok(new ResponseEntityView(
                    map("workflowImportObject",exportObject,
                            "permissions", permissions))).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "The Scheme does not exist, id: " + schemeId, e);
            locale   = LocaleUtil.getLocale(request);
            response = this.responseUtil.getErrorResponse(request, Response.Status.NOT_FOUND,
                    locale, initDataObject.getUser().getUserId(), "Workflow-does-not-exists-scheme-id", schemeId);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // exportscheme.

    /**
     * Returns all the possible default actions associated to the content type workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableDefaultActionsByContentType(@Context final HttpServletRequest request,
            @PathParam("contentTypeId")      final String contentTypeId) {
        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    "Getting the available workflow schemes default action for the ContentType: "
                            + contentTypeId );
            final List<WorkflowDefaultActionView> actions = this.workflowHelper.findAvailableDefaultActionsByContentType(contentTypeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on find Available Default Actions exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }

    } // findAvailableDefaultActionsByContentType.

    /**
     * Returns all the possible default actions associated to the workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableDefaultActionsBySchemes(
            @Context final HttpServletRequest request,
            @QueryParam("ids") final String schemeIds) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    "Getting the available workflow schemes default action for the schemes: "
                            + schemeIds);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findAvailableDefaultActionsBySchemes(schemeIds, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on find Available Default Actions exception message: " + e
                            .getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findAvailableDefaultActionsBySchemes.

    /**
     * Finds the available actions of the initial/first step(s) of the workflow scheme(s) associated
     * with a content type Id.
     * @param request HttpServletRequest
     * @param contentTypeId String
     * @return Response
     */
    @GET
    @Path("/initialactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findInitialAvailableActionsByContentType(
            @Context final HttpServletRequest request,
            @PathParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    "Getting the available actions for the contentlet inode: " + contentTypeId);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findInitialAvailableActionsByContentType(contentTypeId,
                            initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findInitialAvailableActionsByContentType, content type id: "
                            + contentTypeId +
                            ", exception message: " + e.getMessage(), e);
            return mapExceptionResponse(e);
        }
    } // findInitialAvailableActionsByContentType.

    /**
     * Creates a new scheme
     *
     * @param request
     * @param workflowSchemeForm
     * @return
     */
    @POST
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response save(@Context final HttpServletRequest request,
                               final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Logger.debug(this, "Saving scheme named: " + workflowSchemeForm.getSchemeName());
        Response response = null;
        try {
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(null, workflowSchemeForm, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(scheme)).build(); // 200
        } catch ( AlreadyExistException e) {
            response = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception on save, schema named: " + workflowSchemeForm.getSchemeName() + ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


    /**
     * Updates an existing scheme
     *
     * @param request
     * @param workflowSchemeForm
     * @return
     */
    @PUT
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response update(@Context final HttpServletRequest request,
                                 @PathParam("schemeId") final String schemeId,
                                 final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Logger.debug(this, "Updating scheme with id: " + schemeId);
        Response response = null;
        try {
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(schemeId, workflowSchemeForm, initDataObject.getUser());
            response = Response.ok(new ResponseEntityView(scheme)).build(); // 200
        } catch ( DoesNotExistException e) {
            Logger.error(this.getClass(), "Exception attempting to update a nonexistent schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to update schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * Delete an existing scheme
     *
     * @param request
     * @return
     */
    @DELETE
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response delete(@Context final HttpServletRequest request,
            @PathParam("schemeId") final String schemeId) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Logger.debug(this, "Deleting scheme with id: " + schemeId);
        Response response = null;
        try {
            this.workflowHelper.delete(schemeId, initDataObject.getUser());
            response = Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch ( DoesNotExistException e) {
            Logger.error(this.getClass(), "Exception attempting to delete a nonexistent schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to delete schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    this.createUnAuthorizedResponse(e) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

} // E:O:F:WorkflowResource.