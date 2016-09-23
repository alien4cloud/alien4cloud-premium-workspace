package org.alien4cloud.workspace.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {

    @NotNull
    private String csarName;

    @NotNull
    private String csarVersion;

    @NotNull
    private String targetWorkspace;
}
