package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: близость ближайшего раненого союзника, расположенного ниже.
// Возвращает 100 при расстоянии 0, 0 при расстоянии равном длине обзора.
public class SensorWoundedAllyDown extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Раненый союзник снизу";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        int sightRange = Object_in.getSightRange();
        if (sightRange <= 0)
        {
            return 0;
        }

        BattleObject nearest = world.FindNearestWoundedAllyDown(Object_in, sightRange);
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
