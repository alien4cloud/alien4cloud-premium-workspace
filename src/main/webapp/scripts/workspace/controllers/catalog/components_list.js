// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/workspace/controllers/promotion_impact_ctrl');

  // override component list state config to have the list of workspaces
  states.merge('catalog.components.list', {
    controller: 'WorkspaceComponentsListCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });

  modules.get('a4c-catalog', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceComponentsListCtrl', ['$controller', '$scope', 'resizeServices', '$state', 'defaultFilters', 'badges', 'workspaceServices', 'workspaces',
    function ($controller, $scope, resizeServices, $state, defaultFilters, badges, workspaceServices, workspaces) {
      // Apply opensource controller first
      $controller('ComponentsListCtrl', {
        $scope: $scope,
        $state: $state,
        resizeServices: resizeServices,
        defaultFilters: defaultFilters,
        badges: badges
      });

      var processedWorkspaces = workspaceServices.process(workspaces, ['COMPONENTS_MANAGER']);
      // add static facets to allow filtering components on workspaces
      $scope.staticFacets = processedWorkspaces.staticFacets;

    }
  ]);
});
