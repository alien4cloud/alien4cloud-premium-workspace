// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var prefixer = require('scripts/plugin-url-prefixer');
  require('scripts/workspace/directives/display_workspace');

  // override component list to have the list of workspaces
  // prefixer.prefix('views/workspace/component_list.html')
  states.merge('topologycatalog.list', {
    url: '/list',
    templateUrl: prefixer.prefix('views/workspace/topology_template_list.html'),
    controller: 'WorkspaceTopologyTemplateListCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });

  modules.get('a4c-topology-templates', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceTopologyTemplateListCtrl',
    ['$scope', '$modal', '$alresource', '$state', 'authService', '$controller', 'workspaceServices', 'workspaces',
    function ($scope, $modal, $alresource, $state, authService, $controller, workspaceServices, workspaces) {
      // Apply opensource controller first
      $controller('TopologyTemplateListCtrl', { $scope: $scope, $modal: $modal, $alresource: $alresource, $state: $state, authService: authService });

      var processedWorkspaces = workspaceServices.process(workspaces, 'ARCHITECT');
      $scope.defaultFilters = {};
      $scope.staticFacets = processedWorkspaces.staticFacets;
      $scope.workspacesForUpload = processedWorkspaces.writeWorkspaces;
      if(processedWorkspaces.defaultWorkspaces.length > 0) {
        $scope.defaultFilters.workspace =  processedWorkspaces.defaultWorkspaces;
      }

      $scope.selectWorkspaceForUpload = function (workspace) {
        $scope.selectedWorkspaceForUpload = workspace;
        $scope.selectedWorkspaceForUploadData = {
          workspace: $scope.selectedWorkspaceForUpload.id
        };
      };
      if($scope.workspacesForUpload.length>0) {
        $scope.selectWorkspaceForUpload($scope.workspacesForUpload[0]);
      }
    }
  ]);
});
