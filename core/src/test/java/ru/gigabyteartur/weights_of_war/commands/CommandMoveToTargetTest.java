package ru.gigabyteartur.weights_of_war.commands;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Геометрия команды «К цели»: шаг должен иметь реальную длину.
// Класс наследует HeadlessGdxTest, потому что MoveToTarget читает Gdx.graphics.getDeltaTime().
public class CommandMoveToTargetTest extends HeadlessGdxTest
{
    @Test
    public void stepsTowardsFarTargetWithUsableLength()
    {
        // Регрессия: в distance лежал КВАДРАТ расстояния, поэтому шаг получался
        // dx / (dx^2 + dy^2) * 100 — доли пикселя при цели в сотнях пикселей.
        // Юнит практически не приближался к цели.
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        unit.setTarget(300, 0);

        CommandMoveToTarget command = new CommandMoveToTarget();
        command.Execute(unit, world);

        int step = unit.getTargetX() - unit.getX();
        assertTrue(step > 0, "юнит должен сдвинуться к цели, шаг был " + step);
        assertEquals(CommandMoveToTarget.STEP_PX, step,
                "шаг к цели должен быть равен STEP_PX, а не долям пикселя");
    }

    @Test
    public void goesStraightToNearbyTarget()
    {
        // Цель ближе шага — идём прямо в неё.
        TestUnit unit = new TestUnit(0, 0);
        GameWorld world = new GameWorld();
        unit.setTarget(10, 0);

        CommandMoveToTarget command = new CommandMoveToTarget();
        command.Execute(unit, world);

        assertEquals(10, unit.getTargetX());
        assertEquals(0, unit.getTargetY());
    }
}
