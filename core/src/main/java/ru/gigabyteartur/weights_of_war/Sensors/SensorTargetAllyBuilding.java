package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: является ли выбранная цель союзным зданием — 100 если да, иначе 0.
public class SensorTargetAllyBuilding extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Цель - союзное здание";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (!(Object_in instanceof BattleUnitCommon))
        {
            return 0;
        }
        BattleObject target = ((BattleUnitCommon) Object_in).getVisibleTargetObject(world);
        if (target == null || !(target instanceof BattleBuildingCommon))
        {
            return 0;
        }
        return Object_in.IsEnemy(target) ? 0 : 100;
    }
}
