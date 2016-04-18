package edu.fiu.mpact.wifilocalizer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.camera.CropImageIntentBuilder;

import java.io.File;
import java.io.IOException;


public class AddMapActivity extends Activity {
    private static final int IMAGE_PICKER_REQUEST_CODE = 1;
    private static final int IMAGE_CROPPER_REQUEST_CODE = 2;

    private File outputFile = null;
    private ImageView imageView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_map);

        this.imageView = (ImageView) findViewById(R.id.img_map);
    }

    public void startPhotoPicker(View view) {
        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Map Image"), IMAGE_PICKER_REQUEST_CODE);
    }

    public void saveMap(View view) {
        final String mapName = ((EditText) findViewById(R.id.map_name)).getText().toString();

        if (mapName.isEmpty()) {
            Toast.makeText(this, getText(R.string.toast_name_warning), Toast.LENGTH_LONG).show();
            return;
        } else if (this.outputFile == null) {
            Toast.makeText(this, getText(R.string.toast_map_warning), Toast.LENGTH_LONG).show();
            return;
        }

        final ContentValues values = Utils.createNewMapContentValues(mapName, Uri.fromFile(this.outputFile));
        getContentResolver().insert(DataProvider.MAPS_URI, values);

        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == IMAGE_PICKER_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            try {
                this.outputFile = File.createTempFile("croppedMap", ".jpg", getFilesDir());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Uri croppedImage = Uri.fromFile(this.outputFile);
            CropImageIntentBuilder cropImage = new CropImageIntentBuilder(1000, 1000, croppedImage);
            cropImage.setOutlineColor(0xFF03A9F4);
            cropImage.setSourceImage(data.getData());

            startActivityForResult(cropImage.getIntent(this), IMAGE_CROPPER_REQUEST_CODE);
        } else if ((requestCode == IMAGE_CROPPER_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            findViewById(R.id.btn_choose_map_image).setVisibility(View.GONE);
            this.imageView.setImageBitmap(BitmapFactory.decodeFile(this.outputFile.getAbsolutePath()));
            this.imageView.setVisibility(View.VISIBLE);
        }
    }
}
