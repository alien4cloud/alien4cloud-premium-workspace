package org.alien4cloud.workspace.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.workspace.model.CSARWorkspaceDTO;
import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.elasticsearch.common.collect.Maps;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.component.SearchRequest;
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
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @ApiOperation(value = "Get workspaces that the current user has the right to upload to", authorizations = { @Authorization("COMPONENTS_BROWSER"),
            @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "upload", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER')")
    public RestResponse<List<Workspace>> getAuthorizedWorkspacesForUpload() {
        return RestResponseBuilder.<List<Workspace>> builder().data(workspaceService.getAuthorizedWorkspacesForUpload()).build();
    }

    @ApiOperation(value = "Get workspaces that the current user has the right to read csars / components from", authorizations = {
            @Authorization("COMPONENTS_BROWSER"), @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER')")
    public RestResponse<List<Workspace>> getAuthorizedWorkspacesForSearch() {
        return RestResponseBuilder.<List<Workspace>> builder().data(workspaceService.getAuthorizedWorkspacesForSearch()).build();
    }

    @ApiOperation(value = "Search for csars with workspaces information", authorizations = { @Authorization("COMPONENTS_BROWSER"),
            @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(value = "csars", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER')")
    public RestResponse<FacetedSearchResult> searchCSARs(@RequestBody SearchRequest searchRequest) {
        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        FacetedSearchResult searchResult = dao.facetedSearch(Csar.class, searchRequest.getQuery(), filters, null, searchRequest.getFrom(),
                searchRequest.getSize());
        Object[] enrichedData = Arrays.stream(searchResult.getData())
                .map(csarRaw -> new CSARWorkspaceDTO((Csar) csarRaw, workspaceService.getPromotionTargets((Csar) csarRaw))).collect(Collectors.toList())
                .toArray();
        FacetedSearchResult enrichedSearchResult = new FacetedSearchResult(searchResult.getFrom(), searchResult.getTo(), searchResult.getQueryDuration(),
                searchResult.getTotalResults(), searchResult.getTypes(), enrichedData, searchResult.getFacets());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(enrichedSearchResult).build();
    }
}
