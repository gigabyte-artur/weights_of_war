package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: подсчитывает количество врагов в области видимости справа.
// Возвращает 100, если врагов больше 10, иначе — количество врагов * 10.
public class SensorCountEnemyRight extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Врагов справа";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        int sightRange = Object_in.getSightRange();
        if (sightRange <= 0)
        {
            return 0;
        }

        int count = world.CountEnemiesRight(Object_in, sightRange);
        if (count > 10)
        {
            return 100;
        }
        return count * 10;
    }
}
