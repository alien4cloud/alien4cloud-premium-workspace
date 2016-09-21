// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var prefixer = require('scripts/plugin-url-prefixer');
  require('scripts/workspace/directives/display_workspace.js');

  // override component list to have the list of workspaces
  states.merge('components.list', {
    templateUrl: prefixer.prefix('views/workspace/component_list.html'),
    controller: 'WorkspaceSearchComponentCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return response.data;
        });
      }],
    }
  });

  modules.get('a4c-components', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceSearchComponentCtrl', ['$scope', '$state', 'resizeServices', 'defaultFilters', 'badges', 'workspaceServices', 'workspaces',
    function ($scope, $state, resizeServices, defaultFilters, badges, workspaceServices, workspaces) {
      $scope.defaultFilters = defaultFilters;
      $scope.badges = badges;

      var processedWorkspaces = workspaceServices.process(workspaces, 'COMPONENTS_MANAGER');
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

      $scope.uploadSuccessCallback = function (data) {
        $scope.refresh = data;
      };

      $scope.openComponent = function (component) {
        $state.go('components.detail', {id: component.id});
      };

      function onResize(width, height) {
        $scope.heightInfo = {height: height};
        $scope.widthInfo = {width: width};
        $scope.$digest();
      }

      // register for resize events
      window.onresize = function () {
        $scope.onResize();
      };

      resizeServices.register(onResize, 0, 0);
      $scope.heightInfo = {height: resizeServices.getHeight(0)};
      $scope.widthInfo = {width: resizeServices.getWidth(0)};
    }
  ]);
});
