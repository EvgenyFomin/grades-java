package ru.protei.model;

import ru.protei.annotation.JdbcColumn;
import ru.protei.annotation.JdbcEntity;

@JdbcEntity(table = "car")
public class Car {
    @JdbcColumn("id")
    private Long id;

    @JdbcColumn("name")
    private String name;

    @JdbcColumn("number")
    private Double number;
}
