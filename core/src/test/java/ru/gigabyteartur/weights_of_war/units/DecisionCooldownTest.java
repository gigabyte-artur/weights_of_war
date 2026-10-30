package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Решение юнита — тратящийся ресурс, а не бесплатное действие каждый кадр.
 * <p>
 * Раньше пустая очередь команд сама была приглашением думать: юнит мог сменить
 * намерение каждый кадр, поэтому «выбрать цель» (один кадр) и «ударить» (много
 * кадров) стоили одинаково, и обязательство никогда не окупалось. Теперь новое
 * решение берётся не чаще, чем раз в {@link BattleUnitCommon#DECISION_INTERVAL_SEC}
 * игровых секунд.
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

    @Test
    public void freshUnitCannotDecideTwiceInOneTick()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        unit.Update(world);
        // Первый тик: решение взято, очередь непуста.
        assertTrue(unit.getCommandsQueueSize() > 0, "первое решение должно появиться сразу");

        unit.ClearCommands();
        unit.Update(world);
        // Второй тик: кулдаун ещё не истёк, нового решения быть не должно.
        assertEquals(0, unit.getCommandsQueueSize(),
                "в пределах кулдауна юнит не берёт новое решение");
    }

    @Test
    public void decisionsResumeAfterIntervalElapses()
    {
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(1f);

        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        unit.Update(world);
        unit.ClearCommands();

        // Ждём чуть больше интервала: кадров нужно больше, чем тиков в интервале.
        int framesPerSecond = Math.round(1f / DT);
        int frames = (int) Math.ceil(BattleUnitCommon.DECISION_INTERVAL_SEC * framesPerSecond) + 2;
        for (int i = 0; i < frames; i++)
        {
            unit.Update(world);
            if (unit.getCommandsQueueSize() > 0)
            {
                return;
            }
        }
        assertTrue(false, "через " + frames + " кадров кулдаун обязан истечь и решение — появиться");
    }

    @Test
    public void cooldownScalesWithGameSpeed()
    {
        // При gameSpeed 10 игровое время идёт в десять раз быстрее,
        // поэтому решение восстанавливается в десять раз меньшим числом кадров.
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        world.setGameSpeed(10f);

        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        unit.Update(world);
        unit.ClearCommands();

        int framesNeeded = 0;
        int limit = Math.round(1f / DT) * 2;
        while (framesNeeded < limit)
        {
            unit.Update(world);
            framesNeeded++;
            if (unit.getCommandsQueueSize() > 0)
            {
                break;
            }
        }
        int framesAtSpeedOne = (int) Math.ceil(BattleUnitCommon.DECISION_INTERVAL_SEC / DT);
        assertTrue(framesNeeded < framesAtSpeedOne,
                "при gameSpeed 10 кулдаун истекает быстрее по кадрам: было " + framesNeeded
                        + ", при gameSpeed 1 нужно " + framesAtSpeedOne);
    }
}
