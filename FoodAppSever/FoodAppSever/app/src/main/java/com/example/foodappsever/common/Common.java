package com.example.foodappsever.common;

import com.example.foodappsever.model.CategoryModel;
import com.example.foodappsever.model.FoodModel;
import com.example.foodappsever.model.ServerUserModel;

public class Common {
    public static final String SERVER_REF = "Server";
    public static final String CATEGORY_REF = "Category";
    public static ServerUserModel currentServerUser;
    public static CategoryModel categorySelected;
    public static final int DEFAULT_COLUMN_COUNT = 0;
    public static final int FULL_WIDTH_COLUMN = 1;
    public static FoodModel selectedFood;
}
