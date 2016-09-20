package org.alien.workspace.service;

import java.util.ArrayList;
import java.util.List;

import org.alien.workspace.model.Scope;
import org.alien.workspace.model.Workspace;
import org.springframework.stereotype.Service;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;

@Service
public class WorkspaceService {

    public List<Workspace> getAuthorizedWorkspacesForUpload() {
        List<Workspace> workspaces = new ArrayList<>();
        User currentUser = AuthorizationUtil.getCurrentUser();
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_MANAGER)) {
            workspaces.add(Workspace.GLOBAL);
        }
        workspaces.add(new Workspace(Scope.USER, currentUser.getUserId()));
        return workspaces;
    }

    public List<Workspace> getAuthorizedWorkspacesForSearch() {
        List<Workspace> workspaces = new ArrayList<>();
        User currentUser = AuthorizationUtil.getCurrentUser();
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER)) {
            workspaces.add(Workspace.GLOBAL);
        }
        workspaces.add(new Workspace(Scope.USER, currentUser.getUserId()));
        return workspaces;
    }
}
