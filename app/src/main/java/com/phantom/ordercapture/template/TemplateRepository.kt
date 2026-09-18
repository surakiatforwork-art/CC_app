package com.phantom.ordercapture.template

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.phantom.ordercapture.model.*
import com.phantom.ordercapture.storage.ImageStore
import org.json.JSONObject
import java.io.File

class TemplateRepository(private val context: Context) {
    private val directory=File(context.filesDir,"scenes").apply{mkdirs()}
    fun list(): List<Scene> {
        val bundled=context.assets.list("scenes").orEmpty().filter{it.endsWith(".json")}.map {
            context.assets.open("scenes/$it").bufferedReader().use{reader->parse(JSONObject(reader.readText()),false)}
        }
        val local=directory.listFiles().orEmpty().filter{it.extension=="json"}.mapNotNull {
            runCatching{parse(JSONObject(it.readText()),true)}.getOrNull()
        }
        return bundled+local
    }
    private fun parse(json: JSONObject, local: Boolean): Scene {
        val p=json.optJSONObject("paperPlacement") ?: JSONObject()
        fun f(key:String,default:Float)=p.optDouble(key,default.toDouble()).toFloat()
        val placement=Placement(f("centerX",.5f),f("centerY",.5f),f("widthMin",.28f),f("widthMax",.36f),f("rotationMin",-3f),f("rotationMax",3f),f("offsetX",.03f),f("offsetY",.03f))
        require(placement.widthMin>0 && placement.widthMax>=placement.widthMin)
        return Scene(json.getString("id"),json.optString("name",json.getString("id")),json.getString("category"),json.getString("image"),placement,local)
    }
    fun load(scene: Scene, preview: Boolean=false): Bitmap {
        fun stream()=if(scene.local) File(directory,scene.image).inputStream() else context.assets.open("scenes/${scene.image}")
        val options=BitmapFactory.Options()
        stream().use { options.inJustDecodeBounds=true; BitmapFactory.decodeStream(it,null,options) }
        require(options.outWidth>0 && options.outHeight>0) { "ไม่สามารถเปิดฉากหลังได้" }
        require(options.outWidth.toLong()*options.outHeight<=32_000_000) { "ฉากหลังเกิน 32 ล้านพิกเซล" }
        options.inJustDecodeBounds=false
        options.inSampleSize=1
        if(preview) while(maxOf(options.outWidth,options.outHeight)/options.inSampleSize>1600) options.inSampleSize*=2
        return stream().use { checkNotNull(BitmapFactory.decodeStream(it,null,options)) }
    }
    fun import(uri: Uri, category: String): Scene {
        val bitmap=ImageStore(context).read(uri)
        val id="scene_${System.currentTimeMillis()}"
        val image="$id.png"
        try { File(directory,image).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } }
        finally { bitmap.recycle() }
        val json=JSONObject().put("id",id).put("name","ฉากของฉัน ${list().count{it.local}+1}").put("category",category).put("image",image)
        File(directory,"$id.json").writeText(json.toString(2))
        return parse(json,true)
    }
}
