package ru.gigabyteartur.weights_of_war.neuro_net;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class NeuroNetTest
{
    @Test
    public void compileConnectsAdjacentLayersFully()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(3, true, false);
        DenseLayer hidden = new DenseLayer();
        hidden.GenerateLayer(4, false, false);
        net.AddLayer(hidden);
        net.GenerateAddLayer(2, false, true);
        net.Compile();

        // Каждый нейрон скрытого слоя получает аксоны от всех 3 входных нейронов.
        for (Neuron neuron : net.GetLayers().get(1).GetNeurons())
        {
            assertEquals(3, neuron.GetAxons().size());
        }
        // Каждый нейрон выходного слоя получает аксоны от всех 4 скрытых нейронов.
        for (Neuron neuron : net.GetLayers().get(2).GetNeurons())
        {
            assertEquals(4, neuron.GetAxons().size());
        }
    }

    @Test
    public void calcSignalsPropagatesThroughTanh()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(1, true, false);
        net.GenerateAddLayer(1, false, true);
        net.Compile();

        Neuron input = net.GetLayers().get(0).GetNeurons().get(0);
        Neuron output = net.GetLayers().get(1).GetNeurons().get(0);
        input.SetSignal(1.0);
        output.GetAxons().get(0).SetWeight(2.0);

        net.CalcSignals();

        // output = tanh(2.0 * 1.0)
        assertEquals(Math.tanh(2.0), output.GetSignal(), 1e-9);
    }

    @Test
    public void calcSignalsIncludesBias()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(1, true, false);
        net.GenerateAddLayer(1, false, true);
        net.Compile();

        Neuron input = net.GetLayers().get(0).GetNeurons().get(0);
        Neuron output = net.GetLayers().get(1).GetNeurons().get(0);
        input.SetSignal(0.0);
        output.GetAxons().get(0).SetWeight(0.0);
        output.SetBias(0.5);

        net.CalcSignals();

        // output = tanh(bias + 0 * 0) = tanh(0.5)
        assertEquals(Math.tanh(0.5), output.GetSignal(), 1e-9);
    }

    @Test
    public void copyIsDeepAndIndependent()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.GenerateAddLayer(1, false, true);
        net.Compile();

        Neuron output = net.GetLayers().get(1).GetNeurons().get(0);
        output.GetAxons().get(0).SetWeight(0.75);

        NeuroNet copy = net.Copy();

        assertEquals(2, copy.GetLayers().size());
        assertEquals(2, copy.GetLayers().get(0).GetSize());
        assertEquals(1, copy.GetLayers().get(1).GetSize());
        assertEquals(0.75, copy.GetLayers().get(1).GetNeurons().get(0).GetAxons().get(0).GetWeight(), 1e-9);

        // Нейроны — разные объекты (глубокая копия).
        assertNotSame(net.GetLayers().get(0).GetNeurons().get(0), copy.GetLayers().get(0).GetNeurons().get(0));

        // Изменение копии не влияет на оригинал.
        copy.GetLayers().get(1).GetNeurons().get(0).GetAxons().get(0).SetWeight(0.25);
        assertEquals(0.75, net.GetLayers().get(1).GetNeurons().get(0).GetAxons().get(0).GetWeight(), 1e-9);
    }

    @Test
    public void copyModelReplacesExistingLayers()
    {
        NeuroNet source = new NeuroNet();
        source.GenerateAddLayer(2, true, false);
        source.GenerateAddLayer(1, false, true);
        source.Compile();

        NeuroNet target = new NeuroNet();
        target.GenerateAddLayer(5, true, false);
        target.CopyModel(source);

        assertEquals(2, target.GetLayers().size());
        assertEquals(2, target.GetLayers().get(0).GetSize());
        assertEquals(1, target.GetLayers().get(1).GetSize());
    }

    @Test
    public void setInputSignalWritesToFirstLayer()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.SetInputSignal(1, 0.42);
        assertEquals(0.42, net.GetLayers().get(0).GetNeurons().get(1).GetSignal(), 1e-9);
    }

    @Test
    public void randomWeightsAreWithinRange()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.GenerateAddLayer(2, false, true);
        net.Compile();
        net.RandomWeights();

        for (Layer layer : net.GetLayers())
        {
            for (Neuron neuron : layer.GetNeurons())
            {
                for (Axon axon : neuron.GetAxons())
                {
                    double w = axon.GetWeight();
                    assertTrue(w >= -1.0 && w < 1.0, "Weight out of range: " + w);
                }
            }
        }
    }

    @Test
    public void saveAndLoadPreservesStructureAndWeights() throws Exception
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(3, true, false);
        DenseLayer hidden = new DenseLayer();
        hidden.GenerateLayer(4, false, false);
        net.AddLayer(hidden);
        net.GenerateAddLayer(2, false, true);
        net.Compile();
        net.RandomWeights();

        File tempFile = File.createTempFile("neuro_net_save", ".xml");
        String path = tempFile.getAbsolutePath();
        try
        {
            net.SaveToFile(path);

            NeuroNet loaded = new NeuroNet();
            loaded.LoadFromFile(path);

            // Структура совпадает.
            assertEquals(net.GetLayers().size(), loaded.GetLayers().size());
            for (int i = 0; i < net.GetLayers().size(); i++)
            {
                assertEquals(net.GetLayers().get(i).GetSize(), loaded.GetLayers().get(i).GetSize());
                assertEquals(net.GetLayers().get(i).GetIsInput(), loaded.GetLayers().get(i).GetIsInput());
                assertEquals(net.GetLayers().get(i).GetIsOutput(), loaded.GetLayers().get(i).GetIsOutput());
            }

            // Веса совпадают.
            for (int i = 0; i < net.GetLayers().size(); i++)
            {
                ArrayList<Neuron> origNeurons = net.GetLayers().get(i).GetNeurons();
                ArrayList<Neuron> loadedNeurons = loaded.GetLayers().get(i).GetNeurons();
                for (int n = 0; n < origNeurons.size(); n++)
                {
                    ArrayList<Axon> origAxons = origNeurons.get(n).GetAxons();
                    ArrayList<Axon> loadedAxons = loadedNeurons.get(n).GetAxons();
                    assertEquals(origAxons.size(), loadedAxons.size());
                    for (int a = 0; a < origAxons.size(); a++)
                    {
                        assertEquals(origAxons.get(a).GetWeight(), loadedAxons.get(a).GetWeight(), 1e-9);
                    }
                }
            }

            // Смещения (bias) совпадают.
            for (int i = 0; i < net.GetLayers().size(); i++)
            {
                ArrayList<Neuron> origNeurons = net.GetLayers().get(i).GetNeurons();
                ArrayList<Neuron> loadedNeurons = loaded.GetLayers().get(i).GetNeurons();
                for (int n = 0; n < origNeurons.size(); n++)
                {
                    assertEquals(origNeurons.get(n).GetBias(), loadedNeurons.get(n).GetBias(), 1e-9);
                }
            }
        }
        finally
        {
            tempFile.delete();
        }
    }

    @Test
    public void setInputSignalOutOfRangeDoesNotCrash()
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);

        // Индекс равен размеру слоя и за его пределами — не должны падать (off-by-one).
        net.SetInputSignal(2, 0.5);
        net.SetInputSignal(10, 0.5);
    }

    @Test
    public void saveWritesCurrentFormatVersion() throws Exception
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(1, true, false);

        File tempFile = File.createTempFile("neuro_net_version", ".xml");
        try
        {
            net.SaveToFile(tempFile.getAbsolutePath());
            String xml = Files.readString(tempFile.toPath());
            assertTrue(xml.contains("<format_version>" + NeuroNet.NEURO_NET_FORMAT_VERSION + "</format_version>"));
        }
        finally
        {
            tempFile.delete();
        }
    }

    @Test
    public void scheduleLoggingSavesSignalsWeightsAndLabelsOnCalcSignals() throws Exception
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.GenerateAddLayer(1, false, true);
        net.Compile();

        net.SetInputSignal(0, 0.5);
        net.GetLayers().get(1).GetNeurons().get(0).GetAxons().get(0).SetWeight(0.75);

        ArrayList<String> inputNames = new ArrayList<>();
        inputNames.add("Сенсор1");
        inputNames.add("Сенсор2");
        ArrayList<String> outputNames = new ArrayList<>();
        outputNames.add("Команда1");

        File tempFile = File.createTempFile("neuro_net_log", ".xml");
        try
        {
            NeuroNet.ScheduleLogging(net, tempFile.getAbsolutePath(), "Мечник", inputNames, outputNames);
            net.CalcSignals();

            String xml = Files.readString(tempFile.toPath());
            assertTrue(xml.contains("<signal>"));
            assertTrue(xml.contains("<weight>"));
            assertTrue(xml.contains("<unit_name>Мечник</unit_name>"));
            assertTrue(xml.contains("<label>Сенсор1</label>"));
            assertTrue(xml.contains("<label>Команда1</label>"));
        }
        finally
        {
            tempFile.delete();
        }
    }

    @Test
    public void loadRejectsLowerFormatVersion() throws Exception
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(2, true, false);
        net.GenerateAddLayer(1, false, true);
        net.Compile();

        File tempFile = File.createTempFile("neuro_net_old", ".xml");
        try
        {
            net.SaveToFile(tempFile.getAbsolutePath());
            String xml = Files.readString(tempFile.toPath());
            String currentVersion = String.valueOf(NeuroNet.NEURO_NET_FORMAT_VERSION);
            String olderVersion = String.valueOf(NeuroNet.NEURO_NET_FORMAT_VERSION - 1);
            xml = xml.replace("<format_version>" + currentVersion + "</format_version>",
                              "<format_version>" + olderVersion + "</format_version>");
            Files.writeString(tempFile.toPath(), xml);

            NeuroNet loaded = new NeuroNet();
            boolean loadedOk = loaded.LoadFromFile(tempFile.getAbsolutePath());

            assertFalse(loadedOk);
            assertEquals(0, loaded.GetLayers().size());
        }
        finally
        {
            tempFile.delete();
        }
    }
}
