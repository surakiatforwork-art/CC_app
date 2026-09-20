package com.phantom.ordercapture.renderer

import android.graphics.*
import com.phantom.ordercapture.model.Quad
import com.phantom.ordercapture.model.SceneEffects
import kotlin.math.roundToInt

object SceneRenderer {
    fun render(background: Bitmap, document: Bitmap, quad: Quad, effects: SceneEffects=SceneEffects()): Bitmap {
        require(quad.valid())
        val output=Bitmap.createBitmap(background.width,background.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output)
        canvas.drawBitmap(background,0f,0f,null)
        drawDocument(canvas,document,quad,background.width.toFloat(),background.height.toFloat(),background,effects)
        return output
    }
    fun drawDocument(
        canvas: Canvas,
        document: Bitmap,
        quad: Quad,
        width: Float,
        height: Float,
        background: Bitmap?=null,
        effects: SceneEffects=SceneEffects()
    ) {
        val source=floatArrayOf(0f,0f,document.width.toFloat(),0f,document.width.toFloat(),document.height.toFloat(),0f,document.height.toFloat())
        val target=quad.points.flatMap{listOf(it.x*width,it.y*height)}.toFloatArray()
        val matrix=Matrix()
        if(!matrix.setPolyToPoly(source,0,target,0,4)) return
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if(background!=null && (effects.matchLighting || effects.matchWhiteBalance)) {
            val targetColor=meanColor(background,quad)
            val paperColor=meanColor(document)
            paint.colorFilter=ColorMatrixColorFilter(colorMatrix(paperColor,targetColor,effects))
        }
        if(effects.contactShadow) {
            val alpha=(20+effects.strength*48).roundToInt()
            paint.setShadowLayer(3f+effects.strength*8f,0f,2f+effects.strength*7f,Color.argb(alpha,0,0,0))
        }
        canvas.drawBitmap(document,matrix,paint)
    }

    private fun colorMatrix(paper:Int, background:Int, effects:SceneEffects): ColorMatrix {
        fun channel(color:Int, shift:Int)=((color shr shift) and 0xFF).coerceAtLeast(1).toFloat()
        val paperLuma=(channel(paper,16)*.2126f+channel(paper,8)*.7152f+channel(paper,0)*.0722f).coerceAtLeast(1f)
        val backgroundLuma=channel(background,16)*.2126f+channel(background,8)*.7152f+channel(background,0)*.0722f
        fun blend(from:Float,to:Float,strength:Float)=from+(to-from)*strength
        val light=if(effects.matchLighting) blend(1f,(backgroundLuma/paperLuma).coerceIn(.90f,1.10f),effects.strength) else 1f
        fun wb(shift:Int):Float {
            if(!effects.matchWhiteBalance) return 1f
            val sceneRelative=(channel(background,shift)/backgroundLuma).coerceIn(.65f,1.35f)
            val paperRelative=(channel(paper,shift)/paperLuma).coerceIn(.65f,1.35f)
            return blend(1f,(sceneRelative/paperRelative).coerceIn(.90f,1.10f),effects.strength*.45f)
        }
        return ColorMatrix(floatArrayOf(
            light*wb(16),0f,0f,0f,0f,
            0f,light*wb(8),0f,0f,0f,
            0f,0f,light*wb(0),0f,0f,
            0f,0f,0f,1f,0f
        ))
    }

    private fun meanColor(bitmap:Bitmap, quad:Quad?=null):Int {
        var r=0L; var g=0L; var b=0L; var count=0
        val steps=24
        for(y in 0 until steps) for(x in 0 until steps) {
            val nx=(x+.5f)/steps; val ny=(y+.5f)/steps
            if(quad!=null && !inside(nx,ny,quad)) continue
            val pixel=bitmap.getPixel((nx*(bitmap.width-1)).roundToInt(),(ny*(bitmap.height-1)).roundToInt())
            r+=Color.red(pixel); g+=Color.green(pixel); b+=Color.blue(pixel); count++
        }
        return Color.rgb((r/count).toInt(),(g/count).toInt(),(b/count).toInt())
    }

    private fun inside(x:Float,y:Float,quad:Quad):Boolean {
        return quad.points.indices.all { i ->
            val a=quad.points[i]; val b=quad.points[(i+1)%4]
            (b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x)>=0f
        }
    }
}
