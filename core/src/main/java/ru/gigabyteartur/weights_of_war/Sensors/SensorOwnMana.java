package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;

// Сенсор: процент собственной маны (только у жреца; для остальных юнитов — 0).
public class SensorOwnMana extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Мана";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (Object_in instanceof UnitPriest)
        {
            return ((UnitPriest) Object_in).getManaPercent();
        }
        return 0;
    }
}
