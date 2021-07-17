package munna.hometech.quizlearnadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView categoryRecyclerView;
    public static List<CategoryModel> categoryModelList;
    private CategoryAdapter categoryAdapter;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private Dialog loadingDialog,addCategoryDialog;

    private TextInputLayout txtCategoryName;
    private CircleImageView categoryImage;
    private Button btnCancel,btnAdd;

    private Uri image;

    private String namePattern = "[a-zA-Z]+\\.?";

    private String downloadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Categories");

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        setAddCategoryDialog();

        categoryRecyclerView = findViewById(R.id.category_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        categoryRecyclerView.setLayoutManager(layoutManager);

        categoryModelList = new ArrayList<>();

        categoryAdapter = new CategoryAdapter(categoryModelList, new CategoryAdapter.DeleteListener() {
            @Override
            public void onDelete(String key,int position) {
                new AlertDialog.Builder(CategoryActivity.this,R.style.Theme_AppCompat_Light_Dialog).setTitle("Delete Category")
                        .setMessage("Are you sure! you want to delete this category?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialog.show();
                                myRef.child("Categories").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            for (String setIds : categoryModelList.get(position).getSets()) {
                                                myRef.child("Sets").child(setIds).removeValue();
                                            }
                                            categoryModelList.remove(position);
                                            categoryAdapter.notifyDataSetChanged();
                                            loadingDialog.dismiss();
                                        } else {
                                            loadingDialog.dismiss();
                                            Toast.makeText(CategoryActivity.this, "Failed to delete!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }).setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
            }
        });
        categoryRecyclerView.setAdapter(categoryAdapter);

        loadingDialog.show();
        myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    List<String> sets = new ArrayList<>();
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.child("sets").getChildren()) {
                        sets.add(dataSnapshot1.getKey());
                    }
                    categoryModelList.add(new CategoryModel(dataSnapshot.child("name").getValue().toString(),
                            dataSnapshot.child("url").getValue().toString(),
                            sets, dataSnapshot.getKey()));
                }
                categoryAdapter.notifyDataSetChanged();
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.category_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_add) {
            addCategoryDialog.show();
        }
        if (id == R.id.nav_logout) {
            new AlertDialog.Builder(CategoryActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("Log Out")
                    .setMessage("Are you sure! you want to Logout?")
                    .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadingDialog.show();
                            FirebaseAuth.getInstance().signOut();
                            Intent loginIntent = new Intent(CategoryActivity.this, LoginActivity.class);
                            startActivity(loginIntent);
                            finish();
                            loadingDialog.dismiss();
                        }
                    }).setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAddCategoryDialog() {
        addCategoryDialog = new Dialog(this);
        addCategoryDialog.setContentView(R.layout.add_category_dialog);
        addCategoryDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_box));
        addCategoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        addCategoryDialog.setCancelable(false);

        txtCategoryName = addCategoryDialog.findViewById(R.id.txt_category_name);
        categoryImage = addCategoryDialog.findViewById(R.id.category_image);
        btnAdd = addCategoryDialog.findViewById(R.id.btn_add);
        btnCancel = addCategoryDialog.findViewById(R.id.btn_cancel);

        btnCancel.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategoryDialog.dismiss();
            }
        });

        categoryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 101);
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateCategoryImage() || !validateCategoryName()) {
                    return;
                }
                for (CategoryModel model : categoryModelList) {
                    if (txtCategoryName.getEditText().getText().toString().equals(model.getName())) {
                        txtCategoryName.setError("Category name is already exist!");
                        return;
                    }
                }
                addCategoryDialog.dismiss();
                uploadData();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                image = data.getData();
                categoryImage.setImageURI(image);
            }
        }
    }

    private boolean validateCategoryImage() {
        if (image == null) {
            Toast.makeText(CategoryActivity.this, "Category Image is required!", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    private boolean validateCategoryName() {
        String categoryName = txtCategoryName.getEditText().getText().toString().trim();
        if (categoryName.isEmpty()) {
            txtCategoryName.setError("Field can't be empty!");
            return false;
        } else if (categoryName.length() < 3) {
            txtCategoryName.setError("Category name is minimum 3 characters!");
            return false;
        } else if (categoryName.length() > 15) {
            txtCategoryName.setError("Category name is maximum 15 characters!");
            return false;
        }
        else if (!categoryName.matches(namePattern)) {
            txtCategoryName.setError("Category name is only alphabetical characters!");
            return false;
        } else {
            txtCategoryName.setError(null);
            return true;
        }
    }

    private void uploadData() {
        loadingDialog.show();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference imageReference = storageReference.child("categories").child(image.getLastPathSegment());
        UploadTask uploadTask = imageReference.putFile(image);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            downloadUrl = task.getResult().toString();
                            uploadCategoryName();
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(CategoryActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(CategoryActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadCategoryName() {
        Map<String,Object> map = new HashMap<>();
        map.put("name", txtCategoryName.getEditText().getText().toString());
        map.put("sets", 0);
        map.put("url", downloadUrl);

        String id = UUID.randomUUID().toString();

        myRef.child("Categories").child(id).setValue(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            categoryModelList.add(new CategoryModel(txtCategoryName.getEditText().getText().toString(),
                                    downloadUrl,new ArrayList<String>(),id));
                            categoryAdapter.notifyDataSetChanged();
                        } else {
                            String error = task.getException().getMessage();
                            Toast.makeText(CategoryActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                        loadingDialog.dismiss();
                    }
                });
    }
}