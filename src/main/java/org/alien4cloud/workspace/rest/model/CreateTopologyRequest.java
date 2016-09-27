package org.alien4cloud.workspace.rest.model;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Request to create a new topology with workspace.
 */
@Getter
@Setter
public class CreateTopologyRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String version;
    private String workspace;
    private String description;
    private String fromTopologyId;
}
