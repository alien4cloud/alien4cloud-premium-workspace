define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/common/directives/facet_search_panel');
  require('scripts/common/directives/pagination');
  modules.get('alien4cloud-premium-workspace', []).controller('CsarWorkspaceController', ['$scope',
    function ($scope) {

      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

    }]);
});
