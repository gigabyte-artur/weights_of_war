package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Команда перемещения юнита в заданную точку.
public class CommandMove extends CommandUnit
{
    private int x;
    private int y;

    public CommandMove(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        unit.setTarget(x, y);
        unit.MoveToTarget(world);
        return unit.getX() == x && unit.getY() == y;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "В точку";
    }
}
