package ru.protei;

import jakarta.persistence.*;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.spi.PersistentBag;
import org.junit.jupiter.api.Test;
import ru.protei.model.Car;
import ru.protei.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class JpaTest {
    @Test
    public void testPersistAtFirst() {
        withTransaction(em -> {
            User user = new User();
            em.persist(user);

            Car mercedes = new Car();
            mercedes.setName("mercedes");

            Car bmw = new Car();
            bmw.setName("bmw");

            user.setName("hello");
            user.setCars(List.of(mercedes, bmw));

            System.out.println("persist end");
        });
    }

    @Test
    public void testPersistLater() {
        withTransaction(em -> {
            Car mercedes = new Car();
            mercedes.setName("mercedes");

            Car bmw = new Car();
            bmw.setName("bmw");

            User user = new User();

            user.setName("hello");
            user.setCars(new ArrayList<>(List.of(mercedes, bmw)));

            em.persist(user);

            System.out.println("persist end");
        });
    }

    @Test
    public void testPersistedEntityCollection() {
        withTransaction(em -> {
            Car mercedes = new Car();
            mercedes.setName("mercedes");

            Car bmw = new Car();
            bmw.setName("bmw");

            User user = new User();

            user.setName("hello");
            user.setCars(new ArrayList<>(List.of(mercedes, bmw)));

            System.out.println("Cars field is of ArrayList class=" + (user.getCars().getClass() == ArrayList.class));
            System.out.println("Cars field is of PersistentBag class=" + (user.getCars().getClass() == PersistentBag.class));

            em.persist(user);

            System.out.println("Cars field is of ArrayList class=" + (user.getCars().getClass() == ArrayList.class));
            System.out.println("Cars field is of PersistentBag class=" + (user.getCars().getClass() == PersistentBag.class));

            System.out.println("persist end");
        });
    }

    @Test
    public void testUpdate() {
        Long id = createUser();
        withTransaction(em -> {
            User userToUpdate = em.find(User.class, id);
            System.out.println("before set");
            userToUpdate.setName("hello2");
            System.out.println("after set");
        });
    }

    @Test
    public void testReferenceWithUser() {
        Long id = createUser();
        withTransaction(em -> {
            User user = em.getReference(User.class, id);
            System.out.println("before getName");
            System.out.println("name=" + user.getName());
            System.out.println("after getName");
        });
    }

    @Test
    public void testReferenceWithoutUser() {
        withTransaction(em -> {
            User user = em.getReference(User.class, -1);
            System.out.println("before getName");
            try {
                System.out.println("name=" + user.getName());
            } catch (EntityNotFoundException exception) {
                System.err.println("Object Not found!!");
            }
            System.out.println("after getName");
        });
    }

    @Test
    public void testReferenceWithAlreadyExistedInPersistentContextUser() {
        Long id = createUser();
        withTransaction(em -> {
            User loadedUser = em.find(User.class, id);
            User user = em.getReference(User.class, id);
            System.out.println("Users are the same=" + (loadedUser == user));
        });
    }

    @Test
    public void testReferenceEquals() {
        Long id = createUser();
        withTransaction(em -> {
            System.out.println("before reference");
            User user = em.getReference(User.class, id - 1);
            User user2 = em.getReference(User.class, id);
            System.out.println(user.equals(user2));
        });
    }

    @Test
    public void testReferenceOutsideOfPersistentContext() {
        Long id = createUser();
        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            users.add(em.getReference(User.class, id));
        });

        User user = users.get(0);

        try {
            user.getName();
        } catch (LazyInitializationException exception) {
            System.err.println("Session was closed!");
        }
    }

    @Test
    public void testLoadedReferenceOutsideOfPersistentContext() {
        Long id = createUser();
        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            User reference = em.getReference(User.class, id);
            reference.getName();
            users.add(reference);
        });

        User user = users.get(0);
        System.out.println(user.getName());
    }

    @Test
    public void testCollectionLazyInit() {
        Long id = createUser();
        withTransaction(em -> {
            User user = em.find(User.class, id);
            System.out.println("before getCars");
            List<Car> cars = user.getCars();
            System.out.println("after getCars");

            PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();

            System.out.println("Cars field is loaded=" + persistenceUtil.isLoaded(user, "cars"));
            System.out.println("List class is assignable from cars field class=" + List.class.isAssignableFrom(cars.getClass()));
            System.out.println("Cars field is of ArrayList class=" + Objects.equals(cars.getClass(), ArrayList.class));
            System.out.println("Cars field is of PersistentBag class=" + Objects.equals(cars.getClass(), PersistentBag.class));

            System.out.println("before iterator");
            cars.iterator();
            System.out.println("after iterator");
        });
    }

    @Test
    public void testLazyCollectionOutsideOfPersistentContext() {
        Long id = createUser();
        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            users.add(em.find(User.class, id));
        });
        User user = users.get(0);
        try {
            user.getCars().iterator();
        } catch (LazyInitializationException exception) {
            System.err.println("Session was closed!");
        }
    }

    @Test
    public void testRemoveEntity() {
        Long id = createUser();
        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            User user = em.find(User.class, id);
            users.add(user);
            System.out.println("before remove");
            em.remove(user);
            System.out.println("after remove");
        });
        User user = users.get(0);
        System.out.println(user.getCars());
        System.out.println("Cars field is of ArrayList class=" + Objects.equals(user.getCars().getClass(), ArrayList.class));
        System.out.println("Cars field is of PersistentBag class=" + Objects.equals(user.getCars().getClass(), PersistentBag.class));
    }

    @Test
    public void testRecoverEntity() {
        Long id = createUser();
        withTransaction(em -> {
            User user = em.find(User.class, id);
            System.out.println("before remove");
            em.remove(user);
            System.out.println("after remove. id=" + user.getId());
            em.persist(user);
            System.out.println("after persist. id=" + user.getId());
        });
    }

    @Test
    public void testDetach() {
        Long id = createUser();
        withTransaction(em -> {
            User user = em.find(User.class, id);
            em.detach(user);
            try {
                user.getCars().iterator();
            } catch (LazyInitializationException exception) {
                System.err.println("----Session was closed!");
            }
            user.setName("testDetach");
            System.out.println("end of testDetach1");
        });
        withTransaction(em -> {
            User user = em.find(User.class, id);
            System.out.println("detach");
            em.detach(user);
            System.out.println("setName");
            user.setName("testDetach");
            System.out.println("merge");
            em.merge(user);
            System.out.println("end of testDetach2");
        });
    }

    @Test
    public void testMerge() {
        Long id = createUser();
        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            users.add(em.find(User.class, id));
        });
        User user = users.get(0);
        try {
            user.getCars().iterator();
        } catch (LazyInitializationException exception) {
            System.err.println("----Session was closed!");
        }
        withTransaction(em -> {
            try {
                user.getCars().iterator();
            } catch (LazyInitializationException exception) {
                System.err.println("----User is not attached yet!");
            }
            System.out.println("before merge");
            User mergedUser = em.merge(user);
            System.out.println("after merge");
            System.out.println(mergedUser.getCars());
        });
    }

    @Test
    public void testSave() {
        Long id = createUser();

        List<User> users = new ArrayList<>();
        withTransaction(em -> {
            User e = em.find(User.class, id);
            e.getCars().iterator();
            users.add(e);
        });

        User user = users.get(0);

        User userToSave = new User();
        userToSave.setId(id);
        userToSave.setName("testSave");
        userToSave.setCars(new ArrayList<>(user.getCars()));

        withTransaction(em -> {
            User mergedUser = em.merge(userToSave);
            System.out.println("mergedUser.getName()=" + mergedUser.getName());
        });
    }

    private Long createUser() {
        return withTransaction(em -> {
            User user = new User();
            em.persist(user);

            Car mercedes = new Car();
            mercedes.setName("mercedes");

            Car bmw = new Car();
            bmw.setName("bmw");

            user.setName("hello");
            user.setLastName("hello2");
            user.setCars(List.of(mercedes, bmw));

            return user.getId();
        });
    }

    private void withTransaction(Consumer<EntityManager> entityManagerConsumer) {
        withTransaction(em -> {
            entityManagerConsumer.accept(em);
            return null;
        });
    }

    private <T> T withTransaction(Function<EntityManager, T> callbackFunc) {
        T t;

        try (EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("some-name")) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            t = callbackFunc.apply(entityManager);
            transaction.commit();
            System.out.println();
        }

        return t;
    }
}
