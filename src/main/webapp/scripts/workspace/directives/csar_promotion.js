define(function (require) {
  'use strict';

  var modules = require('modules');
  var prefixer = require('scripts/plugin-url-prefixer');
  require('scripts/workspace/directives/csar_promotion_ctrl');

  modules.get('alien4cloud-premium-workspace').directive('csarPromotion', function () {
    return {
      templateUrl: prefixer.prefix('views/workspace/csar_promotion.html'),
      restrict: 'E',
      controller: 'CsarPromotionController',
      scope: true
    };
  });
});
