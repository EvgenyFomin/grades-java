package ru.protei;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityDAO {
    private Map<Long, Entity> entities = new ConcurrentHashMap<>();

    public void persist(Entity entity) {
        entities.put(entity.getId(), entity);
        System.out.println(entity);
        System.out.println("Persisted.");
    }

    public Entity get(Long id) {
        return entities.get(id);
    }
}
