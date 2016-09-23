package org.alien4cloud.workspace.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect that ensure the search filters respect the user workspaces.
 */
@Aspect
@Component
public class SearchWorkspaceAspect {
    @Inject
    private WorkspaceService workspaceService;

    @Pointcut("execution(* org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService+.search(..))")
    public void buildPointCut() {
    }

    // @Around("execution(* org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService.search(..))")
    @Around("buildPointCut()")
    public Object ensureContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String, String[]> filters = (Map<String, String[]>) joinPoint.getArgs()[4];
        String[] workspaces = filters.get("workspace");
        Set<String> userWorkspaces = workspaceService.getUserWorkspaceIds();
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
}