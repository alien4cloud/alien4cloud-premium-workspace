package org.alien4cloud.workspace.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.TopologyCatalogService;
import org.alien4cloud.tosca.catalog.index.ToscaTypeSearchService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.workspace.model.CSARPromotionImpact;
import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
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
    private ToscaTypeSearchService typesCatalogService;
    @Inject
    private CsarService csarService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

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
        if (!getPromotionTargets(csar).stream().filter(workspace -> workspace.getId().equals(targetWorkSpace)).findFirst().isPresent()) {
            // TODO throw proper exception
            throw new RuntimeException("The csar cannot be moved to the given target");
        }
        // Retrieve all transitive dependencies of the promoted CSAR
        List<Csar> csarDependencies = AlienUtils.safe(csar.getDependencies()).stream()
                .map(csarDependency -> csarService.get(csarDependency.getName(), csarDependency.getVersion())).collect(Collectors.toList());
        // Filter out CSARs which are already on the target workspace
        Map<String, Csar> impactedCSARs = Stream
                .concat(Stream.of(csar), csarDependencies.stream().filter(csarDependency -> !csarDependency.getWorkspace().equals(targetWorkSpace)))
                .collect(Collectors.toMap(Csar::getId, element -> element));
        // Filter out usages that concerns impacted CSARs of the promotion
        Map<String, List<Usage>> usageMap = impactedCSARs.values().stream()
                .collect(Collectors.toMap(Csar::getId, impactedCSAR -> csarService.getCsarRelatedResourceList(impactedCSAR).stream()
                        .filter(usage -> !impactedCSARs.containsKey(usage.getResourceId())).collect(Collectors.toList())));
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

    public void promoteCSAR(String csarName, String csarVersion, String targetWorkSpace) {
        Csar csar = csarService.getOrFail(csarName, csarVersion);
        CSARPromotionImpact impact = getCSARPromotionImpact(csar, targetWorkSpace);
        if (impact.isHasWriteAccessOnTarget()) {
            performPromotionImpact(csar, targetWorkSpace, impact);
        } else if (!hasRoles(targetWorkSpace, Collections.singleton(Role.COMPONENTS_BROWSER))) {
            // TODO throw proper exception
            throw new RuntimeException("Invalid promotion request to a workspace that the user has no read access");
        } else {
            // TODO create promotion request here
            throw new RuntimeException("To be implemented");
        }
    }
}
