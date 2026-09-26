package ru.gigabyteartur.weights_of_war.commands;

import com.badlogic.gdx.Gdx;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;

// Команда лечения самого раненого союзника в радиусе видимости.
// Канальная: каждый Execute находит цель заново и накапливает лечение.
public class CommandHealNearestWounded extends CommandUnit
{
    private float healAccum = 0f;   // накопленный дробный объём лечения.

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Команда применима только к жрецу.
        if (!(unit instanceof UnitPriest))
        {
            return true;
        }
        UnitPriest priest = (UnitPriest) unit;

        // Лечение прервано недавним уроном — команда завершается, жрец простаивает.
        if (priest.isHealInterrupted())
        {
            priest.setHealTarget(null);
            return true;
        }

        // Нет маны — команда завершается, жрец простаивает и копит ману.
        if (priest.getMana() <= 10)
        {
            priest.setHealTarget(null);
            return true;
        }

        // Нет раненого союзника в радиусе видимости (или его уже лечат достаточно жрецов) — команда завершается.
        BattleUnitCommon target = world.FindMostWoundedAlly(priest, priest.getSightRange(), UnitPriest.MAX_HEALERS_PER_TARGET);
        if (target == null)
        {
            priest.setHealTarget(null);
            return true;
        }

        // Запоминаем цель лечения для отрисовки пунктирной линии и объект-цель для сенсоров.
        priest.setHealTarget(target);
        priest.setTargetObject(target);

        // Накапливаем лечение в игровых секундах.
        float delta = Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        healAccum += UnitPriest.HEAL_PER_SECOND * delta;
        int healToApply = (int) healAccum;
        if (healToApply > 0)
        {
            healAccum -= healToApply;

            // Оверхила нет: лечим не больше недостающего здоровья и не больше маны.
            int missingHp = target.getMaxHealth() - target.getHealth();
            int actual = Math.min(healToApply, Math.min(missingHp, (int) priest.getMana()));

            if (actual > 0)
            {
                // Критически раненый союзник (менее 20% здоровья) — состояние цели до лечения.
                boolean criticalTarget = BattleUnitCommon.IsCriticallyWounded(target);

                target.setHealth(target.getHealth() + actual);
                priest.setMana(priest.getMana() - actual * UnitPriest.MANA_PER_HEAL);

                // Двойной фитнесс за лечение критически раненого юнита (менее 20% здоровья).
                int fitValue = criticalTarget ? actual * 2 : actual;
                priest.IncreaseFit(fitValue, BattleUnitCommon.FitType.HEALED_ALLIES);
            }
        }

        // Канал активен — блокируем реген маны в этом тике.
        priest.setHealingThisTick(true);
        return false;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Лечит союзника";
    }
}
