package org.alien4cloud.workspace.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.security.model.Role;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Aspect that ensure the search filters respect the user workspaces.
 */
@Aspect
@Component
public class SearchWorkspaceAspect {
    @Inject
    private WorkspaceService workspaceService;

    @Around("execution(* org.alien4cloud.tosca.catalog.index.IArchiveIndexerAuthorizationFilter+.checkAuthorization(..))")
    public void onCatalogUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        // we just override the basic security and mange it here
        ArchiveRoot archiveRoot = (ArchiveRoot) joinPoint.getArgs()[0];
        String workspace = archiveRoot.getArchive().getWorkspace();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            workspaceService.hasRoles(workspace, Sets.newHashSet(Role.ARCHITECT));
        }
        if (archiveRoot.hasToscaTypes()) {
            workspaceService.hasRoles(workspace, Sets.newHashSet(Role.COMPONENTS_MANAGER));
        }
        return;
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService+.search(..))")
    public Object ensureTypeContext(ProceedingJoinPoint joinPoint) throws Throwable {
        return doEnsureContext(joinPoint);
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ITopologyCatalogService+.search(..))")
    public Object ensureTopologyContext(ProceedingJoinPoint joinPoint) throws Throwable {
        return doEnsureContext(joinPoint);
    }

    private Object doEnsureContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String, String[]> filters = (Map<String, String[]>) joinPoint.getArgs()[3];
        String[] workspaces = filters.get("workspace");
        Set<String> userWorkspaces = workspaceService.getUserWorkspaceIds(false);
        if (workspaces == null) {
            // inject all user workspaces
            String[] workspaceIds = userWorkspaces.toArray(new String[userWorkspaces.size()]);
            filters.put("workspace", workspaceIds);
        } else {
            // check that specified workspaces are indeed part of user workspaces
            for (String workspace : workspaces) {
                if (!userWorkspaces.contains(workspace)) {
                    throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                            + "> is not authorized to query workspaces <" + workspace + ">.");
                }
            }
        }
        return joinPoint.proceed();
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ITopologyCatalogService+.getAll(..))")
    public Object getAllMyWorkspaces(ProceedingJoinPoint joinPoint) throws Throwable {
        // Add workspaces filter on all workspaces with a write access.
        Set<String> userWorkspaces = workspaceService.getUserWorkspaceIds(true);
        Map<String, String[]> filters = (Map<String, String[]>) joinPoint.getArgs()[0];
        // add the workspaces
        filters.put("workspace", userWorkspaces.toArray(new String[userWorkspaces.size()]));
        return joinPoint.proceed();
    }

    // ITopologyCatalogService.getAll
    // ITopologyCatalogService.get (should look at the result and check workspace)
    // ITopologyCatalogService.getOrFail (should look at the result and check workspace)
}