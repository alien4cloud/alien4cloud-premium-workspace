package org.alien4cloud.workspace.listener;

import javax.annotation.Resource;

import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import alien4cloud.security.event.UserDeletedEvent;

@Component
public class UserDeletionListener implements ApplicationListener<UserDeletedEvent> {

    @Resource
    private WorkspaceService workspaceService;

    @Override
    public void onApplicationEvent(UserDeletedEvent event) {
        workspaceService.deleteWorkspace(new Workspace(Scope.USER, event.getUser().getUserId(), null).getId());
    }
}
