package com.qimeng.huishou.gpiotest;

public class Entity {

    private String code;
    private int weight;

    public Entity(String code, int weight) {
        this.code = code;
        this.weight = weight;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
