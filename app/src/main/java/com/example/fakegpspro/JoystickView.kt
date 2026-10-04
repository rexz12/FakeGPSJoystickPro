package com.example.fakegpspro
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

class JoystickView @JvmOverloads constructor(c:Context,a:AttributeSet?=null):View(c,a){
    private val p=Paint(1); private val k=PointF()
    private var cx=0f; private var cy=0f; private var r=1f
    private var cb:((Float,Float)->Unit)?=null
    fun onMove(f:(Float,Float)->Unit){cb=f}
    override fun onDraw(c:Canvas){
        cx=width/2f; cy=height/2f; r=min(width,height)*.39f
        if(k.x==0f&&k.y==0f)k.set(cx,cy)
        p.color=0x66000000;c.drawCircle(cx,cy,r,p)
        p.color=0xDDFFFFFF.toInt();c.drawCircle(k.x,k.y,r*.30f,p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        var dx=e.x-cx;var dy=e.y-cy;val d=hypot(dx.toDouble(),dy.toDouble()).toFloat()
        if(d>r){dx=dx/d*r;dy=dy/d*r}
        k.set(cx+dx,cy+dy);cb?.invoke(dx/r,dy/r)
        if(e.action==MotionEvent.ACTION_UP||e.action==MotionEvent.ACTION_CANCEL){
            k.set(cx,cy);cb?.invoke(0f,0f)
        };invalidate();return true
    }
}
