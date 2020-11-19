define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var prefixer = require('scripts/plugin-url-prefixer');

  require('scripts/workspace/controllers/promotion_impact_ctrl');

  // override archive list state config to have the list of workspaces
  states.merge('catalog.archives.list', {
    controller: 'WorkspaceArchiveListCtrl',
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.resource.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    }
  });

  modules.get('a4c-catalog', ['ui.router', 'a4c-auth', 'a4c-common']).controller('WorkspaceArchiveListCtrl', ['$controller', '$scope', '$uibModal', '$state', 'csarService', '$translate', 'toaster', 'workspaceServices', 'workspaces', 'authService', 'searchServiceFactory',
    function ($controller, $scope, $uibModal, $state, csarService, $translate, toaster, workspaceServices, workspaces, authService, searchServiceFactory) {
      // Apply opensource controller first
      $controller('ArchivesListCtrl', {
        $scope: $scope,
        $state: $state,
        csarService: csarService,
        $translate: $translate,
        toaster: toaster,
        authService: authService,
        searchServiceFactory: searchServiceFactory
      });

      var processedWorkspaces = workspaceServices.process(workspaces, ['COMPONENTS_MANAGER', 'ARCHITECT']);
      // $scope.staticFacets = processedWorkspaces.staticFacets;

      //onSearchCompleted callback
      $scope.queryManager.onSearchCompleted = function(searchResponse){
        if(_.defined(searchResponse.data)){
          //processSearchResponse(searchResponse.data);
          $scope.queryManager.searchResult = searchResponse.data;
        }else{
          $scope.queryManager.searchResult = undefined;
        }
      };

      //override the search service
      $scope.searchService = searchServiceFactory('rest/latest/workspaces/csars/search', false, $scope.queryManager, 20);

      //populate a given scope with the necessary tools to handle workspace
      $scope.getWorkspaceSpecifics=function(scope){
          scope.uploadWorkspaceTemplateUrl = prefixer.prefix('views/workspace/upload_workspace_snippet.html');
          scope.archiveWorkspaceTemplateUrl = prefixer.prefix('views/workspace/archive_workspace_promotion_snippet.html');
          scope.writeWorkspaces = processedWorkspaces.writeWorkspaces;

          scope.selectWorkspaceForUpload = function (workspace) {
            scope.selectedWorkspaceForUpload = workspace;
          };
          if(scope.writeWorkspaces.length>0) {
            scope.selectWorkspaceForUpload(scope.writeWorkspaces[0]);
          }
      };

      $scope.getWorkspaceSpecifics($scope);

      $scope.getRequestData = function(scope) {
        return {
          workspace: scope.selectedWorkspaceForUpload.id
        };
      };

      // retrieve the possible promotion target workspaces for a given csar
      $scope.getPromotionTargets = function(csar) {
        $scope.currentAvailablePromotionTargets = [];
        workspaceServices.promotionTargets.get({csarId: csar.name + ":" + csar.version}, function(success) {
          if (_.defined(success.data)) {
            console.log(success.data);
            $scope.currentAvailablePromotionTargets = success.data.availablePromotionTargets;
          }
        })
      }

      // calculate impact of a csar promotion into a given workspace
      $scope.calculateImpact = function (csar, workspace) {
        var modalInstance = $uibModal.open({
          templateUrl: prefixer.prefix('views/workspace/promotion_impact.html'),
          controller: 'PromotionImpactController',
          size: 'lg',
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
              // $scope.searchConfig.service.search();
              csar.workspace = workspace.id;
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

    }
  ]);
});
