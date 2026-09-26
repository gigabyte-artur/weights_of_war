package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: процент текущего здоровья выбранной цели (0..100), 0 если цели нет.
public class SensorTargetHealth extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Здоровье цели";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (!(Object_in instanceof BattleUnitCommon))
        {
            return 0;
        }
        BattleObject target = ((BattleUnitCommon) Object_in).getVisibleTargetObject(world);
        if (target == null || target.getMaxHealth() <= 0)
        {
            return 0;
        }
        return (int) Math.round((target.getHealth() * 100.0) / target.getMaxHealth());
    }
}
