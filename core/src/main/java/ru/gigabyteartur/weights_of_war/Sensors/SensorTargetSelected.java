package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: выбрана ли цель — 100 если есть объект-цель, иначе 0.
public class SensorTargetSelected extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Цель выбрана";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (!(Object_in instanceof BattleUnitCommon))
        {
            return 0;
        }
        BattleObject target = ((BattleUnitCommon) Object_in).getVisibleTargetObject(world);
        return (target == null) ? 0 : 100;
    }
}
