package com.example.foodappsever.model.EventBus;

import com.example.foodappsever.model.AddonModel;

public class SelectAddonModel {
    private AddonModel addonModel;

    public SelectAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }

    public AddonModel getAddonModel() {
        return addonModel;
    }

    public void setAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }
}
