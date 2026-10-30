package ru.gigabyteartur.weights_of_war.units;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.commands.CommandThink;
import ru.gigabyteartur.weights_of_war.neuro_net.Axon;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class BattleUnitCommonTest
{
    @Test
    public void createNeuroNetUnitHasExpectedStructure()
    {
        NeuroNet net = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<Layer> layers = net.GetLayers();

        assertEquals(5, layers.size());
        // Вход сети обязан совпадать с числом базовых сенсоров: иначе часть сенсоров
        // молча не доходит до сети (CommandThink читает min(sensors, входной слой)).
        assertEquals(BattleUnitCommon.SENSOR_COUNT, layers.get(0).GetSize());
        assertEquals(CommandThink.CreateBaseSensors().size(), layers.get(0).GetSize());
        assertTrue(layers.get(0).GetIsInput());
        assertFalse(layers.get(0).GetIsOutput());

        for (int i = 1; i <= 2; i++)
        {
            assertEquals(20, layers.get(i).GetSize());
            assertFalse(layers.get(i).GetIsInput());
            assertFalse(layers.get(i).GetIsOutput());
        }

        assertEquals(BattleUnitCommon.MEMORY_COUNT, layers.get(3).GetSize());
        assertFalse(layers.get(3).GetIsInput());
        assertFalse(layers.get(3).GetIsOutput());

        assertEquals(10, layers.get(4).GetSize());
        assertFalse(layers.get(4).GetIsInput());
        assertTrue(layers.get(4).GetIsOutput());
    }

    // Общая настройка для тестов застоя: 50 фита оплачивают волну, база 4, потолок +8.
    private static void advance(TestUnit unit)
    {
        unit.AdvanceStagnation(BattleUnitCommon.STAGNATION_FIT_PER_WAVE,
                BattleUnitCommon.STAGNATION_BASE_WAVES,
                BattleUnitCommon.STAGNATION_MAX_EXTRA_WAVES);
    }

    @Test
    public void stagnationKillsUnitThatNeverEarns()
    {
        TestUnit unit = new TestUnit(0, 0);
        int base = BattleUnitCommon.STAGNATION_BASE_WAVES;

        // База терпения: столько волн без единого фита юнит ещё жив.
        for (int i = 0; i < base; i++)
        {
            advance(unit);
        }
        assertFalse(unit.IsDead(), "базовое терпение должно пережить " + base + " волн");

        advance(unit);
        assertTrue(unit.IsDead(), "волна сверх базы без прогресса убивает");
    }

    @Test
    public void earnedFitBuysExtraPatience()
    {
        TestUnit idle = new TestUnit(0, 0);
        TestUnit worker = new TestUnit(0, 0);
        // 200 фита = 4 оплаченные волны банка.
        worker.setFit(200);

        int base = BattleUnitCommon.STAGNATION_BASE_WAVES;
        // Первая волна фиксирует lastFit и начисляет банк; далее банк покрывает волны.
        advance(idle);
        advance(worker);
        for (int i = 0; i < base + 2; i++)
        {
            advance(idle);
            advance(worker);
        }
        assertTrue(idle.IsDead(), "без прогресса юнит умирает сразу после базового терпения");
        assertFalse(worker.IsDead(), "заработанный фит оплачивает волны сверх базы");
    }

    @Test
    public void creditBankIsCapped()
    {
        TestUnit rich = new TestUnit(0, 0);
        rich.setFit(1_000_000);

        // Первая волна начисляет банк и сама его тратит.
        advance(rich);
        // Банк ограничен: даже огромный запас не даёт больше базы плюс потолка.
        int limit = BattleUnitCommon.STAGNATION_BASE_WAVES + BattleUnitCommon.STAGNATION_MAX_EXTRA_WAVES;
        for (int i = 1; i < limit; i++)
        {
            advance(rich);
        }
        assertFalse(rich.IsDead(), "оплаченное потолком терпение должно быть доступно");
        advance(rich);
        assertTrue(rich.IsDead(), "за потолком банк не спасает");
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

        assertEquals(5, layers.size());
        // Вход жреца обязан покрывать его набор сенсоров: базовые плюс свои (5).
        assertEquals(BattleUnitCommon.SENSOR_COUNT + UnitPriest.OWN_SENSOR_COUNT, layers.get(0).GetSize());
        assertTrue(layers.get(0).GetIsInput());
        assertFalse(layers.get(0).GetIsOutput());

        for (int i = 1; i <= 2; i++)
        {
            assertEquals(20, layers.get(i).GetSize());
        }

        assertEquals(BattleUnitCommon.MEMORY_COUNT, layers.get(3).GetSize());
        assertFalse(layers.get(3).GetIsOutput());

        assertEquals(11, layers.get(4).GetSize());
        assertFalse(layers.get(4).GetIsInput());
        assertTrue(layers.get(4).GetIsOutput());
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

    // ===== Память сети (рекуррентный слой) =====

    // Слой памяти стоит перед выходным и получает связь ОТ выходного слоя:
    // только так он видит решение, принятое на прошлом вызове.
    @Test
    public void memoryLayerReceivesRecurrentConnectionFromOutput()
    {
        NeuroNet net = BattleUnitCommon.CreateNeuroNetUnit();
        ArrayList<Layer> layers = net.GetLayers();

        Layer memoryLayer = layers.get(3);
        Layer outputLayer = layers.get(4);
        assertEquals(BattleUnitCommon.MEMORY_COUNT, memoryLayer.GetSize());

        // Каждый нейрон памяти имеет по аксону на каждый выходной нейрон.
        for (Neuron memoryNeuron : memoryLayer.GetNeurons())
        {
            int recurrentFromOutput = 0;
            for (Axon axon : memoryNeuron.GetAxons())
            {
                if (outputLayer.GetNeurons().contains(axon.GetSource()))
                {
                    recurrentFromOutput++;
                }
            }
            assertEquals(outputLayer.GetSize(), recurrentFromOutput);
        }
    }

    // Сеть с памятью меняет своё состояние между вызовами: при одинаковых
    // сенсорах второй результат отличается от первого. Это и есть память —
    // сеть без рекуррентных связей при тех же входах дала бы то же самое.
    @Test
    public void memoryMakesNetworkStateDependentOnPreviousCall()
    {
        NeuroNet net = BattleUnitCommon.CreateNeuroNetUnit();

        double[] first = new double[10];
        double[] second = new double[10];

        setSameSensors(net);
        net.CalcSignals();
        for (int i = 0; i < 10; i++)
        {
            first[i] = net.GetLayers().get(4).GetNeurons().get(i).GetSignal();
        }

        setSameSensors(net);
        net.CalcSignals();
        for (int i = 0; i < 10; i++)
        {
            second[i] = net.GetLayers().get(4).GetNeurons().get(i).GetSignal();
        }

        boolean differs = false;
        for (int i = 0; i < 10; i++)
        {
            if (Math.abs(first[i] - second[i]) > 1e-9)
            {
                differs = true;
                break;
            }
        }
        assertTrue(differs, "сеть с памятью при одинаковых входах должна зависеть от прошлого вызова");
    }

    // Обратная сторона того же контракта: обычная сеть без памяти при
    // одинаковых входах обязана давать одинаковый результат. Если бы это было
    // не так, тест выше ничего не доказывал бы.
    @Test
    public void networkWithoutMemoryIsStateless()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.GenerateAddLayer(2, false, true);
        net.Compile();
        net.RandomWeights();

        net.SetInputSignal(0, 0.5);
        net.SetInputSignal(1, -0.25);
        net.CalcSignals();
        double first = net.GetLayers().get(1).GetNeurons().get(0).GetSignal();

        net.SetInputSignal(0, 0.5);
        net.SetInputSignal(1, -0.25);
        net.CalcSignals();
        double second = net.GetLayers().get(1).GetNeurons().get(0).GetSignal();

        assertEquals(first, second, 1e-12);
    }

    private static void setSameSensors(NeuroNet net)
    {
        for (int i = 0; i < BattleUnitCommon.SENSOR_COUNT; i++)
        {
            net.SetInputSignal(i, 0.3);
        }
    }

    // Копирование переносит и рекуррентную связь, иначе размножение юнита
    // молча лишало бы потомка памяти.
    @Test
    public void copyPreservesRecurrentConnection()
    {
        NeuroNet parent = BattleUnitCommon.CreateNeuroNetUnit();
        NeuroNet child = parent.Copy();

        assertEquals(parent.GetLayers().size(), child.GetLayers().size());

        Layer parentMemory = parent.GetLayers().get(3);
        Layer childMemory = child.GetLayers().get(3);
        Layer childOutput = child.GetLayers().get(4);

        int recurrent = 0;
        for (Axon axon : childMemory.GetNeurons().get(0).GetAxons())
        {
            if (childOutput.GetNeurons().contains(axon.GetSource()))
            {
                recurrent++;
            }
        }
        assertEquals(childOutput.GetSize(), recurrent);
        assertEquals(parentMemory.GetNeurons().get(0).GetAxons().size(),
                childMemory.GetNeurons().get(0).GetAxons().size());
    }

    // Сохранение и загрузка обязаны сохранять память: раньше аксоны
    // разрешались только в предыдущем слое, поэтому рекуррентная связь
    // терялась бы при загрузке.
    @Test
    public void saveAndLoadPreservesRecurrentConnection() throws Exception
    {
        NeuroNet net = BattleUnitCommon.CreateNeuroNetUnit();
        File tempFile = File.createTempFile("memory_net", ".xml");
        tempFile.deleteOnExit();
        net.SaveToFile(tempFile.getAbsolutePath());

        NeuroNet loaded = new NeuroNet();
        assertTrue(loaded.LoadFromFile(tempFile.getAbsolutePath()));

        assertEquals(5, loaded.GetLayers().size());
        Layer loadedMemory = loaded.GetLayers().get(3);
        Layer loadedOutput = loaded.GetLayers().get(4);

        int recurrent = 0;
        for (Axon axon : loadedMemory.GetNeurons().get(0).GetAxons())
        {
            if (loadedOutput.GetNeurons().contains(axon.GetSource()))
            {
                recurrent++;
            }
        }
        assertEquals(loadedOutput.GetSize(), recurrent,
                "рекуррентная связь должна переживать сохранение");
    }

}
