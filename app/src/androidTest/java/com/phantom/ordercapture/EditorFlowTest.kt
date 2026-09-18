package com.phantom.ordercapture

import android.graphics.*
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phantom.ordercapture.editor.EditorViewModel
import com.phantom.ordercapture.editor.Page
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun importedImageCanBeCroppedEditedAndSaved() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"test-paper.png")
        val b=Bitmap.createBitmap(900,1400,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(b); canvas.drawColor(Color.rgb(65,76,82))
        val paint=Paint().apply{color=Color.WHITE}; canvas.drawRect(280f,100f,620f,1290f,paint)
        paint.color=Color.BLACK; paint.textSize=20f
        canvas.drawText("ORDER SLIP",300f,145f,paint)
        for(y in 180..1200 step 36) canvas.drawLine(300f,y.toFloat(),600f,y.toFloat(),paint)
        file.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}; b.recycle()
        lateinit var vm: EditorViewModel
        rule.runOnUiThread{vm=ViewModelProvider(rule.activity)[EditorViewModel::class.java]}
        rule.waitUntil(15000){!vm.busy}
        rule.onNodeWithText("ใบฝากสั่ง\nในฉากของคุณ").assertIsDisplayed()
        rule.runOnUiThread{vm.open(Uri.fromFile(file))}
        rule.waitUntil(30000){!vm.busy && vm.page==Page.CROP}
        rule.onNodeWithText("ยืนยันขอบ · ไปจัดฉาก").performClick()
        rule.waitUntil(30000){!vm.busy && vm.page==Page.EDITOR}
        rule.onNodeWithText("สุ่มฉากใหม่").performClick()
        rule.waitUntil(15000){!vm.busy}
        rule.onNodeWithText("ดูตัวอย่างและบันทึก").performClick()
        rule.onNodeWithText("บันทึกลงแกลเลอรี").performClick()
        rule.waitUntil(30000){!vm.busy && vm.saved!=null}
        assertNotNull(vm.saved)
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val screenshotDir=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let{File(it)}?:context.filesDir
        screenshotDir.mkdirs()
        File(screenshotDir,"editor-export.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        context.contentResolver.delete(vm.saved!!,null,null)
        file.delete()
    }
}
