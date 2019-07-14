package com.example.ocrproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private TessBaseAPI tessBaseApi;
    static final int REQUEST = 1;
    Uri outputFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button cameraAccess = findViewById(R.id.main);
        cameraAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraActivity();
            }
        });
    }
    private void cameraActivity() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        String PATH = Environment.getExternalStorageDirectory().toString() + "/OCR/imgs";
        File dir = new File(PATH);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ERROR", "Unable to create folder.");
            }
        } else {
            Log.i("INFO", "Created folder " + PATH);
        }
        String IMG = PATH + "/ocr.jpg";
        outputFile = Uri.fromFile(new File(IMG));
        final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFile);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST);

    }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        //making photo
        if (requestCode == REQUEST && resultCode == Activity.RESULT_OK) {
            Log.i("INFO", "Picture taken");
            OCR();
        } else {
            Toast.makeText(this, "Error taking picture. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
    public void OCR() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        String PATH = Environment.getExternalStorageDirectory().toString() + "/OCR Project/tessdata";
        File dir = new File(PATH);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ERROR", "Unable to create folder." + PATH);
            }
        } else {
            Log.i("INFO", "Created folder " + PATH);
        }
        try {
            String fileList[] = getAssets().list("tessdata");

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = Environment.getExternalStorageDirectory().toString() + "/OCR Project/tessdata/";
                Log.i("LOC", pathToDataFile);

                    Log.d("INFO", "Attempting to copy" + fileName + "to tessdata");
                    InputStream in = getAssets().open("tessdata/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile + fileName);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d("INFO", "Copied " + fileName + "to tessdata");

            }
        } catch (IOException e) {
            Log.e("ERROR", "Unable to copy files to tessdata " + e.toString());
        }
        new properOCR().execute();
    }
    private class properOCR extends AsyncTask<String, Void, String> {
        public String result;
        @Override
        protected String doInBackground(String... params) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
                Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getPath(), options);

                result = extractText(bitmap);
                Log.d("INFO", "RESULT:" +result);
                TextView txt = (TextView) findViewById(R.id.textView);
                txt.setText(result);

            } catch (Exception e) {
                Log.e("ERROR", e.getMessage());
            }
            return "Executed";
        }
        @Override
        protected void onPostExecute(String result) {
            TextView txt = (TextView) findViewById(R.id.textView);
            //txt.setText(result);

        }
        private String extractText(Bitmap bitmap) {
            try {
                tessBaseApi = new TessBaseAPI();
                Log.d("INFO", "TessBaseWorks");
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage());
                if (tessBaseApi == null) {
                    Log.e("ERROR", "TessBaseAPI is null. TessFactory not returning tess object.");
                }
            }

            tessBaseApi.init(Environment.getExternalStorageDirectory().toString() + "/OCR Project/", "eng");

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

            Log.d("INFO", "Training file loaded");
            tessBaseApi.setImage(bitmap);
            String extractedText = "empty result";
            try {
                extractedText = tessBaseApi.getUTF8Text();
            } catch (Exception e) {
                Log.e("ERROR", "Error in recognizing text.");
            }
            tessBaseApi.end();
            return extractedText;
        }
    }
}
