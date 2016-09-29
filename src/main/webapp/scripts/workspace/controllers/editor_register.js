// Override the editor registration to add workspaces filtering on the type browser
// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // add the workspaces resolver
  states.merge('topologycatalog.csar', {
    resolve: {
      workspaces: ['workspaceServices', function (workspaceServices) {
        // FIXME get that with topology context
        return workspaceServices.resource.get().$promise.then(function (response) {
          var processedWorkspaces = workspaceServices.process(response.data);
          return processedWorkspaces.readWorkspaces;
        });
      }]
    }
  });
});
