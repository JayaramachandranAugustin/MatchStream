package com.whizpath.matchstream.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String matchId;
    private String matchType;
    private long matchAttendance;
    private String groundName;
    private String venueCity;
    private String venueState;
    private String venueCountry;
    private String matchDateTime;
    private String result;
    private int firstHalfExtraTime;
    private int secondHalfExtraTime;
}