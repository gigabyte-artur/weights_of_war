package ru.gigabyteartur.weights_of_war;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import static org.junit.jupiter.api.Assertions.*;

// Тесты тумана войны (видимость фракции и прерывание команд).
public class FogOfWarTest extends HeadlessGdxTest
{
    private GameWorld world;
    private final Fraction redFraction = new Fraction("Red", null);
    private final Fraction blueFraction = new Fraction("Blue", null);

    @BeforeEach
    public void setUp()
    {
        world = new GameWorld();
    }

    // Юнит видит врага в радиусе своей видимости.
    @Test
    public void unitSeesEnemyWithinOwnSightRange()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit enemy = new TestUnit(100, 0);
        enemy.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        assertSame(enemy, world.FindNearestEnemy(me));
    }

    // Юнит видит врага в радиусе видимости союзника (совместное зрение фракции).
    @Test
    public void unitSeesEnemyWithinAllySightRange()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit ally = new TestUnit(250, 0);
        ally.setFraction(redFraction);
        ally.setSightRange(150);
        TestUnit enemy = new TestUnit(300, 0);
        enemy.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(ally);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        // Враг за пределами собственного обзора, но виден союзнику.
        assertSame(enemy, world.FindNearestEnemy(me));
    }

    // Юнит не видит врага за пределами тумана войны.
    @Test
    public void unitDoesNotSeeEnemyBeyondFog()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit enemy = new TestUnit(300, 0);
        enemy.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        assertNull(world.FindNearestEnemy(me));
    }

    // Команда атаки отменяется, когда цель скрылась за туманом войны.
    @Test
    public void attackCommandInterruptedWhenTargetHidden()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit enemy = new TestUnit(100, 0);
        enemy.setFraction(blueFraction);

        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        CommandAttackTarget command = new CommandAttackTarget(enemy);

        // Цель уходит за пределы видимости фракции.
        enemy.setXY(1000, 0);
        world.UpdateFogOfWar();

        assertTrue(command.Execute(me, world));
    }

    // Союзный юнит видим даже за пределами собственного радиуса обзора.
    @Test
    public void allyVisibleBeyondOwnSightRange()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit ally = new TestUnit(300, 0);
        ally.setFraction(redFraction);

        world.AddUnit(me);
        world.AddUnit(ally);
        world.UpdateFogOfWar();

        assertTrue(world.IsVisible(redFraction, ally));
    }

    // Юнит не видит цель с 0 здоровья.
    @Test
    public void unitDoesNotSeeDeadTarget()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(redFraction);
        me.setSightRange(150);
        TestUnit enemy = new TestUnit(100, 0);
        enemy.setFraction(blueFraction);
        enemy.setHealth(0);

        world.AddUnit(me);
        world.AddUnit(enemy);
        world.UpdateFogOfWar();

        assertNull(world.FindNearestEnemy(me));
        assertFalse(world.IsVisible(redFraction, enemy));
    }
}
