package com.phantom.ordercapture.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraScreen(onCaptured: (Uri)->Unit, onBack: ()->Unit) {
    val context=LocalContext.current; val owner=LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
    var error by remember{mutableStateOf<String?>(null)}
    var flash by remember{mutableStateOf(false)}; var grid by remember{mutableStateOf(true)}; var capturing by remember{mutableStateOf(false)}
    val preview=remember{PreviewView(context).apply{scaleType=PreviewView.ScaleType.FIT_CENTER}}
    val capture=remember{ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()}
    var camera by remember{mutableStateOf<Camera?>(null)}
    if(!granted) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("อนุญาตกล้องเพื่อถ่ายใบฝากสั่ง")
            Button(onClick={permission.launch(Manifest.permission.CAMERA)}) { Text("อนุญาตกล้อง") }
            TextButton(onClick=onBack){Text("กลับ")}
        }; return
    }
    DisposableEffect(owner) {
        val future=ProcessCameraProvider.getInstance(context); var disposed=false; var provider: ProcessCameraProvider?=null
        future.addListener({
            if(!disposed) try {
                provider=future.get()
                val useCase=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}
                camera=provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,useCase,capture)
            } catch(e:Exception) { error="เปิดกล้องไม่ได้: ${e.message}" }
        },ContextCompat.getMainExecutor(context))
        onDispose { disposed=true; provider?.unbindAll() }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=onBack,enabled=!capturing){Text("กลับ")}
            TextButton(onClick={flash=!flash; capture.flashMode=if(flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF}){Text("แฟลช ${if(flash) "เปิด" else "ปิด"}")}
            TextButton(onClick={grid=!grid}){Text("เส้นกริด")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().pointerInput(camera){detectTapGestures { offset ->
            val point=preview.meteringPointFactory.createPoint(offset.x,offset.y)
            camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
        }}) {
            AndroidView(factory={preview},modifier=Modifier.fillMaxSize())
            if(grid) Canvas(Modifier.fillMaxSize()) { for(i in 1..2) { drawLine(Color.White.copy(alpha=.4f),Offset(size.width*i/3,0f),Offset(size.width*i/3,size.height)); drawLine(Color.White.copy(alpha=.4f),Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3)) } }
        }
        error?.let{Text(it,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.error)}
        Button(onClick={
            capturing=true
            val file=File(context.cacheDir,"camera-${System.currentTimeMillis()}.jpg")
            capture.targetRotation=preview.display?.rotation?:android.view.Surface.ROTATION_0
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result:ImageCapture.OutputFileResults) { capturing=false; onCaptured(Uri.fromFile(file)) }
                override fun onError(exception:ImageCaptureException) { capturing=false; error=exception.message }
            })
        },enabled=!capturing && camera!=null,modifier=Modifier.fillMaxWidth().padding(20.dp).height(56.dp)){Text(if(capturing) "กำลังถ่าย…" else "ถ่ายใบฝากสั่ง")}
    }
}
