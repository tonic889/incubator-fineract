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
package org.apache.fineract.accounting.financialactivityaccount.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.util.List;
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
import org.apache.fineract.accounting.financialactivityaccount.data.FinancialActivityAccountData;
import org.apache.fineract.accounting.financialactivityaccount.service.FinancialActivityAccountReadPlatformService;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/financialactivityaccounts")
@Component
@Scope("singleton")
@Api(tags = {"Mapping Financial Activities to Accounts"})
@SwaggerDefinition(tags = {
        @Tag(name = "Mapping Financial Activities to Accounts", description = "Organization Level Financial Activities like Asset and Liability Transfer can be mapped to GL Account. Integrated accounting takes these accounts into consideration when an Account transfer is made between a savings to loan/savings account and vice-versa\n" + "\n" + "Field Descriptions\n" + "financialActivityId\n" + "The identifier of the Financial Activity\n" + "glAccountId\n" + "The identifier of a GL Account ( Ledger Account) which shall be used as the default account for the selected Financial Activity")
})
public class FinancialActivityAccountsApiResource {

    private final FinancialActivityAccountReadPlatformService financialActivityAccountReadPlatformService;
    private final DefaultToApiJsonSerializer<FinancialActivityAccountData> apiJsonSerializerService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public FinancialActivityAccountsApiResource(final PlatformSecurityContext context,
            final FinancialActivityAccountReadPlatformService officeToGLAccountMappingReadPlatformService,
            final DefaultToApiJsonSerializer<FinancialActivityAccountData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiJsonSerializerService = toApiJsonSerializer;
        this.financialActivityAccountReadPlatformService = officeToGLAccountMappingReadPlatformService;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveTemplate(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(FinancialActivityAccountsConstants.resourceNameForPermission);

        FinancialActivityAccountData financialActivityAccountData = this.financialActivityAccountReadPlatformService
                .getFinancialActivityAccountTemplate();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.apiJsonSerializerService.serialize(settings, financialActivityAccountData,
                FinancialActivityAccountsConstants.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "List Financial Activities to Accounts Mappings", notes = "Example Requests:\n" + "\n" + "financialactivityaccounts")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = FinancialActivityAccountsApiResourceSwagger.GetFinancialActivityAccountsResponse.class, responseContainer = "list" )})
    public String retrieveAll(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(FinancialActivityAccountsConstants.resourceNameForPermission);
        final List<FinancialActivityAccountData> financialActivityAccounts = this.financialActivityAccountReadPlatformService.retrieveAll();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.apiJsonSerializerService.serialize(settings, financialActivityAccounts,
                FinancialActivityAccountsConstants.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{mappingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Retrieve a Financial Activity to Account Mapping\n", notes = "Example Requests:\n" + "\n" + "financialactivityaccounts/1")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = FinancialActivityAccountsApiResourceSwagger.GetFinancialActivityAccountsResponse.class)})
    public String retreive(@PathParam("mappingId") @ApiParam(value = "mappingId") final Long mappingId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(FinancialActivityAccountsConstants.resourceNameForPermission);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        FinancialActivityAccountData financialActivityAccountData = this.financialActivityAccountReadPlatformService.retrieve(mappingId);
        if (settings.isTemplate()) {
            financialActivityAccountData = this.financialActivityAccountReadPlatformService
                    .addTemplateDetails(financialActivityAccountData);
        }

        return this.apiJsonSerializerService.serialize(settings, financialActivityAccountData,
                FinancialActivityAccountsConstants.RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Create a new Financial Activity to Accounts Mapping", notes = "Mandatory Fields\n" + "financialActivityId, glAccountId")
    @ApiImplicitParams({@ApiImplicitParam(value = "Request body", paramType = "body", dataType = "body", dataTypeClass = FinancialActivityAccountsApiResourceSwagger.PostFinancialActivityAccountsRequest.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "", response = FinancialActivityAccountsApiResourceSwagger.PostFinancialActivityAccountsResponse.class)})
    public String createGLAccount(@ApiParam(hidden = true) final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createOfficeToGLAccountMapping().withJson(jsonRequestBody)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.apiJsonSerializerService.serialize(result);
    }

    @PUT
    @Path("{mappingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Update a Financial Activity to Account Mapping", notes = "the API updates the Ledger account linked to a Financial Activity \n")
    @ApiImplicitParams({@ApiImplicitParam(value = "Request body", dataType = "body", paramType = "body", dataTypeClass = FinancialActivityAccountsApiResourceSwagger.PostFinancialActivityAccountsRequest.class)})
    @ApiResponses({@ApiResponse(code = 200, message = "", response = FinancialActivityAccountsApiResourceSwagger.PutFinancialActivityAccountsResponse.class)})
    public String updateGLAccount(@PathParam("mappingId") @ApiParam(value = "mappingId") final Long mappingId, @ApiParam(hidden = true) final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateOfficeToGLAccountMapping(mappingId)
                .withJson(jsonRequestBody).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.apiJsonSerializerService.serialize(result);
    }

    @DELETE
    @Path("{mappingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Delete a Financial Activity to Account Mapping")
    @ApiResponses({@ApiResponse(code = 200, message = "OK", response = FinancialActivityAccountsApiResourceSwagger.DeleteFinancialActivityAccountsResponse.class)})
    public String deleteGLAccount(@PathParam("mappingId") @ApiParam(value = "mappingId") final Long mappingId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteOfficeToGLAccountMapping(mappingId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.apiJsonSerializerService.serialize(result);
    }
}
