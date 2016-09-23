define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');


  var PromotionImpactCtrl = ['$scope', '$modalInstance', 'impact',
    function ($scope, $modalInstance, impact) {
      $scope.impact = impact;
      $scope.ok = function () {
        $modalInstance.close();
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
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
    }
  ];

  modules.get('alien4cloud-premium-workspace', []).controller('CsarWorkspaceController', ['$scope', 'workspaceServices', '$modal',
    function ($scope, workspaceServices, $modal) {

      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

      $scope.calculateImpact = function (csar, workspace) {
        var modalInstance = $modal.open({
          templateUrl: 'views/workspace/promotion_impact.html',
          controller: PromotionImpactCtrl,
          resolve: {
            impact: function () {
              return workspaceServices.promotionImpact.get({
                csarName: csar.name,
                csarVersion: csar.version,
                targetWorkspace: workspace.id
              }).$promise.then(function (response) {
                return response.data;
              });
            }
          }
        });
        modalInstance.result.then(function () {
          workspaceServices.promotions.save({
            csarName: csar.name,
            csarVersion: csar.version,
            targetWorkspace: workspace.id
          }, function () {
            $scope.searchConfig.service.search();
          });
        });
      };

    }]);
});
