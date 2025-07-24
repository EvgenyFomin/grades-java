package ru.protei.model;

import ru.protei.annotation.JdbcColumn;
import ru.protei.annotation.JdbcEntity;
import ru.protei.annotation.JdbcOneToMany;

import java.util.List;

@JdbcEntity(table = "person")
public class Person {
    @JdbcColumn("name")
    private String name;

    @JdbcOneToMany
    private List<Car> carList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Car> getCarList() {
        return carList;
    }

    public void setCarList(List<Car> carList) {
        this.carList = carList;
    }
}
