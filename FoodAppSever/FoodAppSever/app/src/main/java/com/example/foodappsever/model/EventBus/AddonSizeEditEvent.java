package com.example.foodappsever.model.EventBus;

public class AddonSizeEditEvent {
    private boolean isAddon;
    private int pos;

    public AddonSizeEditEvent(boolean isAddon, int pos) {
        this.isAddon = isAddon;
        this.pos = pos;
    }

    public boolean isAddon() {
        return isAddon;
    }

    public void setAddon(boolean addon) {
        isAddon = addon;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
