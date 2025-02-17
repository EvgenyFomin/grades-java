package ru.protei;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

        Map<Integer, String> map = client.getMap("mymap");
        map.put(1, "v1");

        Executor executor = Executors.newScheduledThreadPool(1);
        executor.execute();
        System.out.println(map.size());
    }
}
