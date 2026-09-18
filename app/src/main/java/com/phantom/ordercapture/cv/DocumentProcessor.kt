package com.phantom.ordercapture.cv

import android.graphics.Bitmap
import com.phantom.ordercapture.model.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

class DocumentProcessor {
    private fun initialize() { check(OpenCVLoader.initLocal()) { "โหลด OpenCV ไม่สำเร็จ" } }
    fun detect(bitmap: Bitmap): List<Quad> {
        initialize()
        val scale=min(1.0,1400.0/max(bitmap.width,bitmap.height))
        val small=Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt(),(bitmap.height*scale).toInt(),true)
        val src=Mat(); val gray=Mat(); val edge=Mat(); val hierarchy=Mat()
        val kernel=Imgproc.getStructuringElement(Imgproc.MORPH_RECT,Size(5.0,5.0))
        val contours=mutableListOf<MatOfPoint>()
        val result=mutableListOf<Pair<Double,Quad>>()
        try {
            Utils.bitmapToMat(small,src)
            Imgproc.cvtColor(src,gray,Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray,gray,Size(5.0,5.0),0.0)
            for (pass in 0..1) {
                if(pass==0) Imgproc.Canny(gray,edge,40.0,120.0)
                else Imgproc.adaptiveThreshold(gray,edge,255.0,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,31,7.0)
                Imgproc.morphologyEx(edge,edge,Imgproc.MORPH_CLOSE,kernel)
                Imgproc.findContours(edge,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE)
                for(contour in contours) {
                    val area=abs(Imgproc.contourArea(contour)); val total=small.width.toDouble()*small.height
                    if(area < total*.025 || area > total*.97) continue
                    val curve=MatOfPoint2f(*contour.toArray()); val poly=MatOfPoint2f()
                    try {
                        Imgproc.approxPolyDP(curve,poly,Imgproc.arcLength(curve,true)*.02,true)
                        val arr=poly.toArray()
                        if(arr.size!=4) continue
                        val convex=MatOfPoint(*arr)
                        val isConvex=Imgproc.isContourConvex(convex); convex.release()
                        if(!isConvex) continue
                        val cx=arr.map{it.x}.average(); val cy=arr.map{it.y}.average()
                        val sorted=arr.sortedBy{atan2(it.y-cy,it.x-cx)}
                        val first=sorted.indices.minBy{sorted[it].x+sorted[it].y}
                        val q=Quad((0..3).map { val p=sorted[(it+first)%4]; Pt((p.x/small.width).toFloat(),(p.y/small.height).toFloat()) })
                        if(!q.valid()) continue
                        if(result.any { (_,other)->q.points.zip(other.points).sumOf{(a,b)->hypot((a.x-b.x).toDouble(),(a.y-b.y).toDouble())}<.08 }) continue
                        val w=hypot(arr[0].x-arr[1].x,arr[0].y-arr[1].y); val h=hypot(arr[1].x-arr[2].x,arr[1].y-arr[2].y)
                        val ratio=max(w,h)/max(1.0,min(w,h))
                        result.add((area/total + if(ratio in 2.0..12.0) .3 else 0.0) to q)
                    } finally { curve.release(); poly.release() }
                }
                contours.forEach { it.release() }; contours.clear()
            }
            return result.sortedByDescending{it.first}.take(8).map{it.second}
        } finally {
            contours.forEach{it.release()}; src.release(); gray.release(); edge.release(); hierarchy.release(); kernel.release()
            if(small!==bitmap) small.recycle()
        }
    }
    fun correct(bitmap: Bitmap, quad: Quad): Bitmap {
        initialize(); require(quad.valid()) { "กรุณาปรับมุมไม่ให้ไขว้กัน" }
        val p=quad.points.map{Point(it.x*(bitmap.width-1).toDouble(),it.y*(bitmap.height-1).toDouble())}
        fun distance(a: Int,b: Int)=hypot(p[a].x-p[b].x,p[a].y-p[b].y)
        val w=max(distance(0,1),distance(3,2)).roundToInt().coerceAtLeast(2)
        val h=max(distance(0,3),distance(1,2)).roundToInt().coerceAtLeast(2)
        val src=Mat(); val out=Mat(); val from=MatOfPoint2f(*p.toTypedArray())
        val to=MatOfPoint2f(Point(0.0,0.0),Point(w-1.0,0.0),Point(w-1.0,h-1.0),Point(0.0,h-1.0))
        val transform=Imgproc.getPerspectiveTransform(from,to)
        try {
            Utils.bitmapToMat(bitmap,src)
            Imgproc.warpPerspective(src,out,transform,Size(w.toDouble(),h.toDouble()),Imgproc.INTER_CUBIC,Core.BORDER_REPLICATE)
            return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also{Utils.matToBitmap(out,it)}
        } finally { src.release(); out.release(); from.release(); to.release(); transform.release() }
    }
}
