package com.example.foodappsever.ui.category;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodappsever.callback.ICategoryCallbackListener;
import com.example.foodappsever.common.Common;
import com.example.foodappsever.model.CategoryModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CategoryViewModel extends ViewModel implements ICategoryCallbackListener {

    private MutableLiveData<List<CategoryModel>> categoryListMultable;
    private MutableLiveData<String> messageError=new MutableLiveData<>();
    private ICategoryCallbackListener categoryCallbackListener;

    public CategoryViewModel() {
        categoryCallbackListener=this;
    }

    public MutableLiveData<List<CategoryModel>> getCategoryListMultable() {
        if(categoryListMultable==null)
        {
            categoryListMultable=new MutableLiveData<>();
            messageError=new MutableLiveData<>();
            loadCategories();
        }
        return categoryListMultable;
    }

    public void loadCategories() {
        List<CategoryModel> tempList=new ArrayList<>();
        Log.d("RES",Common.currentServerUser.getRestaurant());
        DatabaseReference categoryRef= FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.CATEGORY_REF);
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    for(DataSnapshot itemSnapShot:snapshot.getChildren()){
                        CategoryModel categoryModel=itemSnapShot.getValue(CategoryModel.class);
                        categoryModel.setMenu_id(itemSnapShot.getKey());
                        tempList.add(categoryModel);
                    }
                    if(tempList.size()>0)
                        categoryCallbackListener.onCategoryLoadSuccess(tempList);
                    else
                        categoryCallbackListener.onCategoryLoadFailed("Category empty");
                }
                else
                    categoryCallbackListener.onCategoryLoadFailed("Category not exists!");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                categoryCallbackListener.onCategoryLoadFailed(error.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onCategoryLoadSuccess(List<CategoryModel> categoryModelList) {
       categoryListMultable.setValue(categoryModelList);
    }

    @Override
    public void onCategoryLoadFailed(String message) {
       messageError.setValue(message);
    }
}