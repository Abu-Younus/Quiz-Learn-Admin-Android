package munna.hometech.quizlearnadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.UUID;

public class SetsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private GridView gridView;
    private SetsAdapter adapter;

    private Dialog loadingDialog;

    private String categoryName;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private List<String> sets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        categoryName = getIntent().getStringExtra("title");
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(categoryName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        gridView = findViewById(R.id.sets_grid_view);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        sets = CategoryActivity.categoryModelList.get(getIntent().getIntExtra("position", 0)).getSets();

        adapter = new SetsAdapter(sets,
                getIntent().getStringExtra("title"), new SetsAdapter.SetsListener() {
            @Override
            public void addSets() {
                loadingDialog.show();
                String id = UUID.randomUUID().toString();
                myRef.child("Categories").child(getIntent().getStringExtra("key"))
                        .child("sets").child(id).setValue("Set ID")
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    sets.add(id);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(SetsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                }
                                loadingDialog.dismiss();
                            }
                        });

            }

            @Override
            public void onLongClick(String setId, int position) {
                new AlertDialog.Builder(SetsActivity.this,R.style.Theme_AppCompat_Light_Dialog).setTitle("Delete Set "+position)
                        .setMessage("Are you sure! you want to delete this Set?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialog.show();
                                myRef.child("Sets").child(setId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            myRef.child("Categories")
                                                    .child(CategoryActivity.categoryModelList.get(getIntent().getIntExtra("position", 0)).getKey())
                                                    .child("sets").child(setId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        sets.remove(setId);
                                                        adapter.notifyDataSetChanged();
                                                    } else {
                                                        Toast.makeText(SetsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                    }
                                                    loadingDialog.dismiss();
                                                }
                                            });
                                        } else {
                                            loadingDialog.dismiss();
                                            Toast.makeText(SetsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }).setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
            }
        });
        gridView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}