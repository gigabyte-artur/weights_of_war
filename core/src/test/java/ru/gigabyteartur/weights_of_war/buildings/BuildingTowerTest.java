package ru.gigabyteartur.weights_of_war.buildings;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingTowerTest extends HeadlessGdxTest
{
    private final Fraction fraction = new Fraction("Red", null);

    @Test
    public void canRespawnWhenOwnCoreAlive()
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        BuildingCore core = new BuildingCore(100, 100);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        assertTrue(tower.CanRespawn(world));
    }

    @Test
    public void cannotRespawnWhenOwnCoreDestroyed()
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        BuildingCore core = new BuildingCore(100, 100);
        core.setFraction(fraction);
        core.setHealth(0);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        assertFalse(tower.CanRespawn(world));
    }

    @Test
    public void cannotRespawnWhenNoOwnCore()
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        GameWorld world = new GameWorld();

        assertFalse(tower.CanRespawn(world));
    }

    @Test
    public void deadTowerRespawnsWhenCoreAlive() throws Exception
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        BuildingCore core = new BuildingCore(100, 100);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        tower.setHealth(0);
        SetRespawnTimer(tower, BuildingTower.RESPAWN_TIME);

        tower.Update(world);

        assertFalse(tower.IsDead());
        assertEquals(tower.getMaxHealth(), tower.getHealth());
    }

    @Test
    public void deadTowerDoesNotRespawnWhenCoreDestroyed() throws Exception
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        BuildingCore core = new BuildingCore(100, 100);
        core.setFraction(fraction);
        core.setHealth(0);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        tower.setHealth(0);
        SetRespawnTimer(tower, BuildingTower.RESPAWN_TIME);

        tower.Update(world);

        assertTrue(tower.IsDead());
        assertEquals(0, tower.getHealth());
    }

    @Test
    public void isTowerDestroyedReturnsTrueWhenTowerDead()
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        tower.setHealth(0);
        GameWorld world = new GameWorld();
        world.AddUnit(tower);

        assertTrue(world.IsTowerDestroyed(fraction));
    }

    @Test
    public void isTowerDestroyedReturnsFalseWhenTowerAlive()
    {
        BuildingTower tower = new BuildingTower(0, 0);
        tower.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(tower);

        assertFalse(world.IsTowerDestroyed(fraction));
    }

    @Test
    public void isTowerDestroyedReturnsFalseWhenNoTower()
    {
        GameWorld world = new GameWorld();

        assertFalse(world.IsTowerDestroyed(fraction));
    }

    // Принудительно выставляет таймер возрождения, чтобы не зависеть от реального времени в тесте.
    private static void SetRespawnTimer(BuildingTower tower, float value) throws Exception
    {
        Field field = BuildingTower.class.getDeclaredField("respawnTimer");
        field.setAccessible(true);
        field.setFloat(tower, value);
    }
}
