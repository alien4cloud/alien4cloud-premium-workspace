package org.alien4cloud.workspace.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Workspace {

    /**
     * The global workspace
     */
    public static final Workspace GLOBAL = new Workspace(Scope.GLOBAL, null);

    /**
     * Scope of the workspace, it can be either user, application or null for global workspace
     */
    private Scope scope;

    /**
     * Name of the workspace identify it within the scope
     */
    @NotNull
    private String name;

    public String getId() {
        return scope + (name != null ? (":" + name) : "");
    }
}
