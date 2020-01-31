package com.example.self;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.self.util.JournalApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.Objects;

import model.Journal;

public class PostJournalActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int GALLERY_CODE = 1;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView addPhotoButton;
    private EditText titleEditText;
    private EditText thoughtEditText;
    private TextView currentUserTextView;
    private ImageView imageView;

    private String currentId;
    private String currentUserName;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;

    //Connection to database
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    private CollectionReference collectionReference = db.collection("Journal");
    private Uri imageUri;
    private String TAG = "PostJournalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_journal);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.post_progressBar);
        titleEditText = findViewById(R.id.post_text_ed);
        thoughtEditText = findViewById(R.id.post_description_et);
        currentUserTextView = findViewById(R.id.post_user_button);

        imageView = findViewById(R.id.imageView2);
        saveButton = findViewById(R.id.post_save_journal_button);
        saveButton.setOnClickListener(this);
        addPhotoButton = findViewById(R.id.postCameraButton);
        addPhotoButton.setOnClickListener(this);
        progressBar.setVisibility(View.INVISIBLE);

        if (JournalApi.getInstance() != null){
            currentId = JournalApi.getInstance().getUserId();
            currentUserName = JournalApi.getInstance().getUsername();

            currentUserTextView.setText(currentUserName);
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null){

                }else {

                }
            }
        };

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.post_save_journal_button:
                saveJournal();
                break;
            case R.id.postCameraButton:
                //get Image from gallery/phone
                Intent galleryintent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryintent.setType("image/*");
                startActivityForResult(galleryintent, GALLERY_CODE);
                break;
        }
    }

    private void saveJournal() {
        final String title = titleEditText.getText().toString().trim();
        final String thought = thoughtEditText.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(thought)
            && imageUri != null){

            final StorageReference filepath = storageReference //..../journal_images/our_images.jpeg
                    .child("journal_image")
                    .child("my_image" + Timestamp.now().getSeconds()); //my_image_8663

            filepath.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.INVISIBLE);
                            filepath.getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            String imageUrl = uri.toString();

                                            //TODO: set a journal object - model
                                            Journal journal = new Journal();
                                            journal.setTitle(title);
                                            journal.setThought(thought);
                                            journal.setImageUrl(imageUrl);
                                            journal.setTimeAdded(new Timestamp(new Date()));
                                            journal.setUserName(currentUserName);
                                            journal.setUserId(currentId);

                                            //Todo: invoke our collectionReference
                                            collectionReference.add(journal)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {

                                                            progressBar.setVisibility(View.INVISIBLE);
                                                            startActivity(new Intent(PostJournalActivity.this,JournalListActivity.class));
                                                            finish();

                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.d(TAG, "onFailure " + e.getMessage());
                                                }
                                            });
                                            //Todo: and save Journal instance

                                        }
                                    });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

        }else {

            progressBar.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
            if (data != null){
                imageUri = data.getData();
                imageView.setImageURI(imageUri);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuth != null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}
