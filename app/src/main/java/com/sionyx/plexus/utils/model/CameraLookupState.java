package com.sionyx.plexus.utils.model;

public class CameraLookupState {

    private enum LookupStateForCamera {
        NONE,
        LOOKING,
        FOUND,
        NOT_FOUND
    }

    private LookupStateForCamera currentState;

    public CameraLookupState() {
        this.currentState = LookupStateForCamera.NONE;
    }

    public LookupStateForCamera getCurrentState() {
        return currentState;
    }
    public boolean isNotFound() {
        return currentState == LookupStateForCamera.NOT_FOUND;
    }
    public boolean isFound() {
        return currentState == LookupStateForCamera.FOUND;
    }

    public boolean isLooking() {
        return currentState == LookupStateForCamera.LOOKING;
    }
    public boolean isNone() {
        return currentState == LookupStateForCamera.NONE;
    }

    public void transitionToLooking() {
        switch (currentState) {
            case NONE:
            case FOUND:
            case NOT_FOUND:
                currentState = LookupStateForCamera.LOOKING;
                System.out.println("Transitioned to LOOKING state");
                break;
            case LOOKING:
                System.out.println("Already in LOOKING state");
                break;
        }
    }

    public void transitionToFound() {
        switch (currentState) {
            case LOOKING:
                currentState = LookupStateForCamera.FOUND;
                System.out.println("Transitioned to FOUND state");
                break;
            case NONE:
            case FOUND:
                System.out.println("Cannot transition to FOUND from " + currentState + " state");
                break;
            case NOT_FOUND:
                System.out.println("Cannot transition to FOUND from NOT_FOUND state");
                break;
        }
    }

    public void transitionToNotFound() {
        switch (currentState) {
            case LOOKING:
                currentState = LookupStateForCamera.NOT_FOUND;
                System.out.println("Transitioned to NOT_FOUND state");
                break;
            case NONE:
            case FOUND:
                System.out.println("Cannot transition to NOT_FOUND from " + currentState + " state");
                break;
            case NOT_FOUND:
                System.out.println("Already in NOT_FOUND state");
                break;
        }
    }
    public void transitionToNone() {
        currentState = LookupStateForCamera.NONE;
        System.out.println("Transitioned to NONE state");
    }
}
