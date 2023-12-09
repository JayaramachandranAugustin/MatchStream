package com.whizpath.matchstream.serde;

import com.whizpath.matchstream.model.Game;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class SerdesFactory {
    public static Serde<Game> gameSerde(){
        JsonSerializer<Game> jsonSerializer=new JsonSerializer<>();
        JsonDeserializer<Game> jsonDeserializer =new JsonDeserializer<>(Game.class);
        return Serdes.serdeFrom(jsonSerializer,jsonDeserializer);
    }
}
