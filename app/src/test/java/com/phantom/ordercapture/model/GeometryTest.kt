package com.phantom.ordercapture.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GeometryTest {
    @Test fun randomPlacementKeepsVeryLongSlipsWithinScene() {
        for(aspect in listOf(.06f,.2f,.5f,1f,4f)) for(scene in listOf(.5f,.75f,1.7f)) repeat(100) { seed ->
            val q=Geometry.randomPlacement(Placement(centerX=.92f,centerY=.05f),aspect,scene,Random(seed))
            assertTrue(q.valid())
            assertTrue(q.points.all{it.x in 0f..1f && it.y in 0f..1f})
        }
    }
    @Test fun cornersCannotCross() {
        val q=Quad.inset()
        assertEquals(q,q.moveCorner(0,Pt(.99f,.99f)))
    }
    @Test fun extremeGestureStaysVisible() {
        val q=Geometry.transform(Quad.inset(),30f,-30f,100f,179f,.75f)
        assertTrue(q.valid())
        assertTrue(q.points.all{it.x in 0f..1f && it.y in 0f..1f})
    }
}
