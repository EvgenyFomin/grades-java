package ru.protei.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

//@Entity
//@Table(name = "product")
@Getter
@Setter
@ToString
public class Product {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "name")
    private String name;

//    @Column(name = "price")
    private Double price;

//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id", nullable = false, updatable = false, insertable = false)
    private Set<Comment> comments;
}
