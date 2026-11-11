package com.textiletracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warping_machines")
data class WarpingMachine(@PrimaryKey val id:String, val minBeam:Int, val maxBeam:Int, val operatorNames:String="")
@Entity(tableName = "operators")
data class Operator(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val phone:String="")
@Entity(tableName = "galas")
data class Gala(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val hamaliRate:Double)
@Entity(tableName = "looms")
data class Loom(@PrimaryKey val id:String, val unit:String, val galaId:Long?=null, val activeBeamId:Long?=null, val isActive:Boolean=false)
@Entity(tableName = "beams")
data class Beam(@PrimaryKey(autoGenerate=true) val id:Long=0, val beamNumber:Int, val machineId:String, val fabricClass:String, val totalCuts:Int=9, val remainingCuts:Int=9, val pieceRate:Double=0.0, val status:String="WARPED", val createdAt:Long=System.currentTimeMillis())
@Entity(tableName = "beam_operators")
data class BeamOperator(@PrimaryKey(autoGenerate=true) val id:Long=0, val beamId:Long, val operatorName:String, val shareAmount:Double)
@Entity(tableName = "beam_movements")
data class BeamMovement(@PrimaryKey(autoGenerate=true) val id:Long=0, val beamId:Long, val destination:String, val hamalName:String, val rate:Double, val movementType:String, val createdAt:Long=System.currentTimeMillis())
@Entity(tableName = "takas")
data class Taka(@PrimaryKey(autoGenerate=true) val id:Long=0, val rollCode:String, val beamId:Long, val loomId:String, val meters:Double, val grossWeightKg:Double, val averageGramsPerMeter:Double, val createdAt:Long=System.currentTimeMillis())
