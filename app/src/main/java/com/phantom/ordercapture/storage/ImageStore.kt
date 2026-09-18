package com.phantom.ordercapture.storage

import android.content.*
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import java.io.File

class ImageStore(private val context: Context) {
    fun read(uri: Uri, preview: Boolean=false): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder, info, _ ->
            val pixels=info.size.width.toLong()*info.size.height
            require(pixels<=32_000_000 || preview) { "ภาพเกิน 32 ล้านพิกเซล กรุณาเลือกภาพขนาดเล็กลง" }
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            if(preview) {
                val scale=1400f/maxOf(info.size.width,info.size.height)
                if(scale<1) decoder.setTargetSize((info.size.width*scale).toInt(),(info.size.height*scale).toInt())
            }
        }
    }
    fun copySource(uri: Uri): Uri {
        val file=File(context.cacheDir,"source-${System.nanoTime()}.img")
        context.contentResolver.openInputStream(uri)!!.use{input->file.outputStream().use{input.copyTo(it)}}
        return Uri.fromFile(file)
    }
    fun save(bitmap: Bitmap, png: Boolean, quality: Int): Uri {
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"OrderSlip_${System.currentTimeMillis()}.${if(png) "png" else "jpg"}")
            put(MediaStore.Images.Media.MIME_TYPE,if(png) "image/png" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/OrderSlipScene")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val uri=checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        try {
            resolver.openOutputStream(uri)!!.use { check(bitmap.compress(if(png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,quality,it)) }
            resolver.update(uri,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
            return uri
        } catch(e: Exception) { resolver.delete(uri,null,null); throw e }
    }
}
