package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Базовый класс сенсора.
public abstract class SensorCommon
{
    public abstract int CheckSensor(BattleObject Object_in, GameWorld world);

    // Имя сенсора для отображения (по умолчанию — имя класса без префикса Sensor).
    public String GetName()
    {
        String simpleName = getClass().getSimpleName();
        if (simpleName.startsWith("Sensor"))
        {
            return simpleName.substring("Sensor".length());
        }
        return simpleName;
    }
}
