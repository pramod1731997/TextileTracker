package com.textiletracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.textiletracker.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val Emerald=Color(0xFF00BA88)
private val Azure=Color(0xFF007DFE)
private val Amber=Color(0xFFFF7A00)
private val Mist=Color(0xFFF3F8F7)
private val Ink=Color(0xFF16332D)

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
        val dao=TrackerDatabase.get(this).dao()
        setContent { TextileTrackerApp(dao) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextileTrackerApp(dao:TrackerDao) {
    val tabs=listOf("Overview","Beams","Looms","TAKAs","Payroll","Settings")
    var selected by remember { mutableStateOf("Overview") }
    val beams by dao.beams().collectAsStateWithLifecycle(emptyList())
    val looms by dao.looms().collectAsStateWithLifecycle(emptyList())
    val takas by dao.takas().collectAsStateWithLifecycle(emptyList())
    val galas by dao.galas().collectAsStateWithLifecycle(emptyList())
    val machines by dao.machines().collectAsStateWithLifecycle(emptyList())
    val operators by dao.operators().collectAsStateWithLifecycle(emptyList())
    val warehouse by dao.warehouseCount().collectAsStateWithLifecycle(0)
    val todayTakas by dao.todayTakaCount().collectAsStateWithLifecycle(0)
    val todayMeters by dao.todayMeters().collectAsStateWithLifecycle(0.0)
    val active by dao.activeLoomCount().collectAsStateWithLifecycle(0)
    val totalLooms by dao.loomCount().collectAsStateWithLifecycle(0)
    val scope= rememberCoroutineScope()
    var dialog by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    Scaffold(containerColor=Mist, topBar={ TopAppBar(title={
        Column { Text("Textile Tracker",fontWeight=FontWeight.Bold,color=Ink); Text("POWERLOOM • FACTORY CONTROL",style=MaterialTheme.typography.labelSmall,color=Emerald) }
    },actions={ IconButton(onClick={selected="Settings"}) { Icon(Icons.Default.Tune,"Settings",tint=Ink) } }) },
    bottomBar={ NavigationBar(containerColor=Color.White.copy(alpha=.94f),tonalElevation=8.dp) {
        val icons=listOf(Icons.Default.Dashboard,Icons.Default.Inventory2,Icons.Default.SettingsInputComponent,Icons.Default.ViewCarousel,Icons.Default.Payments,Icons.Default.Settings)
        tabs.forEachIndexed { i,t -> NavigationBarItem(selected=selected==t,onClick={selected=t},icon={Icon(icons[i],t)},label={Text(t)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Emerald,indicatorColor=Emerald.copy(alpha=.13f),selectedTextColor=Emerald)) }
    } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp)) {
            Spacer(Modifier.height(8.dp))
            when(selected) {
                "Overview" -> Dashboard(warehouse,active,totalLooms,beams,todayTakas,todayMeters,onAction={selected=it})
                "Beams" -> BeamScreen(beams,machines,operators,galas,dialog={dialog=it},create={machine,num,fabric,rate,op -> scope.launch {
                    val id=dao.addBeam(Beam(beamNumber=num,machineId=machine,fabricClass=fabric,pieceRate=rate))
                    if(op.isNotBlank()) dao.addBeamOperator(BeamOperator(beamId=id,operatorName=op,shareAmount=rate))
                    message="Beam B-$num created"
                }})
                "Looms" -> LoomScreen(looms,beams,galas,dao,scope)
                "TAKAs" -> TakaScreen(takas,beams,looms,dao,scope)
                "Payroll" -> PayrollScreen(beams,dao)
                else -> SettingsScreen(machines,operators,galas,looms,dao,scope)
            }
        }
    }
    if(dialog=="beam") BeamDialog(machines,operators,onDismiss={dialog=""},onSave={m,n,f,r,o -> dialog=""; scope.launch {
        val id=dao.addBeam(Beam(beamNumber=n,machineId=m,fabricClass=f,pieceRate=r))
        if(o.isNotBlank()) dao.addBeamOperator(BeamOperator(beamId=id,operatorName=o,shareAmount=r))
    }})
    message?.let { AlertDialog(onDismissRequest={message=null},confirmButton={TextButton(onClick={message=null}){Text("OK")}},text={Text(it)}) }
}

@Composable fun Dashboard(warehouse:Int,active:Int,total:Int,beams:List<Beam>,takas:Int,meters:Double,onAction:(String)->Unit) {
    Text("Factory overview",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink)
    Text("Your production at a glance",color=Color.Gray)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        MetricCard("Warehouse",warehouse.toString(),"Beams",Emerald,Modifier.weight(1f))
        MetricCard("Loom activity",if(total==0)"0%" else "${active*100/total}%","$active / $total running",Azure,Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        MetricCard("Today's TAKAs",takas.toString(),"Rolls reaped",Amber,Modifier.weight(1f))
        MetricCard("Meters woven",String.format(Locale.US,"%.1f",meters),"Today",Emerald,Modifier.weight(1f))
    }
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment=Alignment.CenterVertically) { Text("Production floor",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Ink); Spacer(Modifier.weight(1f)); Text("LIVE",color=Emerald,style=MaterialTheme.typography.labelMedium) }
    Spacer(Modifier.height(8.dp))
    val low=beams.filter{it.status=="MOUNTED" && it.remainingCuts<2}
    if(low.isNotEmpty()) Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF0E4)),shape=RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Text("Warp alerts",fontWeight=FontWeight.Bold,color=Amber); low.forEach { Text("Beam B-${it.beamNumber}: ${it.remainingCuts} cuts remaining") } } }
    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Quick actions",fontWeight=FontWeight.Bold,color=Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ActionChip("New beam",Icons.Default.Add,Emerald){onAction("Beams")}
                ActionChip("Record TAKA",Icons.Default.Addchart,Azure){onAction("TAKAs")}
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ActionChip("Looms",Icons.Default.SettingsInputComponent,Amber){onAction("Looms")}
                ActionChip("Payroll",Icons.Default.Payments,Emerald){onAction("Payroll")}
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Recent beams",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Ink)
    beams.take(4).forEach { BeamRow(it) }
}
@Composable fun MetricCard(title:String,value:String,subtitle:String,color:Color,modifier:Modifier=Modifier) {
    Card(modifier,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(14.dp)) { Box(Modifier.size(30.dp).background(color.copy(alpha=.12f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Circle,"",tint=color,modifier=Modifier.size(12.dp))}; Spacer(Modifier.height(10.dp)); Text(value,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Ink); Text(title,fontWeight=FontWeight.SemiBold,color=Ink); Text(subtitle,style=MaterialTheme.typography.labelSmall,color=Color.Gray) }
    }
}
@Composable fun ActionChip(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color,onClick:()->Unit) {
    FilledTonalButton(onClick=onClick,colors=ButtonDefaults.filledTonalButtonColors(containerColor=color.copy(alpha=.12f),contentColor=color),shape=RoundedCornerShape(14.dp)) { Icon(icon,null); Spacer(Modifier.width(5.dp)); Text(label) }
}
@Composable fun SectionTitle(title:String,subtitle:String) { Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink); Text(subtitle,color=Color.Gray); Spacer(Modifier.height(12.dp)) }
@Composable fun BeamRow(b:Beam) {
    Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) { Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Beam B-${b.beamNumber}",fontWeight=FontWeight.Bold,color=Ink); Text("${b.fabricClass} • ${b.machineId}",style=MaterialTheme.typography.bodySmall,color=Color.Gray) }; Text("${b.remainingCuts}/${b.totalCuts} cuts",color=Emerald,fontWeight=FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Text(b.status,style=MaterialTheme.typography.labelSmall,color=Azure) } }
}
@Composable fun BeamScreen(beams:List<Beam>,machines:List<WarpingMachine>,operators:List<Operator>,galas:List<Gala>,dialog:()->Unit,create:(String,Int,String,Double,String)->Unit) {
    SectionTitle("Beam register","Warping, operator share and beam status")
    Button(onClick=dialog,colors=ButtonDefaults.buttonColors(containerColor=Emerald),modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Add,null); Text("Create beam") }
    LazyColumn { items(beams){BeamRow(it)} }
}
@Composable fun BeamDialog(machines:List<WarpingMachine>,operators:List<Operator>,onDismiss:()->Unit,onSave:(String,Int,String,Double,String)->Unit) {
    var machine by remember { mutableStateOf(machines.firstOrNull()?.id ?: "M1") }; var number by remember { mutableStateOf("") }; var fabric by remember { mutableStateOf("Cotton") }; var rate by remember { mutableStateOf("300") }; var op by remember { mutableStateOf(operators.firstOrNull()?.name ?: "") }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Create warp beam")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(machine,{machine=it},label={Text("Machine ID")})
        OutlinedTextField(number,{number=it},label={Text("Beam number")})
        OutlinedTextField(fabric,{fabric=it},label={Text("Fabric class")})
        OutlinedTextField(rate,{rate=it},label={Text("Piece rate (₹)")})
        OutlinedTextField(op,{op=it},label={Text("Operator name(s), comma separated")})
    }},confirmButton={TextButton(onClick={number.toIntOrNull()?.let{onSave(machine,it,fabric,rate.toDoubleOrNull()?:0.0,op)}}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable fun LoomScreen(looms:List<Loom>,beams:List<Beam>,galas:List<Gala>,dao:TrackerDao,scope:kotlinx.coroutines.CoroutineScope) {
    SectionTitle("Loom floor","Mount beams and monitor active cuts")
    LazyColumn { items(looms){loom ->
        val beam=beams.firstOrNull{it.id==loom.activeBeamId}
        Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp)) { Text(loom.id,fontWeight=FontWeight.Bold,color=Ink); Text(loom.unit,color=Color.Gray); Text(if(beam==null)"Available" else "Beam B-${beam.beamNumber} • ${beam.remainingCuts} cuts",color=if(beam==null)Emerald else Azure)
                if(!loom.isActive && beams.any{it.status=="WAREHOUSE"}) Button(onClick={scope.launch { val b=beams.first{it.status=="WAREHOUSE"}; dao.mountBeam(loom.id,b.id,galas.firstOrNull()?.id); dao.addMovement(BeamMovement(beamId=b.id,destination=galas.firstOrNull()?.name?:"Loom floor",hamalName="Not assigned",rate=galas.firstOrNull()?.hamaliRate?:0.0,movementType="MOUNT")) }}) { Text("Mount next warehouse beam") }
                if(loom.isActive) TextButton(onClick={scope.launch { dao.freeLoom(loom.id) }} ) { Text("Free loom") }
            }
        }
    } }
}
@Composable fun TakaScreen(takas:List<Taka>,beams:List<Beam>,looms:List<Loom>,dao:TrackerDao,scope:kotlinx.coroutines.CoroutineScope) {
    var beamId by remember { mutableStateOf("") }; var loomId by remember { mutableStateOf("") }; var meters by remember { mutableStateOf("200") }; var weight by remember { mutableStateOf("1.2") }
    SectionTitle("TAKA register","Record completed fabric cuts")
    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("New roll",fontWeight=FontWeight.Bold)
        OutlinedTextField(beamId,{beamId=it},label={Text("Beam ID (database ID)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(loomId,{loomId=it},label={Text("Loom ID")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(meters,{meters=it},label={Text("Meters woven")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(weight,{weight=it},label={Text("Gross weight (kg)")},modifier=Modifier.fillMaxWidth())
        val m=meters.toDoubleOrNull()?:0.0; val w=weight.toDoubleOrNull()?:0.0
        Text("Average: ${if(m>0) String.format(Locale.US,"%.2f",w*1000/m) else "0.00"} g/m",color=Emerald,fontWeight=FontWeight.Bold)
        Button(onClick={val id=beamId.toLongOrNull(); val mm=m; val ww=w; if(id!=null && mm>0 && ww>0 && loomId.isNotBlank()) scope.launch {
            val b=dao.beam(id); val l=dao.loom(loomId)
            if(b!=null && l?.activeBeamId==id) { dao.addTaka(Taka(rollCode="ROLL-${System.currentTimeMillis()}",beamId=id,loomId=loomId,meters=mm,grossWeightKg=ww,averageGramsPerMeter=ww*1000/mm)); dao.consumeCut(id); if(b.remainingCuts<=1) dao.freeLoom(loomId) }
        }},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Emerald)) { Text("Save TAKA & consume cut") }
    } }
    Spacer(Modifier.height(12.dp)); Text("Recent rolls",fontWeight=FontWeight.Bold,color=Ink)
    LazyColumn { items(takas){t -> Card(Modifier.fillMaxWidth().padding(vertical=3.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(t.rollCode); Text("${t.meters} m • ${String.format(Locale.US,"%.1f",t.averageGramsPerMeter)} g/m",color=Emerald)}}} }
}
@Composable fun PayrollScreen(beams:List<Beam>,dao:TrackerDao) {
    SectionTitle("Payroll ledger","Fortnightly earnings • 1st–15th / 16th–month end")
    val month=SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date())
    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp)) {
        Text(month,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Ink)
        Text("Warping operator earnings",fontWeight=FontWeight.SemiBold)
        val total=beams.sumOf{it.pieceRate}
        Text("Beam piece-rate pool: ₹${"%.2f".format(total)}",color=Emerald)
        Divider(Modifier.padding(vertical=10.dp))
        Text("Phase 1  •  1st–15th"); Text("Phase 2  •  16th–month end")
        Text("Shared beam allocations are stored per operator in the beam ledger.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
        Divider(Modifier.padding(vertical=10.dp))
        Text("Hamal ledger",fontWeight=FontWeight.SemiBold)
        Text("Movement entries retain destination Gala and applied rate. Assign actual Hamal names during movement to calculate individual payout.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
    } }
    beams.forEach{BeamRow(it)}
}
@Composable fun SettingsScreen(machines:List<WarpingMachine>,operators:List<Operator>,galas:List<Gala>,looms:List<Loom>,dao:TrackerDao,scope:kotlinx.coroutines.CoroutineScope) {
    var machine by remember{mutableStateOf("M1")}; var min by remember{mutableStateOf("1000")}; var max by remember{mutableStateOf("1999")}
    var op by remember{mutableStateOf("")}; var gala by remember{mutableStateOf("")}; var rate by remember{mutableStateOf("50")}
    var unit by remember{mutableStateOf("Unit 1")}; var count by remember{mutableStateOf("90")}
    SectionTitle("Factory setup","Configure machines, people, galas and loom capacity")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { SetupCard("Warping machine") {
            OutlinedTextField(machine,{machine=it},label={Text("Machine ID")},modifier=Modifier.fillMaxWidth())
            Row { OutlinedTextField(min,{min=it},label={Text("Beam from")},modifier=Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(max,{max=it},label={Text("Beam to")},modifier=Modifier.weight(1f)) }
            Button(onClick={scope.launch{dao.addMachine(WarpingMachine(machine,min.toIntOrNull()?:1,max.toIntOrNull()?:9999))}},modifier=Modifier.fillMaxWidth()){Text("Save machine")}
            machines.forEach{Text("${it.id} • ${it.minBeam}–${it.maxBeam}",color=Emerald)}
        } }
        item { SetupCard("Operator directory") {
            OutlinedTextField(op,{op=it},label={Text("Operator name")},modifier=Modifier.fillMaxWidth())
            Button(onClick={if(op.isNotBlank())scope.launch{dao.addOperator(Operator(name=op));op=""}},modifier=Modifier.fillMaxWidth()){Text("Add operator")}
            operators.forEach{Text(it.name)}
        } }
        item { SetupCard("Gala & Hamali rates") {
            OutlinedTextField(gala,{gala=it},label={Text("Gala / destination")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(rate,{rate=it},label={Text("₹ per beam")},modifier=Modifier.fillMaxWidth())
            Button(onClick={if(gala.isNotBlank())scope.launch{dao.addGala(Gala(name=gala,hamaliRate=rate.toDoubleOrNull()?:0.0));gala=""}},modifier=Modifier.fillMaxWidth()){Text("Save Gala rate")}
            galas.forEach{Text("${it.name} • ₹${it.hamaliRate}/beam",color=Emerald)}
        } }
        item { SetupCard("Bulk loom generator") {
            OutlinedTextField(unit,{unit=it},label={Text("Unit name")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(count,{count=it},label={Text("Number of looms")},modifier=Modifier.fillMaxWidth())
            Button(onClick={scope.launch{(1..(count.toIntOrNull()?:0).coerceIn(1,500)).forEach{n->dao.addLoom(Loom(id="${unit.replace(" ","")}-${n.toString().padStart(2,'0')}",unit=unit))}}},modifier=Modifier.fillMaxWidth()){Text("Generate looms")}
            Text("${looms.size} looms configured",color=Emerald)
        } }
        item { SetupCard("Backup & cloud") {
            Text("Room database is stored locally on this device. JSON export/import and optional Supabase sync are planned for the next implementation stage.",color=Color.Gray)
        } }
    }
}
@Composable fun SetupCard(title:String,content:@Composable ColumnScope.()->Unit) { Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content); Text(title,Modifier.padding(start=14.dp,bottom=10.dp),fontWeight=FontWeight.Bold,color=Ink) } }
