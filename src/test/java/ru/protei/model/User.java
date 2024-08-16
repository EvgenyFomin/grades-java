package ru.protei.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column
    protected String name;

    @Column(name = "last_name")
    protected String lastName;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    protected List<Car> cars;

    @Override
    public boolean equals(Object object) {
        System.out.println();
        System.out.println("------equals------");
        System.out.println("classes are equal=" + (getClass() == object.getClass()));
        System.out.println("object is instance of user=" + (object instanceof User));
        if (!(object instanceof User user)) {
            return false;
        }
        System.out.println("this.className=" + getClass().getName());
        System.out.println("user className=" + user.getClass().getName());
        System.out.println("direct this.id=" + id);
        System.out.println("direct this.name=" + name);
        System.out.println("direct user.id=" + user.id);
        System.out.println("direct user.name=" + user.name);
        System.out.println("before user.getId()");
        System.out.println("user.getId()=" + user.getId());
        System.out.println("before user.getName()");
        System.out.println("user.getName()=" + user.getName());
        System.out.println("direct user.id=" + user.id);
        System.out.println("direct user.name=" + user.name);
        System.out.println("------equals------");
        System.out.println();
        return Objects.equals(id, user.getId()) && Objects.equals(name, user.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
