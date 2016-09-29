define(function (require) {
  'use strict';

  var modules = require('modules');
  var prefixer = require('scripts/plugin-url-prefixer');

  modules.get('alien4cloud-premium-workspace').directive('displayWorkspace', function () {
    return {
      templateUrl: prefixer.prefix('views/workspace/display_workspace.html'),
      restrict: 'E',
      scope: {
        'workspaceId': '@'
      },
      controller: 'DisplayWorkspaceController'
    };
  });

  modules.get('alien4cloud-premium-workspace', []).controller('DisplayWorkspaceController', ['$scope',
    function ($scope) {
      $scope.$watch('workspaceId', function (workspaceId) {
        var indexOfTwoPoint = workspaceId.indexOf(':');
        if (indexOfTwoPoint > 0) {
          $scope.workspace = {
            scope: workspaceId.substring(0, indexOfTwoPoint),
            name: workspaceId.substring(indexOfTwoPoint + 1, workspaceId.length)
          };
        } else {
          $scope.workspace = {
            scope: workspaceId
          };
        }
      });
    }]);
});