package com.rammstein.imagepickernewone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class GalleryActivity : AppCompatActivity() {

    private val RES_1080 = 1080

    private val FILE_PROVIDER = ".fileprovider"

    private val CAMERA_IMAGE_CAPTURE = 3
    private val CAMERA_IMAGE_FILE_PREFIX = "JPEG"
    private val CAMERA_IMAGE_FILE_SUFFIX = ".jpg"
    private val CAMERA_IMAGE_FILE_DATE_FORMAT = "yyyMMdd_HHmmss"

    private val STORAGE_IMAGE_GET = 4
    private val STORAGE_IMAGE_MIME_TYPE = "image/*"

    private val compositeDisposable = CompositeDisposable()

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fabCamera.setOnClickListener { view -> dispatchTakePictureFromCameraIntent() }
        fabGallery.setOnClickListener { view -> dispatchTakePictureFromStorageIntent() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            mainImage.setImageResource(0)
            when (requestCode) {
                CAMERA_IMAGE_CAPTURE -> {
                    mainProgress.visibility = View.VISIBLE
                    compositeDisposable.add(BitmapUtils().getBitmapFromFile(contentResolver, currentPhotoPath
                            ?: "", RES_1080)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doAfterTerminate { mainProgress.visibility = View.GONE }
                            .subscribe({ image ->
                                mainImage.setImageBitmap(image)
                            }, { throwable ->
                                throwable.printStackTrace()
                                Toast.makeText(this@GalleryActivity, throwable.message, Toast.LENGTH_LONG).show()
                            }))
                }
                STORAGE_IMAGE_GET -> {
                    data?.let {
                        mainProgress.visibility = View.VISIBLE
                        compositeDisposable.add(
                                Single.fromCallable {
                                    return@fromCallable createImageFile(
                                            imageFileName = getImageFileName(),
                                            imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
                                    )
                                }.flatMap { file -> BitmapUtils().getBitmapFromUri(contentResolver, it.data, file, RES_1080) }
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doAfterTerminate { mainProgress.visibility = View.GONE }
                                        .subscribe({ image ->
                                            mainImage.setImageBitmap(image)
                                        }, { throwable ->
                                            throwable.printStackTrace()
                                            Toast.makeText(this@GalleryActivity, throwable.message, Toast.LENGTH_LONG).show()
                                        }))
                    }
                }
            }
        }
    }

    private fun dispatchTakePictureFromStorageIntent() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = STORAGE_IMAGE_MIME_TYPE
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, STORAGE_IMAGE_GET)
        }
    }

    private fun dispatchTakePictureFromCameraIntent() {
        val takePictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {

            val imageFile = createImageFile(
                    imageFileName = getImageFileName(),
                    imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
            )
            val photoUri = FileProvider.getUriForFile(this, packageName.plus(FILE_PROVIDER), imageFile)
            currentPhotoPath = imageFile.absolutePath

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, CAMERA_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(imageFileName: String, imageFilePath: String): File {
        val storageDir: File = File(imageFilePath)
        return File.createTempFile(imageFileName, CAMERA_IMAGE_FILE_SUFFIX, storageDir)
    }

    private fun getImageFileName(): String = CAMERA_IMAGE_FILE_PREFIX.plus(SimpleDateFormat(CAMERA_IMAGE_FILE_DATE_FORMAT).format(Date())).plus("_")
}
