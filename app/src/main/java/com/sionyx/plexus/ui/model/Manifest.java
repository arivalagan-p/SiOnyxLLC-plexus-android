package com.sionyx.plexus.ui.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Manifest {
    public String getCameraModel() {
        return camera_Model;
    }

    public void setCameraModel(String camera_Model) {
        this.camera_Model = camera_Model;
    }

    public Versions getVersions() {
        return versions;
    }

    public void setVersions(Versions versions) {
        this.versions = versions;
    }

    public ArrayList<String> getSequence() {
        return sequence;
    }

    public void setSequence(ArrayList<String> sequence) {
        this.sequence = sequence;
    }

    public String getReleaseNotes() {
        return release_notes;
    }

    public void setReleaseNotes(String release_notes) {
        this.release_notes = release_notes;
    }

    @SerializedName("camera_Model")
    private String camera_Model;
    @SerializedName("versions")
    private Versions versions;
    @SerializedName("sequence")
    private ArrayList<String> sequence;
    @SerializedName("release_notes")
    private String release_notes;
}
