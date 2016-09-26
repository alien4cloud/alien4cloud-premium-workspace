package org.alien4cloud.workspace.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.catalog.index.TopologyCatalogService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.workspace.model.CSARPromotionImpact;
import org.alien4cloud.workspace.model.PromotionRequest;
import org.alien4cloud.workspace.model.PromotionStatus;
import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.common.Usage;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.AlienUtils;

@Service
public class WorkspaceService {
    @Inject
    private TopologyCatalogService topologyCatalogService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private IToscaTypeSearchService typesCatalogService;
    @Inject
    private CsarService csarService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource(name = "workspace-dao")
    private IGenericSearchDAO workspaceDAO;

    private Workspace getGlobalWorkspace(Workspace globalWorkspace) {
        if (globalWorkspace == null) {
            globalWorkspace = new Workspace();
            globalWorkspace.setScope(Scope.GLOBAL);
        }
        return globalWorkspace;
    }

    private Workspace getUserWorkspace(Workspace userWorkspace, User currentUser) {
        if (userWorkspace == null) {
            userWorkspace = new Workspace(Scope.USER, currentUser.getUserId(),
                    Sets.newHashSet(Role.COMPONENTS_BROWSER, Role.COMPONENTS_MANAGER, Role.ARCHITECT));
        }
        return userWorkspace;
    }

    public boolean isParentWorkspace(String child, String parent) {
        // TODO for the moment only global is the parent workspace
        return !Workspace.getScopeFromId(child).equals(Scope.GLOBAL) && Workspace.getScopeFromId(parent).equals(Scope.GLOBAL);
    }

    /**
     * <p>
     * Get the list of user workspaces with the associated role.
     * </p>
     * <p>
     * Write access on the workspaces of the csar:
     * </p>
     * <ul>
     * <li>Global ==> global rôle COMPONENTS_MANAGER (Types) or ARCHITECT (Topologies)</li>
     * </ul>
     * <ul>
     * <li>User ==> both COMPONENTS_MANAGER (Types) and ARCHITECT (Topologies)</li>
     * </ul>
     * <ul>
     * <li>Application ==> if APPLICATION_MANAGER or APPLICATION_DEVOPS => COMPONENTS_MANAGER (Types) and ARCHITECT (Topologies).</li>
     * </ul>
     *
     * @return list of workspaces that the current user has write access
     */
    public List<Workspace> getUserWorkspaces() {
        User currentUser = AuthorizationUtil.getCurrentUser();
        Workspace globalWorkspace = null;
        Workspace userWorkspace = null;
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_MANAGER)) {
            globalWorkspace = getGlobalWorkspace(globalWorkspace);
            globalWorkspace.getRoles().add(Role.COMPONENTS_MANAGER);
            userWorkspace = getUserWorkspace(userWorkspace, currentUser);
        }
        if (AuthorizationUtil.hasOneRoleIn(Role.ARCHITECT)) {
            globalWorkspace = getGlobalWorkspace(globalWorkspace);
            globalWorkspace.getRoles().add(Role.ARCHITECT);
            userWorkspace = getUserWorkspace(userWorkspace, currentUser);
        }
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER)) {
            globalWorkspace = getGlobalWorkspace(globalWorkspace);
            globalWorkspace.getRoles().add(Role.COMPONENTS_BROWSER);
            userWorkspace = getUserWorkspace(userWorkspace, currentUser);
        }
        List<Workspace> workspaces = new ArrayList<>();
        addIfNotNull(workspaces, globalWorkspace);
        addIfNotNull(workspaces, userWorkspace);
        return workspaces;
    }

    public boolean hasPromotionPrivilege(PromotionRequest request) {
        Csar csar = csarService.getOrFail(request.getCsarName(), request.getCsarVersion());
        return hasWriteRoles(request.getTargetWorkspace(), getExpectedRolesToPromoteCSAR(csar));
    }

    private void addIfNotNull(List<Workspace> workspaces, Workspace workspace) {
        if (workspace != null) {
            workspaces.add(workspace);
        }
    }

    private boolean hasRoles(String workspaceId, Set<Role> expectedRoles) {
        return getUserWorkspaces().stream().filter(workspace -> workspace.getId().equals(workspaceId) && workspace.getRoles().containsAll(expectedRoles))
                .findFirst().isPresent();
    }

    private boolean hasWriteRoles(String workspaceId, Set<Role> expectedRoles) {
        if (expectedRoles.isEmpty()) {
            // either component manager or architect
            return hasRoles(workspaceId, Collections.singleton(Role.COMPONENTS_MANAGER)) || hasRoles(workspaceId, Collections.singleton(Role.ARCHITECT));
        }
        return hasRoles(workspaceId, expectedRoles);
    }

    private Set<Role> getExpectedRolesToPromoteCSAR(Csar csar) {
        Set<Role> expectedRoles = new HashSet<>();
        if (topologyCatalogService.exists(csar.getId())) {
            expectedRoles.add(Role.ARCHITECT);
        }
        if (typesCatalogService.hasTypes(csar.getName(), csar.getVersion())) {
            expectedRoles.add(Role.COMPONENTS_MANAGER);
        }
        return expectedRoles;
    }

    /**
     * Get list of available promotion targets for a CSAR
     * 
     * @param csar the csar to get promotion targets for
     * @return the list of available targets
     */
    public List<Workspace> getPromotionTargets(Csar csar) {
        if (hasWriteRoles(csar.getWorkspace(), getExpectedRolesToPromoteCSAR(csar))) {
            return getUserWorkspaces().stream()
                    .filter(workspace -> !workspace.getId().equals(csar.getWorkspace()) && workspace.getRoles().contains(Role.COMPONENTS_BROWSER))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public CSARPromotionImpact getCSARPromotionImpact(Csar csar, String targetWorkSpace) {
        // Retrieve all transitive dependencies of the promoted CSAR
        List<Csar> csarDependencies = AlienUtils.safe(csar.getDependencies()).stream()
                .map(csarDependency -> csarService.get(csarDependency.getName(), csarDependency.getVersion())).collect(Collectors.toList());
        // Filter out CSARs which are already on the target workspace
        Map<String, Csar> impactedCSARs = Stream
                .concat(Stream.of(csar), csarDependencies.stream().filter(csarDependency -> !csarDependency.getWorkspace().equals(targetWorkSpace)))
                .collect(Collectors.toMap(Csar::getId, element -> element));
        // Filter out usages that concerns impacted CSARs of the promotion
        Map<String, List<Usage>> usageMap;
        if (isParentWorkspace(csar.getWorkspace(), targetWorkSpace)) {
            // Move to the parent workspace then all the CSARs are always available for usage
            usageMap = Collections.emptyMap();
        } else {
            // Else must check the usage to be sure that it's not used
            usageMap = impactedCSARs.values().stream()
                    .collect(Collectors.toMap(Csar::getId, impactedCSAR -> csarService.getCsarRelatedResourceList(impactedCSAR).stream()
                            .filter(usage -> !impactedCSARs.containsKey(usage.getResourceId())).collect(Collectors.toList())));
        }
        // Filter out csar with no usage found
        return new CSARPromotionImpact(
                usageMap.entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                impactedCSARs, hasWriteRoles(targetWorkSpace, getExpectedRolesToPromoteCSAR(csar)));
    }

    public CSARPromotionImpact getCSARPromotionImpact(String csarName, String csarVersion, String targetWorkSpace) {
        Csar csar = csarService.getOrFail(csarName, csarVersion);
        return getCSARPromotionImpact(csar, targetWorkSpace);
    }

    private void performPromotionImpact(Csar csar, String targetWorkSpace, CSARPromotionImpact impact) {
        impact.getImpactedCsars().values().forEach(impactedCsar -> {
            impactedCsar.setWorkspace(targetWorkSpace);
            csarService.save(impactedCsar);
            Topology topology = topologyCatalogService.get(csar.getId());
            if (topology != null) {
                topology.setWorkspace(targetWorkSpace);
                topologyServiceCore.save(topology);
            }
            AbstractToscaType[] types = typesCatalogService.getArchiveTypes(csar.getName(), csar.getVersion());
            Arrays.stream(types).forEach(type -> {
                type.setWorkspace(targetWorkSpace);
                alienDAO.save(type);
            });
        });
    }

    private void savePromotionRequest(PromotionRequest promotionRequest) {
        workspaceDAO.save(promotionRequest);
    }

    public PromotionRequest refuseCSARPromotion(PromotionRequest promotionRequest) {
        PromotionRequest existingRequest = workspaceDAO.findById(PromotionRequest.class, promotionRequest.getId());
        if (existingRequest != null) {
            existingRequest.setProcessDate(new Date());
            existingRequest.setProcessUser(AuthorizationUtil.getCurrentUser().getUserId());
            existingRequest.setStatus(PromotionStatus.REFUSED);
            savePromotionRequest(existingRequest);
            return existingRequest;
        } else {
            throw new NotFoundException("Promotion request cannot be found [" + promotionRequest.getId() + "]");
        }
    }

    public PromotionRequest promoteCSAR(PromotionRequest promotionRequest) {
        Csar csar = csarService.getOrFail(promotionRequest.getCsarName(), promotionRequest.getCsarVersion());
        CSARPromotionImpact impact = getCSARPromotionImpact(csar, promotionRequest.getTargetWorkspace());
        Date currentDate = new Date();
        String currentUser = AuthorizationUtil.getCurrentUser().getUserId();
        if (impact.isHasWriteAccessOnTarget()) {
            // new request and the user manages the target workspace then move immediately
            performPromotionImpact(csar, promotionRequest.getTargetWorkspace(), impact);
            promotionRequest.setRequestUser(currentUser);
            promotionRequest.setRequestDate(currentDate);
            promotionRequest.setProcessUser(currentUser);
            promotionRequest.setProcessDate(currentDate);
            promotionRequest.setStatus(PromotionStatus.ACCEPTED);
        } else {
            // Else must save the promotion request so that people who manages the workspace can validate
            promotionRequest.setRequestUser(currentUser);
            promotionRequest.setRequestDate(currentDate);
            promotionRequest.setStatus(PromotionStatus.INIT);
        }
        savePromotionRequest(promotionRequest);
        return promotionRequest;
    }

    public PromotionRequest acceptCSARPromotion(PromotionRequest promotionRequest) {
        Csar csar = csarService.getOrFail(promotionRequest.getCsarName(), promotionRequest.getCsarVersion());
        CSARPromotionImpact impact = getCSARPromotionImpact(csar, promotionRequest.getTargetWorkspace());
        Date currentDate = new Date();
        String currentUser = AuthorizationUtil.getCurrentUser().getUserId();
        PromotionRequest existingRequest = workspaceDAO.findById(PromotionRequest.class, promotionRequest.getId());
        if (existingRequest != null) {
            // Accept an existing request
            performPromotionImpact(csar, existingRequest.getTargetWorkspace(), impact);
            existingRequest.setProcessUser(currentUser);
            existingRequest.setProcessDate(currentDate);
            existingRequest.setStatus(PromotionStatus.ACCEPTED);
            savePromotionRequest(existingRequest);
            return existingRequest;
        } else {
            throw new NotFoundException("Promotion request cannot be found [" + promotionRequest.getId() + "]");
        }
    }
}
