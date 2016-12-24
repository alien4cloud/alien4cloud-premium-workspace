package org.alien4cloud.workspace.model;

import alien4cloud.utils.AlienConstants;

public enum Scope {
    USER("user"), GROUP("group"), APPLICATION(AlienConstants.APP_WORKSPACE_PREFIX), GLOBAL(AlienConstants.GLOBAL_WORKSPACE_ID);

    private String name;

    Scope(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Scope fromName(String name) {
        for (Scope scope : values()) {
            if (scope.name.equals(name)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Scope with name [" + name + "] do not exist");
    }
}