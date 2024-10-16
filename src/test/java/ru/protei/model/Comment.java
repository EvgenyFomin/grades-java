package ru.protei.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_text")
    private String text;

    @Column(name = "product_id", nullable = false, updatable = false, insertable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", productId=" + productId +
                '}';
    }
}
