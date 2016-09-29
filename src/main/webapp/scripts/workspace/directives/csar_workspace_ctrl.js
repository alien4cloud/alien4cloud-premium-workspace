define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');
  require('scripts/workspace/directives/display_workspace');
  require('scripts/workspace/services/workspace_service');
  require('scripts/workspace/directives/promotion_impact_ctrl');

  modules.get('alien4cloud-premium-workspace', []).controller('CsarWorkspaceController', ['$scope', 'workspaceServices', '$modal', 'toaster', '$translate',
    function ($scope, workspaceServices, $modal, toaster, $translate) {
      // Add methods to handle promotion modal
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };
      $scope.calculateImpact = function (csar, workspace) {
        var modalInstance = $modal.open({
          templateUrl: 'views/workspace/promotion_impact.html',
          controller: 'PromotionImpactController',
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
          }, function (response) {
            if (response.data.status === 'ACCEPTED') {
              $scope.searchConfig.service.search();
              toaster.pop(
                  'success',
                  $translate.instant('WORKSPACES.PROMOTION.MOVE.TITLE'),
                  $translate.instant('WORKSPACES.PROMOTION.MOVE.DONE'),
                  4000, 'trustedHtml', null
              );
            } else {
              toaster.pop(
                  'success',
                  $translate.instant('WORKSPACES.PROMOTION.REQUEST.TITLE'),
                  $translate.instant('WORKSPACES.PROMOTION.REQUEST.DONE'),
                  4000, 'trustedHtml', null
              );
            }
          });
        });
      };
    }]);
});
