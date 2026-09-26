package ru.gigabyteartur.weights_of_war.buildings;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.evo.Evolution;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingCoreTest extends HeadlessGdxTest
{
    private final Fraction fraction = new Fraction("Red", null);

    @Test
    public void firstGenerationSpawnsAllUnitTypesWithNetAndThinkCommand()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        core.ProduceUnits(world);

        assertEquals(1, core.getGenerationNumber());

        ArrayList<UnitSwordman> swordmen = world.GetTopAliveUnits(UnitSwordman.class, 1000, fraction);
        ArrayList<UnitArcher> archers = world.GetTopAliveUnits(UnitArcher.class, 1000, fraction);
        ArrayList<UnitShieldman> shieldmen = world.GetTopAliveUnits(UnitShieldman.class, 1000, fraction);
        ArrayList<UnitScout> scouts = world.GetTopAliveUnits(UnitScout.class, 1000, fraction);

        assertEquals(BuildingCore.SWORDMEN_COUNT, swordmen.size());
        assertEquals(BuildingCore.ARCHERS_COUNT, archers.size());
        assertEquals(BuildingCore.SHIELDMEN_COUNT, shieldmen.size());
        assertEquals(BuildingCore.SCOUTS_COUNT, scouts.size());

        for (UnitSwordman unit : swordmen)
        {
            assertNotNull(unit.getNeuroNet());
            assertEquals(1, unit.getCommandsQueueSize());
            assertEquals(1, unit.getGenerationNumber());
            assertSame(fraction, unit.getFraction());
        }
    }

    @Test
    public void secondGenerationDoublesUnitCountAndKeepsUnitsAlive()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        core.ProduceUnits(world);

        // Задаём фит, чтобы отбор по топ-3 работал детерминированно.
        ArrayList<UnitSwordman> gen1 = world.GetTopAliveUnits(UnitSwordman.class, 1000, fraction);
        for (int i = 0; i < gen1.size(); i++)
        {
            gen1.get(i).setFit(i);
        }

        core.ProduceUnits(world);

        assertEquals(2, core.getGenerationNumber());
        assertEquals(20, world.GetTopAliveUnits(UnitSwordman.class, 1000, fraction).size());
        assertEquals(20, world.GetTopAliveUnits(UnitArcher.class, 1000, fraction).size());
        assertEquals(20, world.GetTopAliveUnits(UnitShieldman.class, 1000, fraction).size());
        assertEquals(20, world.GetTopAliveUnits(UnitScout.class, 1000, fraction).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void coreKeepsThreeBestNetworksPerType() throws Exception
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        core.ProduceUnits(world); // поколение 1.

        // Задаём фит первому поколению, чтобы отбор по топ-3 был детерминированным.
        ArrayList<UnitSwordman> gen1 = world.GetTopAliveUnits(UnitSwordman.class, 1000, fraction);
        for (int i = 0; i < gen1.size(); i++)
        {
            gen1.get(i).setFit(i);
        }

        core.ProduceUnits(world); // поколение 2: в ядре остаются три лучших мечника.

        Field netsField = BuildingCore.class.getDeclaredField("NetSwordmen");
        netsField.setAccessible(true);
        Field fitsField = BuildingCore.class.getDeclaredField("NetSwordmenFits");
        fitsField.setAccessible(true);

        ArrayList<NeuroNet> nets = (ArrayList<NeuroNet>) netsField.get(core);
        ArrayList<Integer> fits = (ArrayList<Integer>) fitsField.get(core);

        assertEquals(3, nets.size());
        assertEquals(3, fits.size());
        assertEquals(9, fits.get(0).intValue()); // топ-1 первого поколения.
        assertEquals(8, fits.get(1).intValue());
        assertEquals(7, fits.get(2).intValue());
        for (NeuroNet net : nets)
        {
            assertNotNull(net);
        }
    }

    @Test
    public void generateOffspringProducesRequestedCountAsDeepCopies()
    {
        Evolution evolutionAlgo = new Evolution();
        NeuroNet coreNet = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<NeuroNet> coreNets = new ArrayList<>();
        coreNets.add(coreNet);

        TestUnit topUnit = new TestUnit(0, 0);
        topUnit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        ArrayList<TestUnit> top = new ArrayList<>();
        top.add(topUnit);

        ArrayList<NeuroNet> offspring = evolutionAlgo.GenerateOffspring(top, coreNets, 5, null);

        assertEquals(5, offspring.size());
        for (NeuroNet child : offspring)
        {
            assertNotNull(child);
            assertEquals(4, child.GetLayers().size()); // 26 → 2×20 → 10
            assertNotSame(coreNet, child);
            assertNotSame(topUnit.getNeuroNet(), child);
        }
    }

    @Test
    public void produceUnitsUsesLoadedNetworksInsteadOfCreatingRandom()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        // Кастомная сеть с нестандартной структурой (2 слоя вместо 26→2×20→10).
        NeuroNet customNet = new NeuroNet();
        customNet.GenerateAddLayer(3, true, false);
        customNet.GenerateAddLayer(4, false, true);
        customNet.Compile();

        core.SetNeuroNets(customNet, customNet, customNet, customNet, customNet);
        core.ProduceUnits(world);

        UnitSwordman unit = world.GetTopAliveUnits(UnitSwordman.class, 1, fraction).get(0);
        assertEquals(2, unit.getNeuroNet().GetLayers().size());
    }

    @Test
    public void gameOverWhenGenerationLimitReached()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);
        world.setMaxGenerations(1);

        assertFalse(world.IsGameOver());

        core.ProduceUnits(world); // ядро достигает 1-го поколения.
        world.Update();           // лимит (1) достигнут → игра завершается.

        assertTrue(world.IsGameOver());
        assertSame(fraction, world.GetWinner());
        // Лучшие нейросети доступны для сохранения — SaveBestNetworks() не «потеряет» победителей.
        assertNotNull(world.GetBestAliveUnit(UnitSwordman.class));
    }

    @Test
    public void effectiveFitAppliesGenerationSurvivalBonus()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);

        core.ProduceUnits(world); // ядро достигает 1-го поколения, юниты — поколения 1.
        core.ProduceUnits(world); // ядро достигает 2-го поколения, новые юниты — поколения 2.

        UnitSwordman gen1 = null;
        UnitSwordman gen2 = null;
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof UnitSwordman && !object.IsDead())
            {
                if (((UnitSwordman) object).getGenerationNumber() == 1 && gen1 == null)
                {
                    gen1 = (UnitSwordman) object;
                }
                if (((UnitSwordman) object).getGenerationNumber() == 2 && gen2 == null)
                {
                    gen2 = (UnitSwordman) object;
                }
            }
        }

        assertNotNull(gen1);
        assertNotNull(gen2);

        gen1.setFit(100);
        gen2.setFit(100);

        // Поколение 1 при текущем поколении 2: бонус (2 - 1) / 10 = 0.1 → 100 * 1.1 = 110.
        assertEquals(110, world.GetEffectiveFit(gen1));
        // Поколение 2 (текущее): бонус 0 → 100.
        assertEquals(100, world.GetEffectiveFit(gen2));
    }

    @Test
    public void skipsProductionWhenTowerDestroyedAndRollSucceeds() throws Exception
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        BuildingTower tower = new BuildingTower(100, 100);
        tower.setFraction(fraction);
        tower.setHealth(0); // башня разрушена.
        GameWorld world = new GameWorld();
        world.AddUnit(core);
        world.AddUnit(tower);

        SetRandom(core, 0.1); // бросок < 0.3 → пропуск.

        core.ProduceUnits(world);

        assertEquals(0, core.getGenerationNumber());
        assertEquals(0, world.GetTopAliveUnits(UnitSwordman.class, 1000, fraction).size());
    }

    @Test
    public void doesNotSkipProductionWhenTowerAlive() throws Exception
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        BuildingTower tower = new BuildingTower(100, 100);
        tower.setFraction(fraction);
        GameWorld world = new GameWorld();
        world.AddUnit(core);
        world.AddUnit(tower);

        SetRandom(core, 0.1); // даже малый бросок не приводит к пропуску при живой башне.

        core.ProduceUnits(world);

        assertEquals(1, core.getGenerationNumber());
    }

    @Test
    public void doesNotSkipProductionWhenRollFailsEvenWithTowerDestroyed() throws Exception
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(fraction);
        BuildingTower tower = new BuildingTower(100, 100);
        tower.setFraction(fraction);
        tower.setHealth(0); // башня разрушена.
        GameWorld world = new GameWorld();
        world.AddUnit(core);
        world.AddUnit(tower);

        SetRandom(core, 0.9); // бросок >= 0.3 → пропуска нет.

        core.ProduceUnits(world);

        assertEquals(1, core.getGenerationNumber());
    }

    // Подменяет генератор случайных чисел ядра на детерминированный (для тестирования вероятности).
    private static void SetRandom(BuildingCore core, double value) throws Exception
    {
        Field field = BuildingCore.class.getDeclaredField("random");
        field.setAccessible(true);
        field.set(core, new Random()
        {
            @Override
            public double nextDouble()
            {
                return value;
            }
        });
    }
}
