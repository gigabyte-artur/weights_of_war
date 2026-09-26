package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: определяет близость ближайшего вражеского объекта, расположенного слева.
// Возвращает 100 при расстоянии 0, 0 при расстоянии равном длине обзора,
// между ними — пропорционально отношению.
public class SensorEnemyLeft extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Враг слева";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        int sightRange = Object_in.getSightRange();
        if (sightRange <= 0)
        {
            return 0;
        }

        BattleObject nearest = world.FindNearestEnemyLeft(Object_in, sightRange);
        if (nearest == null)
        {
            return 0;
        }

        float distance = Object_in.GetDistance(nearest);

        if (distance <= 0)
        {
            return 100;
        }
        if (distance >= sightRange)
        {
            return 0;
        }

        return (int) Math.round(100.0 * (sightRange - distance) / sightRange);
    }
}
