package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Двигается на 10 пикселей в направлении текущей цели.
public class CommandMoveToTarget extends CommandUnit
{
    private boolean initialized = false;
    private int targetX;
    private int targetY;

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Направление к цели вычисляется один раз.
        if (!initialized)
        {
            int dx = unit.getTargetX() - unit.getX();
            int dy = unit.getTargetY() - unit.getY();

            if (dx == 0 && dy == 0)
            {
                return true; // цель достигнута.
            }

            float distance = (float) (dx * dx + dy * dy);

            // Если цель ближе 10 пикселей — двигаемся прямо к ней.
            if (distance <= 100)
            {
                targetX = unit.getX() + dx;
                targetY = unit.getY() + dy;
            }
            else
            {
                targetX = unit.getX() + Math.round((dx / distance) * 100);
                targetY = unit.getY() + Math.round((dy / distance) * 100);
            }

            initialized = true;
        }

        unit.setTarget(targetX, targetY);
        unit.MoveToTarget(world);
        return unit.getX() == targetX && unit.getY() == targetY;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "К цели";
    }
}
