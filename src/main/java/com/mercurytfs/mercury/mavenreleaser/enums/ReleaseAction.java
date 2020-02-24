package com.mercurytfs.mercury.mavenreleaser.enums;

public enum ReleaseAction {

    RELEASE("release"),
    PREPARE("prepare"),
    SOURCES("sources");

    private String action;

    ReleaseAction(String action){this.action=action;}

    public String getAction(){
        return action;
    }
    public static ReleaseAction getReleaseAction(String match){
        for (ReleaseAction action : ReleaseAction.values())
            if (action.getAction().equals(match))
                return action;
        return null;
    }
}
