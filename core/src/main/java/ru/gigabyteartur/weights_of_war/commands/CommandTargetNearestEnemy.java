package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Берёт в цель ближайшего живого врага (объекта другой фракции).
public class CommandTargetNearestEnemy extends CommandUnit
{
    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        BattleObject nearest = world.FindNearestEnemy(unit);
        if (nearest == null)
        {
            return true; // врагов нет — команда завершена.
        }

        unit.setTargetObject(nearest);
        unit.setTarget(nearest.getX(), nearest.getY());
        return true;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Целится во врага";
    }
}
