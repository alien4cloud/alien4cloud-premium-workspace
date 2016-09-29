package org.alien4cloud.workspace.rest;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.Role;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.workspace.rest.model.CreateTopologyRequest;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Collections;

/**
 * Extension to the TopologyCatalogController to provide the ability to create topologies in a specific workspace.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/workspaces/topologies", "/rest/v1/workspaces/topologies", "/rest/latest/workspaces/topologies" })
@Api(description = "Topology catalog with workspace.")
public class WorkspaceTopologyCatalogController {
    @Inject
    private ITopologyCatalogService catalogService;
    @Inject
    private WorkspaceService workspaceService;

    /**
     * Create a topology and register it as a template in the catalog .
     *
     * @param createTopologyRequest The create topology template request.
     * @return A {@link RestResponse} that contains the Id of the newly created topology.
     */
    @ApiOperation(value = "Create a topology and register it in the catalog")
    @RequestMapping(value = "/template", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<String> createAsTemplate(@RequestBody @Valid CreateTopologyRequest createTopologyRequest) {
        // Security is based on the workspace from the request
        if (!workspaceService.hasRoles(createTopologyRequest.getWorkspace(), Collections.singleton(Role.ARCHITECT))) {
            throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                    + "> has no authorization to perform the requested operation on this workspace <" + createTopologyRequest.getWorkspace() + ">.");
        }
        Topology topology = catalogService.createTopologyAsTemplate(createTopologyRequest.getName(), createTopologyRequest.getDescription(),
                createTopologyRequest.getVersion(), createTopologyRequest.getWorkspace(), createTopologyRequest.getFromTopologyId());
        return RestResponseBuilder.<String> builder().data(topology.getId()).build();
    }
}
