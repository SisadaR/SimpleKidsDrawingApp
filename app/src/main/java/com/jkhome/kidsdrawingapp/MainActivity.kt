package com.jkhome.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var mDrawingView: DrawingView
    private lateinit var mPaintColors: LinearLayout
    private  var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDrawingView = findViewById(R.id.drawing_view)
        mDrawingView.setSizeForBrush(20f)

        mPaintColors = findViewById(R.id.ll_paint_color)

        mImageButtonCurrentPaint = mPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))

        findViewById<ImageButton>(R.id.ib_undo).setOnClickListener {
            mDrawingView.onClickUndo()
        }

        findViewById<ImageButton>(R.id.ib_brush).setOnClickListener {
            showBrushSizeChosserDialog()
        }

        findViewById<ImageButton>(R.id.ib_save).setOnClickListener {
            if (isReadStorageAllowed()){
                Log.i("simple draw app", "storage allowed")
                //BitmapAsyncTask( getBitmapFromView(findViewById<View>(R.id.fl_drawing_view_container))).execute()
                saveWithCoroutine(getBitmapFromView(findViewById<View>(R.id.fl_drawing_view_container)))
            }
            else
            {
                Log.i("simple draw app", "storage  not allowed")
                requestStoragePermission()
            }
        }

        findViewById<ImageButton>(R.id.ib_gallery).setOnClickListener {
            if(isReadStorageAllowed())
            {
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent,GALLERY)
            }
            else
            {
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY)
            {
                try {
                    if (data!!.data != null){
                        val iv_background = findViewById<ImageView>(R.id.iv_background)
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }
                    else
                    {
                        Toast.makeText(this,"Error in parsing the image",Toast.LENGTH_SHORT).show()
                    }
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private  fun showBrushSizeChosserDialog(){
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size:")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            mDrawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            mDrawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            mDrawingView.setSizeForBrush(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view != mImageButtonCurrentPaint)
        {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            mDrawingView.setColor(colorTag)

            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint = imageButton

        }
    }

    private fun requestStoragePermission(){
        val permissions =  arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permissions.toString()))
        {
            Toast.makeText(this,"Need permission to add a background images",Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(this,permissions, STORAGE_PERMISSION_CODE)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permissions Granted",Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap);
        val bgDrawable = view.background
        if(bgDrawable != null)
        {
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap

    }

    private fun saveWithCoroutine(mBitmap: Bitmap){


        Toast.makeText(this@MainActivity, "StartLoad",Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.Main).launch{

            val dialog = Dialog(this@MainActivity)
            dialog.setContentView(R.layout.dialog_custom_progress)
            dialog.show()

            val deferred = async(Dispatchers.IO) {
                bitmapSave(mBitmap)
            }.await()

            Toast.makeText(this@MainActivity, "File Saved $deferred",Toast.LENGTH_LONG).show()
            dialog.dismiss()

        }
    }

    private fun showProgressDialog(){
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_custom_progress)
        dialog.show()

    }

    private suspend fun bitmapSave(mBitmap: Bitmap) : String
    {
         var result = ""
            if(mBitmap != null)
            {
                try {
                    val byte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90,byte)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "KidDrawApp_"
                            + System.currentTimeMillis() / 1000
                            + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(byte.toByteArray())
                    fos.close()
                    result = f.absolutePath

                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }

            }

        return result;
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>(){

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap != null)
            {
                try {
                    val byte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90,byte)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "KidDrawApp_"
                            + System.currentTimeMillis() / 1000
                            + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(byte.toByteArray())
                    fos.close()
                    result = f.absolutePath

                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }

            }
            return result;
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!result!!.isEmpty())
            {
                Toast.makeText(this@MainActivity , "File Saved : $result" , Toast.LENGTH_LONG).show()
            }else
            {
                Toast.makeText(this@MainActivity , "File not saved", Toast.LENGTH_LONG).show()
            }
        }
    }
    companion object
    {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}