package org.alien4cloud.workspace.listener;

import javax.annotation.Resource;

import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import alien4cloud.events.BeforeApplicationDeletedEvent;

@Component
public class ApplicationDeletionListener implements ApplicationListener<BeforeApplicationDeletedEvent> {

    @Resource
    private WorkspaceService workspaceService;

    @Override
    public void onApplicationEvent(BeforeApplicationDeletedEvent event) {
        workspaceService.deleteWorkspace(new Workspace(Scope.APPLICATION, event.getApplicationId(), null).getId());
    }
}
