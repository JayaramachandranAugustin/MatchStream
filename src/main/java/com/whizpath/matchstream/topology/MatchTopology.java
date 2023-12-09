package com.whizpath.matchstream.topology;

import com.whizpath.matchstream.generated.MatchScore;
import com.whizpath.matchstream.generated.MatchType;
import com.whizpath.matchstream.generated.PlayerMatchStats;
import com.whizpath.matchstream.model.Game;
import com.whizpath.matchstream.serde.SerdesFactory;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MatchTopology {

    @Autowired
    public Topology buildTopology(StreamsBuilder streamsBuilder){
        KStream<String, MatchScore> matchStream = streamsBuilder.stream("game");
        matchStream.print(Printed.<String,MatchScore>toSysOut().withLabel("game"));
        Map<String, KStream<String, MatchScore>> branches=matchStream.filterNot(matchTypePredicate(MatchType.CLUB_FRIENDLY)).split(Named.as("match-stream-"))
                .branch(matchTypePredicate(MatchType.INTERNATIONAL), Branched.withFunction(Function.identity(),"national"))
                .branch(matchTypePredicate(MatchType.INTERNATIONAL_FRIENDLY), Branched.withFunction(Function.identity(),"national-friendly"))
                .branch(matchTypePredicate(MatchType.CLUB), Branched.withFunction(Function.identity(),"club")).noDefaultBranch();

        KStream<String, MatchScore> nationalStream = branches.get("match-stream-national");

        nationalStream.map((key,matchScore)-> KeyValue.pair(key,gameMapper(matchScore))).to("NationalGame",Produced.with(Serdes.String(), SerdesFactory.gameSerde()));

        //nationalStream.mapValues((readOnlyKey,matchScore)-> gameMapper(matchScore));

        KStream<String, MatchScore> nationalFriendlyStream = branches.get("match-stream-national-friendly");

        KStream<String, MatchScore> mergedNationalStream = nationalStream.merge(nationalFriendlyStream);

        KStream<String, PlayerMatchStats> transformedNationalStream = mergedNationalStream.flatMap((key,matchScore)->{
            var playerMatchStatsList = mapPlayerMatchData(matchScore);
            return playerMatchStatsList.stream().map(playerMatchStats -> {
                return KeyValue.pair(playerMatchStats.getPlayerId(), playerMatchStats);
            }).collect(Collectors.toList());
        });
        transformedNationalStream.to("PlayerMatchStatsNational");

        return streamsBuilder.build();
    }

    private Predicate<String,MatchScore> matchTypePredicate(MatchType matchType){
        return (str, matchScore) -> matchScore.getMatchType().name().equals(matchType.name());
    }

    public Game gameMapper(MatchScore matchScore){
        return Game.builder().matchId(matchScore.getMatchId()).matchType(matchScore.getMatchType().name()).matchAttendance(matchScore.getMatchAttendance())
                .groundName(matchScore.getGroundName()).venueCity(matchScore.getVenueCity()).venueState(matchScore.getVenueState()).venueCountry(matchScore.getVenueCountry())
                .matchDateTime(matchScore.getMatchDateTime()).result(matchScore.getResult()).firstHalfExtraTime(matchScore.getFirstHalfExtraTime())
                .secondHalfExtraTime(matchScore.getSecondHalfExtraTime()).build();
    }

    public List<PlayerMatchStats> mapPlayerMatchData(MatchScore matchScore){
        if(matchScore ==null || matchScore.getPlayers() == null|| matchScore.getPlayers().isEmpty()) return null;
        return matchScore.getPlayers().stream().map(value-> new PlayerMatchStats(value.getPlayerId(),value.getPlayerName(), value.getGoalCount(),
                value.getAssistCount(),value.getKeypass(),value.getDribble(),value.getTackle(),matchScore.getMatchId(),value.getTeamName(),90.00)).collect(Collectors.toList());
    }
}
