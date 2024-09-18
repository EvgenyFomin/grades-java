package ru.protei.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "comment")
@Getter
@Setter
@ToString
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
}
