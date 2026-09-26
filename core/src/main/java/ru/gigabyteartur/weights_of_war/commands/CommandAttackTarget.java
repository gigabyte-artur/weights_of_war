package ru.gigabyteartur.weights_of_war.commands;

import com.badlogic.gdx.Gdx;
import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Команда атаки заданной цели: преследование и нанесение урона.
public class CommandAttackTarget extends CommandUnit
{
    private BattleObject target;           // цель атаки.
    private float damageAccum = 0f;        // накопленный дробный урон (по здоровью цели).
    private float blockedAccum = 0f;       // накопленный дробный урон, заблокированный бронёй цели.


    public CommandAttackTarget(BattleObject target)
    {
        this.target = target;
    }

    // Задаёт цель атаки. При смене цели сбрасывает накопленный урон.
    protected void setTargetUnit(BattleObject target)
    {
        if (this.target != target)
        {
            this.target = target;
            this.damageAccum = 0f;
            this.blockedAccum = 0f;
        }
    }

    // Возвращает текущую цель атаки.
    public BattleObject getTarget()
    {
        return target;
    }

    // Проверяет, находится ли юнит в зазоре с целью.
    private boolean CheckGap(BattleUnitCommon unit)
    {
        boolean rez = false;
        // Вычисляем зазоры между ближайшими краями юнита и цели.
        int gapX = unit.GetGapX(target);
        int gapY = unit.GetGapY(target);
        rez = (gapX < unit.getAttackRange() && gapY < unit.getAttackRange());
        return rez;
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        // Если цели нет, у неё не осталось здоровья, или она скрылась за туманом войны —
        // команда прерывается.
        if (target == null || target.getHealth() <= 0 || !world.IsVisible(unit.getFraction(), target))
        {
            return true;
        }
        // 3. Если оба зазора меньше дистанции атаки — атакуем.
        if (CheckGap(unit))
        {
            // Наносим damage единиц в секунду с учётом брони цели:
            // броня вычитается из урона, а вычтенная часть считается заблокированной.
            int effectiveDamage = Math.max(0, unit.getDamage() - target.getArmor());
            int blockedDamage = unit.getDamage() - effectiveDamage;
            float delta = Gdx.graphics.getDeltaTime() * world.getGameSpeed();

            damageAccum += effectiveDamage * delta;
            blockedAccum += blockedDamage * delta;

            int damageToApply = (int) damageAccum;
            int blockedToApply = (int) blockedAccum;

            if (damageToApply > 0)
            {
                damageAccum -= damageToApply;

                // Критически раненый юнит (менее 20% здоровья) — состояние цели до нанесения урона.
                boolean criticalTarget = target instanceof BattleUnitCommon
                        && BattleUnitCommon.IsCriticallyWounded(target);

                int newHealth = Math.max(0, target.getHealth() - damageToApply);
                target.setHealth(newHealth);

                // Разовый бонус фитнесса за уничтожение вражеского юнита.
                if (newHealth == 0 && target instanceof BattleUnitCommon)
                {
                    unit.IncreaseFit(BattleUnitCommon.KILL_FIT_BONUS, BattleUnitCommon.FitType.KILLED_ENEMY_UNIT);
                }

                // Отмечаем, что цель атакована (для сенсора «Я атакован»).
                if (target instanceof BattleUnitCommon)
                {
                    ((BattleUnitCommon) target).MarkAttacked();
                }

                // Фитнесс атакующего за нанесённый урон.
                BattleUnitCommon.FitType fitType = BattleUnitCommon.FitType.DAMAGE_TO_UNITS;
                if (target instanceof BattleUnitCommon)
                    fitType = BattleUnitCommon.FitType.DAMAGE_TO_UNITS;
                else
                    fitType = BattleUnitCommon.FitType.DAMAGE_TO_BUILDINGS;

                // Двойной фитнесс за урон по критически раненому юниту (менее 20% здоровья).
                int fitValue = criticalTarget ? damageToApply * 2 : damageToApply;
                unit.IncreaseFit(fitValue, fitType);
            }

            // Фитнесс щитовика за заблокированный бронёй урон.
            if (blockedToApply > 0 && target instanceof BattleUnitCommon)
            {
                blockedAccum -= blockedToApply;
                ((BattleUnitCommon)target).IncreaseFit(blockedToApply, BattleUnitCommon.FitType.BLOCKED_ENEMY_DAMAGE);
            }

            return false;
        }

        // Цель далеко — двигаемся к точке стойки на дистанции атаки.
        SetStandoffTarget(unit);
        unit.MoveToTarget(world);
        return false;
    }

    // Вычисляет точку стойки: позицию юнита на расстоянии атаки от цели, чтобы не лезть на занятый тайл цели.
    private void SetStandoffTarget(BattleUnitCommon unit)
    {
        int attackRange = unit.getAttackRange();
        int margin = Math.max(0, attackRange - 1);

        int unitCenterX = unit.getX() + unit.getWidth() / 2;
        int unitCenterY = unit.getY() + unit.getHeight() / 2;
        int targetCenterX = target.getX() + target.getWidth() / 2;
        int targetCenterY = target.getY() + target.getHeight() / 2;

        int standoffX = unit.getX();
        int standoffY = unit.getY();

        // Смещаемся по X, только если юнит ещё вне радиуса атаки по горизонтали.
        if (unit.GetGapX(target) >= attackRange)
        {
            if (unitCenterX < targetCenterX)
            {
                standoffX = target.getX() - unit.getWidth() - margin;
            }
            else
            {
                standoffX = target.getX() + target.getWidth() + margin;
            }
        }

        // Смещаемся по Y, только если юнит ещё вне радиуса атаки по вертикали.
        if (unit.GetGapY(target) >= attackRange)
        {
            if (unitCenterY < targetCenterY)
            {
                standoffY = target.getY() - unit.getHeight() - margin;
            }
            else
            {
                standoffY = target.getY() + target.getHeight() + margin;
            }
        }

        unit.setTarget(standoffX, standoffY);
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Атакует цель";
    }
}
