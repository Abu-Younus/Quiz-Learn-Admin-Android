package munna.hometech.quizlearnadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestionsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView questionsRecyclerView;
    public static List<QuestionsModel> questionsModelList;
    private QuestionsAdapter questionsAdapter;
    private Button btnAdd, btnExcel;
    private String categoryName;
    private String setId;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private Dialog loadingDialog;
    private TextView loadingText;

    public static final int CELL_COUNT = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        categoryName = getIntent().getStringExtra("categoryName");
        setId = getIntent().getStringExtra("setId");
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(categoryName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        loadingText = loadingDialog.findViewById(R.id.textView5);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        btnAdd = findViewById(R.id.btn_add);
        btnExcel = findViewById(R.id.btn_excel);
        questionsRecyclerView = findViewById(R.id.questions_recycler_view);

        btnAdd.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.darkGrey)));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        questionsRecyclerView.setLayoutManager(layoutManager);

        questionsModelList = new ArrayList<>();
        questionsAdapter = new QuestionsAdapter(questionsModelList, categoryName, new QuestionsAdapter.DeleteListener() {
            @Override
            public void onLongClick(int position, String id) {
                new AlertDialog.Builder(QuestionsActivity.this,R.style.Theme_AppCompat_Light_Dialog).setTitle("Delete Question")
                        .setMessage("Are you sure! you want to delete this question?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialog.show();
                                myRef.child("Sets").child(setId).child(id)
                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            questionsModelList.remove(position);
                                            questionsAdapter.notifyItemRemoved(position);
                                        } else {
                                            Toast.makeText(QuestionsActivity.this, "Failed to delete!", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        }).setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
            }
        });
        questionsRecyclerView.setAdapter(questionsAdapter);

        getData();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addQuestionIntent = new Intent(QuestionsActivity.this, AddQuestionActivity.class);
                addQuestionIntent.putExtra("categoryName", categoryName);
                addQuestionIntent.putExtra("setId", setId);
                startActivity(addQuestionIntent);
            }
        });

        btnExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(QuestionsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(QuestionsActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        questionsAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(this, "Please Grant permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent,"Select File"),102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102) {
            if (resultCode == RESULT_OK) {
                String filePath = data.getData().getPath();
                if (filePath.endsWith(".xlsx")) {
                    readFile(data.getData());
                } else {
                    Toast.makeText(this, "Please choose an Excel File", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void readFile(Uri fileUri) {
        loadingText.setText("Scanning Questions...");
        loadingDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Map<String,Object> parentMap = new HashMap<>();
                List<QuestionsModel> tempList = new ArrayList<>();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                    XSSFSheet sheet = workbook.getSheetAt(0);
                    FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    int rowsCount = sheet.getPhysicalNumberOfRows();
                    if (rowsCount > 0) {
                        for (int r = 0;r < rowsCount;r++) {
                            Row row = sheet.getRow(r);
                            if (row.getPhysicalNumberOfCells() == CELL_COUNT) {
                                String question = getCellData(row,0,formulaEvaluator);
                                String optionA = getCellData(row,1,formulaEvaluator);
                                String optionB = getCellData(row,2,formulaEvaluator);
                                String optionC = getCellData(row,3,formulaEvaluator);
                                String optionD = getCellData(row,4,formulaEvaluator);
                                String correctANS = getCellData(row,5,formulaEvaluator);
                                if (correctANS.equals(optionA) || correctANS.equals(optionB) || correctANS.equals(optionC) || correctANS.equals(optionD)) {
                                    Map<String,Object> questionMap = new HashMap<>();
                                    questionMap.put("question",question);
                                    questionMap.put("optionA",optionA);
                                    questionMap.put("optionB",optionB);
                                    questionMap.put("optionC",optionC);
                                    questionMap.put("optionD",optionD);
                                    questionMap.put("correctANS",correctANS);
                                    questionMap.put("setId",setId);

                                    String id = UUID.randomUUID().toString();
                                    parentMap.put(id,questionMap);

                                    tempList.add(new QuestionsModel(id,question,optionA,optionB,optionC,optionD,correctANS,setId));
                                } else {
                                    int finalR = r;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadingText.setText("Loading...");
                                            loadingDialog.dismiss();
                                            Toast.makeText(QuestionsActivity.this, "Row no. "+(finalR +1)+" has no correct option!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                int finalR1 = r;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingText.setText("Loading...");
                                        loadingDialog.dismiss();
                                        Toast.makeText(QuestionsActivity.this, "Row no. "+(finalR1 +1)+" has incorrect data!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return;
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Uploading...");
                                myRef.child("Sets").child(setId).updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            questionsModelList.addAll(tempList);
                                            questionsAdapter.notifyDataSetChanged();
                                        } else {
                                            loadingText.setText("Loading...");
                                            Toast.makeText(QuestionsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Loading...");
                                loadingDialog.dismiss();
                                Toast.makeText(QuestionsActivity.this, "File is empty!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingText.setText("Loading...");
                            loadingDialog.dismiss();
                            Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingText.setText("Loading...");
                            loadingDialog.dismiss();
                            Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private String getCellData(Row row, int cellPosition, FormulaEvaluator formulaEvaluator) {
        String value = "";
        Cell cell = row.getCell(cellPosition);
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                return value+cell.getBooleanCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return value+cell.getNumericCellValue();

            case Cell.CELL_TYPE_STRING:
                return value+cell.getStringCellValue();

            default:
                return value;
        }
    }

    private void getData() {
        loadingDialog.show();
        myRef.child("Sets").child(setId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            String id = dataSnapshot.getKey();
                            String question = dataSnapshot.child("question").getValue().toString();
                            String optionA = dataSnapshot.child("optionA").getValue().toString();
                            String optionB = dataSnapshot.child("optionB").getValue().toString();
                            String optionC = dataSnapshot.child("optionC").getValue().toString();
                            String optionD = dataSnapshot.child("optionD").getValue().toString();
                            String correctANS = dataSnapshot.child("correctANS").getValue().toString();

                            questionsModelList.add(new QuestionsModel(id,question,optionA,optionB,optionC,optionD,correctANS,setId));
                        }
                        loadingDialog.dismiss();
                        questionsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadingDialog.dismiss();
                        finish();
                        Toast.makeText(QuestionsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}