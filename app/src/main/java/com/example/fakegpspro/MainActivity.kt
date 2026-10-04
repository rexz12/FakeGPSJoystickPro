package com.example.fakegpspro

import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.*

data class P(val lat:Double,val lon:Double)

class MainActivity:Activity(){
    private lateinit var map:MapView
    private lateinit var status:TextView
    private lateinit var coords:TextView
    private lateinit var routeInfo:TextView
    private lateinit var locationManager:LocationManager

    private var lat=-6.200000
    private var lon=106.816666
    private var speed=20.0
    private var active=false
    private var auto=false
    private var jx=0f
    private var jy=0f
    private var bearing=0.0

    private val waypoints=mutableListOf<P>()
    private val route=mutableListOf<P>()
    private var routeIndex=0
    private val markers=mutableListOf<Marker>()
    private var line:Polyline?=null

    private val handler=Handler(Looper.getMainLooper())
    private val loop=object:Runnable{
        override fun run(){
            if(active){step(.1);sendMock();refresh();handler.postDelayed(this,100)}
        }
    }

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        Configuration.getInstance().load(this,getSharedPreferences("osm",0))
        Configuration.getInstance().userAgentValue=packageName
        setContentView(R.layout.activity_main)

        map=findViewById(R.id.map)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.0)
        map.controller.setCenter(GeoPoint(lat,lon))

        status=findViewById(R.id.status)
        coords=findViewById(R.id.coords)
        routeInfo=findViewById(R.id.routeInfo)
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val speedBar=findViewById<SeekBar>(R.id.speed)
        val speedText=findViewById<TextView>(R.id.speedText)
        speedBar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){speed=max(1,p).toDouble();speedText.text="${speed.toInt()} km/h"}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })

        findViewById<JoystickView>(R.id.joystick).onMove{x,y->
            jx=x;jy=y
            if(!auto && abs(x)+abs(y)>.05){
                bearing=(Math.toDegrees(atan2(x.toDouble(),-y.toDouble()))+360)%360
            }
        }

        findViewById<Button>(R.id.setPos).setOnClickListener{
            val a=findViewById<EditText>(R.id.lat).text.toString().toDoubleOrNull()
            val o=findViewById<EditText>(R.id.lon).text.toString().toDoubleOrNull()
            if(a!=null&&o!=null&&a in -90.0..90.0&&o in -180.0..180.0){
                lat=a;lon=o;map.controller.setCenter(GeoPoint(lat,lon));refresh()
            }
        }

        findViewById<Button>(R.id.add).setOnClickListener{
            val a=findViewById<EditText>(R.id.lat).text.toString().toDoubleOrNull()
            val o=findViewById<EditText>(R.id.lon).text.toString().toDoubleOrNull()
            if(a!=null&&o!=null)addWaypoint(P(a,o))
        }

        findViewById<Button>(R.id.buildRoute).setOnClickListener{
            if(waypoints.isEmpty()){toast("Tambah waypoint dulu");return@setOnClickListener}
            buildRoadRoute()
        }

        findViewById<Button>(R.id.start).setOnClickListener{
            if(route.isEmpty()){toast("BUILD ROUTE dulu");return@setOnClickListener}
            auto=true;active=true;routeIndex=0
            status.text="AUTO ROAD"
            handler.removeCallbacks(loop);handler.post(loop)
        }

        findViewById<Button>(R.id.manual).setOnClickListener{
            auto=false;active=true;status.text="MANUAL JOYSTICK"
            handler.removeCallbacks(loop);handler.post(loop)
        }

        findViewById<Button>(R.id.clear).setOnClickListener{
            waypoints.clear();route.clear();markers.forEach{map.overlays.remove(it)}
            markers.clear();line?.let{map.overlays.remove(it)};line=null
            map.invalidate();routeInfo.text="Route: 0"
        }

        refresh()
    }

    private fun addWaypoint(p:P){
        waypoints.add(p)
        val m=Marker(map);m.position=GeoPoint(p.lat,p.lon);m.title="Waypoint ${waypoints.size}"
        map.overlays.add(m);markers.add(m);routeInfo.text="Waypoints: ${waypoints.size}"
        map.invalidate()
    }

    private fun buildRoadRoute(){
        val all=mutableListOf<P>()
        var from=P(lat,lon)
        thread{
            try{
                for(to in waypoints){
                    val part=requestOsrm(from,to)
                    if(part.isNotEmpty()){
                        if(all.isNotEmpty())all.removeAt(all.lastIndex)
                        all.addAll(part)
                    }
                    from=to
                }
                runOnUiThread{
                    route.clear();route.addAll(all);routeIndex=0
                    drawRoute()
                    routeInfo.text="Road points: ${route.size}"
                    status.text="ROUTE READY"
                }
            }catch(e:Exception){runOnUiThread{toast("Gagal mengambil rute: ${e.message}")}}
        }
    }

    // OSRM public demo endpoint. For production use, configure your own routing server.
    private fun requestOsrm(a:P,b:P):List<P>{
        val url=URL("https://router.project-osrm.org/route/v1/driving/${a.lon},${a.lat};${b.lon},${b.lat}?overview=full&geometries=geojson")
        val c=url.openConnection() as HttpURLConnection
        c.connectTimeout=10000;c.readTimeout=15000;c.requestMethod="GET"
        if(c.responseCode!=200)throw Exception("HTTP ${c.responseCode}")
        val text=c.inputStream.bufferedReader().readText()
        val root=JSONObject(text)
        val coords=root.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        val out=mutableListOf<P>()
        for(i in 0 until coords.length()){
            val q=coords.getJSONArray(i)
            out.add(P(q.getDouble(1),q.getDouble(0)))
        }
        return out
    }

    private fun drawRoute(){
        line?.let{map.overlays.remove(it)}
        if(route.isEmpty())return
        val pl=Polyline()
        pl.setPoints(route.map{GeoPoint(it.lat,it.lon)})
        pl.width=9f
        map.overlays.add(pl);line=pl;map.invalidate()
    }

    private fun step(dt:Double){
        if(auto){
            if(routeIndex>=route.size){active=false;auto=false;status.text="RUTE SELESAI";return}
            val target=route[routeIndex]
            val north=(target.lat-lat)*111320.0
            val east=(target.lon-lon)*111320.0*cos(Math.toRadians(lat))
            val dist=hypot(north,east)
            val travel=speed*1000/3600*dt
            if(dist<=max(1.5,travel)){
                lat=target.lat;lon=target.lon;routeIndex++
            }else{
                bearing=(Math.toDegrees(atan2(east,north))+360)%360
                lat+=cos(Math.toRadians(bearing))*travel/111320.0
                lon+=sin(Math.toRadians(bearing))*travel/(111320.0*cos(Math.toRadians(lat)))
            }
        }else{
            val mag=hypot(jx.toDouble(),jy.toDouble())
            if(mag<.05)return
            val travel=speed*1000/3600*dt
            val east=jx/mag*travel
            val north=-jy/mag*travel
            lat+=north/111320.0
            lon+=east/(111320.0*cos(Math.toRadians(lat)))
        }
    }

    private fun sendMock(){
        try{
            val l=Location(LocationManager.GPS_PROVIDER).apply{
                latitude=lat;longitude=lon;accuracy=3f
                speed=(speed/3.6).toFloat();bearing=bearing.toFloat()
                time=System.currentTimeMillis()
                elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()
            }
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER,l)
        }catch(_:Exception){}
    }

    private fun refresh(){
        coords.text="Lat: %.6f   Lon: %.6f".format(lat,lon)
        if(auto)routeInfo.text="AUTO: ${routeIndex.coerceAtMost(route.size)}/${route.size}"
        map.controller.setCenter(GeoPoint(lat,lon))
        map.invalidate()
    }

    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onResume(){super.onResume();map.onResume()}
    override fun onPause(){map.onPause();super.onPause()}
}
