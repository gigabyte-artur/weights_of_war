package ru.gigabyteartur.weights_of_war;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.commands.CommandHealNearestWounded;
import ru.gigabyteartur.weights_of_war.testutil.TestBuilding;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class GameWorldTest extends HeadlessGdxTest
{
    private GameWorld world;
    private final Fraction redFraction = new Fraction("Red", null);
    private final Fraction blueFraction = new Fraction("Blue", null);

    @BeforeEach
    public void setUp()
    {
        world = new GameWorld();
    }

    @Test
    public void findNearestEnemyReturnsClosest()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(1000);
        TestUnit near = new TestUnit(10, 0);
        near.setFraction(blueFraction);
        TestUnit far = new TestUnit(100, 0);
        far.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(near);
        world.AddUnit(far);

        world.UpdateFogOfWar();
        assertSame(near, world.FindNearestEnemy(me));
    }

    @Test
    public void findNearestEnemyRespectsSightRange()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(50);

        TestUnit beyond = new TestUnit(100, 0);
        beyond.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(beyond);

        world.UpdateFogOfWar();
        // Враг за пределами радиуса обзора не виден.
        assertNull(world.FindNearestEnemy(me));

        // Расширяем радиус обзора — враг становится видимым.
        me.setSightRange(200);
        world.UpdateFogOfWar();
        assertSame(beyond, world.FindNearestEnemy(me));
    }

    @Test
    public void findNearestEnemyIgnoresAlliesAndDead()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(1000);
        TestUnit ally = new TestUnit(1, 0);
        ally.setFraction(redFraction);
        TestUnit deadEnemy = new TestUnit(2, 0);
        deadEnemy.setFraction(blueFraction);
        deadEnemy.setHealth(0);
        TestUnit enemy = new TestUnit(50, 0);
        enemy.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(ally);
        world.AddUnit(deadEnemy);
        world.AddUnit(enemy);

        world.UpdateFogOfWar();
        assertSame(enemy, world.FindNearestEnemy(me));
    }

    @Test
    public void findNearestEnemyBuildingIgnoresUnits()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(1000);
        TestUnit enemyUnit = new TestUnit(5, 0);
        enemyUnit.setFraction(blueFraction);
        TestBuilding enemyBuilding = new TestBuilding(20, 0);
        enemyBuilding.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(enemyUnit);
        world.AddUnit(enemyBuilding);

        world.UpdateFogOfWar();
        assertSame(enemyBuilding, world.FindNearestEnemyBuilding(me));
    }

    @Test
    public void findMostWoundedAllySkipsTargetsAtHealerCap()
    {
        UnitPriest searcher = new UnitPriest(0, 0);
        searcher.setFraction(redFraction);
        world.AddUnit(searcher);

        UnitPriest mostWounded = new UnitPriest(10, 0);
        mostWounded.setFraction(redFraction);
        mostWounded.setHealth(20);
        world.AddUnit(mostWounded);

        UnitPriest secondWounded = new UnitPriest(20, 0);
        secondWounded.setFraction(redFraction);
        secondWounded.setHealth(40);
        world.AddUnit(secondWounded);

        // Самого раненого уже лечат максимально допустимое число жрецов.
        for (int i = 0; i < UnitPriest.MAX_HEALERS_PER_TARGET; i++)
        {
            UnitPriest healer = new UnitPriest(5 + i, 0);
            healer.setFraction(redFraction);
            healer.setHealTarget(mostWounded);
            world.AddUnit(healer);
        }

        // Поиск должен пропустить «заполненную» цель и вернуть следующего по раненности.
        assertSame(secondWounded, world.FindMostWoundedAlly(searcher, 150, UnitPriest.MAX_HEALERS_PER_TARGET));
    }

    @Test
    public void priestHealCommandDoesNotHealWhileInterrupted()
    {
        UnitPriest priest = new UnitPriest(0, 0);
        priest.setFraction(redFraction);
        world.AddUnit(priest);

        UnitPriest ally = new UnitPriest(10, 0);
        ally.setFraction(redFraction);
        ally.setHealth(40);
        world.AddUnit(ally);

        int healthBefore = ally.getHealth();

        // Урон прерывает лечение.
        priest.MarkAttacked();
        assertTrue(priest.isHealInterrupted());

        CommandHealNearestWounded command = new CommandHealNearestWounded();
        boolean completed = command.Execute(priest, world);

        assertTrue(completed);                        // команда завершилась.
        assertEquals(healthBefore, ally.getHealth()); // здоровье цели не изменилось.
        assertNull(priest.getHealTarget());
    }

    @Test
    public void countEnemiesByDirection()
    {
        TestUnit me = new TestUnit(50, 50);
        me.setFraction(redFraction);
        me.setSightRange(1000);

        TestUnit up = new TestUnit(50, 60);
        up.setFraction(blueFraction);
        TestUnit down = new TestUnit(50, 40);
        down.setFraction(blueFraction);
        TestUnit left = new TestUnit(40, 50);
        left.setFraction(blueFraction);
        TestUnit right = new TestUnit(60, 50);
        right.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(up);
        world.AddUnit(down);
        world.AddUnit(left);
        world.AddUnit(right);

        assertEquals(1, world.CountEnemiesUp(me, 1000));
        assertEquals(1, world.CountEnemiesDown(me, 1000));
        assertEquals(1, world.CountEnemiesLeft(me, 1000));
        assertEquals(1, world.CountEnemiesRight(me, 1000));
    }

    // Подсчёт врагов в полном круге обзора (без ограничения направления).
    @Test
    public void countEnemiesInSightReturnsFullCircleCount()
    {
        TestUnit me = new TestUnit(50, 50);
        me.setFraction(redFraction);
        me.setSightRange(1000);

        TestUnit near = new TestUnit(60, 50);
        near.setFraction(blueFraction);
        TestUnit far = new TestUnit(2000, 50);
        far.setFraction(blueFraction);
        TestUnit ally = new TestUnit(70, 50);
        ally.setFraction(redFraction);

        world.AddUnit(me);
        world.AddUnit(near);
        world.AddUnit(far);
        world.AddUnit(ally);

        assertEquals(1, world.CountEnemies(me, 1000));
    }

    @Test
    public void countAlliesByDirection()
    {
        TestUnit me = new TestUnit(50, 50);
        me.setFraction(redFraction);
        me.setSightRange(1000);

        TestUnit ally = new TestUnit(50, 60);
        ally.setFraction(redFraction);
        TestUnit enemy = new TestUnit(50, 70);
        enemy.setFraction(blueFraction); // враг не считается союзником

        world.AddUnit(me);
        world.AddUnit(ally);
        world.AddUnit(enemy);

        assertEquals(1, world.CountAlliesUp(me, 1000));
    }

    @Test
    public void getTopAliveUnitsSortsByFitAndFiltersFraction()
    {
        TestUnit a = new TestUnit(0, 0);
        a.setFraction(redFraction);
        a.setFit(10);
        TestUnit b = new TestUnit(1, 0);
        b.setFraction(redFraction);
        b.setFit(30);
        TestUnit c = new TestUnit(2, 0);
        c.setFraction(redFraction);
        c.setFit(20);
        TestUnit otherFraction = new TestUnit(3, 0);
        otherFraction.setFraction(blueFraction);
        otherFraction.setFit(999);
        TestUnit dead = new TestUnit(4, 0);
        dead.setFraction(redFraction);
        dead.setFit(999);
        dead.setHealth(0);

        world.AddUnit(a);
        world.AddUnit(b);
        world.AddUnit(c);
        world.AddUnit(otherFraction);
        world.AddUnit(dead);

        ArrayList<TestUnit> top = world.GetTopAliveUnits(TestUnit.class, 2, redFraction);
        assertEquals(2, top.size());
        assertSame(b, top.get(0));
        assertSame(c, top.get(1));
    }

    @Test
    public void findDeadUnitReturnsDeadUnitOfClassOrNull()
    {
        TestUnit alive = new TestUnit(0, 0);
        alive.setFraction(redFraction);
        world.AddUnit(alive);
        assertNull(world.FindDeadUnit(TestUnit.class));

        TestUnit dead = new TestUnit(1, 0);
        dead.setHealth(0);
        world.AddUnit(dead);
        assertSame(dead, world.FindDeadUnit(TestUnit.class));
    }

    @Test
    public void checkUnitsStagnationKillsStagnantUnits()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(redFraction);
        unit.setFit(5);
        world.AddUnit(unit);

        world.CheckUnitsStagnation(1); // фиксирует lastFit = 5
        assertFalse(unit.IsDead());
        world.CheckUnitsStagnation(1); // фит не изменился → смерть
        assertTrue(unit.IsDead());
    }

    @Test
    public void isAreaOccupiedDetectsOverlap()
    {
        TestUnit unit = new TestUnit(0, 0); // 10x10 в (0,0)
        world.AddUnit(unit);

        assertTrue(world.IsAreaOccupied(5, 5, 10, 10));
        assertFalse(world.IsAreaOccupied(20, 20, 10, 10));
    }

    @Test
    public void getBestAliveUnitReturnsHighestFitAcrossFactionsAndIgnoresDead()
    {
        TestUnit a = new TestUnit(0, 0);
        a.setFraction(redFraction);
        a.setFit(10);
        TestUnit b = new TestUnit(1, 0);
        b.setFraction(blueFraction);
        b.setFit(30);
        TestUnit c = new TestUnit(2, 0);
        c.setFraction(redFraction);
        c.setFit(20);
        TestUnit dead = new TestUnit(3, 0);
        dead.setFraction(redFraction);
        dead.setFit(999);
        dead.setHealth(0);

        world.AddUnit(a);
        world.AddUnit(b);
        world.AddUnit(c);
        world.AddUnit(dead);

        assertSame(b, world.GetBestAliveUnit(TestUnit.class));
        assertNull(world.GetBestAliveUnit(UnitSwordman.class));
    }

    @Test
    public void findOwnCoreReturnsOwnFactionCore()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(redFraction);

        BuildingCore ownCore = new BuildingCore(100, 100);
        ownCore.setFraction(redFraction);
        TestBuilding otherBuilding = new TestBuilding(150, 150);
        otherBuilding.setFraction(redFraction);
        TestBuilding enemyCore = new TestBuilding(200, 200);
        enemyCore.setFraction(blueFraction);
        TestBuilding deadCore = new TestBuilding(300, 300);
        deadCore.setFraction(redFraction);
        deadCore.setHealth(0);

        world.AddUnit(unit);
        world.AddUnit(ownCore);
        world.AddUnit(otherBuilding);
        world.AddUnit(enemyCore);
        world.AddUnit(deadCore);

        assertSame(ownCore, world.FindOwnCore(unit));
    }

    @Test
    public void calculateSeparationPushesAwayFromNearbyAllies()
    {
        TestUnit self = new TestUnit(0, 0);
        self.setFraction(redFraction);

        TestUnit ally = new TestUnit(10, 0);
        ally.setFraction(redFraction);
        TestUnit enemy = new TestUnit(5, 0); // враг — игнорируется.
        enemy.setFraction(blueFraction);
        TestUnit farAlly = new TestUnit(100, 0); // за пределами радиуса — игнорируется.
        farAlly.setFraction(redFraction);
        TestUnit deadAlly = new TestUnit(0, 10); // мёртвый — игнорируется.
        deadAlly.setFraction(redFraction);
        deadAlly.setHealth(0);

        world.AddUnit(self);
        world.AddUnit(ally);
        world.AddUnit(enemy);
        world.AddUnit(farAlly);
        world.AddUnit(deadAlly);

        float[] sep = self.CalculateSeparation(world);

        // Отталкивание только от ally (расстояние 10, радиус 30).
        assertEquals(-(30f - 10f) / 30f, sep[0], 0.001f);
        assertEquals(0f, sep[1], 0.001f);
    }

    @Test
    public void getDistanceSquaredReturnsSquaredDistance()
    {
        TestUnit a = new TestUnit(0, 0);
        TestUnit b = new TestUnit(3, 4);
        assertEquals(25f, a.GetDistanceSquared(b), 0.001f);
    }
}
