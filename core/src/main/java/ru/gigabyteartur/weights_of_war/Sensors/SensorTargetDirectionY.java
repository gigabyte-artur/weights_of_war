package ru.gigabyteartur.weights_of_war.Sensors;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сенсор: вертикальное направление на выбранную цель, -100..100 (0 - цели нет).
public class SensorTargetDirectionY extends SensorCommon
{
    @Override
    public String GetName()
    {
        return "Направление на цель по Y";
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
        if (distance <= 0f)
        {
            return 0;
        }
        return Math.round((dy / distance) * 100f);
    }
}
