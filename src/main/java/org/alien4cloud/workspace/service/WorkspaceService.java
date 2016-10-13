package org.alien4cloud.workspace.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.workspace.model.CSARPromotionImpact;
import org.alien4cloud.workspace.model.PromotionRequest;
import org.alien4cloud.workspace.model.PromotionStatus;
import org.alien4cloud.workspace.model.Scope;
import org.alien4cloud.workspace.model.Workspace;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.Usage;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.security.users.IAlienUserDao;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.AlienUtils;

@Service
public class WorkspaceService {
    @Resource
    private IAlienUserDao alienUserDao;
    @Inject
    private ITopologyCatalogService topologyCatalogService;
    @Inject
    private IToscaTypeSearchService typesCatalogService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private ICsarService csarService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource(name = "workspace-dao")
    private IGenericSearchDAO workspaceDAO;

    private Workspace getGlobalWorkspace(Workspace globalWorkspace) {
        if (globalWorkspace == null) {
            globalWorkspace = new Workspace();
            globalWorkspace.setScope(Scope.GLOBAL);
        }
        return globalWorkspace;
    }

    private Workspace getPersonalWorkspace(Workspace userWorkspace, User currentUser) {
        if (userWorkspace == null) {
            userWorkspace = new Workspace(Scope.USER, currentUser.getUserId(),
                    Sets.newHashSet(Role.COMPONENTS_BROWSER, Role.COMPONENTS_MANAGER, Role.ARCHITECT));
        }
        return userWorkspace;
    }

    public boolean isParentWorkspace(String child, String parent) {
        // TODO for the moment only global is the parent workspace
        return !Workspace.getScopeFromId(child).equals(Scope.GLOBAL) && Workspace.getScopeFromId(parent).equals(Scope.GLOBAL);
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
            globalWorkspace.getRoles().add(Role.COMPONENTS_BROWSER);
            userWorkspace = getPersonalWorkspace(userWorkspace, currentUser);
        }
        if (AuthorizationUtil.hasOneRoleIn(Role.ARCHITECT)) {
            globalWorkspace = getGlobalWorkspace(globalWorkspace);
            globalWorkspace.getRoles().add(Role.ARCHITECT);
            globalWorkspace.getRoles().add(Role.COMPONENTS_BROWSER);
            userWorkspace = getPersonalWorkspace(userWorkspace, currentUser);
        }
        if (AuthorizationUtil.hasOneRoleIn(Role.COMPONENTS_BROWSER)) {
            globalWorkspace = getGlobalWorkspace(globalWorkspace);
            globalWorkspace.getRoles().add(Role.COMPONENTS_BROWSER);
            userWorkspace = getPersonalWorkspace(userWorkspace, currentUser);
        }
        List<Workspace> workspaces = new ArrayList<>();
        addIfNotNull(workspaces, globalWorkspace);
        workspaces.addAll(getUserApplicationWorkspaces());
        if (AuthorizationUtil.hasOneRoleIn(Role.ADMIN)) {
            // If the user is admin, he also has access to every user's workspaces
            workspaces.addAll(getAllPersonalWorkspaces());
        } else {
            // Else he has access only to his own personal workspace
            addIfNotNull(workspaces, userWorkspace);
        }
        return workspaces;
    }

    private List<Workspace> getAllPersonalWorkspaces() {
        // Get all users in the system
        Object[] userResult = alienUserDao.find(Collections.emptyMap(), Integer.MAX_VALUE).getData();
        if (userResult != null && userResult.length > 0) {
            return Arrays.stream(userResult).map(userObject -> {
                User user = (User) userObject;
                return new Workspace(Scope.USER, user.getUserId(),
                        ImmutableSet.<Role> builder().add(Role.COMPONENTS_BROWSER).add(Role.COMPONENTS_MANAGER).add(Role.ARCHITECT).build());
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<Workspace> getUserApplicationWorkspaces() {
        List<Workspace> workspaces = new ArrayList<>();
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();
        // Get all application in the system
        FacetedSearchResult applicationsSearchResult = alienDAO.facetedSearch(Application.class, null, null, authorizationFilter, null, 0, Integer.MAX_VALUE);
        if (applicationsSearchResult.getData() != null && applicationsSearchResult.getData().length > 0) {
            Arrays.stream(applicationsSearchResult.getData()).forEach(applicationRaw -> {
                Application application = (Application) applicationRaw;
                if (AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_DEVOPS)) {
                    workspaces.add(new Workspace(Scope.APPLICATION, application.getId(),
                            ImmutableSet.<Role> builder().add(Role.COMPONENTS_BROWSER).add(Role.COMPONENTS_MANAGER).add(Role.ARCHITECT).build()));
                } else if (AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_USER)) {
                    // A deployer must be an application user
                    workspaces.add(new Workspace(Scope.APPLICATION, application.getId(), ImmutableSet.<Role> builder().add(Role.COMPONENTS_BROWSER).build()));
                }
            });
        }
        return workspaces;
    }

    /**
     * Get the ids of the user workspaces.
     *
     * @param expectedRoles If set we should only provide workspaces with the expected roles.
     * @return A set of workspaces ids that the user can access.
     */
    public Set<String> getUserWorkspaceIds(Set<Role> expectedRoles) {
        return getUserWorkspaces().stream().filter(workspace -> expectedRoles == null || workspace.getRoles().containsAll(expectedRoles)).map(Workspace::getId)
                .collect(Collectors.toSet());
    }

    public boolean hasAcceptPromotionPrivilege(PromotionRequest request) {
        Csar csar = csarService.getOrFail(request.getCsarName(), request.getCsarVersion());
        // User must have expected roles on the target workspace
        return hasRoles(request.getTargetWorkspace(), getExpectedRolesToPromoteCSAR(csar));
    }

    private boolean hasPromotionPrivilege(Csar csar) {
        // User must have expected roles on the csar's workspace
        return hasRoles(csar.getWorkspace(), getExpectedRolesToPromoteCSAR(csar));
    }

    private void addIfNotNull(List<Workspace> workspaces, Workspace workspace) {
        if (workspace != null) {
            workspaces.add(workspace);
        }
    }

    /**
     * Check if a user has a role in the specified workspace.
     *
     * @param workspaceId The if of the workspace for which to check roles.
     * @param expectedRoles The sets of expected roles, all roles must be matched.
     * @return
     */
    public boolean hasRoles(String workspaceId, Set<Role> expectedRoles) {
        return getUserWorkspaces().stream().filter(workspace -> workspace.getId().equals(workspaceId) && workspace.getRoles().containsAll(expectedRoles))
                .findFirst().isPresent();
    }

    private Set<Role> getExpectedRolesToPromoteCSAR(Csar csar) {
        Set<Role> expectedRoles = new HashSet<>();
        if (topologyCatalogService.exists(csar.getId())) {
            expectedRoles.add(Role.ARCHITECT);
        }
        if (typesCatalogService.hasTypes(csar.getName(), csar.getVersion())) {
            expectedRoles.add(Role.COMPONENTS_MANAGER);
        }
        return expectedRoles;
    }

    /**
     * Get list of available promotion targets for a CSAR
     * 
     * @param csar the csar to get promotion targets for
     * @return the list of available targets
     */
    public List<Workspace> getPromotionTargets(Csar csar) {
        if (hasPromotionPrivilege(csar)) {
            return getUserWorkspaces().stream()
                    .filter(workspace -> !workspace.getId().equals(csar.getWorkspace()) && workspace.getRoles().contains(Role.COMPONENTS_BROWSER))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isUsageStillSatisfiedAfterPromotion(Usage resource, String targetWorkSpace) {
        // If the target workspace is the parent of the workspace of the resource that requires the CSAR, then the resource can still use the CSAR
        return resource.getWorkspace() != null && targetWorkSpace.equals(resource.getWorkspace())
                || isParentWorkspace(resource.getWorkspace(), targetWorkSpace);
    }

    public CSARPromotionImpact getCSARPromotionImpact(Csar csar, String targetWorkSpace) {
        // Retrieve all transitive dependencies of the promoted CSAR
        List<Csar> csarDependencies = AlienUtils.safe(csar.getDependencies()).stream()
                .map(csarDependency -> csarService.get(csarDependency.getName(), csarDependency.getVersion())).collect(Collectors.toList());
        // Filter out CSARs which are already on the target workspace
        Map<String, Csar> impactedCSARs = Stream
                // Target workspace is the child of the dependency's workspace then do not try to move the dependency
                // The reason is because, the dependency is still visible to the promoted CSAR once moved to the target workspace
                .concat(Stream.of(csar), csarDependencies.stream().filter(csarToMove -> !isParentWorkspace(targetWorkSpace, csarToMove.getWorkspace())))
                // Target workspace is the workspace of the CSAR then do not try to move it
                .filter(csarToMove -> !csarToMove.getWorkspace().equals(targetWorkSpace)).collect(Collectors.toMap(Csar::getId, element -> element));
        // Filter out usages that concerns impacted CSARs of the promotion
        Map<String, List<Usage>> usageMap = impactedCSARs.values().stream()
                .collect(Collectors.toMap(Csar::getId, impactedCSAR -> csarService.getCsarRelatedResourceList(impactedCSAR).stream()
                        // Filter out usages between the moved CSARs and usages that are still satisfied after the promotion
                        .filter(usage -> !impactedCSARs.containsKey(usage.getResourceId()) && !isUsageStillSatisfiedAfterPromotion(usage, targetWorkSpace))
                        .collect(Collectors.toList())));
        // Filter out csar with no usage found
        return new CSARPromotionImpact(
                usageMap.entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                impactedCSARs, hasRoles(targetWorkSpace, getExpectedRolesToPromoteCSAR(csar)));
    }

    public CSARPromotionImpact getCSARPromotionImpact(String csarName, String csarVersion, String targetWorkSpace) {
        Csar csar = csarService.getOrFail(csarName, csarVersion);
        return getCSARPromotionImpact(csar, targetWorkSpace);
    }

    private void performPromotionImpact(Csar csar, String targetWorkSpace, CSARPromotionImpact impact) {
        impact.getImpactedCsars().values().forEach(impactedCsar -> {
            impactedCsar.setWorkspace(targetWorkSpace);
            csarService.save(impactedCsar);
            Topology topology = topologyCatalogService.get(csar.getId());
            if (topology != null) {
                topology.setWorkspace(targetWorkSpace);
                topologyServiceCore.save(topology);
            }
            AbstractToscaType[] types = typesCatalogService.getArchiveTypes(impactedCsar.getName(), impactedCsar.getVersion());
            Arrays.stream(types).forEach(type -> {
                type.setWorkspace(targetWorkSpace);
                alienDAO.save(type);
            });
        });
    }

    private void savePromotionRequest(PromotionRequest promotionRequest) {
        workspaceDAO.save(promotionRequest);
    }

    public PromotionRequest promoteCSAR(PromotionRequest promotionRequest) {
        if (StringUtils.isBlank(promotionRequest.getTargetWorkspace())) {
            throw new InvalidArgumentException("Promotion request's target workspace is mandatory");
        }
        Csar csar = csarService.getOrFail(promotionRequest.getCsarName(), promotionRequest.getCsarVersion());
        if (!getPromotionTargets(csar).stream().filter(workspace -> workspace.getId().equals(promotionRequest.getTargetWorkspace())).findFirst().isPresent()) {
            throw new AccessDeniedException("You don't have authorization to promote the CSAR [" + promotionRequest.getCsarName() + ":"
                    + promotionRequest.getCsarVersion() + "] to [" + promotionRequest.getTargetWorkspace() + "]");
        }
        CSARPromotionImpact impact = getCSARPromotionImpact(csar, promotionRequest.getTargetWorkspace());
        if (!impact.getCurrentUsages().isEmpty()) {
            throw new InvalidArgumentException("The CSAR is still being used and cannot be promoted");
        }
        Date currentDate = new Date();
        String currentUser = AuthorizationUtil.getCurrentUser().getUserId();
        if (impact.isHasWriteAccessOnTarget()) {
            // new request and the user manages the target workspace then move immediately
            performPromotionImpact(csar, promotionRequest.getTargetWorkspace(), impact);
            promotionRequest.setRequestUser(currentUser);
            promotionRequest.setRequestDate(currentDate);
            promotionRequest.setProcessUser(currentUser);
            promotionRequest.setProcessDate(currentDate);
            promotionRequest.setStatus(PromotionStatus.ACCEPTED);
        } else {
            // Else must save the promotion request so that people who manages the workspace can validate
            promotionRequest.setRequestUser(currentUser);
            promotionRequest.setRequestDate(currentDate);
            promotionRequest.setStatus(PromotionStatus.INIT);
        }
        // Generate a technical id
        promotionRequest.setId(UUID.randomUUID().toString());
        savePromotionRequest(promotionRequest);
        return promotionRequest;
    }

    public PromotionRequest refuseCSARPromotion(PromotionRequest promotionRequest) {
        PromotionRequest existingRequest = workspaceDAO.findById(PromotionRequest.class, promotionRequest.getId());
        if (existingRequest != null) {
            checkAcceptPromotionPrivilege(existingRequest);
            existingRequest.setProcessDate(new Date());
            existingRequest.setProcessUser(AuthorizationUtil.getCurrentUser().getUserId());
            existingRequest.setStatus(PromotionStatus.REFUSED);
            savePromotionRequest(existingRequest);
            return existingRequest;
        } else {
            throw new NotFoundException("Promotion request cannot be found [" + promotionRequest.getId() + "]");
        }
    }

    public PromotionRequest acceptCSARPromotion(PromotionRequest promotionRequest) {
        PromotionRequest existingRequest = workspaceDAO.findById(PromotionRequest.class, promotionRequest.getId());
        if (existingRequest != null) {
            checkAcceptPromotionPrivilege(existingRequest);
            Csar csar = csarService.getOrFail(existingRequest.getCsarName(), existingRequest.getCsarVersion());
            CSARPromotionImpact impact = getCSARPromotionImpact(csar, existingRequest.getTargetWorkspace());
            Date currentDate = new Date();
            String currentUser = AuthorizationUtil.getCurrentUser().getUserId();
            // Accept an existing request
            performPromotionImpact(csar, existingRequest.getTargetWorkspace(), impact);
            existingRequest.setProcessUser(currentUser);
            existingRequest.setProcessDate(currentDate);
            existingRequest.setStatus(PromotionStatus.ACCEPTED);
            savePromotionRequest(existingRequest);
            return existingRequest;
        } else {
            throw new NotFoundException("Promotion request cannot be found [" + promotionRequest.getId() + "]");
        }
    }

    private void checkAcceptPromotionPrivilege(PromotionRequest promotionRequest) {
        if (!hasAcceptPromotionPrivilege(promotionRequest)) {
            throw new AccessDeniedException("You don't have authorization to accept/refuse the CSAR's promotion");
        }
    }
}
