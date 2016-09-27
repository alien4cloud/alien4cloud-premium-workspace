define(function (require) {
  'use strict';
  var prefixer = require('scripts/plugin-url-prefixer');
  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/workspace/directives/csar_workspace');
  require('scripts/workspace/directives/csar_promotion');

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
    }
  });

  modules.get('alien4cloud-premium-workspace', []).controller('WorkspaceController', ['$scope',
    function ($scope) {

      $scope.selectWorkspaceTab = function () {
        $scope.workspaceActive = true;
        $scope.promotionActive = false;
      };

      $scope.selectPromotionTab = function () {
        $scope.workspaceActive = false;
        $scope.promotionActive = true;
      };

      $scope.selectWorkspaceTab();
    }
  ]);
});
