package ru.protei;

import com.hazelcast.client.Client;
import com.hazelcast.client.ClientListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Server {
    public static void main(String[] args) {
        Config config = new Config();
        MapConfig mapConfig = new MapConfig("mymap");
        mapConfig.setTimeToLiveSeconds(10);
        config.addMapConfig(mapConfig);
        HazelcastInstance server = Hazelcast.newHazelcastInstance(config);
        server.getClientService().addClientListener(new ClientListener() {
            @Override
            public void clientConnected(Client client) {
                System.out.println("Client connected: " + client.getUuid());
            }

            @Override
            public void clientDisconnected(Client client) {
                System.out.println("Client disconnected" + client.getUuid());
            }
        });
    }
}
