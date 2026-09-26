package ru.gigabyteartur.weights_of_war.Sensors;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SensorTest extends HeadlessGdxTest
{
    @Test
    public void ownHealthReturnsPercentage()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setMaxHealth(200);
        unit.setHealth(50);
        SensorOwnHealth sensor = new SensorOwnHealth();
        assertEquals(25, sensor.CheckSensor(unit, null));
    }

    @Test
    public void ownHealthReturnsZeroWhenNoMaxHealth()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setMaxHealth(0);
        SensorOwnHealth sensor = new SensorOwnHealth();
        assertEquals(0, sensor.CheckSensor(unit, null));
    }

    @Test
    public void enemyUpReturnsZeroWhenNoEnemy()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(new Fraction("Red", null));
        unit.setSightRange(100);
        GameWorld world = new GameWorld();
        world.AddUnit(unit);

        SensorEnemyUp sensor = new SensorEnemyUp();
        assertEquals(0, sensor.CheckSensor(unit, world));
    }

    @Test
    public void enemyUpReturnsProximity()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(new Fraction("Red", null));
        unit.setSightRange(100);
        TestUnit enemy = new TestUnit(0, 50);
        enemy.setFraction(new Fraction("Blue", null));

        GameWorld world = new GameWorld();
        world.AddUnit(unit);
        world.AddUnit(enemy);

        SensorEnemyUp sensor = new SensorEnemyUp();
        assertEquals(50, sensor.CheckSensor(unit, world));
    }

    @Test
    public void countEnemyUpReturnsCountTimesTen()
    {
        TestUnit unit = new TestUnit(50, 50);
        unit.setFraction(new Fraction("Red", null));
        unit.setSightRange(1000);

        GameWorld world = new GameWorld();
        world.AddUnit(unit);
        for (int i = 0; i < 3; i++)
        {
            TestUnit enemy = new TestUnit(50, 60 + i);
            enemy.setFraction(new Fraction("Blue", null));
            world.AddUnit(enemy);
        }

        SensorCountEnemyUp sensor = new SensorCountEnemyUp();
        assertEquals(30, sensor.CheckSensor(unit, world));
    }

    @Test
    public void distanceToOwnCoreReturnsPercentage()
    {
        Fraction red = new Fraction("Red", null);
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);
        BuildingCore core = new BuildingCore(100, 0);
        core.setFraction(red);

        GameWorld world = new GameWorld();
        world.AddUnit(unit);
        world.AddUnit(core);

        SensorDistanceToOwnCore sensor = new SensorDistanceToOwnCore();
        assertEquals(10, sensor.CheckSensor(unit, world)); // 100 px → 10.
    }

    @Test
    public void distanceToOwnCoreCapsAt100()
    {
        Fraction red = new Fraction("Red", null);
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);
        BuildingCore core = new BuildingCore(2000, 0);
        core.setFraction(red);

        GameWorld world = new GameWorld();
        world.AddUnit(unit);
        world.AddUnit(core);

        SensorDistanceToOwnCore sensor = new SensorDistanceToOwnCore();
        assertEquals(100, sensor.CheckSensor(unit, world)); // 2000 px → 100.
    }

    @Test
    public void distanceToOwnCoreReturns100WhenNoCore()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(new Fraction("Red", null));

        GameWorld world = new GameWorld();
        world.AddUnit(unit); // только юнит, своего ядра нет.

        SensorDistanceToOwnCore sensor = new SensorDistanceToOwnCore();
        assertEquals(100, sensor.CheckSensor(unit, world));
    }

    @Test
    public void targetTypeSensorsReturnZeroWhenNoTarget()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(new Fraction("Red", null));

        assertEquals(0, new SensorTargetEnemyBuilding().CheckSensor(unit, null));
        assertEquals(0, new SensorTargetEnemyUnit().CheckSensor(unit, null));
        assertEquals(0, new SensorTargetAllyBuilding().CheckSensor(unit, null));
        assertEquals(0, new SensorTargetAllyUnit().CheckSensor(unit, null));
    }

    @Test
    public void targetEnemyBuildingSensorDistinguishesTypeAndFraction()
    {
        Fraction red = new Fraction("Red", null);
        Fraction blue = new Fraction("Blue", null);

        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);

        BuildingCore enemyBuilding = new BuildingCore(10, 0);
        enemyBuilding.setFraction(blue);
        unit.setTargetObject(enemyBuilding);
        assertEquals(100, new SensorTargetEnemyBuilding().CheckSensor(unit, null));

        // Вражеский юнит — не здание.
        TestUnit enemyUnit = new TestUnit(20, 0);
        enemyUnit.setFraction(blue);
        unit.setTargetObject(enemyUnit);
        assertEquals(0, new SensorTargetEnemyBuilding().CheckSensor(unit, null));

        // Союзное здание — не враг.
        BuildingCore allyBuilding = new BuildingCore(30, 0);
        allyBuilding.setFraction(red);
        unit.setTargetObject(allyBuilding);
        assertEquals(0, new SensorTargetEnemyBuilding().CheckSensor(unit, null));
    }

    @Test
    public void targetEnemyUnitSensorDistinguishesTypeAndFraction()
    {
        Fraction red = new Fraction("Red", null);
        Fraction blue = new Fraction("Blue", null);

        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);

        TestUnit enemyUnit = new TestUnit(10, 0);
        enemyUnit.setFraction(blue);
        unit.setTargetObject(enemyUnit);
        assertEquals(100, new SensorTargetEnemyUnit().CheckSensor(unit, null));

        // Вражеское здание — не юнит.
        BuildingCore enemyBuilding = new BuildingCore(20, 0);
        enemyBuilding.setFraction(blue);
        unit.setTargetObject(enemyBuilding);
        assertEquals(0, new SensorTargetEnemyUnit().CheckSensor(unit, null));

        // Союзный юнит — не враг.
        TestUnit allyUnit = new TestUnit(30, 0);
        allyUnit.setFraction(red);
        unit.setTargetObject(allyUnit);
        assertEquals(0, new SensorTargetEnemyUnit().CheckSensor(unit, null));
    }

    @Test
    public void targetAllyBuildingSensorDistinguishesTypeAndFraction()
    {
        Fraction red = new Fraction("Red", null);
        Fraction blue = new Fraction("Blue", null);

        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);

        BuildingCore allyBuilding = new BuildingCore(10, 0);
        allyBuilding.setFraction(red);
        unit.setTargetObject(allyBuilding);
        assertEquals(100, new SensorTargetAllyBuilding().CheckSensor(unit, null));

        // Союзный юнит — не здание.
        TestUnit allyUnit = new TestUnit(20, 0);
        allyUnit.setFraction(red);
        unit.setTargetObject(allyUnit);
        assertEquals(0, new SensorTargetAllyBuilding().CheckSensor(unit, null));

        // Вражеское здание — не союзник.
        BuildingCore enemyBuilding = new BuildingCore(30, 0);
        enemyBuilding.setFraction(blue);
        unit.setTargetObject(enemyBuilding);
        assertEquals(0, new SensorTargetAllyBuilding().CheckSensor(unit, null));
    }

    @Test
    public void targetAllyUnitSensorDistinguishesTypeAndFraction()
    {
        Fraction red = new Fraction("Red", null);
        Fraction blue = new Fraction("Blue", null);

        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(red);

        TestUnit allyUnit = new TestUnit(10, 0);
        allyUnit.setFraction(red);
        unit.setTargetObject(allyUnit);
        assertEquals(100, new SensorTargetAllyUnit().CheckSensor(unit, null));

        // Союзное здание — не юнит.
        BuildingCore allyBuilding = new BuildingCore(20, 0);
        allyBuilding.setFraction(red);
        unit.setTargetObject(allyBuilding);
        assertEquals(0, new SensorTargetAllyUnit().CheckSensor(unit, null));

        // Вражеский юнит — не союзник.
        TestUnit enemyUnit = new TestUnit(30, 0);
        enemyUnit.setFraction(blue);
        unit.setTargetObject(enemyUnit);
        assertEquals(0, new SensorTargetAllyUnit().CheckSensor(unit, null));
    }
}
