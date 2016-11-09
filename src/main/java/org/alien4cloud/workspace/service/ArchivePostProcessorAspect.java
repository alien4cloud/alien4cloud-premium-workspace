package org.alien4cloud.workspace.service;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Aspect to perform further validation on the archive based on its workspace
 */
@Aspect
@Component
public class ArchivePostProcessorAspect {
    @Inject
    private WorkspaceService workspaceService;
    @Inject
    private CsarService csarService;

    @Around("execution(* org.alien4cloud.tosca.catalog.IArchivePostProcessor+.process(..))")
    public Object process(ProceedingJoinPoint joinPoint) throws Throwable {
        ParsingResult<ArchiveRoot> parsingResult = (ParsingResult<ArchiveRoot>) joinPoint.proceed();
        // Check that the CSAR is not using dependencies that it's not supposed to
        Set<CSARDependency> dependencies = parsingResult.getResult().getArchive().getDependencies();
        if (dependencies != null) {
            dependencies.forEach(dependency -> {
                Csar dependencyCsar = csarService.get(dependency.getName(), dependency.getVersion());
                if (dependencyCsar != null && parsingResult.getResult().getArchive().getWorkspace() != null
                        && !Objects.equals(parsingResult.getResult().getArchive().getWorkspace(), dependencyCsar.getWorkspace())
                        && !workspaceService.isParentWorkspace(parsingResult.getResult().getArchive().getWorkspace(), dependencyCsar.getWorkspace())) {
                    // Only those kinds of dependencies are accessible
                    // 1. if the csar's dependency and the csar is in the same workspace
                    // 2. if the csar's dependency is in a workspace parent of the csar's workspace (everything in the parent is visible to the child)
                    parsingResult.getContext().getParsingErrors()
                            .add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.DEPENDENCY_NOT_VISIBLE_FROM_TARGET_WORKSPACE, "", null,
                                    "The archive's dependency " + dependency + " is not visible from the target workspace.", null, dependency.toString()));
                }
            });
        }
        return parsingResult;
    }
}
