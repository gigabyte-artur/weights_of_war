package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Команда: идти к своему ядру (зданию своей фракции).
public class CommandMoveToOwnCore extends CommandUnit
{
    private boolean initialized = false;
    private int targetX;
    private int targetY;

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        if (!initialized)
        {
            BattleBuildingCommon core = world.FindOwnCore(unit);
            if (core == null)
            {
                return true; // своего ядра нет — команда завершена.
            }

            int dx = core.getX() - unit.getX();
            int dy = core.getY() - unit.getY();
            if (dx == 0 && dy == 0)
            {
                return true; // уже у ядра.
            }

            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            int step = 10;
            if (distance <= step)
            {
                targetX = core.getX();
                targetY = core.getY();
            }
            else
            {
                targetX = unit.getX() + Math.round((dx / distance) * step);
                targetY = unit.getY() + Math.round((dy / distance) * step);
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
        return "К ядру";
    }
}
