package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: расстояние до своего ядра (здания своей фракции).
// Возвращает расстояние в процентах от 1000 пикселей (0 — у ядра, 100 — 1000 px и дальше).
public class SensorDistanceToOwnCore extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Дистанция до ядра";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        BattleBuildingCommon core = world.FindOwnCore(Object_in);
        if (core == null)
        {
            return 100; // своего ядра нет — считаем расстояние максимальным.
        }

        float distance = Object_in.GetDistance(core);
        int value = (int) Math.round(distance * 100.0 / 1000.0);
        if (value > 100)
        {
            value = 100;
        }
        return value;
    }
}
