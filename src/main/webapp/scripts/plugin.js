// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var states = require('states');
  var prefixer = require('scripts/plugin-url-prefixer');
  var plugins = require('plugins');
  require('scripts/workspace/services/workspace_service');

  // override component list to have the list of workspaces
  states.state('components.list', {
    url: '/list',
    templateUrl: 'views/components/component_list.html',
    controller: 'SearchComponentCtrl',
    resolve: {
      defaultFilters: [function () {
        return {};
      }],

      // badges to display. object with the following properties:
      //   name: the name of the badge
      //   tooltip: the message to display on the tooltip
      //   imgSrc: the image to display
      //   canDisplay: a funtion to decide if the badge is displayable for a component. takes as param the component and must return true or false.
      //   onClick: callback for the click on the displayed badge. takes as param: the component, the $state object
      badges: [function () {
        return [];
      }],
      workspacesForUpload: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.upload.get().$promise.then(function (response) {
          return response.data;
        });
      }],
      workspacesForSearch: ['workspaceServices', function (workspaceServices) {
        return workspaceServices.search.get().$promise.then(function (response) {
          return response.data;
        });
      }]
    },
    menu: {
      id: 'cm.components.list',
      state: 'components.list',
      key: 'NAVBAR.MENU_COMPONENTS',
      icon: 'fa fa-cubes',
      priority: 10
    }
  });

  //register translations
  plugins.registerTranslations(prefixer.prefix('data/languages/workspace-'));
});
