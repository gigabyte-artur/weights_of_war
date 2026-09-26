package ru.gigabyteartur.weights_of_war.evo;

import java.util.ArrayList;
import java.util.Random;
import ru.gigabyteartur.weights_of_war.neuro_net.Axon;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Эволюционные алгоритмы: генерация потомков с мутацией и элитизм (пул из трёх чемпионов в ядре).
public class Evolution
{
    // Вероятности мутации по долям популяции: первые 30% — 0.05, следующие 30% — 0.10, остальные — 0.15.
    public final static double MUTATION_PROBABILITY_LOW = 0.05;
    public final static double MUTATION_PROBABILITY_MEDIUM = 0.10;
    public final static double MUTATION_PROBABILITY_HIGH = 0.15;
    public final static double LOW_SHARE = 0.1;     // Доля потомков с низкой вероятностью мутации.
    public final static double MEDIUM_SHARE = 0.30;  // Доля потомков со средней вероятностью мутации.
    public final static double CROSSOVER_RATE = 0.5; // Вероятность взять вес от второго родителя при кроссинговере.
    public final static int LIMIT_SAVE = 5;    // Количество лучших юнитов, которые берутся в ядро
    private Random random = new Random();

    // Формирует список потомков-нейросетей из лучших юнитов и нейросетей-чемпионов ядра.
    // Каждый потомок — кроссинговер двух случайных родителей с последующей мутацией.
    public ArrayList<NeuroNet> GenerateOffspring(ArrayList<? extends BattleUnitCommon> topUnits, ArrayList<NeuroNet> coreNets, int count, BattleUnitCommon enemyUnit)
    {
        ArrayList<NeuroNet> parents = new ArrayList<NeuroNet>();
        for (BattleUnitCommon unit : topUnits)
        {
            if (unit.getNeuroNet() != null)
            {
                parents.add(unit.getNeuroNet());
            }
        }
        if (coreNets != null)
        {
            for (NeuroNet coreNet : coreNets)
            {
                if (coreNet != null)
                {
                    parents.add(coreNet);
                }
            }
        }
        // Лучший вражеский юнит — горизонтальный перенос генов.
        if (enemyUnit != null && enemyUnit.getNeuroNet() != null)
        {
            parents.add(enemyUnit.getNeuroNet());
        }

        ArrayList<NeuroNet> offspring = new ArrayList<NeuroNet>();
        for (int i = 0; i < count; i++)
        {
            NeuroNet parentA = parents.get(random.nextInt(parents.size()));
            NeuroNet parentB = parents.get(random.nextInt(parents.size()));
            NeuroNet child = Crossover(parentA, parentB);
            child.Mutate(GetMutationProbability(i, count));
            offspring.add(child);
        }
        return offspring;
    }

    // Uniform crossover: потомок — копия parentA, у которого каждый вес
    // с вероятностью CROSSOVER_RATE заменён на соответствующий вес parentB.
    private NeuroNet Crossover(NeuroNet parentA, NeuroNet parentB)
    {
        NeuroNet child = parentA.Copy();
        ArrayList<Layer> childLayers = child.GetLayers();
        ArrayList<Layer> layersB = parentB.GetLayers();

        for (int l = 0; l < childLayers.size(); l++)
        {
            ArrayList<Neuron> childNeurons = childLayers.get(l).GetNeurons();
            ArrayList<Neuron> neuronsB = layersB.get(l).GetNeurons();
            for (int n = 0; n < childNeurons.size(); n++)
            {
                ArrayList<Axon> childAxons = childNeurons.get(n).GetAxons();
                ArrayList<Axon> axonsB = neuronsB.get(n).GetAxons();
                for (int a = 0; a < childAxons.size(); a++)
                {
                    if (random.nextDouble() < CROSSOVER_RATE)
                    {
                        childAxons.get(a).SetWeight(axonsB.get(a).GetWeight());
                    }
                }
                // Кроссинговер смещения (bias).
                if (random.nextDouble() < CROSSOVER_RATE)
                {
                    childNeurons.get(n).SetBias(neuronsB.get(n).GetBias());
                }
            }
        }
        return child;
    }

    // Возвращает вероятность мутации для потомка с индексом index из count:
    // первые 30% — низкая, следующие 30% — средняя, остальные — высокая.
    private double GetMutationProbability(int index, int count)
    {
        int lowCount = (int) Math.round(count * LOW_SHARE);
        int mediumCount = (int) Math.round(count * MEDIUM_SHARE);
        if (index < lowCount)
        {
            return MUTATION_PROBABILITY_LOW;
        }
        if (index < lowCount + mediumCount)
        {
            return MUTATION_PROBABILITY_MEDIUM;
        }
        return MUTATION_PROBABILITY_HIGH;
    }

    // Элитизм: объединяет прежний пул чемпионов ядра и текущих топ-юнитов, сортирует
    // по фиту и оставляет до трёх лучших — чемпионы не деградируют между поколениями.
    public void UpdateCoreNets(ArrayList<? extends BattleUnitCommon> topUnits, ArrayList<NeuroNet> coreNets, ArrayList<Integer> coreFits)
    {
        // Кандидаты: прежние чемпионы (уже копии, принадлежат ядру) и нейросети топ-юнитов.
        ArrayList<NeuroNet> candidates = new ArrayList<NeuroNet>(coreNets);
        ArrayList<Integer> candidateFits = new ArrayList<Integer>(coreFits);
        for (BattleUnitCommon unit : topUnits)
        {
            if (unit.getNeuroNet() != null)
            {
                candidates.add(unit.getNeuroNet().Copy());
                candidateFits.add(unit.getFit());
            }
        }

        // Сортировка вставками по убыванию фита (синхронно обе коллекции).
        for (int i = 1; i < candidates.size(); i++)
        {
            NeuroNet netKey = candidates.get(i);
            int fitKey = candidateFits.get(i);
            int j = i - 1;
            while (j >= 0 && candidateFits.get(j) < fitKey)
            {
                candidates.set(j + 1, candidates.get(j));
                candidateFits.set(j + 1, candidateFits.get(j));
                j--;
            }
            candidates.set(j + 1, netKey);
            candidateFits.set(j + 1, fitKey);
        }

        // Оставляем до трёх лучших.
        coreNets.clear();
        coreFits.clear();
        int limit = Math.min(LIMIT_SAVE, candidates.size());
        for (int i = 0; i < limit; i++)
        {
            coreNets.add(candidates.get(i));
            coreFits.add(candidateFits.get(i));
        }
    }
}
