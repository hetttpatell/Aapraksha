package com.example.aapraksha.ai.anomaly;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routine_profiles")
public class RoutineProfile {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public double latitude;
    public double longitude;
    public String geohash;
    
    public int dayOfWeek;      // 1-7 (Sunday-Saturday)
    public int hourOfDay;      // 0-23
    
    public int visitCount;     // frequency counter
    public long lastVisitMs;
    
    public String locationType; // "HOME", "WORK", "FREQUENT", "UNKNOWN"
}
