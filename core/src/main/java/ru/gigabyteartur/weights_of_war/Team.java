package ru.gigabyteartur.weights_of_war;

import java.util.Objects;

// Команда: объединение фракций, которые считают друг друга союзниками.
//
// Отношение «свой-чужой» задаётся здесь и только здесь: фракция хранит свою
// команду (Fraction.getTeam), а вопросы «враг или союзник» и «кто с кем заодно»
// решают методы Team — в бою (BattleObject), в тумане войны (FogOfWar), при
// отборе доноров генов и при определении победителя (GameWorld).
//
// Фракция без команды (team == null) играет сама за себя: союзник у неё только
// она сама. Это прежнее поведение игры, и оно сохранено.
public final class Team
{
    // Имя команды — её единственная характеристика: две команды с одним именем
    // это одна и та же команда.
    private final String name;

    public Team(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    // Союзники, если у обеих фракций задана одна и та же команда. Фракция без
    // команды — союзник только себе (та же ссылка), как было раньше.
    public static boolean SameTeam(Fraction first, Fraction second)
    {
        if (first == null || second == null)
        {
            return false;
        }
        if (first == second)
        {
            return true;
        }
        Team firstTeam = first.getTeam();
        Team secondTeam = second.getTeam();
        if (firstTeam == null || secondTeam == null)
        {
            return false;
        }
        return firstTeam.equals(secondTeam);
    }

    // Враги — всё, что не союзники. Фракции без команды остаются врагами всем,
    // кроме самих себя, как и раньше.
    public static boolean AreEnemies(Fraction first, Fraction second)
    {
        if (first == null || second == null)
        {
            return first != second;
        }
        return !SameTeam(first, second);
    }

    // Ключ стороны, за которую играет фракция: её команда, а если команды нет —
    // сама фракция. Им сравниваются стороны между собой, например в условии
    // победы: две одинаково названные фракции без команды — две разные стороны,
    // а не одна, иначе «каждый сам за себя» переставало бы быть верным.
    public static Object KeyOf(Fraction fraction)
    {
        if (fraction == null)
        {
            return null;
        }
        Team team = fraction.getTeam();
        return (team != null) ? team : fraction;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof Team))
        {
            return false;
        }
        return Objects.equals(name, ((Team) other).name);
    }

    @Override
    public int hashCode()
    {
        return (name == null) ? 0 : name.hashCode();
    }

    @Override
    public String toString()
    {
        return name;
    }
}
