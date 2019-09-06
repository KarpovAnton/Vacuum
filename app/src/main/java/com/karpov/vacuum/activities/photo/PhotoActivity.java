package com.karpov.vacuum.activities.photo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karpov.vacuum.R;
import com.karpov.vacuum.utils.ImageUtils;
import com.karpov.vacuum.views.adapters.PhotoEditAdapter;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.support.DaggerAppCompatActivity;

public class PhotoActivity extends DaggerAppCompatActivity implements PhotoContract.View {

    private static final int ADD_IMAGE_REQUEST_CODE = 1;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @Inject
    PhotoPresenter presenter;

    @Inject
    PhotoRouter router;

    ArrayList<String> photoImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);
        photoImages = getIntent().getParcelableExtra("imageList");
        initViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.dropView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.takeView(this);
        if (photoImages != null)
            presenter.setPhotos(photoImages);
    }

    void initViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        //presenter.setUpAdapter();
    }

    @OnClick({R.id.addPhotoButton})
    void onAddPhotoClick() {
        Intent intent = CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                .getIntent(this);
        startActivityForResult(intent, ADD_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri imageUri = result.getUri();

            File photoFile = new File(ImageUtils.getRealPathFromURIAndResize(this, imageUri));
            int size = (int) photoFile.length();
            byte[] bytes = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(photoFile));
                buf.read(bytes, 0, bytes.length);
                buf.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String stringPhoto = Base64.encodeToString(bytes, Base64.DEFAULT);

            presenter.sendPhotoImage(photoFile, imageUri.toString());
        }
    }

    @Override
    public void setAdapter(PhotoEditAdapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRemoveImage(int pos) {
        photoImages.remove(pos);
    }
}
