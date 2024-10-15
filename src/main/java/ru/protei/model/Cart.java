package ru.protei.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString
public class Cart {
    private Long id;
    private String name;
    private Set<Product> products;
}
