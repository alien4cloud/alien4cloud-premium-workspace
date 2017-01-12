// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  var prefixer = require('scripts/plugin-url-prefixer');
  require('scripts/workspace/directives/display_workspace');
  require('scripts/workspace/controllers/editor_register');

  // override component list to have the list of workspaces
  // prefixer.prefix('views/workspace/component_list.html')
  states.merge('topologycatalog.list', {
    url: '/list',
    templateUrl: prefixer.prefix('views/workspace/topology_template_list.html'),
    controller: 'WorkspaceTopologyTemplateListCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return _.filter(response.data, function(workspace) { return workspace.scope !== 'APPLICATION'; });
        });
      }]
    }
  });

  modules.get('a4c-topology-templates', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceTopologyTemplateListCtrl',
    ['$scope', '$uibModal', '$alresource', '$state', 'authService', '$controller', 'workspaceServices', 'workspaces',
    function ($scope, $uibModal, $alresource, $state, authService, $controller, workspaceServices, workspaces) {
      // Apply opensource controller first
      $controller('TopologyTemplateListCtrl', { $scope: $scope, $uibModal: $uibModal, $alresource: $alresource, $state: $state, authService: authService });

      // Override the new topology template modal to add workspace information
      var createTopologyTemplateResource = $alresource('/rest/latest/workspaces/topologies/template');
      $scope.createTopologyTemplate = function(topologyTemplate) {
        // create a new topologyTemplate from the given name, version and description.
        createTopologyTemplateResource.create([], angular.toJson(topologyTemplate), function(response) {
          // Response contains topology id
          if (_.defined(response.data)) {
          // the id is in form: archiveName:archiveVersion
            var tokens = response.data.trim().split(':');
            if (tokens.length > 1) {
              var archiveName = tokens[0];
              var archiveVersion = tokens[1];
              $scope.openTopology(archiveName, archiveVersion);
            }
          }
        });
      };

      $scope.openNewTopologyTemplate = function(topology) {
        var modalConfiguration = {
          templateUrl: 'views/topologytemplates/topology_template_new.html',
          controller: 'NewTopologyTemplateCtrl',
          resolve: { topology: function() { return topology; } }
        };

        var modalInstance = $uibModal.open(modalConfiguration);
        modalInstance.result.then(function(topologyTemplate) {
          topologyTemplate.workspace = $scope.selectedWorkspaceForUpload.id;
          $scope.createTopologyTemplate(topologyTemplate);
        });
      };

      var processedWorkspaces = workspaceServices.process(workspaces, 'ARCHITECT');
      $scope.defaultFilters = {};
      $scope.staticFacets = processedWorkspaces.staticFacets;
      $scope.writeWorkspaces = processedWorkspaces.writeWorkspaces;

      $scope.selectWorkspaceForUpload = function (workspace) {
        $scope.selectedWorkspaceForUpload = workspace;
        $scope.selectedWorkspaceForUploadData = {
          workspace: $scope.selectedWorkspaceForUpload.id
        };
      };
      if($scope.writeWorkspaces.length>0) {
        $scope.selectWorkspaceForUpload($scope.writeWorkspaces[0]);
      }
    }
  ]);
});
