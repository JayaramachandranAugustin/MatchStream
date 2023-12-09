package com.whizpath.matchstream.topology;

import com.whizpath.matchstream.generated.MatchScore;
import com.whizpath.matchstream.generated.MatchType;
import com.whizpath.matchstream.generated.Player;
import com.whizpath.matchstream.generated.Team;
import com.whizpath.matchstream.model.Game;
import com.whizpath.matchstream.serde.SerdesFactory;
import com.whizpath.matchstream.topology.MatchTopology;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MatchTopologyTest {
    private static final String SCHEMA_REGISTRY_URL= MatchTopology.class.getName();
    private static final String MOCK_SCHEMA_REGISTRY_URL="mock://"+SCHEMA_REGISTRY_URL;

    private TopologyTestDriver testDriver;

    private TestInputTopic<String, MatchScore> inputTopic;
    private TestOutputTopic<String, Game> nationalGameOutputTopic;
    @BeforeEach
    void setup(){
        StreamsBuilder streamsBuilder=new StreamsBuilder();
        MatchTopology matchTopology=new MatchTopology();
        Topology topology=matchTopology.buildTopology(streamsBuilder);


        Properties properties=new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"game-app");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "mock:1234");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        Serde<String> keyStringSerde =Serdes.String();
        Serde<MatchScore> matchScoreSerde=new SpecificAvroSerde<>();
        Serde<Game> gameSerde= SerdesFactory.gameSerde();

        testDriver=new TopologyTestDriver(topology, properties);
        Map<String, String> config= Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        matchScoreSerde.configure(config, false);

        inputTopic = testDriver.createInputTopic("game", keyStringSerde.serializer(), matchScoreSerde.serializer());
        nationalGameOutputTopic =testDriver.createOutputTopic("NationalGame", keyStringSerde.deserializer(), gameSerde.deserializer());

    }
    @AfterEach
    void tearDown(){
        testDriver.close();
        MockSchemaRegistry.dropScope(SCHEMA_REGISTRY_URL);
    }
    @Test
    void testFlow(){
        MatchScore matchScore=new MatchScore();
        matchScore.setMatchId("1");
        matchScore.setMatchType(MatchType.INTERNATIONAL);
        matchScore.setMatchAttendance(32432424);
        matchScore.setGroundName("Lusail Stadium");
        matchScore.setVenueCity("Lusail");
        matchScore.setVenueState("Lusail");
        matchScore.setVenueCountry("Qatar");
        matchScore.setMatchDateTime("11/22/2023");
        matchScore.setResult("SAUDI_ARABIA");
        matchScore.setFirstHalfExtraTime(4);
        matchScore.setSecondHalfExtraTime(14);

        List<Team> teamList=new ArrayList<>();
        setTeamList("SAUDI_ARABIA", "50", 2, false, 6, 0, teamList);
        setTeamList("ARGENTINA", "5", 1, false, 0, 0, teamList);
        matchScore.setTeams(teamList);

        List<Player> playerList=new ArrayList<>();
        setPlayerList("Lionel Messi", "1", 1, 0, 3, 3, 0, "ARGENTINA", playerList);
        setPlayerList("Saleh Alshehri", "235", 1, 0, 0, 0, 0, "SAUDI_ARABIA", playerList);
        matchScore.setPlayers(playerList);

        inputTopic.pipeInput("1", matchScore);
        assertEquals(nationalGameOutputTopic.readValue().getMatchId(),"1");

    }

    private List<Team> setTeamList(String teamName, String teamId, int teamGoal, boolean isHome, int yellowCardCount, int redCardCount, List<Team> teamList){
        Team team=new Team();
        team.setTeamName(teamName);
        team.setTeamId(teamId);
        team.setTeamGoal(teamGoal);
        team.setIsHome(isHome);
        team.setYellowCardCount(yellowCardCount);
        team.setRedCardCount(redCardCount);
        teamList.add(team);
        return teamList;
    }

    private List<Player> setPlayerList(String playerName, String playerId, int goalCount, int assistCount, int keypass, int dribble, int tackle, String teamName, List<Player> playerList){
        Player player=new Player();
        player.setPlayerName(playerName);
        player.setPlayerId(playerId);
        player.setGoalCount(goalCount);
        player.setAssistCount(assistCount);
        player.setKeypass(keypass);
        player.setDribble(dribble);
        player.setTackle(tackle);
        player.setTeamName(teamName);
        playerList.add(player);
        return playerList;
    }
}
