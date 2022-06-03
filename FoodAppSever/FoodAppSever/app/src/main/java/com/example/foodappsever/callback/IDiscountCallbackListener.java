package com.example.foodappsever.callback;

import com.example.foodappsever.model.DiscountModel;

import java.util.List;

public interface IDiscountCallbackListener {
    void onListDiscountLoadSuccess(List<DiscountModel> discountModelList);
    void onListDiscountLoadFailed(String message);
}
