package org.alien4cloud.workspace.listener;

import javax.annotation.Resource;

import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.alien4cloud.workspace.service.WorkspaceService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import org.alien4cloud.alm.events.BeforeApplicationDeleted;

@Component
public class ApplicationDeletionListener implements ApplicationListener<BeforeApplicationDeleted> {

    @Resource
    private WorkspaceService workspaceService;

    @Override
    public void onApplicationEvent(BeforeApplicationDeleted event) {
        workspaceService.deleteWorkspace(new Workspace(Scope.APPLICATION, event.getApplicationId(), null).getId());
    }
}
