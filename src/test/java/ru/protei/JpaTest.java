package ru.protei;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import ru.protei.model.*;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JpaTest {
    @Test
    public void testPersistAtFirst() {
        withTransaction(em -> {
            Cart cart = new Cart();
            cart.setName("test-cart");
            em.persist(cart);

            Product product = new Product();
            product.setName("test-product");
            product.setPrice(100.0);

            cart.setProducts(Set.of(product));

            Comment comment1 = new Comment();
            comment1.setText("comment1");

            Comment comment2 = new Comment();
            comment2.setText("comment2");

            Comment comment3 = new Comment();
            comment3.setText("comment3");

            product.setComments(Set.of(comment1, comment2, comment3));
        });
    }

    @Test
    public void requestCarts() {
        withTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Cart> criteriaQuery = cb.createQuery(Cart.class);
            Root<Cart> cartRoot = criteriaQuery.from(Cart.class);
            cartRoot.fetch(Cart_.PRODUCTS).fetch(Product_.COMMENTS);
            criteriaQuery.select(cartRoot);
            List<Cart> carts = em.createQuery(criteriaQuery).getResultList();
            System.out.println(carts.size());
        });
    }

    @Test
    public void requestProducts() {
        withTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = cb.createQuery(Product.class);
            Root<Product> productRoot = criteriaQuery.from(Product.class);
            productRoot.join(Product_.COMMENTS, JoinType.LEFT);
            criteriaQuery.select(productRoot);
            List<Product> products = em.createQuery(criteriaQuery).getResultList();
            System.out.println(products.size());
            System.out.println(products.stream().map(Product::getId).collect(Collectors.toList()));
//            System.out.println(products.stream().map(product -> product.getComments().size()).collect(Collectors.toSet()));
        });
    }

    @Test
    public void requestComments() {
        withTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Comment> criteriaQuery = cb.createQuery(Comment.class);
            Root<Comment> commentRoot = criteriaQuery.from(Comment.class);
            commentRoot.fetch(Comment_.PRODUCT);
            criteriaQuery.select(commentRoot)
                    .where(cb.equal(commentRoot.get(Comment_.PRODUCT).get(Product_.NAME), "test-product"))
            ;
            List<Comment> comments = em.createQuery(criteriaQuery).getResultList();
            System.out.println(comments.stream().map(Comment::getId).collect(Collectors.toSet()));
            System.out.println(comments.stream().map(comment -> comment.getProduct() == null ? "null" : comment.getProduct().getName()).collect(Collectors.toSet()));
        });
    }

    @Test
    public void requestCommentsSubQuery() {
        withTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Comment> criteriaQuery = cb.createQuery(Comment.class);
            Root<Comment> commentRoot = criteriaQuery.from(Comment.class);
            Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
            Root<Product> productRoot = subquery.from(Product.class);
            subquery.select(productRoot.get(Product_.ID)).where(cb.equal(productRoot.get(Product_.NAME), "product-to-find"));
            criteriaQuery.select(commentRoot).where(cb.in(commentRoot.get(Comment_.PRODUCT_ID)).value(subquery));
            List<Comment> carts = em.createQuery(criteriaQuery).getResultList();
            System.out.println(carts.stream().map(Comment::getId).collect(Collectors.toSet()));
        });
    }

    @Test
    public void requestCartsForProducts() {
        withTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Cart> criteriaQuery = cb.createQuery(Cart.class);
            Root<Cart> cartRoot = criteriaQuery.from(Cart.class);
            Join<Cart, Product> join = cartRoot.join(Cart_.PRODUCTS);
            criteriaQuery.select(cartRoot).where(cb.equal(join.get(Product_.NAME), "product-to-find"));
            List<Cart> carts = em.createQuery(criteriaQuery).getResultList();
            System.out.println(carts.stream().map(Cart::getId).collect(Collectors.toSet()));
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
