package com.example.foodappsever.callback;

import com.example.foodappsever.model.OrderModel;

import java.util.List;

public interface IOrderCallbackListener {
    void onOrderLoadSuccess(List<OrderModel> orderModelList);
    void onOrderLoadFailed(String message);
}
