package com.phantom.ordercapture.editor

import android.app.Application
import android.graphics.*
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phantom.ordercapture.cv.DocumentProcessor
import com.phantom.ordercapture.model.*
import com.phantom.ordercapture.renderer.SceneRenderer
import com.phantom.ordercapture.storage.ImageStore
import com.phantom.ordercapture.template.TemplateRepository
import kotlinx.coroutines.*

enum class Page { HOME, CAMERA, CROP, EDITOR, EXPORT, TEMPLATES }
class EditorViewModel(app: Application): AndroidViewModel(app) {
    var page by mutableStateOf(Page.HOME)
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    var source by mutableStateOf<Bitmap?>(null); private set
    var document by mutableStateOf<Bitmap?>(null); private set
    var documentPreview by mutableStateOf<Bitmap?>(null); private set
    var background by mutableStateOf<Bitmap?>(null); private set
    var candidates by mutableStateOf<List<Quad>>(emptyList()); private set
    var crop by mutableStateOf(Quad.inset())
    var placement by mutableStateOf(Quad.inset())
    var scenes by mutableStateOf<List<Scene>>(emptyList()); private set
    var scene by mutableStateOf<Scene?>(null); private set
    var category by mutableStateOf("all")
    var advanced by mutableStateOf(false)
    var png by mutableStateOf(false)
    var quality by mutableStateOf(95f)
    var saved by mutableStateOf<Uri?>(null); private set
    private var sourceUri: Uri?=null
    private var turns=0
    private val images=ImageStore(app)
    private val templates=TemplateRepository(app)
    private val cv=DocumentProcessor()
    init { work { scenes=withContext(Dispatchers.IO){templates.list()} } }
    private fun work(block: suspend ()->Unit) {
        if(busy) return
        busy=true
        viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { message=e.message?:"เกิดข้อผิดพลาด กรุณาลองใหม่" }
            catch(e: OutOfMemoryError) { message="หน่วยความจำไม่พอ กรุณาใช้ภาพขนาดเล็กลง" }
            finally { busy=false }
        }
    }
    fun open(uri: Uri)=work {
        val result=withContext(Dispatchers.IO) { val cached=images.copySource(uri); cached to images.read(cached,true) }
        sourceUri?.path?.let { java.io.File(it).delete() }
        sourceUri=result.first; source=result.second; turns=0; saved=null
        page=Page.CROP
        candidates=withContext(Dispatchers.Default){cv.detect(result.second)}
        crop=candidates.firstOrNull()?:Quad.inset()
    }
    fun autoDetect()=work { candidates=withContext(Dispatchers.Default){cv.detect(checkNotNull(source))}; crop=candidates.firstOrNull()?:Quad.inset() }
    fun rotate()=work {
        source=withContext(Dispatchers.Default) { val b=checkNotNull(source); Bitmap.createBitmap(b,0,0,b.width,b.height,Matrix().apply{postRotate(90f)},true) }
        turns=(turns+1)%4; candidates=emptyList(); crop=Quad.inset()
    }
    fun confirmCrop()=work {
        val quad=crop
        document=withContext(Dispatchers.Default) {
            val raw=images.read(checkNotNull(sourceUri))
            val rotated=if(turns==0) raw else Bitmap.createBitmap(raw,0,0,raw.width,raw.height,Matrix().apply{postRotate(turns*90f)},true)
            try { cv.correct(rotated,quad) } finally { if(rotated!==raw) rotated.recycle(); raw.recycle() }
        }
        documentPreview=withContext(Dispatchers.Default) {
            val d=checkNotNull(document)
            val scale=minOf(1f,1600f/maxOf(d.width,d.height))
            if(scale==1f) d else Bitmap.createScaledBitmap(d,maxOf(1,(d.width*scale).toInt()),maxOf(1,(d.height*scale).toInt()),true)
        }
        if(scene==null) scene=scenes.firstOrNull()
        background=scene?.let{withContext(Dispatchers.IO){templates.load(it,true)}}
        randomPosition(); page=Page.EDITOR
    }
    fun selectScene(value: Scene)=work {
        val bitmap=withContext(Dispatchers.IO){templates.load(value,true)}
        background=bitmap; scene=value; randomPosition()
    }
    fun randomScene() {
        val options=scenes.filter{category=="all" || it.category==category}
        if(options.isEmpty()) { message="หมวดนี้ยังไม่มีฉาก เพิ่มภาพฉากหลังได้จากปุ่ม +"; return }
        selectScene((options.filter{it.id!=scene?.id}.ifEmpty{options}).random())
    }
    fun randomPosition() {
        val d=document?:return; val b=background?:return; val s=scene?:return
        placement=Geometry.randomPlacement(s.placement,d.width.toFloat()/d.height,b.width.toFloat()/b.height)
    }
    fun fit() {
        val d=document?:return; val b=background?:return
        placement=Geometry.randomPlacement(Placement(widthMin=.7f,widthMax=.7f,rotationMin=0f,rotationMax=0f,offsetX=0f,offsetY=0f),d.width.toFloat()/d.height,b.width.toFloat()/b.height)
    }
    fun center() { placement=Geometry.contain(Quad(placement.points.map{Pt(it.x+.5f-placement.points.map{p->p.x}.average().toFloat(),it.y+.5f-placement.points.map{p->p.y}.average().toFloat())})) }
    fun importScene(uri: Uri)=work {
        val s=withContext(Dispatchers.IO){templates.import(uri,if(category=="all") "plain" else category)}
        scenes=withContext(Dispatchers.IO){templates.list()}; scene=s
        background=withContext(Dispatchers.IO){templates.load(s,true)}
        randomPosition(); message="เพิ่มฉากหลังแล้ว"
    }
    fun export()=work {
        val s=checkNotNull(scene); val d=checkNotNull(document); val q=placement
        saved=withContext(Dispatchers.Default) {
            val bg=templates.load(s)
            try { val output=SceneRenderer.render(bg,d,q); try { images.save(output,png,quality.toInt()) } finally { output.recycle() } }
            finally { bg.recycle() }
        }
        message="บันทึกแล้วใน Pictures/OrderSlipScene"
    }
}
