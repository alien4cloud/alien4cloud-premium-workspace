package org.alien4cloud.workspace.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Sets;

import alien4cloud.security.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Workspace {
    /** Scope of the workspace, it can be either user, application or null for global workspace. */
    private Scope scope;

    /** Name of the workspace identify it within the scope */
    @NotNull
    private String name;
    /** Use role on the workspace. */
    private Set<Role> roles = Sets.newHashSet();

    public String getId() {
        return scope + (name != null ? (":" + name) : "");
    }
}