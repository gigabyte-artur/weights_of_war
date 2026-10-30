package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.commands.CommandMove;
import ru.gigabyteartur.weights_of_war.commands.CommandMoveUp;
import ru.gigabyteartur.weights_of_war.commands.CommandTargetNearestEnemy;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.commands.OrderTracker;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Решение юнита — тратящийся ресурс, а выбранное действие — приказ со сроком жизни.
 * <p>
 * Раньше пустая очередь команд сама была приглашением думать: юнит мог сменить
 * намерение каждый кадр, поэтому «выбрать цель» (один кадр) и «ударить» (много
 * кадров) стоили одинаково, и обязательство никогда не окупалось. Теперь новое
 * решение берётся не чаще {@link BattleUnitCommon#DECISION_INTERVAL_SEC}, а само
 * действие живёт {@link BattleUnitCommon#ORDER_MIN_SEC}..{@link BattleUnitCommon#ORDER_MAX_SEC}
 * игровых секунд и меняется только по событию (бьют, цель пропала, враг в радиусе удара).
 */
public class DecisionCooldownTest
{
    // Шаг кадра в тестах: 1/60 при gameSpeed 1.
    private static final float DT = 1f / 60f;

    @BeforeEach
    public void stubGraphics()
    {
        Gdx.graphics = (com.badlogic.gdx.Graphics) Proxy.newProxyInstance(
                com.badlogic.gdx.Graphics.class.getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.Graphics.class},
                (proxy, method, args) ->
                {
                    if (method.getName().equals("getDeltaTime"))
                    {
                        return DT;
                    }
                    Class<?> t = method.getReturnType();
                    if (t == int.class) return 0;
                    if (t == float.class) return 0f;
                    if (t == boolean.class) return false;
                    if (t == long.class) return 0L;
                    return null;
                });
    }

    // Приказ, который ничего не делает и никогда не завершается: проверяем сам срок,
    // не завися от геометрии мира и от того, куда пойдёт конкретная команда.
    private static class CommandHoldOrder extends CommandUnit
    {
        @Override
        public boolean Execute(BattleUnitCommon unit, GameWorld world)
        {
            return false;
        }

        @Override
        public boolean IsPersistentOrder()
        {
            return true;
        }
    }

    @Test
    public void freshUnitCannotDecideTwiceInOneTick()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        unit.Update(world);
        // Первый тик: решение взято — кулдаун взведён на полный интервал.
        float afterFirst = unit.getDecisionCooldown();
        assertTrue(afterFirst > 0f, "первое решение взводит кулдаун, было " + afterFirst);

        unit.Update(world);
        // Второй тик: кулдаун только убывает, но нового решения не даёт.
        assertTrue(unit.getDecisionCooldown() < afterFirst, "кулдаун обязан тикать");
        assertTrue(unit.getDecisionCooldown() > 0f, "за один кадр интервал не истекает");
    }

    @Test
    public void decisionsResumeAfterIntervalElapses()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        // Кулдаун, взведённый решением, наблюдается как скачок вверх: сама величина
        // до нуля не доходит — как только она истекает, следующее решение взводит её заново.
        float previous = unit.getDecisionCooldown();
        int decisions = 0;
        int framesPerSecond = Math.round(1f / DT);
        int frames = (int) Math.ceil(BattleUnitCommon.DECISION_INTERVAL_SEC * framesPerSecond) + 4;
        for (int i = 0; i < frames; i++)
        {
            unit.Update(world);
            if (unit.getDecisionCooldown() > previous)
            {
                decisions++;
            }
            previous = unit.getDecisionCooldown();
        }
        assertTrue(decisions >= 2,
                "через " + frames + " кадров юнит обязан решить повторно, решений было " + decisions);
    }

    @Test
    public void cooldownScalesWithGameSpeed()
    {
        // При gameSpeed 10 игровое время идёт в десять раз быстрее,
        // поэтому интервал истекает в десять раз меньшим числом кадров.
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(10f);
        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());

        unit.Update(world);
        int framesAtSpeedOne = (int) Math.ceil(BattleUnitCommon.DECISION_INTERVAL_SEC / DT);
        int framesNeeded = 0;
        int limit = Math.round(1f / DT) * 2;
        while (framesNeeded < limit && unit.getDecisionCooldown() > 0f)
        {
            unit.Update(world);
            framesNeeded++;
        }
        assertTrue(framesNeeded < framesAtSpeedOne,
                "при gameSpeed 10 кулдаун истекает быстрее по кадрам: было " + framesNeeded
                        + ", при gameSpeed 1 нужно " + framesAtSpeedOne);
    }

    @Test
    public void persistentCommandBecomesOrderWithLifetime()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        unit.BeginOrder(3, new CommandMoveUp());
        assertTrue(unit.hasActiveOrder(), "движение — приказ, а не разовое действие");
        assertEquals(3, unit.getOrderIndex());
        assertTrue(unit.getOrderRemaining() >= OrderTracker.ORDER_MIN_SEC - 1e-4f);
        assertTrue(unit.getOrderRemaining() <= OrderTracker.ORDER_MAX_SEC + 1e-4f);
    }

    @Test
    public void oneShotCommandIsNotAnOrder()
    {
        TestUnit unit = new TestUnit(0, 0);

        // Выбор цели — разовая команда: «обязаться» выбирать цель на десять секунд нельзя,
        // иначе юнит простоит весь срок.
        unit.BeginOrder(6, new CommandTargetNearestEnemy());
        assertFalse(unit.hasActiveOrder(), "выбор цели не должен становиться приказом");
        assertEquals(-1, unit.getOrderIndex());
    }

    @Test
    public void orderSurvivesManyTicksAndReissuesItsCommand()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        CommandHoldOrder command = new CommandHoldOrder();
        unit.BeginOrder(5, command);
        unit.AddCommand(command);

        for (int i = 0; i < 60; i++)
        {
            unit.Update(world);
        }
        assertTrue(unit.hasActiveOrder(), "приказ живёт секунды, а не один тик");
        assertEquals(5, unit.getOrderIndex(), "сеть не спрашивают заново, пока приказ жив");
        assertSame(command, unit.GetCurrentCommand(),
                "исполняющаяся команда не пересоздаётся, пока не завершится");
    }

    @Test
    public void orderExpiresAndUnitDecidesAgain()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        // 1/6 игровой секунды за кадр: десять игровых секунд укладываются в 60 кадров.
        world.setGameSpeed(10f);

        CommandHoldOrder command = new CommandHoldOrder();
        unit.BeginOrder(5, command);
        unit.AddCommand(command);

        int ticks = (int) Math.ceil(OrderTracker.ORDER_MAX_SEC * 6f) + 2;
        for (int i = 0; i < ticks; i++)
        {
            unit.Update(world);
        }
        assertFalse(unit.hasActiveOrder(), "срок приказа истекает сам");
    }

    @Test
    public void beingAttackedInterruptsOrder()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        CommandHoldOrder command = new CommandHoldOrder();
        unit.BeginOrder(5, command);
        unit.AddCommand(command);
        unit.Update(world);
        assertTrue(unit.hasActiveOrder());

        // Юнита ударили — держать прежний курс больше нельзя.
        unit.MarkAttacked();
        unit.Update(world);
        assertFalse(unit.hasActiveOrder(), "под атакой приказ прерывается");
    }

    @Test
    public void beingAttackedInterruptsOnlyOnceWhileSignalDecays()
    {
        // Сигнал «под атакой» затухает несколько секунд; если прерывать на каждом тике,
        // новый приказ живёт ровно один кадр и приказ как механизм исчезает.
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        CommandHoldOrder command = new CommandHoldOrder();
        unit.BeginOrder(5, command);
        unit.AddCommand(command);
        unit.MarkAttacked();
        unit.Update(world);
        assertFalse(unit.hasActiveOrder(), "первый удар прерывает приказ");

        // Новый приказ под тем же затухающим сигналом обязан выжить.
        CommandHoldOrder second = new CommandHoldOrder();
        unit.BeginOrder(5, second);
        unit.AddCommand(second);
        assertTrue(unit.getAttacked() > OrderTracker.ORDER_INTERRUPT_ATTACKED,
                "сигнал всё ещё выше порога, тест проверяет именно это состояние");
        unit.Update(world);
        assertTrue(unit.hasActiveOrder(), "повторный тик под огнём не сжигает новый приказ");
    }

    @Test
    public void enemyInAttackRangeInterruptsOnlyOnce()
    {
        // Иначе юнит, которому сеть велела идти, стоял бы рядом с врагом и «решал»
        // каждые 0.25 с: приказ сгорал бы быстрее, чем успевал что-то сделать.
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(new ru.gigabyteartur.weights_of_war.Fraction("Red", null));
        unit.setSightRange(1000);
        unit.setAttackRange(200);

        TestUnit enemy = new TestUnit(30, 0);
        enemy.setFraction(new ru.gigabyteartur.weights_of_war.Fraction("Blue", null));

        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);
        world.AddUnit(unit);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandHoldOrder command = new CommandHoldOrder();
        unit.BeginOrder(5, command);
        unit.AddCommand(command);
        unit.Update(world);
        assertFalse(unit.hasActiveOrder(), "враг в радиусе удара прерывает движение");

        // Тот же враг не прерывает следующий приказ снова: на него уже отреагировали.
        CommandHoldOrder second = new CommandHoldOrder();
        unit.BeginOrder(5, second);
        unit.AddCommand(second);
        for (int i = 0; i < 60; i++)
        {
            unit.Update(world);
        }
        assertTrue(unit.hasActiveOrder(), "одна и та же угроза не сжигает приказ повторно");
    }

    @Test
    public void attackOrderEndsWhenItsTargetIsGone()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        // Приказ атаки без цели: цели нет — и приказ не должен изображать погоню.
        unit.BeginOrder(0, new CommandAttackTarget(null));
        unit.AddCommand(new CommandAttackTarget(null));
        unit.Update(world);
        assertFalse(unit.hasActiveOrder(), "без цели атака не держится");
    }

    @Test
    public void escapeMoveIsNotAnOrder()
    {
        // Отлипание от затора — служебная команда: приказом не становится,
        // иначе юнит «обязался» бы отходить десять секунд.
        TestUnit unit = new TestUnit(0, 0);
        assertFalse(new CommandMove(10, 10).IsPersistentOrder());
    }

    @Test
    public void movingOrdersArePersistent()
    {
        assertTrue(new CommandMoveUp().IsPersistentOrder());
        assertTrue(new ru.gigabyteartur.weights_of_war.commands.CommandMoveToTarget().IsPersistentOrder());
    }
}
