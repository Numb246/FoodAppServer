package com.example.foodappsever.ui.order;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodappsever.R;
import com.example.foodappsever.TrackingOrderActivity;
import com.example.foodappsever.adapter.MyOrderAdapter;
import com.example.foodappsever.adapter.MyShipperSelectionAdapter;
import com.example.foodappsever.callback.IShipperLoadCallbackListener;
import com.example.foodappsever.common.BottomSheetOrderFragment;
import com.example.foodappsever.common.Common;
import com.example.foodappsever.common.MySwipeHelper;
import com.example.foodappsever.model.EventBus.ChangeMenuClick;
import com.example.foodappsever.model.EventBus.LoadOrderEvent;
import com.example.foodappsever.model.FCMSenData;
import com.example.foodappsever.model.OrderModel;
import com.example.foodappsever.model.ShipperModel;
import com.example.foodappsever.model.ShippingOrderModel;
import com.example.foodappsever.model.TokenModel;
import com.example.foodappsever.remote.IFCMService;
import com.example.foodappsever.remote.RetrofitFCMClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class OrderFragment extends Fragment implements IShipperLoadCallbackListener {
    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    @BindView(R.id.txt_order_filter)
    TextView txt_order_filter;

    RecyclerView recycler_shipper;

    Unbinder unbinder;
    LayoutAnimationController layoutAnimationController;
    MyOrderAdapter adapter;

    private CompositeDisposable compositeDisposable=new CompositeDisposable();
    private IFCMService ifcmService;
    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    private OrderViewModel orderViewModel;
    private MyShipperSelectionAdapter myShipperSelectedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        orderViewModel =
                new ViewModelProvider(this).get(OrderViewModel.class);


        View root = inflater.inflate(R.layout.fragment_order,container,false);
        unbinder= ButterKnife.bind(this,root);
        initViews();
        orderViewModel.getMessageError().observe(getViewLifecycleOwner(),s -> {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        });
        orderViewModel.getOrderModelMutableLiveData().observe(getViewLifecycleOwner(),orderModels -> {
            if(orderModels!=null)
            {
                adapter=new MyOrderAdapter(getContext(),orderModels);
                recycler_order.setAdapter(adapter);
                recycler_order.setLayoutAnimation(layoutAnimationController);

                updateTextCounter();
            }
        });
        return root;
    }

    private void initViews() {
        ifcmService= RetrofitFCMClient.getInstance().create(IFCMService.class);
        shipperLoadCallbackListener=this;
        setHasOptionsMenu(true);
        recycler_order.setHasFixedSize(true);
        recycler_order.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController= AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        DisplayMetrics displayMetrics=new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width=displayMetrics.widthPixels;

        MySwipeHelper mySwipeHelper=new MySwipeHelper(getContext(),recycler_order,width/6) {
            @Override
            public void instantialMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(),"Directions",30,0, Color.parseColor("#9b0000"),
                        pos -> {
                    OrderModel orderModel=((MyOrderAdapter)recycler_order.getAdapter())
                            .getItemAtPosition(pos);
                    if(orderModel.getOrderStatus()==1)
                    {
                        Common.currentOrderSelected=orderModel;
                        startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                    }
                    else
                    {
                        Toast.makeText(getContext(),new StringBuilder("Your order is ")
                                .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                                .append(". So you can't track directions"),Toast.LENGTH_SHORT).show();
                    }
                        }));

                buf.add(new MyButton(getContext(),"Call",30,0, Color.parseColor("#560027"),
                        pos -> {
                            Dexter.withActivity(getActivity())
                                    .withPermission(Manifest.permission.CALL_PHONE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse response) {
                                            OrderModel orderModel=adapter.getItemAtPosition(pos);
                                            Intent intent=new Intent();
                                            intent.setAction(Intent.ACTION_DIAL);
                                            intent.setData(Uri.parse(new StringBuilder("tel: ")
                                            .append(orderModel.getUserPhone()).toString()));
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse response) {
                                            Toast.makeText(getContext(), "You must accept"+response.getPermissionName(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                        }
                                    }).check();
                        }));
                buf.add(new MyButton(getContext(),"Remove",30,0, Color.parseColor("#12005e"),
                        pos -> {
                    AlertDialog.Builder builder=new AlertDialog.Builder(getContext())
                            .setTitle("Delete")
                            .setMessage("Do you really want to delete this order?")
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    OrderModel orderModel=adapter.getItemAtPosition(pos);
                                    FirebaseDatabase.getInstance()
                                            .getReference(Common.RESTAURANT_REF)
                                            .child(Common.currentServerUser.getRestaurant())
                                            .child(Common.ORDER_REF)
                                            .child(orderModel.getKey())
                                            .removeValue()
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    adapter.removeItem(pos);
                                                    adapter.notifyItemRemoved(pos);
                                                    updateTextCounter();
                                                    dialogInterface.dismiss();
                                                    Toast.makeText(getContext(), "Order has been delete", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                    AlertDialog dialog=builder.create();
                    dialog.show();
                            Button negativeButton=dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                            negativeButton.setTextColor(Color.GRAY);
                            Button positiveButton=dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setTextColor(Color.RED);
                        }));
                buf.add(new MyButton(getContext(),"Edit",30,0, Color.parseColor("#336699"),
                        pos -> {
                    showEditDialog(adapter.getItemAtPosition(pos),pos);
                        }));
            }
        };
    }

    private void showEditDialog(OrderModel orderModel, int pos) {
        View layout_dialog;
        AlertDialog.Builder builder;
        if(orderModel.getOrderStatus()==0)
        {
            layout_dialog=LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipping,null);
            recycler_shipper=layout_dialog.findViewById(R.id.recycler_shippers);
            builder=new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
            .setView(layout_dialog);
        }
        else if(orderModel.getOrderStatus()==-1)
        {
            layout_dialog=LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_cancelled,null);
            builder=new AlertDialog.Builder(getContext())
            .setView(layout_dialog);
        }
        else
        {
            layout_dialog=LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipped,null);
            builder=new AlertDialog.Builder(getContext())
            .setView(layout_dialog);
        }
        Button btn_ok=(Button) layout_dialog.findViewById(R.id.btn_ok);
        Button btn_cancel=(Button) layout_dialog.findViewById(R.id.btn_cancel);

        RadioButton rdi_shipping=(RadioButton) layout_dialog.findViewById(R.id.rdi_shipping);
        RadioButton rdi_shipped=(RadioButton) layout_dialog.findViewById(R.id.rdi_shipped);
        RadioButton rdi_cancelled=(RadioButton) layout_dialog.findViewById(R.id.rdi_cancelled);
        RadioButton rdi_delete=(RadioButton) layout_dialog.findViewById(R.id.rdi_delete);
        RadioButton rdi_restore_placed=(RadioButton) layout_dialog.findViewById(R.id.rdi_restore_placed);

        TextView txt_status=(TextView) layout_dialog.findViewById(R.id.txt_status);
        txt_status.setText(new StringBuilder("Order Status(")
                .append(Common.convertStatusToString(orderModel.getOrderStatus())));

        AlertDialog dialog=builder.create();
        if(orderModel.getOrderStatus()==0)
            loadShipperList(pos,orderModel,dialog,btn_ok,btn_cancel,  rdi_shipping,  rdi_shipped,  rdi_cancelled,  rdi_delete,  rdi_restore_placed);
        else
            showDialog(pos,orderModel,dialog,btn_ok,btn_cancel,rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed);
        

    }

    private void loadShipperList(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        List<ShipperModel> tempList=new ArrayList<>();
        DatabaseReference shipperRef=FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPER);
        Query shipperActive=shipperRef.orderByChild("active").equalTo(true);
        shipperActive.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot shipperSnapShot:snapshot.getChildren())
                {
                    ShipperModel shipperModel=shipperSnapShot.getValue(ShipperModel.class);
                    shipperModel.setKey(shipperSnapShot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos,orderModel,tempList,
                        dialog,
                        btn_ok,btn_cancel,
                        rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shipperLoadCallbackListener.onShipperLoadFailed(error.getMessage());
            }
        });
    }

    private void showDialog(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);
        btn_cancel.setOnClickListener(view -> dialog.dismiss());
        btn_ok.setOnClickListener(view -> {
            if(rdi_cancelled!=null && rdi_cancelled.isChecked()) {
                updateOrder(pos, orderModel, -1);
                dialog.dismiss();
            }
            else if(rdi_shipping!=null && rdi_shipping.isChecked()) {
                ShipperModel shipperModel=null;
                if(myShipperSelectedAdapter != null)
                {
                    shipperModel=myShipperSelectedAdapter.getSelectedShipper();
                    if(shipperModel!=null)
                    {
                        createShippingOrder(pos,shipperModel,orderModel,dialog);
                    }
                    else
                        Toast.makeText(getContext(),"Please select Shipper",Toast.LENGTH_SHORT).show();
                }
            }
            else if(rdi_shipped!=null && rdi_shipped.isChecked()) {
                updateOrder(pos, orderModel, 2);
                dialog.dismiss();
            }
            else if(rdi_restore_placed!=null && rdi_restore_placed.isChecked()) {
                updateOrder(pos, orderModel, 0);
                dialog.dismiss();
            }
            else if(rdi_delete!=null && rdi_delete.isChecked()) {
                deleteOrder(pos, orderModel);
                dialog.dismiss();

            }

        });
    }

    private void createShippingOrder(int pos,ShipperModel shipperModel, OrderModel orderModel, AlertDialog dialog) {
        ShippingOrderModel shippingOrder=new ShippingOrderModel();
        shippingOrder.setRestaurantKey(Common.currentServerUser.getRestaurant());
        shippingOrder.setShipperPhone(shipperModel.getPhone());
        shippingOrder.setShipperName(shipperModel.getName());
        shippingOrder.setOrderModel(orderModel);
        shippingOrder.setStartTrip(false);
        shippingOrder.setCurrentLat(-1.0);
        shippingOrder.setCurrentLng(-1.0);

        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPING_ORDER_REF)
                .child(orderModel.getKey())
                .setValue(shippingOrder)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        dialog.dismiss();
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(shipperModel.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.exists())
                                        {
                                            TokenModel tokenModel=snapshot.getValue(TokenModel.class);
                                            Map<String,String> notiData=new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE,"You have new order need ship ");
                                            notiData.put(Common.NOTI_CONTENT,new StringBuilder("You have new order need ship to ").append(orderModel.getUserPhone()).toString());
                                            FCMSenData senData=new FCMSenData(tokenModel.getToken(),notiData);
                                            compositeDisposable.add(ifcmService.sendNotification(senData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if(fcmResponse.getSuccess()==1)
                                                        {
                                                            updateOrder(pos,orderModel,1);
                                                        }
                                                        else
                                                        {
                                                            Toast.makeText(getContext(), "Failed to send to shipper ! Order wasn't update!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(),""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                                                    }));
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(),"Token not found",Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(),""+error,Toast.LENGTH_SHORT).show();

                                    }
                                });
                    }
                });

    }

    private void deleteOrder(int pos, OrderModel orderModel) {
        if(!TextUtils.isEmpty(orderModel.getKey()))
        {

            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)                    .child(orderModel.getKey())
                    .removeValue()
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            adapter.removeItem(pos);
                            adapter.notifyItemRemoved(pos);
                            updateTextCounter();
                            Toast.makeText(getContext(), "Delete order success!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else
        {
            Toast.makeText(getContext(), "Order number must not be null or empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrder(int pos,OrderModel orderModel,int status)
    {
        if(!TextUtils.isEmpty(orderModel.getKey()))
        {
            Map<String,Object> updateData=new HashMap<>();
            updateData.put("orderStatus",status);
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)                    .child(orderModel.getKey())
                    .updateChildren(updateData)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            android.app.AlertDialog dialog=new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
                            dialog.show();
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.TOKEN_REF)
                                    .child(orderModel.getUseId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot.exists())
                                            {
                                                TokenModel tokenModel=snapshot.getValue(TokenModel.class);
                                                Map<String,String> notiData=new HashMap<>();
                                                notiData.put(Common.NOTI_TITLE,"Your order was update");
                                                notiData.put(Common.NOTI_CONTENT,new StringBuilder("Your order ").append(orderModel.getKey())
                                                .append(" was update to ")
                                                .append(Common.convertStatusToString(status)).toString());
                                                FCMSenData senData=new FCMSenData(tokenModel.getToken(),notiData);
                                                compositeDisposable.add(ifcmService.sendNotification(senData)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(fcmResponse -> {
                                                    dialog.dismiss();
                                                    if(fcmResponse.getSuccess()==1)
                                                    {
                                                        Toast.makeText(getContext(), "Update order success!", Toast.LENGTH_SHORT).show();
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(getContext(), "Update order success but failed to send notification!", Toast.LENGTH_SHORT).show();
                                                    }
                                                }, throwable -> {
                                                    dialog.dismiss();
                                                    Toast.makeText(getContext(),""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                                                }));
                                            }
                                            else
                                            {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(),"Token not found",Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(),""+error,Toast.LENGTH_SHORT).show();

                                        }
                                    });
                            adapter.removeItem(pos);
                            adapter.notifyItemRemoved(pos);
                            updateTextCounter();
                        }
                    });
        }
        else
        {
            Toast.makeText(getContext(), "Order number must not be null or empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTextCounter() {
        txt_order_filter.setText(new StringBuilder("Orders (")
                .append(adapter.getItemCount())
                .append(")"));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.order_filter_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.action_filter)
        {
            BottomSheetOrderFragment bottomSheetOrderFragment=BottomSheetOrderFragment.getInstance();
            bottomSheetOrderFragment.show(getActivity().getSupportFragmentManager(),"OrderFilter");
            return true;

        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent.class);
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangeMenuClick(true));
        super.onDestroy();
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onLoadOrderEvent(LoadOrderEvent event)
    {
        orderViewModel.loadOrderByStatus(event.getStatus());
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {

    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        if(recycler_shipper!=null)
        {
            recycler_shipper.setHasFixedSize(true);
            LinearLayoutManager layoutManager=new LinearLayoutManager(getContext());
            recycler_shipper.setLayoutManager(layoutManager);
            recycler_shipper.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

            myShipperSelectedAdapter=new MyShipperSelectionAdapter(getContext(),shipperModels);
            recycler_shipper.setAdapter(myShipperSelectedAdapter);

        }
        showDialog(pos, orderModel, dialog, btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
    }

    @Override
    public void onShipperLoadFailed(String message) {
        Toast.makeText(getContext(),""+message,Toast.LENGTH_SHORT).show();

    }
}