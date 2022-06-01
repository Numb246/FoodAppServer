package com.example.foodappsever;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.foodappsever.common.Common;
import com.example.foodappsever.model.EventBus.CategoryClick;
import com.example.foodappsever.model.EventBus.ChangeMenuClick;
import com.example.foodappsever.model.EventBus.ToastEvent;
import com.example.foodappsever.model.FCMResponse;
import com.example.foodappsever.model.FCMSenData;
import com.example.foodappsever.remote.IFCMService;
import com.example.foodappsever.remote.RetrofitFCMClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodappsever.databinding.ActivityHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 7171;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeBinding binding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private int menuClick=-1;

    private ImageView img_upload;
    private CompositeDisposable compositeDisposable=new CompositeDisposable();
    private IFCMService ifcmService;
    private Uri imgUri=null;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    @OnClick(R.id.fab_chat)
    void onOpenChatList(){
        startActivity(new Intent(this,ChatListActivity.class));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarHome.toolbar);
        ButterKnife.bind(this);
        ifcmService= RetrofitFCMClient.getInstance().create(IFCMService.class);
        storage=FirebaseStorage.getInstance();
        storageReference=storage.getReference();
        subscribeToTopic(Common.createTopicOrder());
        updateToken();

        drawer = binding.drawerLayout;
        navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_category, R.id.nav_food_list, R.id.nav_order,R.id.nav_shipper)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();
        View headerView=navigationView.getHeaderView(0);
        TextView txt_user=(TextView) headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hey",Common.currentServerUser.getName(),txt_user);
        menuClick=R.id.nav_category;
        checkIsOpenFromActivity();
    }

    private void checkIsOpenFromActivity() {
        boolean isOpenFromNewOrder=getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER,false);
        if(isOpenFromNewOrder)
        {
            navController.popBackStack();
            navController.navigate(R.id.nav_order);
            menuClick=R.id.nav_order;
        }
    }

    private void updateToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(s -> {
                    Common.updateToken(HomeActivity.this,s,true,false);
                });
    }

    private void subscribeToTopic(String topicOrder) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topicOrder)
                .addOnFailureListener(e -> {
                    Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if(!task.isSuccessful())
                        Toast.makeText(this,"Failed: "+task.isSuccessful(),Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void  onCategoryClick(CategoryClick event)
    {
        if(event.isSuccess())
        {
            if(menuClick != R.id.nav_food_list)
            {
                navController.navigate(R.id.nav_food_list);
                menuClick=R.id.nav_food_list;
            }
        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void  onChangeMenuClick(ChangeMenuClick event)
    {
        Toast.makeText(this, "TEST"+event.isFromFoodList(), Toast.LENGTH_SHORT).show();
        if(event.isFromFoodList())
        {
            Toast.makeText(this, "TEST", Toast.LENGTH_SHORT).show();
            navController.popBackStack(R.id.nav_category,true);
            navController.navigate(R.id.nav_category);
        }
        else
        {
            navController.popBackStack(R.id.nav_food_list,true);
            navController.navigate(R.id.nav_food_list);
        }
        menuClick=-1;
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void  onToastEvent(ToastEvent event)
    {
        if(event.getAction() == Common.ACTION.CREATE)
        {
            Toast.makeText(this, "Create Success", Toast.LENGTH_SHORT).show();
        }
        else if(event.getAction() == Common.ACTION.UPDATE)
        {
            Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Delete Success", Toast.LENGTH_SHORT).show();
        }
        EventBus.getDefault().postSticky(new ChangeMenuClick(event.isFromFoodList()));


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        drawer.closeDrawers();
        switch (item.getItemId())
        {
            case R.id.nav_category:
                if(item.getItemId()!=menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_category);
                }
                break;
            case R.id.nav_order:
                if(item.getItemId()!=menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_order);
                }
                break;
            case R.id.nav_shipper:
                if(item.getItemId()!=menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_shipper);
                }
                break;
            case R.id.nav_best_deals:
                if(item.getItemId()!=menuClick) {
                    //navController.popBackStack();
                    navController.navigate(R.id.nav_best_deals);
                }
                break;
            case R.id.nav_most_popular:
                if(item.getItemId()!=menuClick) {
                    //navController.popBackStack();
                    navController.navigate(R.id.nav_most_popular);
                }
                break;
            case R.id.nav_send_news:
                showNewsDialog();
                break;
            case R.id.nav_sign_out:
                signOut();
                break;
            default:
                menuClick=-1;
                break;
        }
        menuClick=item.getItemId();
        return true;
    }

    private void showNewsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder=new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("News System");
        builder.setMessage("Send news notification to all client");
        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_news_system,null);

        EditText edt_title=(EditText) itemView.findViewById(R.id.edt_title);
        EditText edt_content=(EditText) itemView.findViewById(R.id.edt_content);
        EditText edt_link=(EditText) itemView.findViewById(R.id.edt_link);
        img_upload=(ImageView) itemView.findViewById(R.id.img_upload);
        RadioButton rdi_none=(RadioButton) itemView.findViewById(R.id.rdi_none);
        RadioButton rdi_link=(RadioButton) itemView.findViewById(R.id.rdi_link);
        RadioButton rdi_upload=(RadioButton) itemView.findViewById(R.id.rdi_image);

        rdi_none.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_link.setOnClickListener(view -> {
            edt_link.setVisibility(View.VISIBLE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_upload.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.VISIBLE);
        });
        img_upload.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });
        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
           dialogInterface.dismiss();
        });
        builder.setPositiveButton("SEND", (dialogInterface, i) -> {
            if(rdi_none.isChecked())
            {
                sendNews(edt_title.getText().toString(),edt_content.getText().toString());
            }
            else if(rdi_link.isChecked())
            {
                sendNews(edt_title.getText().toString(),edt_content.getText().toString(),edt_link.getText().toString());
            }
            else if(rdi_upload.isChecked())
            {
                if(imgUri!= null)
                {
                    AlertDialog dialog=new AlertDialog.Builder(this).setMessage("Uploading...").create();
                    dialog.show();

                    String file_name= UUID.randomUUID().toString();
                    StorageReference storageRef = storage.getReference();
                    Log.d("DATAIMG",imgUri.toString());
                    StorageReference newsImages=storageReference.child("news/"+file_name);
                    //StorageReference mountainsRef = storageRef.child("mountains.jpg");
                    //StorageReference newsImages = storageRef.child("images/mountains.jpg");

                    //StorageReference storageRef = storage.getReferenceFromUrl("gs://your-id.appspot.com");
                    //StorageReference newsImages = storageReference.child("images/test.jpg");
                    //StorageReference newsImages=storageReference.child("images/"+file_name);
                    newsImages.putFile(imgUri)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                Log.e("IMGERROR",e.getMessage());
                            }).addOnSuccessListener(taskSnapshot -> {
                                dialog.dismiss();
                                newsImages.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        sendNews(edt_title.getText().toString(),edt_content.getText().toString(),uri.toString());
                                    }
                                });
                            }).addOnProgressListener(snapshot -> {
                                double progress=Math.round((100.0 * snapshot.getBytesTransferred()/snapshot.getTotalByteCount()));
                                dialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));
                            });
                }
            }
        });
        AlertDialog dialog=builder.create();
        dialog.show();


    }

    private void sendNews(String title, String content, String url) {
        Map<String,String> notificationData=new HashMap<String,String>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT,content);
        notificationData.put(Common.IS_SEND_IMAGE,"true");
        notificationData.put(Common.IMAGE_URL,url);

        FCMSenData fcmSenData=new FCMSenData(Common.getNewsTopic(),notificationData);
        AlertDialog dialog=new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSenData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if(fcmResponse.getMessage_id()!=0)
                        Toast.makeText(this,"News has been sent",Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this,"News send failed!",Toast.LENGTH_SHORT).show();
                },throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this,"Error: "+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                }));
    }

    private void sendNews(String title, String content) {
        Map<String,String> notificationData=new HashMap<String,String>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT,content);
        notificationData.put(Common.IS_SEND_IMAGE,"false");

        FCMSenData fcmSenData=new FCMSenData(Common.getNewsTopic(),notificationData);
        AlertDialog dialog=new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSenData)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(fcmResponse -> {
            dialog.dismiss();
            if(fcmResponse.getMessage_id()!=0)
                Toast.makeText(this,"News has been sent",Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this,"News send failed!",Toast.LENGTH_SHORT).show();
        },throwable -> {
            dialog.dismiss();
            Toast.makeText(this,"Error: "+throwable.getMessage(),Toast.LENGTH_SHORT).show();
        }));
    }

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Signout")
                .setMessage("Do you want to sign out?")
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Common.selectedFood = null;
                Common.categorySelected = null;
                Common.currentServerUser = null;
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(HomeActivity.this,MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK)
        {
            if(data!=null && data.getData()!=null)
            {
                Log.d("TEST",data.getData().toString());
                imgUri=data.getData();
                img_upload.setImageURI(imgUri);
            }
        }
    }
}