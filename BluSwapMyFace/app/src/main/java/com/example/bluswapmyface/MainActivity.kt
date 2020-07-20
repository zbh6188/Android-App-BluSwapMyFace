package com.example.bluswapmyface

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.divyanshu.draw.widget.DrawView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private var mydrawView1: DrawView? = null
    private var mydrawView2: DrawView? = null
    private var imageUri1: Uri? = null
    private var imageUri2: Uri? = null
    //private var pixels1 = IntArray(100)
    private var faceData1 = arrayOf(0, 0, 0, 0, 0, 0)
    private var faceData2 = arrayOf(0, 0, 0, 0, 0, 0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mydrawView1 = findViewById(R.id.draw_view1)
        mydrawView2 = findViewById(R.id.draw_view2)
        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
        savedInstanceState?.let {
            imageUri1 = it.getParcelable(KEY_IMAGE_URI_1)
            imageUri2 = it.getParcelable(KEY_IMAGE_URI_2)
        }

    }


    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info =
                this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>();
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }
        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    fun startCamera1(view: View) {
        imageUri1 = null
        mydrawView1?.background = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera 1")
            imageUri1 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri1)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_TAKE_1)
        }
    }

    fun startCamera2(view: View) {
        imageUri2 = null
        mydrawView2?.background = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera 2")
            imageUri2 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri2)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_TAKE_2)
        }
    }

    fun chooseImageIntentForResult1(view:View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE_1)
    }

    fun chooseImageIntentForResult2(view:View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE_2)
    }

    private fun displayImage1() {
        try {
            if (imageUri1 == null) {
                return
            }

            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
                ImageDecoder.decodeBitmap(source)
            }
            var bmpCopy = imageBitmap.copy(Bitmap.Config.ARGB_8888,true)
            draw_view1?.background = BitmapDrawable(resources, bmpCopy)
            draw_view1?.setColor(Color.BLACK)
            var image = FirebaseVisionImage.fromBitmap(bmpCopy)
            faceDetection1(image)
        } catch (e: IOException) {

        }
    }

    private fun displayImage2() {
        try {
            if (imageUri2 == null) {
                return
            }

            val imageBitmap2 = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
            } else {
                val source2 = ImageDecoder.createSource(contentResolver, imageUri2!!)
                ImageDecoder.decodeBitmap(source2)
            }
            var bmpCopy2 = imageBitmap2.copy(Bitmap.Config.ARGB_8888,true)
            draw_view2?.background = BitmapDrawable(resources, imageBitmap2)
            draw_view2?.setColor(Color.BLACK)
            var image = FirebaseVisionImage.fromBitmap(bmpCopy2)
            faceDetection2(image)
        } catch (e: IOException) {

        }
    }

    fun swap(view: View) {
        if (faceData1[0] == 0 || faceData2[0] == 0) {
            return
        }
        val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
            ImageDecoder.decodeBitmap(source)
        }
        var bmpCopy = imageBitmap.copy(Bitmap.Config.ARGB_8888,true)
        var face1 = Bitmap.createBitmap(bmpCopy, faceData1[0], faceData1[1], faceData1[2] - faceData1[0], faceData1[5] - faceData1[1])
        var pix1 = IntArray(face1.width * face1.height)
        face1.getPixels(pix1, 0, face1.width/1, 0, 0, face1.width/1, face1.height/1)
        val imageBitmap2 = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
        } else {
            val source2 = ImageDecoder.createSource(contentResolver, imageUri2!!)
            ImageDecoder.decodeBitmap(source2)
        }
        var bmpCopy2 = imageBitmap2.copy(Bitmap.Config.ARGB_8888,true)
        var face2 = Bitmap.createBitmap(bmpCopy2, faceData2[0], faceData2[1], faceData2[2] - faceData2[0], faceData2[5] - faceData2[1])
        var pix2 = IntArray(face2.width * face2.height)
        face2.getPixels(pix2, 0, face2.width/1, 0, 0, face2.width/1, face2.height/1)

        bmpCopy.setPixels(pix2, 0, face2.width/1, faceData1[0]/1, faceData1[1]/1, face2.width/1, face2.height/1)
        bmpCopy2.setPixels(pix1, 0, face1.width/1, faceData2[0]/1, faceData2[1]/1, face1.width/1, face1.height/1)
        draw_view1?.background = BitmapDrawable(resources, bmpCopy)
        draw_view2?.background = BitmapDrawable(resources, bmpCopy2)
    }

    fun displayBlur1(view: View) {
         if (faceData1[0] == 0) {
             return
         }
         val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
             MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
         } else {
             val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
             ImageDecoder.decodeBitmap(source)
         }
         var bmpCopy = imageBitmap.copy(Bitmap.Config.ARGB_8888,true)
         var face1 = Bitmap.createBitmap(bmpCopy, faceData1[0], faceData1[1], faceData1[2] - faceData1[0], faceData1[5] - faceData1[1])
         var blurFace1 = bitmapBlur(face1, 1.0f, face1.width/2)
         val w = blurFace1!!.width
         val h = blurFace1!!.height
         var pixels1 = IntArray(w * h)
         blurFace1!!.getPixels(pixels1,0, w/1,0,0, w/1, h/1)
         bmpCopy.setPixels(pixels1, 0, w/1, faceData1[0]/1, faceData1[1]/1, w/1, h/1)
         draw_view1?.background = BitmapDrawable(resources, bmpCopy)
    }

    fun clear1(view: View) {
        val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
            ImageDecoder.decodeBitmap(source)
        }
        var bmpCopy = imageBitmap.copy(Bitmap.Config.ARGB_8888,true)
        draw_view1?.background = BitmapDrawable(resources, bmpCopy)
    }

    fun displayBlur2(view: View) {
        if (faceData2[0] == 0) {
            return
        }
        val imageBitmap2 = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri2!!)
            ImageDecoder.decodeBitmap(source)
        }
        var bmpCopy2 = imageBitmap2.copy(Bitmap.Config.ARGB_8888,true)
        var face2 = Bitmap.createBitmap(bmpCopy2, faceData2[0], faceData2[1], faceData2[2] - faceData2[0], faceData2[5] - faceData2[1])
        var blurFace2 = bitmapBlur(face2, 1.0f, face2.width/2)
        val w = blurFace2!!.width
        val h = blurFace2!!.height
        var pixels1 = IntArray(w * h)
        blurFace2!!.getPixels(pixels1,0, w/1,0,0, w/1, h/1)
        bmpCopy2.setPixels(pixels1, 0, w/1, faceData2[0]/1, faceData2[1]/1, w/1, h/1)
        draw_view2?.background = BitmapDrawable(resources, bmpCopy2)
    }

    fun clear2(view: View) {
        val imageBitmap2 = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri2!!)
            ImageDecoder.decodeBitmap(source)
        }
        var bmpCopy2 = imageBitmap2.copy(Bitmap.Config.ARGB_8888,true)
        draw_view2?.background = BitmapDrawable(resources, bmpCopy2)
    }

    private fun faceDetection1(image: FirebaseVisionImage) {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                processFace1(faces)
            }
            .addOnFailureListener { e -> // Task failed with an exception
            }
    }

    private fun faceDetection2(image: FirebaseVisionImage) {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                processFace2(faces)
            }
            .addOnFailureListener { e -> // Task failed with an exception
            }
    }


    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) { putParcelable(KEY_IMAGE_URI_1, imageUri1)
            putParcelable(KEY_IMAGE_URI_2, imageUri2)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_TAKE_1 && resultCode == Activity.RESULT_OK) {
            displayImage1()
        } else if (requestCode == REQUEST_IMAGE_TAKE_2 && resultCode == Activity.RESULT_OK) {
            displayImage2()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE_1 && resultCode == Activity.RESULT_OK) {
            imageUri1 = data!!.data
            displayImage1()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE_2 && resultCode == Activity.RESULT_OK) {
            imageUri2 = data!!.data
            displayImage2()
        }
    }


    private fun processFace1(faces: List<FirebaseVisionFace>) {
        if (faces.size == 0) {
            return
        }
        val face = faces[0]
        val center_x = (face.boundingBox.centerX().toInt())
        val center_y = (face.boundingBox.centerY().toInt())
        var leftEyePosX = 0
        var leftEyePosY = 0
        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
        leftEye?.let {
            leftEyePosX = leftEye.position.x.toInt()
            leftEyePosY = leftEye.position.y.toInt()
        }
        var rightEyePosX  = 0
        var rightEyePosY  = 0
        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
        rightEye?.let {
            rightEyePosX = rightEye.position.x.toInt()
            rightEyePosY = rightEye.position.y.toInt()
        }
        var mouthX = 0
        var mouthY = 0
        val mouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
        mouth?.let {
            mouthX = mouth.position.x.toInt()
            mouthY = mouth.position.y.toInt()
        }
        faceData1 = arrayOf(leftEyePosX, leftEyePosY, rightEyePosX, rightEyePosY, mouthX, mouthY)
    }

    private fun processFace2(faces: List<FirebaseVisionFace>) {
        if (faces.size == 0) {
            return
        }
        val face = faces[0]
        val center_x = (face.boundingBox.centerX().toInt())
        val center_y = (face.boundingBox.centerY().toInt())
        var leftEyePosX = 0
        var leftEyePosY = 0
        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
        leftEye?.let {
            leftEyePosX = leftEye.position.x.toInt()
            leftEyePosY = leftEye.position.y.toInt()
        }
        var rightEyePosX  = 0
        var rightEyePosY  = 0
        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
        rightEye?.let {
            rightEyePosX = rightEye.position.x.toInt()
            rightEyePosY = rightEye.position.y.toInt()
        }
        var mouthX = 0
        var mouthY = 0
        val mouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
        mouth?.let {
            mouthX = mouth.position.x.toInt()
            mouthY = mouth.position.y.toInt()
        }
        faceData2 = arrayOf(leftEyePosX, leftEyePosY, rightEyePosX, rightEyePosY, mouthX, mouthY)
    }


    fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        yw = yi
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }



    companion object {
        private const val KEY_IMAGE_URI_1 = "123"
        private const val KEY_IMAGE_URI_2 = "456"
        private const val REQUEST_IMAGE_TAKE_1 = 2
        private const val REQUEST_IMAGE_TAKE_2 = 7
        private const val REQUEST_CHOOSE_IMAGE_1 = 3
        private const val REQUEST_CHOOSE_IMAGE_2 = 4
        private const val PERMISSION_REQUESTS = 1
    }
}


