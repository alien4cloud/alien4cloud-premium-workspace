define(function (require) {
  'use strict';
  var prefixer = require('scripts/plugin-url-prefixer');
  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/workspace/directives/csar_workspace.js');

  // register plugin state
  states.state('components.workspace', {
    url: '/workspace',
    templateUrl: prefixer.prefix('views/workspace/workspace.html'),
    controller: 'WorkspaceController',
    menu: {
      id: 'cm.components.workspace',
      state: 'components.workspace',
      key: 'NAVBAR.MENU_WORKSPACE',
      icon: 'fa fa-folder-open-o',
      priority: 10
    }
  });

  modules.get('alien4cloud-premium-workspace', []).controller('WorkspaceController', [
    function () {
    }
  ]);
});
