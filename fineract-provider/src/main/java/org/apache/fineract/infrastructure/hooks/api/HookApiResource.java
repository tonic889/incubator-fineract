/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.hooks.api;

import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.HOOK_RESOURCE_NAME;
import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.RESPONSE_DATA_PARAMETERS;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.hooks.data.HookData;
import org.apache.fineract.infrastructure.hooks.service.HookReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/hooks")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Component
@Scope("singleton")
@Api(tags = {"Hooks"})
@SwaggerDefinition(tags = {
  @Tag(name = "Hooks", description = "Hooks are a mechanism to trigger custom code on the occurence of events. ")
})
public class HookApiResource {

    private final PlatformSecurityContext context;
    private final HookReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<HookData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public HookApiResource(
            final PlatformSecurityContext context,
            final HookReadPlatformService readPlatformService,
            final DefaultToApiJsonSerializer<HookData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @ApiOperation(value = "Retrieve Hooks", notes = "Returns the list of hooks.\n" + "\n" + "Example Requests:\n" + "\n" + "hooks", responseContainer = "List", response = HookApiResourceSwagger.GetHookResponse.class)
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.GetHookResponse.class, responseContainer = "list")})
    public String retrieveHooks(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(
                HOOK_RESOURCE_NAME);

        final Collection<HookData> hooks = this.readPlatformService
                .retrieveAllHooks();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
                .process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, hooks,
                RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{hookId}")
    @ApiOperation(value = "Retrieve a Hook", notes = "Returns the details of a Hook.\n" + "\n" + "Example Requests:\n" + "\n" + "hooks/1")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.GetHookResponse.class)})
    public String retrieveHook(@PathParam("hookId") @ApiParam(value = "hookId") final Long hookId,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(
                HOOK_RESOURCE_NAME);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
                .process(uriInfo.getQueryParameters());

        HookData hook = this.readPlatformService.retrieveHook(hookId);

        if (settings.isTemplate()) {
            final HookData hookData = this.readPlatformService
                    .retrieveNewHookDetails(hook.getTemplateName());
            hook = HookData.templateExisting(hook, hookData.getTemplates(),
                    hookData.getGroupings());
        }
        return this.toApiJsonSerializer.serialize(settings, hook,
                RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @ApiOperation(value = "Retrieve Hooks Template", notes = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n" + "\n" + "Field Defaults\n" + "Allowed Value Lists\n" + "Example Request:\n" + "\n" + "hooks/template")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.GetHookTemplateResponse.class)})
    public String template(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(
                HOOK_RESOURCE_NAME);

        final HookData hook = this.readPlatformService
                .retrieveNewHookDetails(null);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper
                .process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, hook,
                RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @ApiOperation(value = "Create a Hook", notes = "The following parameters can be passed for the creation of a hook :-\n" + "\n" + "name - string - Required. The name of the template that is being called. (See /hooks/template for the list of valid hook names.)\n" + "\n" + "isActive - boolean - Determines whether the hook is actually triggered.\n" + "\n" + "events - array - Determines what events the hook is triggered for.\n" + "\n" + "config - hash - Required. Key/value pairs to provide settings for this hook. These settings vary between the templates.\n" + "\n" + "templateId - Optional. The UGD template ID associated with the same entity (client or loan).")
    @ApiImplicitParams({@ApiImplicitParam(paramType = "body", dataType = "body", required = true, type = "body", dataTypeClass = HookApiResourceSwagger.PostHookRequest.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.PostHookResponse.class)})
    public String createHook(@ApiParam(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createHook().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService
                .logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{hookId}")
    @ApiOperation(value = "Update a Hook", notes = "Updates the details of a hook.")
    @ApiImplicitParams({@ApiImplicitParam(paramType = "body", dataType = "body", required = true, type = "body", dataTypeClass = HookApiResourceSwagger.PutHookRequest.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.PutHookResponse.class)})
    public String updateHook(@PathParam("hookId") @ApiParam(value = "hookId") final Long hookId,
            @ApiParam(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .updateHook(hookId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService
                .logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{hookId}")
    @ApiOperation(value = "Delete a Hook", notes = "Deletes a hook.")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = HookApiResourceSwagger.DeleteHookResponse.class)})
    public String deleteHook(@PathParam("hookId") @ApiParam(value = "hookId") final Long hookId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .deleteHook(hookId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService
                .logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

}
