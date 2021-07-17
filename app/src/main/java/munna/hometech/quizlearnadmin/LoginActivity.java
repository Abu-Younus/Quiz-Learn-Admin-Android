package munna.hometech.quizlearnadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout txtEmailAddress, txtPassword;
    private Button btnLogin;
    private Dialog loadingDialog;
    private FirebaseAuth firebaseAuth;

    private String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtEmailAddress = findViewById(R.id.txt_email_address);
        txtPassword = findViewById(R.id.txt_password);
        btnLogin = findViewById(R.id.btn_login);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private boolean validateEmailAddress() {
        String email = txtEmailAddress.getEditText().getText().toString().trim();
        if (email.isEmpty()) {
            txtEmailAddress.setError("Field can't be empty!");
            return false;
        } else if (!email.matches(emailPattern)) {
            txtEmailAddress.setError("Please enter valid email!");
            return false;
        } else {
            txtEmailAddress.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = txtPassword.getEditText().getText().toString().trim();
        if (password.isEmpty()) {
            txtPassword.setError("Field can't be empty!");
            return false;
        } else if (password.length() < 8) {
            txtPassword.setError("Password is minimum 8 characters!");
            return false;
        } else {
            txtPassword.setError(null);
            return true;
        }
    }

    private void login() {
        if (!validateEmailAddress() || !validatePassword()) {
            return;
        } else {
            loadingDialog.show();
            firebaseAuth.signInWithEmailAndPassword(txtEmailAddress.getEditText().getText().toString(),
                    txtPassword.getEditText().getText().toString())
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Intent categoryIntent = new Intent(LoginActivity.this, CategoryActivity.class);
                                startActivity(categoryIntent);
                                finish();
                            } else {
                                String error = task.getException().getMessage();
                                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                            loadingDialog.dismiss();
                        }
                    });
        }
    }
}