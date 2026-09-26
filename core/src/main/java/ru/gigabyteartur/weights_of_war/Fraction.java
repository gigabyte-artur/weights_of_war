package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.Color;

// Фракция объекта: имя и цвет.
public class Fraction
{
    private Color color;
    private String name;

    public Fraction(String name, Color color)
    {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
