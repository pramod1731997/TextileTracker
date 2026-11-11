package com.textiletracker.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM warping_machines ORDER BY id") fun machines(): Flow<List<WarpingMachine>>
    @Query("SELECT * FROM operators ORDER BY name") fun operators(): Flow<List<Operator>>
    @Query("SELECT * FROM galas ORDER BY name") fun galas(): Flow<List<Gala>>
    @Query("SELECT * FROM looms ORDER BY unit, id") fun looms(): Flow<List<Loom>>
    @Query("SELECT * FROM beams ORDER BY createdAt DESC") fun beams(): Flow<List<Beam>>
    @Query("SELECT * FROM takas ORDER BY createdAt DESC") fun takas(): Flow<List<Taka>>
    @Query("SELECT * FROM beam_operators WHERE beamId=:beamId") suspend fun beamOperators(beamId:Long):List<BeamOperator>
    @Query("SELECT * FROM beam_movements ORDER BY createdAt DESC") fun movements(): Flow<List<BeamMovement>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addMachine(item:WarpingMachine)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addOperator(item:Operator)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addGala(item:Gala)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addLoom(item:Loom)
    @Insert suspend fun addBeam(item:Beam):Long
    @Insert suspend fun addBeamOperator(item:BeamOperator)
    @Insert suspend fun addMovement(item:BeamMovement)
    @Insert suspend fun addTaka(item:Taka):Long
    @Query("UPDATE beams SET remainingCuts=MAX(remainingCuts-1,0), status=CASE WHEN remainingCuts<=1 THEN 'COMPLETED' ELSE status END WHERE id=:beamId") suspend fun consumeCut(beamId:Long)
    @Query("UPDATE looms SET activeBeamId=NULL,isActive=0 WHERE id=:loomId") suspend fun freeLoom(loomId:String)
    @Query("UPDATE looms SET activeBeamId=:beamId,isActive=1,galaId=:galaId WHERE id=:loomId") suspend fun mountBeam(loomId:String,beamId:Long,galaId:Long?)
    @Query("SELECT COUNT(*) FROM beams WHERE status='WAREHOUSE'") fun warehouseCount():Flow<Int>
    @Query("SELECT COUNT(*) FROM takas WHERE date(createdAt/1000,'unixepoch','localtime')=date('now','localtime')") fun todayTakaCount():Flow<Int>
    @Query("SELECT COALESCE(SUM(meters),0) FROM takas WHERE date(createdAt/1000,'unixepoch','localtime')=date('now','localtime')") fun todayMeters():Flow<Double>
    @Query("SELECT COUNT(*) FROM looms WHERE isActive=1") fun activeLoomCount():Flow<Int>
    @Query("SELECT COUNT(*) FROM looms") fun loomCount():Flow<Int>
    @Query("SELECT * FROM beams WHERE id=:id") suspend fun beam(id:Long):Beam?
    @Query("SELECT * FROM looms WHERE id=:id") suspend fun loom(id:String):Loom?
}

@Database(entities=[WarpingMachine::class,Operator::class,Gala::class,Loom::class,Beam::class,BeamOperator::class,BeamMovement::class,Taka::class],version=1,exportSchema=false)
abstract class TrackerDatabase:RoomDatabase() {
    abstract fun dao():TrackerDao
    companion object {
        @Volatile private var INSTANCE:TrackerDatabase?=null
        fun get(context:android.content.Context):TrackerDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext,TrackerDatabase::class.java,"textile_tracker.db").build().also { INSTANCE=it }
        }
    }
}
