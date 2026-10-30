package ru.gigabyteartur.weights_of_war.brains;

import com.badlogic.gdx.Gdx;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

import java.util.Random;

// Шаг в случайном направлении со сроком жизни.
//
// Срок считается здесь, а не в ScriptedBrain: Execute вызывается каждый кадр,
// пока команда в очереди, поэтому время копится честно в игровых секундах.
// Если бы срок считал сам мозг, он бы тикал только в момент решения (не чаще
// DECISION_INTERVAL_SEC) и «пять секунд» превратились бы в десятки реальных.
//
// Направление выбирается один раз на всю жизнь команды: так юнит идёт, а не
// дёргается из стороны в сторону каждый кадр.
public class CommandWander extends CommandUnit
{
    // Длина шага вперёд, px. Держится маленькой: юнит просто переставляет точку
    // притяжения на каждом кадре и идёт в выбранную сторону.
    public final static int STEP_PX = 30;
    // Срок жизни одного направления, игровых секунд.
    public final static float WANDER_SECONDS = 5f;

    private final Random random;
    private boolean initialized = false;
    private int dx = 0;
    private int dy = 0;
    private float elapsed = 0f;

    public CommandWander()
    {
        this(new Random());
    }

    // Источник случайности передаётся снаружи, чтобы тесты могли его зафиксировать.
    public CommandWander(Random random)
    {
        this.random = random;
    }

    // Направление выбранного шага (0, пока шаг не начался).
    public int getDx()
    {
        return dx;
    }

    public int getDy()
    {
        return dy;
    }

    // Сколько игровых секунд шаг уже идёт.
    public float getElapsed()
    {
        return elapsed;
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        float delta = Gdx.graphics.getDeltaTime();
        if (world != null)
        {
            delta *= world.getGameSpeed();
        }
        elapsed += delta;

        if (!initialized)
        {
            PickDirection(unit, world);
            initialized = true;
        }

        // Раненый больше не бродит: правило «меньше половины здоровья — к ядру»
        // важнее срока шага. Без этой проверки юнит, потерявший здоровье на
        // четвёртой секунде шага, отступал бы только на девятой — то есть
        // отступление запаздывало бы ровно на остаток случайного шага.
        if (!ScriptedBrain.IsHealthy(unit))
        {
            return true;
        }

        // Срок направления истёк — юнит выбирает заново (или идёт в бой).
        if (elapsed >= WANDER_SECONDS)
        {
            return true;
        }

        // Точка притяжения — на шаг вперёд по выбранной оси. Юнит идёт к ней
        // штатным MoveToTarget: тот сам обходит препятствия скольжением по осям.
        int targetX = dx != 0 ? unit.getX() + dx : unit.getX();
        int targetY = dy != 0 ? unit.getY() + dy : unit.getY();
        unit.setTarget(targetX, targetY);
        unit.MoveToTarget(world);
        return false;
    }

    // Одно из четырёх направлений, не выводящее юнита за край карты.
    // Если все упираются в границу (юнит в углу), берётся первое из выбранных.
    private void PickDirection(BattleUnitCommon unit, GameWorld world)
    {
        int[] chosen = PickDirection(unit, world, random);
        dx = chosen[0];
        dy = chosen[1];
    }

    // Отдельной статической функцией: то же правило проверяется тестом без
    // создания команды и без подмены Gdx.
    public static int[] PickDirection(BattleUnitCommon unit, GameWorld world, Random random)
    {
        int[][] directions = {{0, STEP_PX}, {0, -STEP_PX}, {-STEP_PX, 0}, {STEP_PX, 0}};
        int first = random.nextInt(directions.length);
        for (int attempt = 0; attempt < directions.length; attempt++)
        {
            int[] candidate = directions[(first + attempt) % directions.length];
            if (IsInsideWorld(world, unit, candidate[0], candidate[1]))
            {
                return candidate;
            }
        }
        return directions[first];
    }

    // Проверка, что шаг оставляет юнита внутри мира. Мир без границ
    // (world == null) не ограничивает направление.
    public static boolean IsInsideWorld(GameWorld world, BattleUnitCommon unit, int dx, int dy)
    {
        if (world == null)
        {
            return true;
        }
        int x = unit.getX() + dx;
        int y = unit.getY() + dy;
        return x >= world.getWorldLeft() && x + unit.getWidth() <= world.getWorldRight()
                && y >= world.getWorldBottom() && y + unit.getHeight() <= world.getWorldTop();
    }

    @Override
    public boolean IsPersistentOrder()
    {
        // Шаг сам знает свой срок; приказ ему не нужен, иначе срок считался бы
        // дважды и в разных единицах.
        return false;
    }

    @Override
    public String GetName()
    {
        return "Случайный шаг";
    }
}
