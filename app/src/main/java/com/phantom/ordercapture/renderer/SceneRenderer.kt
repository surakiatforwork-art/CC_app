package com.phantom.ordercapture.renderer

import android.graphics.*
import com.phantom.ordercapture.model.Quad

object SceneRenderer {
    fun render(background: Bitmap, document: Bitmap, quad: Quad): Bitmap {
        require(quad.valid())
        val output=Bitmap.createBitmap(background.width,background.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output)
        canvas.drawBitmap(background,0f,0f,null)
        drawDocument(canvas,document,quad,background.width.toFloat(),background.height.toFloat())
        return output
    }
    fun drawDocument(canvas: Canvas, document: Bitmap, quad: Quad, width: Float, height: Float) {
        val source=floatArrayOf(0f,0f,document.width.toFloat(),0f,document.width.toFloat(),document.height.toFloat(),0f,document.height.toFloat())
        val target=quad.points.flatMap{listOf(it.x*width,it.y*height)}.toFloatArray()
        val matrix=Matrix()
        if(matrix.setPolyToPoly(source,0,target,0,4)) canvas.drawBitmap(document,matrix,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }
}
