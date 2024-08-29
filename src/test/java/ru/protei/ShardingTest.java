package ru.protei;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.protei.model.Car;
import ru.protei.model.User;
import ru.protei.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ShardingTest {
    @Autowired
    UserRepository userRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testWithoutCars() {
        long shardingId = new Date().getTime();

        String name = UUID.randomUUID().toString();

        User user = new User();
        user.setName(name);
        user.setId(shardingId);

        User user2 = new User();
        user2.setName(name);
        user2.setId(shardingId + 1);

        userRepository.saveAll(List.of(user, user2));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testWithCarsOnTheSameShard() {
        long shardingId = new Date().getTime();

        User user = new User();
        user.setName("hello");
        user.setId(shardingId);

        Car car = new Car();
        car.setName("mycar");
        car.setId(shardingId + 2);

        user.setCars(List.of(car));

        userRepository.save(user);
    }

    @Test
    @Transactional
    public void testGetUsers() {
        Page<User> page = userRepository.findAll(PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "id")));
        System.out.println(page.getContent());
    }
}
