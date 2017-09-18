define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('alien4cloud-premium-workspace', []).controller('PromotionImpactController', ['$scope', '$uibModalInstance', 'impact',
    function ($scope, $uibModalInstance, impact) {
      $scope.impact = impact;
      $scope.ok = function () {
        $uibModalInstance.close();
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      $scope.getResourceVersion = function (resourceId) {
        var indexOfTwoPoint = resourceId.indexOf(':');
        if (indexOfTwoPoint >= 0) {
          return resourceId.substring(indexOfTwoPoint + 1);
        } else {
          return '';
        }
      };

      $scope.isPromotionPossible = Object.keys($scope.impact.currentUsages).length === 0;
    }]);
});