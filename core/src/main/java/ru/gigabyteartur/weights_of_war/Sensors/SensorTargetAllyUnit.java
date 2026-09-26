package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: является ли выбранная цель союзным юнитом — 100 если да, иначе 0.
public class SensorTargetAllyUnit extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Цель - союзный юнит";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (!(Object_in instanceof BattleUnitCommon))
        {
            return 0;
        }
        BattleObject target = ((BattleUnitCommon) Object_in).getVisibleTargetObject(world);
        if (target == null || !(target instanceof BattleUnitCommon))
        {
            return 0;
        }
        return Object_in.IsEnemy(target) ? 0 : 100;
    }
}
