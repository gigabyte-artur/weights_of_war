package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Базовая команда перемещения на заданное число единиц в направлении (dx, dy).
public abstract class CommandMoveDirection extends CommandUnit
{
    private final int dx;
    private final int dy;

    private boolean initialized = false;
    private int targetX;
    private int targetY;

    protected CommandMoveDirection(int dx, int dy)
    {
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Цель вычисляется один раз — от текущей позиции юнита.
        if (!initialized)
        {
            targetX = unit.getX() + dx;
            targetY = unit.getY() + dy;
            initialized = true;
        }

        unit.setTarget(targetX, targetY);
        unit.MoveToTarget(world);
        return unit.getX() == targetX && unit.getY() == targetY;
    }
}
