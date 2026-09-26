package ru.gigabyteartur.weights_of_war.units;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.commands.CommandThink;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class BattleUnitCommonTest
{
    @Test
    public void createNeuroNetUnitHasExpectedStructure()
    {
        NeuroNet net = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<Layer> layers = net.GetLayers();

        assertEquals(4, layers.size());
        assertEquals(26, layers.get(0).GetSize());
        assertTrue(layers.get(0).GetIsInput());
        assertFalse(layers.get(0).GetIsOutput());

        for (int i = 1; i <= 2; i++)
        {
            assertEquals(20, layers.get(i).GetSize());
            assertFalse(layers.get(i).GetIsInput());
            assertFalse(layers.get(i).GetIsOutput());
        }

        assertEquals(10, layers.get(3).GetSize());
        assertFalse(layers.get(3).GetIsInput());
        assertTrue(layers.get(3).GetIsOutput());
    }

    @Test
    public void checkStagnationKillsAfterMaxUnchangedWaves()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFit(10);

        // Первый вызов фиксирует lastFit = 10 (фит изменился), счётчик не растёт.
        unit.CheckStagnation(3);
        assertFalse(unit.IsDead());

        unit.CheckStagnation(3); // staleWaves = 1
        unit.CheckStagnation(3); // staleWaves = 2
        assertFalse(unit.IsDead());
        unit.CheckStagnation(3); // staleWaves = 3 → смерть
        assertTrue(unit.IsDead());
    }

    @Test
    public void checkStagnationResetsWhenFitChanges()
    {
        TestUnit unit = new TestUnit(0, 0);
        unit.setFit(10);

        unit.CheckStagnation(2); // фиксирует lastFit = 10
        unit.CheckStagnation(2); // staleWaves = 1
        unit.setFit(20);
        unit.CheckStagnation(2); // фит изменился → staleWaves = 0
        assertFalse(unit.IsDead());
    }

    @Test
    public void commandsQueueSizeReflectsAddedCommands()
    {
        TestUnit unit = new TestUnit(0, 0);
        assertEquals(0, unit.getCommandsQueueSize());

        unit.AddCommand(new CommandThink());
        assertEquals(1, unit.getCommandsQueueSize());

        unit.ClearCommands();
        assertEquals(0, unit.getCommandsQueueSize());
    }

    @Test
    public void boidsDefaultsMatchSpecification()
    {
        assertEquals(0.6f, BattleUnitCommon.MAX_BOIDS, 1e-9);

        assertEquals(50f, UnitSwordman.DEFAULT_SEPARATION_RADIUS, 1e-9);
        assertEquals(1.2f, UnitSwordman.DEFAULT_SEPARATION_WEIGHT, 1e-9);
        assertEquals(0.15f, UnitSwordman.DEFAULT_COHESION_WEIGHT, 1e-9);
        assertEquals(0.10f, UnitSwordman.DEFAULT_ALIGNMENT_WEIGHT, 1e-9);

        assertEquals(70f, UnitArcher.DEFAULT_SEPARATION_RADIUS, 1e-9);
        assertEquals(1.5f, UnitArcher.DEFAULT_SEPARATION_WEIGHT, 1e-9);
        assertEquals(0.0f, UnitArcher.DEFAULT_COHESION_WEIGHT, 1e-9);
        assertEquals(0.0f, UnitArcher.DEFAULT_ALIGNMENT_WEIGHT, 1e-9);

        assertEquals(50f, UnitShieldman.DEFAULT_SEPARATION_RADIUS, 1e-9);
        assertEquals(1.0f, UnitShieldman.DEFAULT_SEPARATION_WEIGHT, 1e-9);
        assertEquals(0.30f, UnitShieldman.DEFAULT_COHESION_WEIGHT, 1e-9);
        assertEquals(0.20f, UnitShieldman.DEFAULT_ALIGNMENT_WEIGHT, 1e-9);
    }

    @Test
    public void priestNeuroNetHasExpectedStructure()
    {
        NeuroNet net = UnitPriest.CreateNeuroNetPriest();
        ArrayList<Layer> layers = net.GetLayers();

        assertEquals(4, layers.size());
        assertEquals(31, layers.get(0).GetSize());
        assertTrue(layers.get(0).GetIsInput());
        assertFalse(layers.get(0).GetIsOutput());

        for (int i = 1; i <= 2; i++)
        {
            assertEquals(20, layers.get(i).GetSize());
        }

        assertEquals(11, layers.get(3).GetSize());
        assertFalse(layers.get(3).GetIsInput());
        assertTrue(layers.get(3).GetIsOutput());
    }

    @Test
    public void priestManaConstantsMatchSpecification()
    {
        assertEquals(200f, UnitPriest.MANA_MAX, 1e-6);
        assertEquals(2f, UnitPriest.HEAL_PER_SECOND, 1e-6);
        assertEquals(1f, UnitPriest.MANA_PER_HEAL, 1e-6);
        // MANA_MAX / (2 * PRODUCTION_INTERVAL) = 200 / 120 ≈ 1.6667 (PRODUCTION_INTERVAL = 60).
        assertEquals(200f / 120f, UnitPriest.MANA_REGEN_PER_SECOND, 1e-4);
    }
}
