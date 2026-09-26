package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: накопленное время атаки (сколько секунд юнит находится под атакой).
// Значение ограничено диапазоном 0..100.
public class SensorAttacked extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Под атакой";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (Object_in instanceof BattleUnitCommon)
        {
            float attacked = ((BattleUnitCommon) Object_in).getAttacked();
            return (int) Math.min(100, Math.round(attacked));
        }
        return 0;
    }
}
