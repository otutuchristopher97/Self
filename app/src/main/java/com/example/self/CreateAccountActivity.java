package com.example.self;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.self.util.JournalApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CreateAccountActivity extends AppCompatActivity {
    private Button createAcctButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUsers;

    //Firestore connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference collectionReference = db.collection("Users");

    private EditText emailEditText;
    private EditText passwordEditText;
    private ProgressBar progressBar;
    private EditText userNameEditEditText;
    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        createAcctButton = findViewById(R.id.creat_Button);
        progressBar = findViewById(R.id.create_acct_progress);
        passwordEditText = findViewById(R.id.password_account);
        userNameEditEditText = findViewById(R.id.username_account);
        emailEditText = findViewById(R.id.email_accout);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUsers = firebaseAuth.getCurrentUser();

                if (currentUsers != null){
                    //user is already Loggeding...
                }else {
                    //No user yet...
                }

            }
        };
        createAcctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!TextUtils.isEmpty(emailEditText.getText().toString())
                        && !TextUtils.isEmpty(passwordEditText.getText().toString())
                        && !TextUtils.isEmpty(userNameEditEditText.getText().toString())){
                    String email = emailEditText.getText().toString().trim();
                    String password = passwordEditText.getText().toString().trim();
                    String username = userNameEditEditText.getText().toString().trim();
                    createUserEmailAccount(email,password, username);
                }
                else {
                    Toast.makeText(CreateAccountActivity.this, "Empty Field Not Allowed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    private void createUserEmailAccount(String email, String password, final String username) {
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)
            && !TextUtils.isEmpty(username)) {

            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()){
                                // we take user to AddJournalActivity
                                currentUsers = firebaseAuth.getCurrentUser();
                                assert currentUsers != null;
                                final String currentUserId = currentUsers.getUid();
                                //craete a user Map so that we can create a user in the user collection
                                Map<String, String> UserObj = new HashMap<>();
                                UserObj.put("UserId", currentUserId);
                                UserObj.put("Username", username);

                                //save to our fierstore database
                                collectionReference.add(UserObj)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {

                                                documentReference.get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (Objects.requireNonNull(task.getResult()).exists()){
                                                                    progressBar.setVisibility(View.INVISIBLE);
                                                                    String name = task.getResult()
                                                                            .getString("username");

                                                                    JournalApi journalApi = JournalApi.getInstance();
                                                                    journalApi.setUserId(currentUserId);
                                                                    journalApi.setUsername(name);

                                                                    Intent intent = new Intent(CreateAccountActivity.this, PostJournalActivity.class);
                                                                    intent.putExtra("username", name);
                                                                    intent.putExtra("UserId", currentUserId);
                                                                    startActivity(intent);
                                                                }

                                                            }
                                                        });

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG,"Problem : " + e.getMessage());
                                    }
                                });

                            }else {

                                progressBar.setVisibility(View.INVISIBLE);

                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        }else {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUsers = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}
