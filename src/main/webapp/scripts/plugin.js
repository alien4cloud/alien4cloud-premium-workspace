// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var states = require('states');
  var prefixer = require('scripts/plugin-url-prefixer');
  var plugins = require('plugins');
  require('scripts/workspace/services/workspace_service');

  // override component list to have the list of workspaces
  states.merge('components.list', {
    resolve: {
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
    }
  });

  //register translations
  plugins.registerTranslations(prefixer.prefix('data/languages/workspace-'));
});
