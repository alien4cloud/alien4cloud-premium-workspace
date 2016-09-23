define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  
  modules.get('alien4cloud-premium-workspace', []).controller('CsarWorkspaceController', ['$scope',
    function ($scope) {

      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };

    }]);
});
