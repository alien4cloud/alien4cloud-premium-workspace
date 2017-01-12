// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  
  // override component list to have the list of workspaces
  states.merge('components.csars.list', {
    controller: 'WorkspaceCsarListCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });
  
  modules.get('a4c-components', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceCsarListCtrl', ['$controller', '$scope', '$uibModal', '$state', 'csarService', '$translate', 'toaster', 'workspaceServices', 'workspaces', 'authService',
    function ($controller, $scope, $uibModal, $state, csarService, $translate, toaster, workspaceServices, workspaces, authService) {
      // Apply opensource controller first
      $controller('CsarListCtrl', {
        $scope: $scope,
        $uibModal: $uibModal,
        $state: $state,
        csarService: csarService,
        $translate: $translate,
        toaster: toaster,
        authService: authService
      });
      
      var processedWorkspaces = workspaceServices.process(workspaces, ['COMPONENTS_MANAGER', 'ARCHITECT']);
      $scope.staticFacets = processedWorkspaces.staticFacets;
      $scope.writeWorkspaces = _.map(processedWorkspaces.writeWorkspaces, function (writeWorkspace) {
        return writeWorkspace.id;
      });
    }
  ]);
});
