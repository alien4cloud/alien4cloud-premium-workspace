package org.alien4cloud.workspace.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.Csar;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.utils.AlienConstants;

/**
 * Aspect that ensure the search filters respect the user workspaces.
 */
@Aspect
@Component
public class SearchWorkspaceAspect {
    @Inject
    private WorkspaceService workspaceService;
    @Inject
    private ITopologyCatalogService catalogService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Around("execution(* org.alien4cloud.tosca.catalog.index.IArchiveIndexerAuthorizationFilter+.checkAuthorization(..))")
    public void onCatalogUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return;
        }
        // we just override the basic security and manage it here
        ArchiveRoot archiveRoot = (ArchiveRoot) joinPoint.getArgs()[0];
        String workspace = archiveRoot.getArchive().getWorkspace();
        if (archiveRoot.hasToscaTopologyTemplate()) {
            if (!workspaceService.hasRoles(workspace, Sets.newHashSet(Role.ARCHITECT))) {
                throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                        + "> is not authorized to upload to workspace <" + workspace + ">.");
            }
        }
        if (archiveRoot.hasToscaTypes()) {
            if (!workspaceService.hasRoles(workspace, Sets.newHashSet(Role.COMPONENTS_MANAGER))) {
                throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                        + "> is not authorized to upload to workspace <" + workspace + ">.");
            }
        }
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.IArchiveIndexerAuthorizationFilter+.preCheckAuthorization(..))")
    public void onBeforeCatalogUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return;
        }
        // we just override the basic security and manage it here
        String workspace = (String) joinPoint.getArgs()[0];
        if (!workspaceService.hasRoles(workspace, Sets.newHashSet(Role.COMPONENTS_MANAGER))
                && !workspaceService.hasRoles(workspace, Sets.newHashSet(Role.ARCHITECT))) {
            throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                    + "> is not authorized to upload to workspace <" + workspace + ">.");
        }
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter+.checkWriteAccess(..))")
    public void onCsarUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return;
        }
        Csar csar = (Csar) joinPoint.getArgs()[0];
        // if this csar has node types, check the COMPONENTS_MANAGER Role
        if (toscaTypeSearchService.hasTypes(csar.getName(), csar.getVersion())) {
            if (!workspaceService.hasRoles(csar.getWorkspace(), Sets.newHashSet(Role.COMPONENTS_MANAGER))) {
                throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                        + "> is not authorized to update csar <" + csar.getId() + "> of workspace <" + csar.getWorkspace() + ">.");
            }
        }
        // if the csar is bound to a topology, check the ARCHITECT Role
        if (catalogService.exists(csar.getId())) {
            if (!workspaceService.hasRoles(csar.getWorkspace(), Sets.newHashSet(Role.ARCHITECT))) {
                throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName()
                        + "> is not authorized to update csar <" + csar.getId() + "> of workspace <" + csar.getWorkspace() + ">.");
            }
        }
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter+.checkReadAccess(..))")
    public void onCsarAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return;
        }
        Csar csar = (Csar) joinPoint.getArgs()[0];
        if (!workspaceService.hasRoles(csar.getWorkspace(), Sets.newHashSet(Role.COMPONENTS_BROWSER))) {
            throw new AccessDeniedException("user <" + SecurityContextHolder.getContext().getAuthentication().getName() + "> is not authorized to access csar <"
                    + csar.getId() + "> of workspace <" + csar.getWorkspace() + ">.");
        }
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ICsarSearchService+.search(..))")
    public Object ensureCSARContext(ProceedingJoinPoint joinPoint) throws Throwable {
        return doEnsureContext(joinPoint, workspace -> true);
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService+.search(..))")
    public Object ensureTypeContext(ProceedingJoinPoint joinPoint) throws Throwable {
        return doEnsureContext(joinPoint, workspace -> true);
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ITopologyCatalogService+.search(..))")
    public Object ensureTopologyContext(ProceedingJoinPoint joinPoint) throws Throwable {
        // Topology from application workspace should not appear in topology catalog
        return doEnsureContext(joinPoint, workspace -> !workspace.startsWith(AlienConstants.APP_WORKSPACE_PREFIX + ":"));
    }

    private Object doEnsureContext(ProceedingJoinPoint joinPoint, Predicate<String> workspaceFilter) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return joinPoint.proceed();
        }
        Map<String, String[]> filters = (Map<String, String[]>) joinPoint.getArgs()[3];
        if (filters == null) {
            filters = new HashMap<>();
            joinPoint.getArgs()[3] = filters;
        }
        String[] workspaces = filters.get("workspace");
        Set<String> userWorkspaces = workspaceService.getUserWorkspaceIds(Collections.singleton(Role.COMPONENTS_BROWSER)).stream().filter(workspaceFilter)
                .collect(Collectors.toSet());
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
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Around("execution(* org.alien4cloud.tosca.catalog.index.ITopologyCatalogService+.getAll(..))")
    public Object getAllMyWorkspaces(ProceedingJoinPoint joinPoint) throws Throwable {
        // Called by Alien it-self and not by an active user so do not try to intercept
        if (AuthorizationUtil.getCurrentUser() == null) {
            return joinPoint.proceed();
        }
        // Add workspaces filter on all workspaces with a write access.
        Set<String> userWorkspaces = workspaceService.getUserWorkspaceIds(Collections.singleton(Role.COMPONENTS_BROWSER));
        Map<String, String[]> filters = (Map<String, String[]>) joinPoint.getArgs()[0];
        if (filters == null) {
            filters = new HashMap<>();
            joinPoint.getArgs()[0] = filters;
        }
        // add the workspaces
        filters.put("workspace", userWorkspaces.toArray(new String[userWorkspaces.size()]));
        return joinPoint.proceed(joinPoint.getArgs());
    }

    // ITopologyCatalogService.getAll
    // ITopologyCatalogService.get (should look at the result and check workspace)
    // ITopologyCatalogService.getOrFail (should look at the result and check workspace)
}