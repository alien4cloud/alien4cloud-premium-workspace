// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var prefixer = require('scripts/plugin-url-prefixer');
  var plugins = require('plugins');
  require('scripts/workspace/controllers/component_list');
  require('scripts/workspace/controllers/topology_list');
  require('scripts/workspace/services/workspace_service');

  //register translations
  plugins.registerTranslations(prefixer.prefix('data/languages/workspace-'));
});
