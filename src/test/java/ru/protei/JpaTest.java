package ru.protei;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.junit.jupiter.api.Test;
import ru.protei.model.Cart;
import ru.protei.model.Comment;
import ru.protei.model.Product;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class JpaTest {
    @Test
    public void testPersistAtFirst() {
        withTransaction(em -> {
//            Cart cart = new Cart();
//            cart.setName("test-cart");
//            em.persist(cart);

            Product product = new Product();
            product.setName("test-product");
            product.setPrice(100.0);
            em.persist(product);

//            cart.setProducts(List.of(product));

            Comment comment1 = new Comment();
            comment1.setText("comment1");

            Comment comment2 = new Comment();
            comment2.setText("comment2");

            Comment comment3 = new Comment();
            comment3.setText("comment3");

            product.setComments(List.of(comment1, comment2, comment3));
        });
    }

    @Test
    public void requests() {
        withTransaction(em -> {
            Cart cart = em.find(Cart.class, 3L);
            System.out.println("before");
            System.out.println(cart);

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Cart> cq = cb.createQuery(Cart.class);
//            cq.select(cq.from(Cart.class));
//            TypedQuery<Cart> query = em.createQuery(cq);
//            List<Cart> carts = query.getResultList();
//            System.out.println("before");
//            System.out.println(carts);
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
