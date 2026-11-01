package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.Color;

// Фракция объекта: имя, цвет и команда (Team), за которую она играет.
// Отношения «свой-чужой» определяет команда — см. Team; без команды фракция
// играет сама за себя.
public class Fraction
{
    private Color color;
    private String name;
    private Team team;    // null — фракция играет сама за себя (старое поведение).

    public Fraction(String name, Color color)
    {
        this(name, color, null);
    }

    public Fraction(String name, Color color, Team team)
    {
        this.name = name;
        this.color = color;
        this.team = team;
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

    // Команда фракции (null — фракция играет сама за себя).
    public Team getTeam() {
        return team;
    }

    // Имя команды фракции (null — команды нет). Для отчётов, подписей и списков.
    public String getTeamName() {
        return team == null ? null : team.getName();
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
