package ru.gigabyteartur.weights_of_war.units;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import static org.junit.jupiter.api.Assertions.*;

// Тесты юнита-разведчика.
public class UnitScoutTest extends HeadlessGdxTest
{
    private final Fraction redFraction = new Fraction("Red", null);

    // Разведчик получает фитнесс за одиночную разведку (без союзников в поле зрения).
    @Test
    public void scoutGainsFitnessWhenAlone()
    {
        UnitScout scout = new UnitScout(0, 0);
        scout.setFraction(redFraction);

        GameWorld world = new GameWorld();
        world.AddUnit(scout);

        scout.ApplyFitnessGain(1.0f, world);

        assertEquals(1, scout.getFit());
    }

    // Фитнесс уменьшается с ростом числа союзников в поле зрения (делитель 1.5 на союзника).
    @Test
    public void fitnessDecreasesPerAllyInSight()
    {
        assertEquals(8, gainOverSeconds(0)); // базовая ставка.
        assertEquals(5, gainOverSeconds(1));
        assertEquals(3, gainOverSeconds(2));
        assertEquals(2, gainOverSeconds(3));
    }

    // Фитнесс не начисляется, если в поле зрения больше 10 союзников.
    @Test
    public void noFitnessWhenTooManyAllies()
    {
        UnitScout scout = new UnitScout(0, 0);
        scout.setFraction(redFraction);

        GameWorld world = new GameWorld();
        world.AddUnit(scout);
        for (int i = 0; i < 11; i++)
        {
            TestUnit ally = new TestUnit(10 + i * 10, 0);
            ally.setFraction(redFraction);
            world.AddUnit(ally);
        }

        scout.ApplyFitnessGain(8.0f, world);

        assertEquals(0, scout.getFit());
    }

    // Разведчик получает фитнесс только за разведку и урон по зданиям.
    @Test
    public void increaseFitOnlyCountsScoutingAndBuildingDamage()
    {
        UnitScout scout = new UnitScout(0, 0);

        scout.IncreaseFit(10, BattleUnitCommon.FitType.SCOUTING);
        assertEquals(10, scout.getFit());

        scout.IncreaseFit(3, BattleUnitCommon.FitType.DAMAGE_TO_BUILDINGS);
        assertEquals(25, scout.getFit()); // 10 + 3 * 5.

        // Остальные виды приспособленности фитнесса не приносят.
        scout.IncreaseFit(7, BattleUnitCommon.FitType.DAMAGE_TO_UNITS);
        scout.IncreaseFit(7, BattleUnitCommon.FitType.KILLED_ENEMY_UNIT);
        scout.IncreaseFit(7, BattleUnitCommon.FitType.BLOCKED_ENEMY_DAMAGE);
        scout.IncreaseFit(7, BattleUnitCommon.FitType.HEALED_ALLIES);
        assertEquals(25, scout.getFit());
    }

    // Создаёт разведчика с заданным числом союзников и возвращает фитнесс после 8 секунд накопления.
    private int gainOverSeconds(int allyCount)
    {
        UnitScout scout = new UnitScout(0, 0);
        scout.setFraction(redFraction);

        GameWorld world = new GameWorld();
        world.AddUnit(scout);
        for (int i = 0; i < allyCount; i++)
        {
            TestUnit ally = new TestUnit(10 + i * 10, 0);
            ally.setFraction(redFraction);
            world.AddUnit(ally);
        }

        scout.ApplyFitnessGain(8.0f, world);
        return scout.getFit();
    }
}
