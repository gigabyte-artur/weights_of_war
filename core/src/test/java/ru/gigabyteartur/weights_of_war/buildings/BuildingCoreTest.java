package ru.gigabyteartur.weights_of_war.buildings;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.evo.Evolution;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Axon;
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
            assertEquals(5, child.GetLayers().size()); // 26 → 2×20 → 10
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
    public void effectiveFitIgnoresGenerationSoNewerNetworksCanWin()
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

        // Равный фит — равный приоритет независимо от номера поколения.
        // Раньше старое поколение получало множитель (1 + (текущее - своё) / 10),
        // то есть преимущество за то, что юнит произведён РАНЬШЕ. Это вытесняло
        // новые сети из отбора и блокировало обучение (измерено на стенде:
        // средний фит популяции падал 70 → 53 при росте фита чемпионов).
        assertEquals(100, world.GetEffectiveFit(gen1));
        assertEquals(100, world.GetEffectiveFit(gen2));

        // Больший собственный фит всегда выигрывает у меньшего, кто бы его ни набрал.
        gen1.setFit(150);
        assertTrue(world.GetEffectiveFit(gen1) > world.GetEffectiveFit(gen2));
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

    // --- Адаптивный шаг мутации ---

    @Test
    public void evolutionShrinksMutationStepWhileProgressContinues()
    {
        Evolution evo = new Evolution();
        double start = evo.GetSigma();
        evo.AdaptSigma(10);
        evo.AdaptSigma(20);
        evo.AdaptSigma(30);
        assertTrue(evo.GetSigma() < start,
                "при прогрессе шаг мутации должен уменьшаться (шлифовка)");
    }

    @Test
    public void evolutionGrowsMutationStepDuringStagnation()
    {
        Evolution evo = new Evolution();
        // Сначала немного прогресса, чтобы уйти от стартового значения вниз.
        evo.AdaptSigma(1000);
        double afterProgress = evo.GetSigma();

        // Застой: лучший фит не растёт.
        for (int i = 0; i < Evolution.STAGNATION_LIMIT * 3; i++)
        {
            evo.AdaptSigma(1000);
        }
        assertTrue(evo.GetSigma() > afterProgress,
                "при застое шаг мутации должен расти (разведка)");
    }

    @Test
    public void evolutionSigmaStaysWithinBounds()
    {
        Evolution evo = new Evolution();
        for (int i = 0; i < 200; i++)
        {
            evo.AdaptSigma(9999);   // непрерывный прогресс
        }
        assertTrue(evo.GetSigma() >= Evolution.SIGMA_MIN);
        assertTrue(evo.GetSigma() <= Evolution.SIGMA_MAX);

        for (int i = 0; i < 200; i++)
        {
            evo.AdaptSigma(9999);   // непрерывный застой
        }
        assertTrue(evo.GetSigma() >= Evolution.SIGMA_MIN);
        assertTrue(evo.GetSigma() <= Evolution.SIGMA_MAX);
    }

    @Test
    public void offspringStillContainsExactEliteCopy()
    {
        // Первый потомок — точная копия чемпиона, чтобы найденное не терялось.
        Evolution evo = new Evolution();
        NeuroNet champion = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<NeuroNet> coreNets = new ArrayList<>();
        coreNets.add(champion);

        ArrayList<NeuroNet> offspring = evo.GenerateOffspring(new ArrayList<TestUnit>(), coreNets, 5, null);
        assertEquals(5, offspring.size());

        NeuroNet elite = offspring.get(0);
        ArrayList<Axon> a = champion.GetLayers().get(1).GetNeurons().get(0).GetAxons();
        ArrayList<Axon> b = elite.GetLayers().get(1).GetNeurons().get(0).GetAxons();
        for (int i = 0; i < a.size(); i++)
        {
            assertEquals(a.get(i).GetWeight(), b.get(i).GetWeight(), 1e-12);
        }
        assertNotSame(champion, elite);
    }

    // --- Турнирный отбор и иммигранты ---

    @Test
    public void tournamentSelectionFavoursFitterParents()
    {
        // Турнир должен давать давление отбора: при резко различающихся фитах
        // большинство потомков несёт гены сильного родителя.
        Evolution evo = new Evolution();

        NeuroNet strong = BattleUnitCommon.CreateNeuroNetUnit();
        for (Layer l : strong.GetLayers())
            for (Neuron n : l.GetNeurons())
                for (Axon a : n.GetAxons())
                    a.SetWeight(1.0);

        NeuroNet weak = BattleUnitCommon.CreateNeuroNetUnit();
        for (Layer l : weak.GetLayers())
            for (Neuron n : l.GetNeurons())
                for (Axon a : n.GetAxons())
                    a.SetWeight(-1.0);

        ArrayList<NeuroNet> coreNets = new ArrayList<>();
        coreNets.add(strong);
        coreNets.add(weak);
        ArrayList<Integer> coreFits = new ArrayList<>();
        coreFits.add(1000);
        coreFits.add(0);

        int strongish = 0;
        int generations = 40;
        for (int g = 0; g < generations; g++)
        {
            ArrayList<NeuroNet> kids = evo.GenerateOffspring(new ArrayList<TestUnit>(), coreNets, coreFits, 5, null);
            // Первый потомок — копия чемпиона (strong). Считаем остальных.
            for (int i = 1; i < kids.size(); i++)
            {
                double w = kids.get(i).GetLayers().get(1).GetNeurons().get(0).GetAxons().get(0).GetWeight();
                if (w > 0.5) strongish++;
            }
        }
        // При равномерном случайном выборе доля была бы ~50%. Ждём заметно больше.
        int total = generations * 4;
        assertTrue(strongish > total / 2,
                "турнирный отбор не даёт давления: " + strongish + " из " + total);
    }

    @Test
    public void offspringContainsRandomImmigrantForDiversity()
    {
        Evolution evo = new Evolution();
        NeuroNet champion = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<NeuroNet> coreNets = new ArrayList<>();
        coreNets.add(champion);

        ArrayList<NeuroNet> kids = evo.GenerateOffspring(new ArrayList<TestUnit>(), coreNets, null, 6, null);
        assertEquals(6, kids.size());

        // Последний потомок — иммигрант с полностью случайными весами,
        // поэтому он не обязан совпадать с чемпионом.
        NeuroNet immigrant = kids.get(kids.size() - 1);
        assertNotNull(immigrant);
        assertEquals(5, immigrant.GetLayers().size());
    }

    @Test
    public void offspringWithFitsStillProducesRequestedCount()
    {
        Evolution evo = new Evolution();
        NeuroNet a = BattleUnitCommon.CreateNeuroNetUnit();
        NeuroNet b = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<NeuroNet> coreNets = new ArrayList<>();
        coreNets.add(a);
        coreNets.add(b);
        ArrayList<Integer> fits = new ArrayList<>();
        fits.add(10);
        fits.add(5);

        ArrayList<NeuroNet> kids = evo.GenerateOffspring(new ArrayList<TestUnit>(), coreNets, fits, 7, null);
        assertEquals(7, kids.size());
    }
}
