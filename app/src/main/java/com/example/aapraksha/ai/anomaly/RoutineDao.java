package com.example.aapraksha.ai.anomaly;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RoutineDao {
    @Insert
    void insert(RoutineProfile profile);

    @Update
    void update(RoutineProfile profile);

    @Query("SELECT * FROM routine_profiles WHERE geohash = :geohash AND dayOfWeek = :dayOfWeek AND hourOfDay = :hourOfDay LIMIT 1")
    RoutineProfile getProfile(String geohash, int dayOfWeek, int hourOfDay);
    
    @Query("SELECT * FROM routine_profiles WHERE geohash = :geohash")
    List<RoutineProfile> getProfilesForLocation(String geohash);

    @Query("SELECT * FROM routine_profiles ORDER BY visitCount DESC LIMIT 10")
    List<RoutineProfile> getFrequentLocations();
}
