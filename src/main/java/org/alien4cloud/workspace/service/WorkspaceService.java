package org.alien4cloud.workspace.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.springframework.stereotype.Service;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Service
public class WorkspaceService {

    /**
     * Get list of workspaces that the current user has write access
     * <p>
     * Write access on the workspaces of the csar
     * </p>
     * <p>
     * - Global ==> must be component manager
     * </p>
     * <p>
     * - Personal ==> yes
     * </p>
     * <p>
     * - Application ==> application manager + application devops
     * </p>
     *
     * @return list of workspaces that the current user has write access
     */
    public List<Workspace> getAuthorizedWorkspacesForUpload() {
        List<Workspace> workspaces = new ArrayList<>();
        User currentUser = AuthorizationUtil.getCurrentUser();
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_MANAGER)) {
            workspaces.add(Workspace.GLOBAL);
        }
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER, Role.COMPONENTS_MANAGER)) {
            workspaces.add(new Workspace(Scope.USER, currentUser.getUserId()));
        }
        return workspaces;
    }

    public List<Workspace> getAuthorizedWorkspacesForSearch() {
        List<Workspace> workspaces = new ArrayList<>();
        User currentUser = AuthorizationUtil.getCurrentUser();
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER, Role.COMPONENTS_MANAGER)) {
            workspaces.add(Workspace.GLOBAL);
            workspaces.add(new Workspace(Scope.USER, currentUser.getUserId()));
        }
        return workspaces;
    }

    private boolean hasReadAccess(Workspace workspace) {
        return getAuthorizedWorkspacesForSearch().contains(workspace);
    }

    private boolean hasWriteAccess(String workspaceId) {
        return getAuthorizedWorkspacesForUpload().stream().filter(workspace -> workspace.getId().equals(workspaceId)).findFirst().isPresent();
    }

    /**
     * Get list of available promotion targets for a CSAR
     * 
     * @param csar the csar to get promotion targets for
     * @return the list of available targets
     */
    public List<Workspace> getPromotionTargets(Csar csar) {
        if (hasWriteAccess(csar.getWorkspace())) {
            return getAuthorizedWorkspacesForSearch().stream().filter(workspace -> !workspace.getId().equals(csar.getWorkspace())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
