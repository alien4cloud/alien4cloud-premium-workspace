define(function (require) {
  'use strict';
  var prefixer = require('scripts/plugin-url-prefixer');
  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');

  // register plugin state
  states.state('components.workspace', {
    url: '/workspace',
    templateUrl: prefixer.prefix('views/workspace/workspace.html'),
    controller: 'WorkspaceController',
    menu: {
      id: 'cm.components.workspace',
      state: 'components.workspace',
      key: 'NAVBAR.MENU_WORKSPACE',
      icon: 'fa fa-folder-open-o',
      priority: 10
    },
    resolve: {
      readAccessWorkspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.upload.get().$promise.then(function (response) {
          return response.data;
        });
      }],
      writeAccessWorkspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.search.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });

  modules.get('alien4cloud-premium-workspace', []).controller('WorkspaceController', ['$scope', 'readAccessWorkspaces', 'writeAccessWorkspaces',
    function ($scope, readAccessWorkspaces, writeAccessWorkspaces) {
      $scope.readAccessWorkspaces = readAccessWorkspaces;
      $scope.writeAccessWorkspaces = writeAccessWorkspaces;
    }
  ]);
});
