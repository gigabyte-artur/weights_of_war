package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Двигается на 10 пикселей в направлении текущей цели.
public class CommandMoveToTarget extends CommandUnit
{
    public final static int STEP_PX = 100;   // Длина шага к цели за одно исполнение команды, px.

    private boolean initialized = false;
    private int targetX;
    private int targetY;

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Направление к цели вычисляется один раз.
        if (!initialized)
        {
            int dx = unit.getTargetX() - unit.getX();
            int dy = unit.getTargetY() - unit.getY();

            if (dx == 0 && dy == 0)
            {
                return true; // цель достигнута.
            }

            // ВАЖНО: здесь нужно ЕВКЛИДОВО расстояние, а не его квадрат.
            // Раньше в distance лежало dx*dx + dy*dy, поэтому шаг получался
            // dx / (dx^2+dy^2) * 100 — доли пикселя при цели в сотнях пикселей.
            // Юнит практически не двигался к цели.
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= STEP_PX)
            {
                targetX = unit.getX() + dx;
                targetY = unit.getY() + dy;
            }
            else
            {
                targetX = unit.getX() + Math.round((dx / distance) * STEP_PX);
                targetY = unit.getY() + Math.round((dy / distance) * STEP_PX);
            }

            initialized = true;
        }

        unit.setTarget(targetX, targetY);
        unit.MoveToTarget(world);
        return unit.getX() == targetX && unit.getY() == targetY;
    }

    @Override
    public boolean IsPersistentOrder()
    {
        return true;
    }

    // Цель приказа могла сдвинуться: переиздавая «К цели», берём её текущие координаты,
    // иначе юнит шёл бы в точку, где враг стоял десять секунд назад.
    @Override
    public void OnOrderRepeat(BattleUnitCommon unit, GameWorld world)
    {
        BattleObject goal = unit.getVisibleTargetObject(world);
        if (goal != null)
        {
            unit.setTarget(goal.getX(), goal.getY());
        }
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "К цели";
    }
}
