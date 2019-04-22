package com.example.imagepart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    ImageView profile_image;
    Button btn;

    private static final int REQUEST_CODE_IMAGE_CAMERA = 102;
    private static final int REQUEST_CODE_PICK_FILE_GALLERY = 103;
    public static final int REQUEST_PERMISSION_TAKE_PICTURE = 11;
    public static final int REQUEST_PERMISSION_PICK_PHOTO = 12;
    private String deviceId, selectedFilePath, mCurrentPhotoPath;

    // Permissions
    public static String[] PERMISSIONS_TAKE_PICTURE = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
    public static String[] PERMISSIONS_PICK_PHOTO = {Manifest.permission.READ_EXTERNAL_STORAGE};

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceId = Settings.Secure.getString(getApplication().getContentResolver(), Settings.Secure.ANDROID_ID);

        profile_image = findViewById(R.id.profile_image);
        profile_image.setOnClickListener(this);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_image:
                selectImage();
                break;
            case R.id.btn:
                uploadDocuments();
//                uploadImagePartDocuments();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap bitmap;
            if (requestCode == REQUEST_CODE_PICK_FILE_GALLERY) {
                selectedFilePath = getFilePath(this, data.getData());
                bitmap = createBitMap(selectedFilePath);
                profile_image.setImageBitmap(bitmap);
            } else if (requestCode == REQUEST_CODE_IMAGE_CAMERA) {
                selectedFilePath = mCurrentPhotoPath;
                bitmap = createBitMap(selectedFilePath);
                profile_image.setImageBitmap(bitmap);
            }
        }
    }

    public static Bitmap createBitMap(String capturingImageURl) {
        File file = new File(capturingImageURl);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        return bitmap;
    }

    public static String getFilePath(Context context, Uri contentUri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select Any Option !");
        builder.setCancelable(true);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    int camPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
                    int storePermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (camPermission != PackageManager.PERMISSION_GRANTED || storePermission != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_TAKE_PICTURE, REQUEST_PERMISSION_TAKE_PICTURE);
                    } else {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            File photoFile = createImageFile();
                            if (photoFile != null) {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                startActivityForResult(takePictureIntent, REQUEST_CODE_IMAGE_CAMERA);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "You can't take pictures...", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (options[item].equals("Choose from Gallery")) {
                    // Check if we have write permission
                    int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission not granted, granting now...");
                        // We don't have permission so prompt the user
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_PICK_PHOTO, REQUEST_PERMISSION_PICK_PHOTO);
                    } else {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhoto, REQUEST_CODE_PICK_FILE_GALLERY);
                    }
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private File createImageFile() {
        // Create an image file name
        String imageFileName = "Me" + System.currentTimeMillis();
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Me");
        if (!storageDir.exists())
            storageDir.mkdir();
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Save a file: path for use with ACTION_VIEW intents
        if (image != null) {
            mCurrentPhotoPath = image.getAbsolutePath();
        }

        return image;
    }

    private void uploadDocuments() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://onmyway.clientdemos.in")
                .addConverterFactory(StringConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Api onMyWayApis = retrofit.create(Api.class);

        File file = new File(selectedFilePath);
        RequestBody mFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), mFile);

        Call<ResponseBody> uploadDocuments = onMyWayApis.registrationPart("YWRtaW46MTIzNA",
                "taya@321",
                "maha",
                "maha@gmail.com",
                "8679093465",
                "8679093465",
                "12345"
                , deviceId,
                "1",
                "device",
                "123456",
                fileToUpload);
        uploadDocuments.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: " + response.raw());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "onFailure: ");
            }
        });
    }

    private void uploadImagePartDocuments() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://onmyway.clientdemos.in")
                .addConverterFactory(StringConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Api onMyWayApis = retrofit.create(Api.class);

        RequestBody nameRequest = RequestBody.create(MediaType.parse("text/plain"), "maha");
        RequestBody emailRequest = RequestBody.create(MediaType.parse("text/plain"), "maha@gmail.com");
        RequestBody phoneNumberRequest = RequestBody.create(MediaType.parse("text/plain"), "8679093465");
        RequestBody emergencyPhoneRequest = RequestBody.create(MediaType.parse("text/plain"), "8679093465");
        RequestBody passwordRequest = RequestBody.create(MediaType.parse("text/plain"), "12345");
        RequestBody deviceIdRequest = RequestBody.create(MediaType.parse("text/plain"), deviceId);
        RequestBody roleRequest = RequestBody.create(MediaType.parse("text/plain"), "1");
        RequestBody deviceRequest = RequestBody.create(MediaType.parse("text/plain"), "device");
        RequestBody appIdRequest = RequestBody.create(MediaType.parse("text/plain"), "");

        File file = new File(selectedFilePath);
        RequestBody mFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), mFile);

        Call<ResponseBody> uploadDocuments = onMyWayApis.registrationImagePart("YWRtaW46MTIzNA",
                "taya@321",
                nameRequest,
                emailRequest,
                phoneNumberRequest,
                emergencyPhoneRequest,
                passwordRequest,
                deviceIdRequest,
                roleRequest,
                deviceRequest,
                appIdRequest,
                fileToUpload);
        uploadDocuments.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: " + response.raw());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "onFailure: ");
            }
        });
    }
}
