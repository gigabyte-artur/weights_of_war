package ru.gigabyteartur.weights_of_war.brains;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.commands.CommandHealNearestWounded;
import ru.gigabyteartur.weights_of_war.commands.CommandMoveToOwnCore;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;

import java.util.Random;

// Простая стратегия для одного ядра: контрольная линейка, с которой сравнивают
// обучение. Внутри нет ни сети, ни сенсоров, ни фита — только правила. Поэтому
// поведение этой стратегии не меняется от поколения к поколению, и разница
// в замерах относится к обучению, а не к тому, что стратегия тоже «подросла».
//
// Правила по приоритету (порядок важен, ветки не пересекаются):
//   1. здоровья меньше половины — отступаем к своему ядру, кого бы ни видели:
//      раненый юнит не воюет;
//   2. здоровья не меньше половины и известен (виден фракции) живой враг —
//      разбираемся с ним: обычный юнит атакует, жрец лечит самого раненого
//      союзника, а если лечить некого, ведёт себя как все остальные здоровые;
//   3. здоровья не меньше половины и врагов не видно вовсе — идём в случайном
//      направлении пять игровых секунд, затем выбираем заново.
public class ScriptedBrain implements UnitBrain
{
    // Порог «здоров настолько, чтобы идти в бой», доля от максимума.
    public final static float HEALTHY_FRACTION = 0.5f;

    private final Random random;

    public ScriptedBrain()
    {
        this(new Random());
    }

    // Источник случайности передаётся снаружи, чтобы тесты могли его зафиксировать.
    public ScriptedBrain(Random random)
    {
        this.random = random;
    }

    @Override
    public String GetName()
    {
        return "Тривиальная стратегия";
    }

    @Override
    public CommandUnit Decide(BattleUnitCommon unit, GameWorld world)
    {
        // Раненый отступает независимо от того, видит он врага или нет:
        // в правилах владельца отступление привязано только к здоровью.
        if (!IsHealthy(unit))
        {
            return new CommandMoveToOwnCore();
        }

        BattleObject enemy = FindNearestKnownEnemy(unit, world);
        if (enemy != null)
        {
            return CreateCombatCommand(unit, world, enemy);
        }

        // Здоров, но врагов не видно — идём искать.
        return new CommandWander(random);
    }

    // Жрец лечит, остальные атакуют. Ветка выбирается по типу юнита, а не по
    // выходу сети: у этого мозга сети нет.
    private CommandUnit CreateCombatCommand(BattleUnitCommon unit, GameWorld world, BattleObject enemy)
    {
        if (unit instanceof UnitPriest)
        {
            UnitPriest priest = (UnitPriest) unit;
            BattleUnitCommon wounded = world.FindMostWoundedAlly(
                    priest, priest.getSightRange(), UnitPriest.MAX_HEALERS_PER_TARGET);
            if (wounded != null)
            {
                // Цель лечения видна сенсорам и отрисовке так же, как у обученного жреца.
                priest.setTargetObject(wounded);
                return new CommandHealNearestWounded();
            }
            // Лечить некого — жрец не воюет и не стоит: идёт искать, как все
            // здоровые юниты без цели.
            return new CommandWander(random);
        }

        // Цель боя записывается в объект-цель юнита: сенсоры, отрисовка и
        // прерывания приказа должны видеть то, что юнит действительно атакует.
        unit.setTargetObject(enemy);
        return new CommandAttackTarget(enemy);
    }

    // Ближайший известный враг: юнит или здание, видимое фракции через туман
    // войны. Оба поиска в GameWorld уже учитывают туман, поэтому «известный»
    // здесь означает именно «видимый фракции», а не «запомненный когда-то».
    private BattleObject FindNearestKnownEnemy(BattleUnitCommon unit, GameWorld world)
    {
        if (world == null)
        {
            return null;
        }
        BattleObject nearestUnit = world.FindNearestEnemy(unit);
        BattleObject nearestBuilding = world.FindNearestEnemyBuilding(unit);
        if (nearestUnit == null)
        {
            return nearestBuilding;
        }
        if (nearestBuilding == null)
        {
            return nearestUnit;
        }
        return unit.GetDistance(nearestUnit) <= unit.GetDistance(nearestBuilding) ? nearestUnit : nearestBuilding;
    }

    // Ровно половина считается «ещё здоров»: «больше 50%» и «меньше 50%» не
    // покрывают границу, а выбирать на ней отступление значило бы менять
    // правило на противоположное из-за одного HP.
    public static boolean IsHealthy(BattleUnitCommon unit)
    {
        return unit.getHealth() >= unit.getMaxHealth() * HEALTHY_FRACTION;
    }
}
