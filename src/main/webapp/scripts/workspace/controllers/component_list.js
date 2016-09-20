// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var prefixer = require('scripts/plugin-url-prefixer');

  // override component list to have the list of workspaces
  states.merge('components.list', {
    templateUrl: prefixer.prefix('views/workspace/component_list.html'),
    controller: 'WorkspaceSearchComponentCtrl',
    resolve: {
      workspacesForUpload: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.upload.get().$promise.then(function (response) {
          return response.data;
        });
      }],
      workspacesForSearch: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.search.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });

  modules.get('a4c-components', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceSearchComponentCtrl', ['$scope', '$state', 'resizeServices', 'defaultFilters', 'badges', 'workspacesForUpload', 'workspacesForSearch',
    function ($scope, $state, resizeServices, defaultFilters, badges, workspacesForUpload, workspacesForSearch) {
      $scope.defaultFilters = defaultFilters;
      $scope.badges = badges;

      $scope.workspacesForUpload = workspacesForUpload;
      $scope.workspacesForSearch = workspacesForSearch;

      $scope.staticFacets = {};

      var defaultWorkspaces = [];
      _.each(workspacesForSearch, function(searchWorkspace) {
        if(_.undefined($scope.staticFacets.workspace)) {
          $scope.staticFacets.workspace = [];
        }
        $scope.staticFacets.workspace.push({facetValue: searchWorkspace.id, count: ''});
        defaultWorkspaces.push(searchWorkspace.id);
      });
      if(defaultWorkspaces.length > 0) {
        $scope.defaultFilters.workspace =  defaultWorkspaces;
      }

      $scope.selectWorkspaceForUpload = function (workspace) {
        $scope.selectedWorkspaceForUpload = workspace;
        $scope.selectedWorkspaceForUploadData = {
          workspace: $scope.selectedWorkspaceForUpload.id
        };
      };
      $scope.selectWorkspaceForUpload($scope.workspacesForUpload[0]);

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
