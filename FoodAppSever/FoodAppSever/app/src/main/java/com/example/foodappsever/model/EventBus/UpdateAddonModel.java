package com.example.foodappsever.model.EventBus;

import com.example.foodappsever.model.AddonModel;

import java.util.List;

public class UpdateAddonModel {
    private List<AddonModel> addonModels;

    public UpdateAddonModel() {

    }

    public List<AddonModel> getAddonModels() {
        return addonModels;
    }

    public void setAddonModels(List<AddonModel> addonModels) {
        this.addonModels = addonModels;
    }
}
