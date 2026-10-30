package ru.gigabyteartur.weights_of_war.brains;

import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.commands.CommandHealNearestWounded;
import ru.gigabyteartur.weights_of_war.commands.CommandMoveToOwnCore;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;

import java.lang.reflect.Proxy;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

// Вписанная стратегия: правила, которые можно проверить без сети.
//
// Проверяется именно то, что просил владелец: атака известного врага при
// здоровье выше половины, отход к ядру и случайный шаг, когда врагов нет,
// лечение вместо атаки у жреца, и роль пяти секунд в случайном шаге.
public class ScriptedBrainTest extends HeadlessGdxTest
{
    private static final float DT = 1f / 60f;

    private GameWorld world;
    private final Fraction red = new Fraction("Red", null);
    private final Fraction blue = new Fraction("Blue", null);

    @BeforeEach
    public void setUp()
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
        world = new GameWorld();
    }

    // Юнит с заданной долей здоровья.
    private TestUnit newUnit(int health, int maxHealth)
    {
        TestUnit unit = new TestUnit(1000, 1000);
        unit.setFraction(red);
        unit.setSightRange(1000);
        unit.setMaxHealth(maxHealth);
        unit.setHealth(health);
        return unit;
    }

    // 1. Виден враг, здоровья больше половины — атака именно этой цели.
    @Test
    public void healthyUnitAttacksKnownEnemy()
    {
        TestUnit me = newUnit(80, 100);
        TestUnit enemy = new TestUnit(1100, 1000);
        enemy.setFraction(blue);
        enemy.setMaxHealth(100);
        enemy.setHealth(100);
        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(me, world);

        assertTrue(command instanceof CommandAttackTarget, "здоровый юнит при виде врага должен атаковать");
        assertSame(enemy, ((CommandAttackTarget) command).getTarget());
        assertSame(enemy, me.getTargetObject(), "цель боя должна попасть в объект-цель юнита");
    }

    // 2. Здоров, но врагов не видно — идёт в случайном направлении искать.
    @Test
    public void healthyUnitWithoutEnemiesWanders()
    {
        TestUnit me = newUnit(80, 100);
        world.AddUnit(me);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(me, world);

        assertTrue(command instanceof CommandWander, "здоровый юнит без врагов должен идти искать");
    }

    // 2б. Раненый отступает к ядру, даже когда враг виден: раненый не воюет.
    @Test
    public void woundedUnitRetreatsEvenWithVisibleEnemy()
    {
        TestUnit me = newUnit(40, 100);
        TestUnit enemy = new TestUnit(1100, 1000);
        enemy.setFraction(blue);
        enemy.setMaxHealth(100);
        enemy.setHealth(100);
        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(me, world);

        assertTrue(command instanceof CommandMoveToOwnCore, "раненый должен отходить к ядру, а не драться");
    }

    // 3. Врагов не видно и здоровья меньше половины — отход к своему ядру.
    @Test
    public void woundedUnitWithoutEnemiesGoesToOwnCore()
    {
        TestUnit me = newUnit(40, 100);
        world.AddUnit(me);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(me, world);

        assertTrue(command instanceof CommandMoveToOwnCore, "раненый без врагов должен идти к ядру");
    }

    // 4. Ровно половина здоровья считается «ещё здоров».
    @Test
    public void exactlyHalfHealthCountsAsHealthy()
    {
        TestUnit me = newUnit(50, 100);
        world.AddUnit(me);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(me, world);

        assertTrue(command instanceof CommandWander, "на границе 50% юнит ещё здоров и идёт искать, а не отступает");
        assertTrue(ScriptedBrain.IsHealthy(me));
    }

    // 5. Здоровье 49% — уже ранен, поведение меняется.
    @Test
    public void justBelowHalfHealthIsWounded()
    {
        TestUnit me = newUnit(49, 100);
        world.AddUnit(me);
        world.UpdateFogOfWar();

        assertFalse(ScriptedBrain.IsHealthy(me));
        assertTrue(new ScriptedBrain(new Random(1)).Decide(me, world) instanceof CommandMoveToOwnCore);
    }

    // 6. Жрец при виде врага лечит раненого союзника, а не атакует.
    @Test
    public void priestHealsWoundedAllyInsteadOfAttacking()
    {
        UnitPriest priest = new UnitPriest(1000, 1000);
        priest.setFraction(red);
        priest.setSightRange(1000);
        priest.setHealth(priest.getMaxHealth());

        TestUnit wounded = new TestUnit(1030, 1000);
        wounded.setFraction(red);
        wounded.setMaxHealth(100);
        wounded.setHealth(30);

        TestUnit enemy = new TestUnit(1100, 1000);
        enemy.setFraction(blue);
        enemy.setMaxHealth(100);
        enemy.setHealth(100);

        world.AddUnit(priest);
        world.AddUnit(wounded);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(priest, world);

        assertTrue(command instanceof CommandHealNearestWounded, "жрец лечит, а не атакует");
        assertSame(wounded, priest.getTargetObject(), "цель лечения должна попасть в объект-цель жреца");
    }

    // 7. Жрец без раненых не атакует: он отходит к ядру.
    @Test
    public void priestWithoutWoundedDoesNotAttack()
    {
        UnitPriest priest = new UnitPriest(1000, 1000);
        priest.setFraction(red);
        priest.setSightRange(1000);
        priest.setHealth(priest.getMaxHealth());

        TestUnit enemy = new TestUnit(1100, 1000);
        enemy.setFraction(blue);
        enemy.setMaxHealth(100);
        enemy.setHealth(100);

        world.AddUnit(priest);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandUnit command = new ScriptedBrain(new Random(1)).Decide(priest, world);

        assertFalse(command instanceof CommandAttackTarget, "жрец не должен атаковать");
        assertFalse(command instanceof CommandHealNearestWounded, "лечить некого");
        assertTrue(command instanceof CommandWander, "здоровый жрец без раненых идёт искать");
    }

    // 7б. Раненый бросает случайный шаг, не дожидаясь конца пяти секунд:
    //     иначе отступление запаздывало бы ровно на остаток шага.
    @Test
    public void woundedUnitAbandonsWanderImmediately()
    {
        TestUnit me = newUnit(80, 100);
        me.setSpeed(0);
        CommandWander wander = new CommandWander(new Random(11));

        // Здоровый юнит начинает шаг и не завершает его на первом кадре.
        assertFalse(wander.Execute(me, world));

        // Юнит получил урон — шаг должен кончиться на следующем кадре.
        me.setHealth(30);
        assertTrue(wander.Execute(me, world),
                "раненый юнит должен бросить случайный шаг сразу, а не дожидаться пяти секунд");
    }

    // 8. Случайный шаг держится пять игровых секунд и только потом завершается.
    @Test
    public void wanderLastsFiveGameSeconds()
    {
        // Здоровый юнит: раненый бросает шаг сразу (см. woundedUnitAbandonsWanderImmediately).
        TestUnit me = newUnit(80, 100);
        me.setSpeed(0); // стоит на месте, чтобы шаг не упирался в другие объекты
        CommandWander wander = new CommandWander(new Random(7));

        // Первый вызов выбирает направление и сразу тикает один кадр.
        assertFalse(wander.Execute(me, world));
        assertTrue(wander.getDx() != 0 || wander.getDy() != 0, "направление должно быть выбрано");

        int frames = 1;
        int limit = (int) (CommandWander.WANDER_SECONDS / DT) + 30;
        while (frames < limit && !wander.Execute(me, world))
        {
            frames++;
        }

        // Срок измеряется накоплением float по кадрам, поэтому сравниваем
        // длительность, а не номер кадра: (5/DT = 300) даёт 301-302 вызова.
        float livedSeconds = frames * DT;
        assertTrue(livedSeconds >= CommandWander.WANDER_SECONDS - DT,
                "шаг не должен заканчиваться раньше пяти игровых секунд, прожил " + livedSeconds);
        assertTrue(livedSeconds <= CommandWander.WANDER_SECONDS + DT,
                "шаг не должен жить дольше пяти игровых секунд, прожил " + livedSeconds);
    }

    // 9. Направление упирается в край карты — выбирается другое.
    @Test
    public void wanderAvoidsMapEdge()
    {
        // Юнит у нижней границы мира: шаг вниз вывел бы его за карту.
        TestUnit corner = new TestUnit(0, 0);
        corner.setFraction(red);

        int[] direction = CommandWander.PickDirection(corner, world, new Random(3));

        assertFalse(direction[1] < 0, "у нижней границы шаг вниз недопустим");
    }

    // 10. Мозг возвращает команду, которую можно выполнить: она не null и у неё есть имя.
    @Test
    public void decideAlwaysReturnsUsableCommand()
    {
        TestUnit me = newUnit(10, 100);
        world.AddUnit(me);
        world.UpdateFogOfWar();

        ScriptedBrain brain = new ScriptedBrain(new Random(1));
        CommandUnit command = brain.Decide(me, world);

        assertNotNull(command);
        assertNotNull(command.GetName());
        assertEquals("Тривиальная стратегия", brain.GetName());
    }
}
