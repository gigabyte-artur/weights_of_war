package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Берёт в цель ближайшее здание противника.
public class CommandTargetEnemyBuilding extends CommandUnit
{
    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        BattleBuildingCommon nearest = world.FindNearestEnemyBuilding(unit);
        if (nearest == null)
        {
            return true; // зданий противника нет — команда завершена.
        }

        unit.setTargetObject(nearest);
        unit.setTarget(nearest.getX(), nearest.getY());
        return true;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Целится в здание";
    }
}
