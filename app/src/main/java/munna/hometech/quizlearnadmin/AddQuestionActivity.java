package munna.hometech.quizlearnadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddQuestionActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout txtQuestion;
    private RadioGroup optionsContainer;
    private LinearLayout answerContainer;
    private Button btnUpload;

    private String categoryName,setId,id;
    private int position;
    private QuestionsModel questionsModel;

    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        categoryName = getIntent().getStringExtra("categoryName");
        setId = getIntent().getStringExtra("setId");
        position = getIntent().getIntExtra("position", -1);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        if (position != -1) {
            getSupportActionBar().setTitle("Edit Question");
        } else {
            getSupportActionBar().setTitle("Add Question");
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        txtQuestion = findViewById(R.id.question);
        optionsContainer = findViewById(R.id.options_container);
        answerContainer = findViewById(R.id.answer_container);
        btnUpload = findViewById(R.id.btn_upload);

        if (setId == null) {
            finish();
            return;
        }
        if (position != -1) {
            questionsModel = QuestionsActivity.questionsModelList.get(position);
            setData();
        }

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtQuestion.getEditText().getText().toString().trim().isEmpty()) {
                    txtQuestion.setError("Field can't be empty!");
                    return;
                }
                if (txtQuestion.getEditText().getText().toString().trim().length() < 10) {
                    txtQuestion.setError("Question is minimum 10 characters!");
                    return;
                }
                uploadData();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setData() {
        txtQuestion.getEditText().setText(questionsModel.getQuestion());
        ((TextInputLayout)answerContainer.getChildAt(0)).getEditText().setText(questionsModel.getOptionA());
        ((TextInputLayout)answerContainer.getChildAt(1)).getEditText().setText(questionsModel.getOptionB());
        ((TextInputLayout)answerContainer.getChildAt(2)).getEditText().setText(questionsModel.getOptionC());
        ((TextInputLayout)answerContainer.getChildAt(3)).getEditText().setText(questionsModel.getOptionD());

        for (int i = 0;i < answerContainer.getChildCount();i++) {
            if (((TextInputLayout)answerContainer.getChildAt(i)).getEditText().getText().toString().equals(questionsModel.getCorrectAns())) {
                RadioButton radioButton = (RadioButton) optionsContainer.getChildAt(i);
                radioButton.setChecked(true);
                break;
            }
        }
    }

    private void uploadData() {
        int correct = -1;
        for (int i = 0;i < optionsContainer.getChildCount();i++) {
            TextInputLayout answer = (TextInputLayout) answerContainer.getChildAt(i);
            if (answer.getEditText().getText().toString().trim().isEmpty()) {
                answer.setError("Field can't be empty!");
                return;
            }
            RadioButton radioButton = (RadioButton) optionsContainer.getChildAt(i);
            if (radioButton.isChecked()) {
                correct = i;
                break;
            }
        }
        if (correct == -1) {
            Toast.makeText(this, "Please mark the correct option!", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String,Object> map = new HashMap<>();
        map.put("correctANS",((TextInputLayout)answerContainer.getChildAt(correct)).getEditText().getText().toString());
        map.put("optionA",((TextInputLayout)answerContainer.getChildAt(0)).getEditText().getText().toString());
        map.put("optionB",((TextInputLayout)answerContainer.getChildAt(1)).getEditText().getText().toString());
        map.put("optionC",((TextInputLayout)answerContainer.getChildAt(2)).getEditText().getText().toString());
        map.put("optionD",((TextInputLayout)answerContainer.getChildAt(3)).getEditText().getText().toString());
        map.put("question",txtQuestion.getEditText().getText().toString());
        map.put("setId",setId);

        if (position != -1) {
            id = questionsModel.getId();
        } else {
            id = UUID.randomUUID().toString();
        }
        loadingDialog.show();
        FirebaseDatabase.getInstance().getReference().child("Sets").child(setId).child(id).setValue(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            QuestionsModel model = new QuestionsModel(id,map.get("question").toString(),map.get("optionA").toString(),
                                    map.get("optionB").toString(),map.get("optionC").toString(),map.get("optionD").toString(),
                                    map.get("correctANS").toString(),map.get("setId").toString());
                            if (position != -1) {
                                QuestionsActivity.questionsModelList.set(position,model);
                            } else {
                                QuestionsActivity.questionsModelList.add(model);
                            }
                            finish();
                        } else {
                            Toast.makeText(AddQuestionActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                        }
                        loadingDialog.dismiss();
                    }
                });
    }
}