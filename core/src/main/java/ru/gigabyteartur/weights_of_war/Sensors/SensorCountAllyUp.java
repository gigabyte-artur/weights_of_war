package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: подсчитывает количество союзников в области видимости сверху.
// Возвращает 100, если союзников больше 10, иначе — количество союзников * 10.
public class SensorCountAllyUp extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Союзников сверху";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        int sightRange = Object_in.getSightRange();
        if (sightRange <= 0)
        {
            return 0;
        }

        int count = world.CountAlliesUp(Object_in, sightRange);
        if (count > 10)
        {
            return 100;
        }
        return count * 10;
    }
}
