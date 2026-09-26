package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Команда атаки ближайшего врага.
public class CommandAttackNearest extends CommandAttackTarget
{
    public CommandAttackNearest()
    {
        super(null);
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Каждый кадр ищем ближайшего живого врага.
        BattleObject nearest = world.FindNearestEnemy(unit);
        if (nearest == null)
        {
            return true; // врагов нет — команда завершена.
        }
        setTargetUnit(nearest);
        unit.setTargetObject(nearest);
        return super.Execute(unit, world);
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Атакует ближайшего";
    }
}
