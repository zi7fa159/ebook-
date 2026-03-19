package com.alsclone.astrostacker;

public class StretchParams {
    public float scale;
    public float midFactor;
    public float blackOffset;
    public float whiteClip;
    public boolean useAuto;

    public StretchParams() {
        this.scale = 1.0f;
        this.midFactor = 1.0f;
        this.blackOffset = 0.0f;
        this.whiteClip = 16383.0f;
        this.useAuto = true;
    }
}
