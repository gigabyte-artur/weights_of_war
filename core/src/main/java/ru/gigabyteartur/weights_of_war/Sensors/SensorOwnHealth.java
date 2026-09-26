package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Сенсор: процент собственного здоровья (от 0 до 100).
public class SensorOwnHealth extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Своё здоровье";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        int maxHealth = Object_in.getMaxHealth();
        if (maxHealth <= 0)
        {
            return 0;
        }
        return (int) Math.round((Object_in.getHealth() * 100.0) / maxHealth);
    }
}
