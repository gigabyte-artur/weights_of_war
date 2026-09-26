package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: накопленное время застревания (сколько секунд юнит не может сдвинуться).
// Значение ограничено диапазоном 0..100.
public class SensorStuck extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Застревание";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (Object_in instanceof BattleUnitCommon)
        {
            float stuck = ((BattleUnitCommon) Object_in).getStuck();
            return (int) Math.min(100, Math.round(stuck));
        }
        return 0;
    }
}
