define(function (require) {
  'use strict';

  var modules = require('modules');
  var prefixer = require('scripts/plugin-url-prefixer');
  require('scripts/workspace/directives/csar_workspace_ctrl.js');
  require('scripts/workspace/directives/display_workspace.js');

  modules.get('alien4cloud-premium-workspace').directive('csarWorkspace', function () {
    return {
      templateUrl: prefixer.prefix('views/workspace/csar_workspace.html'),
      restrict: 'E',
      controller: 'CsarWorkspaceController'
    };
  });
});
