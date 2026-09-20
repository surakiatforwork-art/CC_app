package com.phantom.ordercapture.model

import kotlin.math.*
import kotlin.random.Random

data class Pt(val x: Float, val y: Float)
data class Quad(val points: List<Pt>) {
    init { require(points.size == 4) }
    fun valid(): Boolean {
        val cross = points.indices.map { i ->
            val a = points[i]; val b = points[(i + 1) % 4]; val c = points[(i + 2) % 4]
            (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        }
        return points.all { it.x.isFinite() && it.y.isFinite() } && cross.all { it > 0.00005f }
    }
    fun moveCorner(i: Int, p: Pt): Quad {
        val next = Quad(points.toMutableList().also { it[i] = Pt(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f)) })
        return if (next.valid()) next else this
    }
    companion object {
        fun inset() = Quad(listOf(Pt(.15f,.08f),Pt(.85f,.08f),Pt(.85f,.92f),Pt(.15f,.92f)))
    }
}
data class Placement(val centerX: Float=.5f, val centerY: Float=.5f, val widthMin: Float=.28f, val widthMax: Float=.36f, val rotationMin: Float=-3f, val rotationMax: Float=3f, val offsetX: Float=.03f, val offsetY: Float=.03f)
data class SceneEffects(
    val matchLighting: Boolean = true,
    val matchWhiteBalance: Boolean = true,
    val contactShadow: Boolean = true,
    val strength: Float = .55f
)

data class Scene(
    val id: String,
    val name: String,
    val category: String,
    val image: String,
    val placement: Placement=Placement(),
    val effects: SceneEffects=SceneEffects(),
    val local: Boolean=false
)
object Geometry {
    fun randomPlacement(p: Placement, documentAspect: Float, sceneAspect: Float, random: Random = Random.Default): Quad {
        fun between(a: Float,b: Float) = a + random.nextFloat() * (b-a)
        val width = between(p.widthMin,p.widthMax).coerceIn(.05f,.85f)
        val height = width * sceneAspect / documentAspect
        val fit = min(1f,.85f / max(width,height))
        val angle = Math.toRadians(between(p.rotationMin,p.rotationMax).toDouble())
        val cx = p.centerX + between(-p.offsetX,p.offsetX)
        val cy = p.centerY + between(-p.offsetY,p.offsetY)
        val pts = listOf(Pt(-1f,-1f),Pt(1f,-1f),Pt(1f,1f),Pt(-1f,1f)).map {
            val x=it.x*width*fit/2; val y=it.y*height*fit/2
            Pt(cx + (x*cos(angle)-y/sceneAspect*sin(angle)).toFloat(), cy+(x*sceneAspect*sin(angle)+y*cos(angle)).toFloat())
        }
        return contain(Quad(pts))
    }
    fun contain(q: Quad): Quad {
        val minX=q.points.minOf{it.x}; val maxX=q.points.maxOf{it.x}
        val minY=q.points.minOf{it.y}; val maxY=q.points.maxOf{it.y}
        val factor=min(1f, .96f/max(maxX-minX,maxY-minY))
        val cx=(minX+maxX)/2; val cy=(minY+maxY)/2
        val pts=q.points.map{Pt((it.x-cx)*factor+cx,(it.y-cy)*factor+cy)}
        val dx=max(0f,.02f-pts.minOf{it.x}) + min(0f,.98f-pts.maxOf{it.x})
        val dy=max(0f,.02f-pts.minOf{it.y}) + min(0f,.98f-pts.maxOf{it.y})
        return Quad(pts.map{Pt(it.x+dx,it.y+dy)})
    }
    fun transform(q: Quad, dx: Float, dy: Float, scale: Float, degrees: Float, aspect: Float): Quad {
        val cx=q.points.map{it.x}.average().toFloat(); val cy=q.points.map{it.y}.average().toFloat()
        val a=Math.toRadians(degrees.toDouble()); val s=scale.coerceIn(.8f,1.2f)
        val result=Quad(q.points.map {
            val x=(it.x-cx)*s; val y=(it.y-cy)*s
            Pt(cx+dx+(x*cos(a)-y/aspect*sin(a)).toFloat(),cy+dy+(x*aspect*sin(a)+y*cos(a)).toFloat())
        })
        return if(result.valid()) contain(result) else q
    }
}
