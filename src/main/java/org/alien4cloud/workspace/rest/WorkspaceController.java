package org.alien4cloud.workspace.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.workspace.model.CSARPromotionImpact;
import org.alien4cloud.workspace.model.CSARWorkspaceDTO;
import org.alien4cloud.workspace.model.PromotionDTO;
import org.alien4cloud.workspace.model.PromotionRequest;
import org.alien4cloud.workspace.model.PromotionStatus;
import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import com.google.common.collect.Maps;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/workspaces", "/rest/v1/workspaces", "/rest/latest/workspaces" })
@Api(description = "Operations on workspaces")
public class WorkspaceController {

    @Inject
    private WorkspaceService workspaceService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;
    @Resource(name = "workspace-dao")
    private IGenericSearchDAO workspaceDAO;
    @Resource
    private CsarService csarService;

    @ApiOperation(value = "Get workspaces that the current user has the right to upload to", authorizations = { @Authorization("COMPONENTS_BROWSER"),
            @Authorization("COMPONENTS_MANAGER"), @Authorization("ARCHITECT") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<List<Workspace>> getUserWorkspaces() {
        return RestResponseBuilder.<List<Workspace>> builder().data(workspaceService.getUserWorkspaces()).build();
    }

    @ApiOperation(value = "Search for csars with workspaces information", authorizations = { @Authorization("COMPONENTS_BROWSER"),
            @Authorization("COMPONENTS_MANAGER"), @Authorization("ARCHITECT") })
    @RequestMapping(value = "csars/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<FacetedSearchResult> searchCSARs(@RequestBody FilteredSearchRequest searchRequest) {
        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        List<String> userWorkspaces = workspaceService.getUserWorkspaces().stream().map(Workspace::getId).collect(Collectors.toList());
        filters.put("workspace", userWorkspaces.toArray(new String[userWorkspaces.size()]));
        FacetedSearchResult searchResult = dao.facetedSearch(Csar.class, searchRequest.getQuery(), filters, null, searchRequest.getFrom(),
                searchRequest.getSize());
/*        Object[] enrichedData = Arrays.stream(searchResult.getData())
                .map(csarRaw -> new CSARWorkspaceDTO((Csar) csarRaw, workspaceService.getPromotionTargets((Csar) csarRaw))).collect(Collectors.toList())
                .toArray();
        FacetedSearchResult enrichedSearchResult = new FacetedSearchResult(searchResult.getFrom(), searchResult.getTo(), searchResult.getQueryDuration(),
                searchResult.getTotalResults(), searchResult.getTypes(), enrichedData, searchResult.getFacets());*/
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    @RequestMapping(value = "csars/{csarId:.+?}/promotionTargets", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<CSARWorkspaceDTO> getPromotionTargets(@PathVariable String csarId) {
        Csar csar = csarService.getOrFail(csarId);
        CSARWorkspaceDTO dto = new CSARWorkspaceDTO(csar, workspaceService.getPromotionTargets(csar));
        return RestResponseBuilder.<CSARWorkspaceDTO> builder().data(dto).build();
    }

    @ApiOperation(value = "Calculate the impact of the promotion", authorizations = { @Authorization("COMPONENTS_BROWSER"),
            @Authorization("COMPONENTS_MANAGER"), @Authorization("ARCHITECT") })
    @RequestMapping(value = "promotion-impact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<CSARPromotionImpact> getPromotionImpact(@RequestParam String csarName, @RequestParam String csarVersion,
            @RequestParam String targetWorkspace) {
        return RestResponseBuilder.<CSARPromotionImpact> builder().data(workspaceService.getCSARPromotionImpact(csarName, csarVersion, targetWorkspace))
                .build();
    }

    @ApiOperation(value = "Perform or accept the promotion", authorizations = { @Authorization("COMPONENTS_BROWSER"), @Authorization("COMPONENTS_MANAGER"),
            @Authorization("ARCHITECT") })
    @RequestMapping(value = "promotions", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<PromotionRequest> performPromotion(@RequestBody PromotionRequest promotionRequest) {
        PromotionStatus status = promotionRequest.getStatus();
        if (status == null) {
            status = PromotionStatus.INIT;
            promotionRequest.setStatus(status);
        }
        switch (status) {
        case REFUSED:
            return RestResponseBuilder.<PromotionRequest> builder().data(workspaceService.refuseCSARPromotion(promotionRequest)).build();
        case INIT:
            return RestResponseBuilder.<PromotionRequest> builder().data(workspaceService.promoteCSAR(promotionRequest)).build();
        case ACCEPTED:
            return RestResponseBuilder.<PromotionRequest> builder().data(workspaceService.acceptCSARPromotion(promotionRequest)).build();
        default:
            throw new InvalidArgumentException("Unrecognized status for promotion request [" + promotionRequest.getStatus() + "]");
        }
    }

    @ApiOperation(value = "Search for promotion", authorizations = { @Authorization("COMPONENTS_BROWSER"), @Authorization("COMPONENTS_MANAGER"),
            @Authorization("ARCHITECT") })
    @RequestMapping(value = "promotions/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_BROWSER', 'COMPONENTS_MANAGER', 'ARCHITECT')")
    public RestResponse<FacetedSearchResult> listPromotion(@RequestBody FilteredSearchRequest searchRequest) {
        Map<String, String[]> filters = searchRequest.getFilters();
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        List<String> userWorkspaces = workspaceService.getUserWorkspaces().stream().map(Workspace::getId).collect(Collectors.toList());
        filters.put("targetWorkspace", userWorkspaces.toArray(new String[userWorkspaces.size()]));
        FacetedSearchResult searchResult = workspaceDAO.facetedSearch(PromotionRequest.class, searchRequest.getQuery(), filters, null, null,
                searchRequest.getFrom(), searchRequest.getSize(), "requestDate", "date", true);
        Object[] enrichedData = Arrays.stream(searchResult.getData()).map(promotionRequestRaw -> {
            PromotionRequest promotionRequest = (PromotionRequest) promotionRequestRaw;
            return new PromotionDTO(promotionRequest, workspaceService.hasAcceptPromotionPrivilege(promotionRequest));
        }).collect(Collectors.toList()).toArray();
        FacetedSearchResult enrichedSearchResult = new FacetedSearchResult(searchResult.getFrom(), searchResult.getTo(), searchResult.getQueryDuration(),
                searchResult.getTotalResults(), searchResult.getTypes(), enrichedData, searchResult.getFacets());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(enrichedSearchResult).build();
    }

}
