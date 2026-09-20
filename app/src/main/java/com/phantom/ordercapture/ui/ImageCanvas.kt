package com.phantom.ordercapture.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.phantom.ordercapture.model.*
import com.phantom.ordercapture.renderer.SceneRenderer
import kotlin.math.hypot

@Composable
fun ImageCanvas(background: Bitmap, document: Bitmap?=null, quad: Quad?=null, handles:Boolean=false, candidates:List<Quad> = emptyList(), effects:SceneEffects=SceneEffects(), onQuad: (Quad)->Unit={}, modifier:Modifier=Modifier) {
    val current by rememberUpdatedState(quad); val onChange by rememberUpdatedState(onQuad)
    var dimensions by remember{mutableStateOf(IntSize.Zero)}
    val aspect=background.width.toFloat()/background.height
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val width=if(maxWidth/maxHeight>aspect) maxHeight*aspect else maxWidth
        val height=width/aspect
        var touch=Modifier.onSizeChanged{dimensions=it}
        if(quad!=null && handles) touch=touch.pointerInput(background,handles) {
            var selected=-1
            detectDragGestures(onDragStart={p->
                selected=current!!.points.indices.minBy{hypot(p.x-current!!.points[it].x*dimensions.width,p.y-current!!.points[it].y*dimensions.height)}
                val point=current!!.points[selected]
                if(hypot(p.x-point.x*dimensions.width,p.y-point.y*dimensions.height)>48* density) selected=-1
            },onDragEnd={selected=-1},onDragCancel={selected=-1}) { change, amount ->
                if(selected>=0) {
                    change.consume(); val q=current!!; val p=q.points[selected]
                    onChange(q.moveCorner(selected,Pt(p.x+amount.x/dimensions.width,p.y+amount.y/dimensions.height)))
                }
            }
        }.pointerInput(candidates) {
            detectTapGestures { p ->
                val x=p.x/dimensions.width; val y=p.y/dimensions.height
                candidates.lastOrNull { q -> q.points.indices.all { i -> val a=q.points[i]; val b=q.points[(i+1)%4]; (b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x)>=0 } }?.let(onChange)
            }
        }
        else if(quad!=null) touch=touch.pointerInput(background,handles) {
            detectTransformGestures { _,pan,zoom,rotation ->
                onChange(Geometry.transform(current!!,pan.x/dimensions.width,pan.y/dimensions.height,zoom,rotation,aspect))
            }
        }
        Canvas(Modifier.width(width).height(height).then(touch)) {
            drawImage(background.asImageBitmap(),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
            if(document!=null && quad!=null) drawContext.canvas.nativeCanvas.let { SceneRenderer.drawDocument(it,document,quad,size.width,size.height,background,effects) }
            fun outline(q:Quad,color:Color,stroke:Float) {
                val path=Path().apply { q.points.forEachIndexed { i,p -> if(i==0) moveTo(p.x*size.width,p.y*size.height) else lineTo(p.x*size.width,p.y*size.height) }; close() }
                drawPath(path,color,style=Stroke(stroke))
            }
            candidates.forEach{outline(it,Color(0xFFFFC857),2f)}
            if(quad!=null && (handles || document==null)) {
                outline(quad,Color(0xFF5BE4CB),3f)
                quad.points.forEachIndexed { _,p ->
                    drawCircle(Color(0xFF093C3A),12*density,Offset(p.x*size.width,p.y*size.height))
                    drawCircle(Color(0xFF5BE4CB),8*density,Offset(p.x*size.width,p.y*size.height))
                }
            }
        }
    }
}
