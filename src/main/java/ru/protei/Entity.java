package ru.protei;

public class Entity {
    private Long id;
    private String name;
    private int weight;
    private int length;

    public Entity(Long id, String name, int weight, int length) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.length = length;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", weight=" + weight +
                ", length=" + length +
                '}';
    }
}
