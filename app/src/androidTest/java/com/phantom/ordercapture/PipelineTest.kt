package com.phantom.ordercapture

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phantom.ordercapture.cv.DocumentProcessor
import com.phantom.ordercapture.model.*
import com.phantom.ordercapture.renderer.SceneRenderer
import com.phantom.ordercapture.storage.ImageStore
import com.phantom.ordercapture.template.TemplateRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PipelineTest {
    @Test fun detectCropCompositeAndExportAtOriginalResolution() {
        val source=Bitmap.createBitmap(1200,1800,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(source); canvas.drawColor(Color.DKGRAY)
        val paint=Paint().apply{color=Color.WHITE}
        canvas.drawRect(420f,140f,780f,1670f,paint)
        paint.color=Color.BLACK; paint.strokeWidth=2f
        for(y in 220..1600 step 40) canvas.drawLine(445f,y.toFloat(),755f,y.toFloat(),paint)
        val cv=DocumentProcessor(); val candidates=cv.detect(source)
        assertTrue("OpenCV must detect the long paper",candidates.isNotEmpty())
        val crop=cv.correct(source,candidates.first())
        assertTrue(crop.height>crop.width*3)
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val repo=TemplateRepository(context); val scene=repo.list().first()
        val preview=repo.load(scene,true)
        assertTrue(maxOf(preview.width,preview.height)<=1600)
        preview.recycle()
        val background=repo.load(scene)
        val q=Geometry.randomPlacement(scene.placement,crop.width.toFloat()/crop.height,background.width.toFloat()/background.height)
        val output=SceneRenderer.render(background,crop,q)
        assertEquals(1800,output.width); assertEquals(2400,output.height)
        val store=ImageStore(context)
        for(png in listOf(false,true)) {
            val uri=store.save(output,png,95)
            try {
                val saved=store.read(uri)
                assertEquals(output.width,saved.width); assertEquals(output.height,saved.height)
                saved.recycle()
            } finally { context.contentResolver.delete(uri,null,null) }
        }
        source.recycle(); crop.recycle(); background.recycle(); output.recycle()
    }
}
