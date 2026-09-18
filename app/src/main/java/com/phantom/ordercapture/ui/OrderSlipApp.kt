package com.phantom.ordercapture.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.ordercapture.editor.*

private val categories=listOf("all" to "ทั้งหมด","freezer" to "ตู้แช่","plain" to "พื้นเรียบ","plastic" to "พลาสติก","promotion" to "โปรโมชั่น/ป้าย")

@Composable
fun OrderSlipApp(vm: EditorViewModel) {
    val colors=darkColorScheme(primary=Color(0xFF68DEC4),onPrimary=Color(0xFF00382D),background=Color(0xFF10191D),surface=Color(0xFF19262C),surfaceVariant=Color(0xFF25363C),secondary=Color(0xFFF2C879))
    MaterialTheme(colorScheme=colors) {
        val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){it?.let(vm::open)}
        val scenePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){it?.let(vm::importScene)}
        val snackbar=remember{SnackbarHostState()}
        val context=LocalContext.current
        LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message=null } }
        val goBack:()->Unit={ vm.page=when(vm.page) { Page.EXPORT->Page.EDITOR; Page.EDITOR->Page.CROP; Page.TEMPLATES->if(vm.document!=null) Page.EDITOR else Page.HOME; else->Page.HOME } }
        BackHandler(enabled=vm.page!=Page.HOME) { if(!vm.busy) goBack() }
        Scaffold(snackbarHost={SnackbarHost(snackbar)}) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when(vm.page) {
                    Page.HOME -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        Spacer(Modifier.height(28.dp))
                        Text("ORDER SLIP / SCENE",color=colors.primary,fontSize=12.sp,letterSpacing=2.sp)
                        Text("ใบฝากสั่ง\nในฉากของคุณ",fontSize=38.sp,lineHeight=48.sp,fontWeight=FontWeight.Bold)
                        Text("ถ่าย • ตัดขอบ • จัดวาง • บันทึก",color=colors.onSurfaceVariant)
                        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                    Box(Modifier.width(58.dp).height(136.dp).background(Color(0xFFEDE9DF),RoundedCornerShape(4.dp)).padding(8.dp)) {
                                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){repeat(14){HorizontalDivider(color=Color(0xFF9EABA7))}}
                                    }
                                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        Text("เริ่มจากใบฝากสั่ง",fontWeight=FontWeight.SemiBold,fontSize=20.sp)
                                        Text("รองรับใบเปล่าและใบที่เขียนแล้ว\nปรับขอบกระดาษได้ทั้ง 4 มุม",fontSize=14.sp)
                                        Text("ประมวลผลบนเครื่อง",color=colors.primary,fontSize=12.sp)
                                    }
                                }
                                Button(onClick={vm.page=Page.CAMERA},modifier=Modifier.fillMaxWidth().height(54.dp)){Text("ถ่ายใบฝากสั่ง")}
                                OutlinedButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},modifier=Modifier.fillMaxWidth().height(54.dp)){Text("เลือกภาพจากแกลเลอรี")}
                            }
                        }
                        OutlinedButton(onClick={vm.page=Page.TEMPLATES},modifier=Modifier.fillMaxWidth()){Text("ฉากหลังของฉัน  ·  ${vm.scenes.size} ฉาก")}
                        Text("1  เลือกภาพและตรวจขอบ\n2  จัดฉากและปรับมุม\n3  บันทึก JPG หรือ PNG",lineHeight=30.sp,color=colors.onSurfaceVariant)
                    }
                    Page.CAMERA -> CameraScreen(vm::open,goBack)
                    Page.CROP -> Column(Modifier.fillMaxSize()) {
                        Header("ปรับขอบกระดาษ","ขั้นตอน 1 / 3",goBack)
                        Text("ลากจุดทั้ง 4 ให้ตรงขอบใบฝากสั่ง",Modifier.padding(horizontal=20.dp),color=colors.onSurfaceVariant)
                        vm.source?.let { ImageCanvas(it,quad=vm.crop,handles=true,candidates=vm.candidates,onQuad={q->vm.crop=q},modifier=Modifier.weight(1f).fillMaxWidth().padding(24.dp)) }
                        if(vm.candidates.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            vm.candidates.forEachIndexed { i,q -> FilterChip(selected=vm.crop==q,onClick={vm.crop=q},label={Text("ใบ ${i+1}")}) }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                            TextButton(onClick=vm::autoDetect){Text("อัตโนมัติ")}
                            TextButton(onClick={vm.crop=com.phantom.ordercapture.model.Quad.inset()}){Text("เริ่มใหม่")}
                            TextButton(onClick=vm::rotate){Text("หมุน 90°")}
                        }
                        Button(onClick=vm::confirmCrop,enabled=vm.crop.valid(),modifier=Modifier.fillMaxWidth().padding(16.dp).height(52.dp)){Text("ยืนยันขอบ · ไปจัดฉาก")}
                    }
                    Page.EDITOR -> Column(Modifier.fillMaxSize()) {
                        Header("จัดฉากใบฝากสั่ง","ขั้นตอน 2 / 3",goBack)
                        vm.background?.let { bg -> ImageCanvas(bg,vm.documentPreview,vm.placement,vm.advanced,onQuad={vm.placement=it},modifier=Modifier.weight(1f).fillMaxWidth().padding(16.dp)) }
                            ?: Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("เพิ่มฉากหลังเพื่อเริ่มจัดวาง")}
                        Text(if(vm.advanced) "ลากมุมเพื่อปรับ perspective" else "ลากเพื่อเลื่อน • ใช้สองนิ้วย่อ ขยาย และหมุน",Modifier.align(Alignment.CenterHorizontally),fontSize=12.sp,color=colors.onSurfaceVariant)
                        Column(Modifier.heightIn(max=265.dp).verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Categories(vm)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Button(onClick=vm::randomScene,modifier=Modifier.weight(1f)){Text("สุ่มฉากใหม่")}
                                OutlinedButton(onClick=vm::randomPosition,modifier=Modifier.weight(1f)){Text("สุ่มตำแหน่ง")}
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                TextButton(onClick={vm.page=Page.TEMPLATES}){Text("เลือก / เพิ่มฉาก")}
                                TextButton(onClick=vm::center){Text("จัดกึ่งกลาง")}
                                TextButton(onClick=vm::fit){Text("พอดีฉาก")}
                            }
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Switch(checked=vm.advanced,onCheckedChange={vm.advanced=it})
                                Text("  ปรับ perspective 4 มุม")
                            }
                        }
                        Button(onClick={vm.page=Page.EXPORT},enabled=vm.background!=null,modifier=Modifier.fillMaxWidth().padding(16.dp).height(52.dp)){Text("ดูตัวอย่างและบันทึก")}
                    }
                    Page.EXPORT -> Column(Modifier.fillMaxSize()) {
                        Header("บันทึกภาพ","ขั้นตอน 3 / 3",goBack)
                        vm.background?.let{ImageCanvas(it,vm.documentPreview,vm.placement,modifier=Modifier.weight(1f).fillMaxWidth().padding(16.dp))}
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("ขนาดภาพตามฉากต้นฉบับ • ไม่มี timestamp",fontSize=13.sp,color=colors.onSurfaceVariant)
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                FilterChip(!vm.png,{vm.png=false},label={Text("JPEG")})
                                FilterChip(vm.png,{vm.png=true},label={Text("PNG")})
                            }
                            if(!vm.png) { Text("คุณภาพ JPEG ${vm.quality.toInt()}%"); Slider(vm.quality,{vm.quality=it},valueRange=80f..100f,steps=19) }
                            Button(onClick=vm::export,modifier=Modifier.fillMaxWidth().height(52.dp)){Text("บันทึกลงแกลเลอรี")}
                            vm.saved?.let { uri -> TextButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure{vm.message="ไม่มีแอปเปิดรูปภาพ"} },modifier=Modifier.fillMaxWidth()){Text("เปิดภาพที่บันทึกแล้ว")} }
                        }
                    }
                    Page.TEMPLATES -> Column(Modifier.fillMaxSize()) {
                        Header("ฉากหลังของฉัน","เพิ่มฉากจริงได้จากแกลเลอรี",goBack)
                        Column(Modifier.padding(16.dp)) {
                            Categories(vm)
                            Button(onClick={scenePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},modifier=Modifier.fillMaxWidth()){Text("+ เพิ่มภาพฉากหลังในหมวดนี้")}
                            Text("เลือกภาพฉากที่ไม่มีใบฝากสั่งเดิมหรือ timestamp",fontSize=12.sp,color=colors.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            vm.scenes.filter{vm.category=="all" || it.category==vm.category}.forEach { scene ->
                                Card(onClick={vm.selectScene(scene); if(vm.document!=null) vm.page=Page.EDITOR},modifier=Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(20.dp)) { Text(scene.name,fontWeight=FontWeight.Bold); Text(categories.firstOrNull{it.first==scene.category}?.second?:scene.category,color=colors.primary,fontSize=12.sp) }
                                }
                            }
                        }
                    }
                }
                if(vm.busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.75f)).clickable(enabled=true,onClick={}),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(); Text("กำลังประมวลผล…") }
                }
            }
        }
    }
}

@Composable private fun Header(title:String,subtitle:String,onBack:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
        TextButton(onClick=onBack){Text("‹ กลับ")}
        Column { Text(title,fontSize=22.sp,fontWeight=FontWeight.Bold); Text(subtitle,fontSize=11.sp,color=MaterialTheme.colorScheme.primary) }
    }
}
@Composable private fun Categories(vm:EditorViewModel) {
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        categories.forEach{(id,label)->FilterChip(vm.category==id,{vm.category=id},label={Text(label)})}
    }
}
