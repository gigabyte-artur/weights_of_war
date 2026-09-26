package ru.gigabyteartur.weights_of_war.neuro_net;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

// Нейрон нейронной сети.
public class Neuron
{
    private double signal;                                  // Сигнал нейрона.
    private double bias;                                    // Смещение (bias) нейрона.
    private ArrayList<Axon> Axons = new ArrayList<>();      // Аксоны.
    private Layer parent_layer;                             // Родительский слой.
    private String id;                                      // Идентификатор.

    // Конструктор.
    public Neuron()
    {
        UUID uuid = UUID.randomUUID();
        this.id = uuid.toString();
    }

    // Получает сигнал нейрона.
    public double GetSignal()
    {
        return this.signal;
    }

    // Устанавливает сигнал нейрона в signal_in.
    public void SetSignal(double signal_in)
    {
        this.signal = signal_in;
    }

    // Получает смещение (bias) нейрона.
    public double GetBias()
    {
        return this.bias;
    }

    // Устанавливает смещение (bias) нейрона в bias_in.
    public void SetBias(double bias_in)
    {
        this.bias = bias_in;
    }

    // Добавляет аксон axon_in в список аксонов текщего нейрона.
    public void AddAxon(Axon axon_in)
    {
        this.Axons.add(axon_in);
    }

    // Создает аксон текщего нейрона с нейроном neuron_in со весом свзяи weight_in.
    public void GenerateAddAxon(Neuron neuron_in, double weight_in)
    {
        Axon new_axon = new Axon();
        new_axon.Set(neuron_in, weight_in);
        this.AddAxon(new_axon);
    }

    // Возвращает значение родительского слоя.
    public Layer GetParentLayer()
    {
        return this.parent_layer;
    }

    // Устанавливает родительским слоем слой parent_layer_in.
    public void SetParentLayer(Layer parent_layer_in)
    {
        this.parent_layer = parent_layer_in;
    }

    // Возвращает все нейроны слоя.
    public ArrayList<Axon> GetAxons()
    {
        return Axons;
    }

    // С вероятностью probablity_in устанавливает всем аксонам текущего нейрона
    // случайные веса.
    public void Mutate(double probablity_in)
    {
        double new_weight;
        final Random random = new Random();
        int dice;
        for (Axon curr_axon:this.Axons)
        {
            dice = random.nextInt(100);
            if (dice <= probablity_in * 100)
            {
                new_weight = (random.nextDouble() * 2) - 1;
                curr_axon.SetWeight(new_weight);
            }
        }
        // Мутация смещения (bias) в диапазоне -4..4.
        dice = random.nextInt(100);
        if (dice <= probablity_in * 100)
        {
            this.bias = (random.nextDouble() * 8) - 4;
        }
    }

    // Возвращает идентификатор нейрона.
    public String GetId()
    {
        return this.id;
    }

    // Устанавливает идентификатор в значение id_in.
    public void SetId(String id_in)
    {
        this.id = id_in;
    }
}
