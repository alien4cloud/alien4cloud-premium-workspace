package org.alien4cloud.workspace.model;

import alien4cloud.common.AlienConstants;

public enum Scope {

    USER("user"), GROUP("group"), APPLICATION("application"), GLOBAL(AlienConstants.GLOBAL_WORKSPACE_ID);

    private String name;

    Scope(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
