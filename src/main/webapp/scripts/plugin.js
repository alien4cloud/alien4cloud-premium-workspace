// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var prefixer = require('scripts/plugin-url-prefixer');
  var plugins = require('plugins');
  require('scripts/workspace/services/workspace_service');

  require('scripts/workspace/controllers/catalog/archive_list');
  require('scripts/workspace/controllers/catalog/components_list');
  require('scripts/workspace/controllers/catalog/catalog_archives-promotion');

  //register translations
  plugins.registerTranslations(prefixer.prefix('data/languages/workspace-'));
});
