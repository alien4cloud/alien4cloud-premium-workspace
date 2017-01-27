define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');
  require('scripts/workspace/directives/display_workspace');
  require('scripts/workspace/services/workspace_service');
  require('scripts/workspace/directives/promotion_impact_ctrl');
  var prefixer = require('scripts/plugin-url-prefixer');

  modules.get('alien4cloud-premium-workspace', []).controller('CsarPromotionController', ['$scope', 'workspaceServices', '$uibModal',
    function ($scope, workspaceServices, $uibModal) {
      // Add methods to handle promotion modal
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };
      $scope.defaultFilters = {
        status: 'INIT'
      };

      $scope.openPromoteModal = function (promotionRequest) {
        var modalInstance = $uibModal.open({
          templateUrl: prefixer.prefix('views/workspace/promotion_impact.html'),
          controller: 'PromotionImpactController',
          resolve: {
            impact: function () {
              return workspaceServices.promotionImpact.get({
                csarName: promotionRequest.csarName,
                csarVersion: promotionRequest.csarVersion,
                targetWorkspace: promotionRequest.targetWorkspace
              }).$promise.then(function (response) {
                return response.data;
              });
            }
          }
        });
        modalInstance.result.then(function () {
          workspaceServices.promotions.save({
            id: promotionRequest.id,
            status: 'ACCEPTED'
          }, function () {
            $scope.searchConfig.service.search();
          });
        });
      };

      $scope.refusePromotion = function (promotionRequest) {
        workspaceServices.promotions.save({
          id: promotionRequest.id,
          status: 'REFUSED'
        }, function () {
          $scope.searchConfig.service.search();
        });
      };
    }]);
});
