package com.example.firebaseupload;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.firebaseupload.views.MyButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HomeScreen extends Fragment implements ITabbedFragment {


    @BindView(R.id.button_choose_image)
    MyButton buttonChooseImage;
    @BindView(R.id.caption_text_file_name)
    TextView captionTextFileName;
    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.image_button)
    Button imageButton;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.dummy)
    Space dummy;
    @BindView(R.id.button_upload)
    MyButton buttonUpload;
    @BindView(R.id.camera_button)
    MyButton cameraButton;
    @BindView(R.id.elapsed_time)
    TextView elapsedTime;
    @BindView(R.id.narrate_logo)
    ImageView narrateLogo;

    private static final int PICK_IMAGE_REQUEST = 1000;
    private static final int CAMERA_PIC_REQUEST = 1001;
    private static final int PERMISSION_CODE = 1002;
    //    private static Boolean TRANSLATOR_DOWNLOADED_SUCCESFULLY = Boolean.FALSE;
    private static int INITIAL_CALL = 1;

    private String cap;
    private String cap_original;
    private String cap_custom;
    private Uri mImageUri;

    private long tStart;
    private long tEnd;

    private TextToSpeech mTTS;
    private TextToSpeech mTTS_custom;
    private TextToSpeech mTTS_original;
    private final int width = 299;
    private final int height = 299;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private byte[] data;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DocumentReference docRef = db.collection("users").document("CAPTION");
    private final DocumentReference transValRef = db.collection("users").document("TransferVal");

    private IAdapter mListener;
    private static final String modelFile = "encoder.tflite";
    private Bitmap imageTF;
    private FirebaseModelInterpreter encoder = null;
    private FirebaseCustomLocalModel localModel;
    private FirebaseModelInputOutputOptions inOutOptions;
    private ListenerRegistration registration;

    private HomeScreen(IAdapter listener) {
        // Required empty public constructor
        if (listener != null) {
            mListener = listener;
        }
    }

    static HomeScreen newInstance(IAdapter listener) {
        return new HomeScreen(listener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_screen, container, false);
        ButterKnife.bind(this, view);
        progressBar.setVisibility(View.GONE);
        buttonUpload.setEnabled(true);
        imageButton.setEnabled(false);

        Locale pLang = Locale.getDefault();

        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("LISTENER", "Listen failed.", e);
                    return;
                }
                if (INITIAL_CALL == 1) {
                    INITIAL_CALL = 0;
                    captionTextFileName.setText(R.string.caption_placeholder);
                } else if (MainActivity.getActivityStart()) {
                    MainActivity.setActivityStart(false);
                    captionTextFileName.setText(R.string.caption_placeholder);
                } else if (snapshot != null && snapshot.exists()) {

                    cap_original = Objects.requireNonNull(Objects.requireNonNull(snapshot.getData()).get("caption_original")).toString();
                    cap_custom = Objects.requireNonNull(Objects.requireNonNull(snapshot.getData()).get("caption_custom")).toString();

                    if (MainActivity.getLanguage()) {
                        captionTextFileName.setText(cap_original);
                    } else {
                        captionTextFileName.setText(cap_custom);
                    }
                    tEnd = SystemClock.elapsedRealtime();
                    long tPassed = tEnd - tStart;
                    double timeElapsed = tPassed/1000.0;
                    String pTime = timeElapsed + " seconds";
                    Log.d("Time passed:", String.valueOf(tPassed));
                    narrateLogo.setVisibility(View.VISIBLE);
                    elapsedTime.setVisibility(View.VISIBLE);
                    imageButton.setEnabled(true);
                    elapsedTime.setText(pTime);
                } else {
                    Log.d("LISTENER", "Current data: null");
                }
                progressBar.setVisibility(View.GONE);

                HomeScreen.this.enableScreen();
            }
        });

        mTTS_original = new TextToSpeech(getActivity(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int mTTSresult = mTTS_original.setLanguage(Locale.US);
//                int mTTSresult = mTTS_original.setLanguage(pLang);

                if (mTTSresult == TextToSpeech.LANG_MISSING_DATA
                        || mTTSresult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("mTTS", "Language not supported");
                }
            } else {
                Log.e("mTTS", "Initialization failed");
            }

        });

        mTTS_custom = new TextToSpeech(getActivity(), status -> {
            if (status == TextToSpeech.SUCCESS) {
//                  int mTTSresult = mTTS_custom.setLanguage(Locale.US);
                int mTTSresult = mTTS_custom.setLanguage(pLang);

                if (mTTSresult == TextToSpeech.LANG_MISSING_DATA
                        || mTTSresult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("mTTS", "Language not supported");
                }
            } else {
                Log.e("mTTS", "Initialization failed");
            }

        });

        configureLocalModelSource();
        try {
            encoder = createInterpreter(localModel);
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
        try {
            inOutOptions = createInputOutputOptions();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
        narrateLogo.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onStop() {

        super.onStop();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

    }


    @OnClick({R.id.button_choose_image, R.id.image_button, R.id.button_upload, R.id.camera_button})
    public void onClick(View view) {
        imageButton.setEnabled(false);
        switch (view.getId()) {
            case R.id.button_choose_image:
                openFileChooser();
                break;
            case R.id.image_button:
                narrate();
                break;
            case R.id.button_upload:
                tStart = SystemClock.elapsedRealtime();
                try {
                    runInference();

                } catch (FirebaseMLException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.camera_button:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_DENIED) {
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

                        requestPermissions(permission, PERMISSION_CODE);

                    } else {
                        openCamera();
                    }
                } else {
                    openCamera();
                }
                break;
        }
    }


    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        mImageUri = Objects.requireNonNull(getActivity()).getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//        Camera Intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();

            Picasso.get().load(mImageUri).into(imageView);

            try {
                InputStream inputStream = Objects.requireNonNull(getContext()).getContentResolver().openInputStream(mImageUri);
                imageTF = BitmapFactory.decodeStream(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


        if (requestCode == CAMERA_PIC_REQUEST && resultCode == Activity.RESULT_OK) {

            Picasso.get().load(mImageUri).into(imageView);
        }
        captionTextFileName.setText(R.string.caption_placeholder);
        narrateLogo.setVisibility(View.INVISIBLE);
        elapsedTime.setVisibility(View.INVISIBLE);

    }

    private void narrate() {
        if (MainActivity.getLanguage()) {
            String text = cap_original;
            float pitch = (float) 1;
            float speed = (float) 1;
            mTTS_original.setPitch(pitch);
            mTTS_original.setSpeechRate(speed);
            mTTS_original.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            String text = cap_custom;
            float pitch = (float) 1;
            float speed = (float) 1;
            mTTS_custom.setPitch(pitch);
            mTTS_custom.setSpeechRate(speed);
            mTTS_custom.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }


    }

    @Override
    public void onReceive(Object o) {
//        updateImage((byte[]) o);
        tStart = SystemClock.elapsedRealtime();
        updateImage((byte[]) o);
    }

    private void updateImage(byte[] newImage) {

        imageTF = BitmapFactory.decodeByteArray(newImage, 0, newImage.length);

        imageView.setImageBitmap(Bitmap.createScaledBitmap(imageTF, imageView.getWidth(), imageView.getHeight(), false));


        try {
            runInference();

        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }

        MainActivity.openHomeScreen();
        disableScreen();
//        uploadFileFromByteArray();

    }

    private void enableScreen() {
//        MainActivity.enablePager();
        imageButton.setEnabled(true);
        buttonUpload.setEnabled(true);
        buttonChooseImage.setEnabled(true);
    }

    private void disableScreen() {
//        MainActivity.disablePager();
        imageButton.setEnabled(false);
        buttonUpload.setEnabled(false);
        buttonChooseImage.setEnabled(false);
    }


    private void configureLocalModelSource() {
        // [START mlkit_local_model_source]

        localModel = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("encoder.tflite")
                .build();
        // [END mlkit_local_model_source]
    }

    private FirebaseModelInterpreter createInterpreter(FirebaseCustomLocalModel localModel) throws FirebaseMLException {
        // [START mlkit_create_interpreter]
        FirebaseModelInterpreter interpreter = null;
        try {
            FirebaseModelInterpreterOptions options =
                    new FirebaseModelInterpreterOptions.Builder(localModel).build();
            interpreter = FirebaseModelInterpreter.getInstance(options);
        } catch (FirebaseMLException e) {
            // ...
        }
        // [END mlkit_create_interpreter]

        return interpreter;
    }

    private FirebaseModelInputOutputOptions createInputOutputOptions() throws FirebaseMLException {
        // [START mlkit_create_io_options]
        FirebaseModelInputOutputOptions inputOutputOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 299, 299, 3})
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 2048})
                        .build();
        // [END mlkit_create_io_options]

        return inputOutputOptions;
    }


    private float[][][][] bitmapToInputArray() {
        // [START mlkit_bitmap_input]
        Bitmap bitmap = getYourInputImage();
        bitmap = Bitmap.createScaledBitmap(bitmap, 299, 299, true);

        int batchNum = 0;
        float[][][][] input = new float[1][299][299][3];
        for (int x = 0; x < 299; x++) {
            for (int y = 0; y < 299; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.
                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;
            }
        }
        // [END mlkit_bitmap_input]

        return input;
    }

    private void runInference() throws FirebaseMLException {
//        FirebaseCustomLocalModel localModel = new FirebaseCustomLocalModel.Builder().build();
//        FirebaseModelInterpreter firebaseInterpreter = createInterpreter(localModel);


        float[][][][] input = bitmapToInputArray();
        FirebaseModelInputOutputOptions inputOutputOptions = createInputOutputOptions();

        // [START mlkit_run_inference]
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build();
        encoder.run(inputs, inOutOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                // [START_EXCLUDE]
                                // [START mlkit_read_result]
                                float[][] output = result.getOutput(0);
                                float[] probabilities = output[0];
                                uploadTransferValues(probabilities);
                                // [END mlkit_read_result]
                                // [END_EXCLUDE]

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });

        // [END mlkit_run_inference]

    }

    private Bitmap getYourInputImage() {
        // This method is just for show
//        return Bitmap.createBitmap(0, 0, Bitmap.Config.ALPHA_8);
        return this.imageTF;
    }

    private void uploadTransferValues(float[] values) {
        String arr = Arrays.toString(values);


        Map<String, Object> docData = new HashMap<>();
        docData.put("Transfer Values", arr);
        db.collection("users").document("TransferValues")
                .set(docData)
                .addOnSuccessListener(aVoid -> Log.d("TAG", "Document Succesfully written"))
                .addOnFailureListener(e -> Log.w("TAG", "Error adding document", e));


        Map<String, Object> lang = new HashMap<>();
        lang.put("LANGUAGE", Locale.getDefault().getLanguage());

        // Add a new document with a generated ID
        db.collection("users").document("LANGUAGE")
                .set(lang)
                .addOnSuccessListener(aVoid -> Log.d("TAG", "Document Succesfully written"))
                .addOnFailureListener(e -> Log.w("TAG", "Error adding document", e));

        progressBar.setVisibility(View.VISIBLE);
    }

//    public void detachListener() {
//
//        // Stop listening to changes
//        registration.remove();
//        // [END detach_listener]
//    }
//
//    public void listenToDocument() {
//        // [START listen_document]
//        final DocumentReference docRef = db.collection("cities").document("SF");
//        registration = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(@Nullable DocumentSnapshot snapshot,
//                                @Nullable FirebaseFirestoreException e) {
//                if (e != null) {
//                    Log.w(TAG, "Listen failed.", e);
//                    return;
//                }
//
//                if (snapshot != null && snapshot.exists()) {
//                    Log.d(TAG, "Current data: " + snapshot.getData());
//                } else {
//                    Log.d(TAG, "Current data: null");
//                }
//            }
//        });
//        // [END listen_document]
//    }

}


