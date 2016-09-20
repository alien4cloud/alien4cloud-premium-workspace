package org.alien4cloud.workspace.rest;

import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/workspaces", "/rest/v1/workspaces", "/rest/latest/workspaces" })
@Api(value = "", description = "Operations on workspaces")
public class WorkspaceController {

    @Inject
    private WorkspaceService workspaceService;

    @ApiOperation(value = "Search for repositories", authorizations = { @Authorization("COMPONENTS_BROWSER"), @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "upload", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER')")
    public RestResponse<List<Workspace>> getAuthorizedWorkspacesForUpload() {
        return RestResponseBuilder.<List<Workspace>> builder().data(workspaceService.getAuthorizedWorkspacesForUpload()).build();
    }

    @ApiOperation(value = "Search for repositories", authorizations = { @Authorization("COMPONENTS_BROWSER"), @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER')")
    public RestResponse<List<Workspace>> getAuthorizedWorkspacesForSearch() {
        return RestResponseBuilder.<List<Workspace>> builder().data(workspaceService.getAuthorizedWorkspacesForSearch()).build();
    }
}
