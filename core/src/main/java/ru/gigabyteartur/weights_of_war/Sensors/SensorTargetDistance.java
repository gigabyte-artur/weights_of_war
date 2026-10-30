package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: дистанция до выбранной цели, 0..100 (100 - далеко или цели нет).
// Насыщается на SATURATION_DISTANCE: за ней значение уже неважно, важен сам факт «далеко».
public class SensorTargetDistance extends SensorCommon
{
    public final static float SATURATION_DISTANCE = 800f;   // Дистанция, на которой сенсор доходит до 100, px.

    @Override
    public String GetName()
    {
        return "Дистанция до цели";
    }

    @Override
    public int CheckSensor(BattleObject Object_in, GameWorld world)
    {
        if (!(Object_in instanceof BattleUnitCommon))
        {
            return 0;
        }
        BattleObject target = ((BattleUnitCommon) Object_in).getVisibleTargetObject(world);
        if (target == null)
        {
            return 0;
        }
        float dx = target.getX() - Object_in.getX();
        float dy = target.getY() - Object_in.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        int value = Math.round(distance / SATURATION_DISTANCE * 100f);
        return Math.max(0, Math.min(100, value));
    }
}
