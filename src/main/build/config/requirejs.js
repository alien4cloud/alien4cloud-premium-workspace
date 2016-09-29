'use strict';

// Require js optimizer configuration
module.exports = function (grunt) {
  var config = {
    dist: {
      options: {
        appDir: '<%= yeoman.app %>',
        dir: '<%= yeoman.dist %>',
        mainConfigFile:'./src/main/webapp/scripts/plugin-require.config.js',
        modules:[
          {
            name:'alien4cloud-premium-workspace'
          }
        ],
        paths: {
          'states': 'empty:',
          'modules': 'empty:',
          'plugins': 'empty:',
          'lodash': 'empty:',
          'angular': 'empty:',
          'jquery': 'empty:',
          'componentService': 'empty:',
          'scripts/common/directives/pagination': 'empty:',
          'scripts/common/directives/facet_search_panel': 'empty:',
          'scripts/components/services/component_services': 'empty:',
          'scripts/common/directives/date_time_form': 'empty:',
          'scripts/common/directives/empty_place_holder': 'empty:',
          'scripts/applications/services/application_services': 'empty:',
          'scripts/applications/services/application_environment_services': 'empty:'
        },
        namespace: 'alien4CloudPremiumWorkspace',

        baseUrl: '.',

        keepBuildDir: true,

        optimize: 'uglify',
        // optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        fileExclusionRegExp: /^(bower_components|api-doc|data|images|js-lib|META-INF|styles|views)$/,

        removeCombined: true,
        findNestedDependencies: true,
        normalizeDirDefines: 'all',
        inlineText: true,
        skipPragmas: true,

        done: function (done, output) {
          var analysis = require('rjs-build-analysis');
          var tree = analysis.parse(output);
          var duplicates = analysis.duplicates(tree);

          if (duplicates.length > 0) {
            grunt.log.subhead('Duplicates found in requirejs build:');
            grunt.log.warn(duplicates);
            return done(new Error('r.js built duplicate modules, please check the excludes option.'));
          } else {
            var relative = [];
            var bundles = tree.bundles || [];
            bundles.forEach(function (bundle) {
              bundle.children.forEach(function (child) {
                if (child.match(/\.\//)) {
                  relative.push(child + ' is relative to ' + bundle.parent);
                }
              });
            });

            if (relative.length) {
              grunt.log.subhead('Relative modules found in requirejs build:');
              grunt.log.warn(relative);
              return done(new Error('r.js build contains relative modules, duplicates probably exist'));
            }
          }

          done();
        }
      }
    }
  };
  return config;
};
