package org.alien4cloud.workspace.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import alien4cloud.common.AlienConstants;
import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Service
public class WorkspaceService {
    @Inject
    private ITopologyCatalogService topologyCatalogService;
    @Inject
    private IToscaTypeSearchService typesCatalogService;

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

    public Set<String> getUserWorkspaceIds() {
        Set<String> workspaces = Sets.newHashSet();
        User currentUser = AuthorizationUtil.getCurrentUser();
        if (AuthorizationUtil.hasOneRoleIn(Role.ARCHITECT, Role.COMPONENTS_MANAGER)) {
            workspaces.add(AlienConstants.GLOBAL_WORKSPACE_ID);
            workspaces.add(Scope.USER.name() + currentUser.getUserId());
        }
        return workspaces;
    }

    private void addIfNotNull(List<Workspace> workspaces, Workspace workspace) {
        if (workspace != null) {
            workspaces.add(workspace);
        }
    }

    private boolean hasRoles(String workspaceId, List<Role> expectedRoles) {
        return getUserWorkspaces().stream().filter(workspace -> workspace.getId().equals(workspaceId)).filter(workspace -> {
            for (Role expectedRole : expectedRoles) {
                if (workspace.getRoles().contains(expectedRole)) {
                    continue;
                }
                return false;
            }
            return true;
        }).findFirst().isPresent();
    }

    private boolean hasWriteRoles(String workspaceId, List<Role> expectedRoles) {
        if (expectedRoles.isEmpty()) { // either component manager or architect
            return hasRoles(workspaceId, Lists.newArrayList(Role.COMPONENTS_MANAGER)) || hasRoles(workspaceId, Lists.newArrayList(Role.ARCHITECT));
        }
        return hasRoles(workspaceId, expectedRoles);
    }

    /**
     * Get list of available promotion targets for a CSAR
     * 
     * @param csar the csar to get promotion targets for
     * @return the list of available targets
     */
    public List<Workspace> getPromotionTargets(Csar csar) {
        List<Role> expectedRoles = Lists.newArrayList();
        if (topologyCatalogService.exists(csar.getId())) {
            expectedRoles.add(Role.ARCHITECT);
        }
        if (typesCatalogService.hasTypes(csar.getName(), csar.getVersion())) {
            expectedRoles.add(Role.COMPONENTS_MANAGER);
        }
        if (hasWriteRoles(csar.getWorkspace(), expectedRoles)) {
            return getUserWorkspaces().stream().filter(workspace -> !workspace.getId().equals(csar.getWorkspace())).filter(workspace -> {
                return workspace.getRoles().contains(Role.COMPONENTS_BROWSER);
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
