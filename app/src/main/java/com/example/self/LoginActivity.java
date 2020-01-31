
package com.example.self;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.self.util.JournalApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import model.Journal;

public class LoginActivity extends AppCompatActivity {
    private Button loginButton;
    private Button createAcctButton;

    private AutoCompleteTextView emailAddress;
    private EditText password;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);


        firebaseAuth = FirebaseAuth.getInstance();

        emailAddress = findViewById(R.id.email);
        password = findViewById(R.id.password);
        progressBar = findViewById(R.id.login_progress);

        loginButton = findViewById(R.id.email_sign_in_Button);
        createAcctButton = findViewById(R.id.create_Button);

        createAcctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loginEmailPasswordUser(emailAddress.getText().toString().trim(),
                        password.getText().toString().trim());

            }
        });
    }

    private void loginEmailPasswordUser(String email, String pwd) {
        progressBar.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pwd)){
            firebaseAuth.signInWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            assert user != null;
                            String currentUserId = user.getUid();

                            collectionReference
                                    .whereEqualTo("UserId", currentUserId)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                            @Nullable FirebaseFirestoreException e) {
                                            if (e != null){
                                            }
                                            assert queryDocumentSnapshots != null;
                                            if (!queryDocumentSnapshots.isEmpty()){
                                                progressBar.setVisibility(View.INVISIBLE);
                                                for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots){
                                                    JournalApi journalApi = JournalApi.getInstance();
                                                    journalApi.setUsername(snapshot.getString("Username"));
                                                    journalApi.setUserId(snapshot.getString("UserId"));

                                                    // go to ListActivity
                                                    startActivity(new Intent(LoginActivity.this, PostJournalActivity.class));
                                                }
                                            }
                                        }
                                    });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_LONG).show();
        }
    }
}
