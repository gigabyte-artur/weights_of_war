package ru.gigabyteartur.weights_of_war.evo;

import java.util.ArrayList;
import java.util.Random;
import ru.gigabyteartur.weights_of_war.neuro_net.Axon;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Эволюционные алгоритмы: генерация потомков с мутацией и элитизм (пул из трёх чемпионов в ядре).
//
// SEARCH NOTE. Исходный оператор мутации заменял вес случайным числом из [-1, 1].
// Для сети 26-20-20-10 это 1120 весов, поэтому при вероятности 0.15 потомок терял
// около 168 весов — структура, найденная родителем, уничтожалась целиком, и поиск
// не сходился: лучший результат колебался, а средний фит популяции падал.
// Замена на гауссово возмущение (weight += N(0, sigma)) сохраняет найденное и
// сдвигает его локально. Разброс sigma адаптивный: пока фит чемпиона растёт, шаг
// уменьшается (шлифовка), при застое — снова растёт (разведка).
public class Evolution
{
    public final static double MUTATION_PROBABILITY = 0.35;  // Доля весов, которые возмущаются у потомка.
    public final static double CROSSOVER_RATE = 0.5;         // Вероятность взять вес от второго родителя при кроссинговере.
    public final static int LIMIT_SAVE = 3;                  // Количество чемпионов, которые берутся в ядро.

    // Адаптивный шаг мутации.
    public final static double SIGMA_START = 0.35;      // Стартовый разброс возмущения.
    public final static double SIGMA_MIN = 0.03;        // Минимальный разброс (тонкая шлифовка).
    public final static double SIGMA_MAX = 0.60;        // Максимальный разброс (широкая разведка).
    public final static double SIGMA_DECAY = 0.85;      // Во сколько раз шаг уменьшается при прогрессе.
    public final static double SIGMA_GROW = 1.60;       // Во сколько раз шаг растёт при застое.
    public final static int STAGNATION_LIMIT = 2;       // Столько поколений без роста — и шаг растёт.

    public final static int ELITE_COPIES = 1;           // Сколько потомков — точные копии чемпиона (без мутации).

    private Random random = new Random();
    private double sigma = SIGMA_START;
    private int bestFitSeen = Integer.MIN_VALUE;
    private int stagnantGenerations = 0;

    // Возвращает текущий разброс мутации (для отладки и тестов).
    public double GetSigma()
    {
        return sigma;
    }

    // Пересчитывает разброс мутации по лучшему фиту поколения: рост — шлифуем,
    // застой — расширяем поиск, чтобы не застрять в локальном оптимуме.
    public void AdaptSigma(int bestFit)
    {
        if (bestFit > bestFitSeen)
        {
            bestFitSeen = bestFit;
            stagnantGenerations = 0;
            sigma *= SIGMA_DECAY;
        }
        else
        {
            stagnantGenerations++;
            if (stagnantGenerations >= STAGNATION_LIMIT)
            {
                sigma *= SIGMA_GROW;
                stagnantGenerations = 0;
            }
        }
        if (sigma < SIGMA_MIN) sigma = SIGMA_MIN;
        if (sigma > SIGMA_MAX) sigma = SIGMA_MAX;
    }

    // Турнирный отбор: из tournamentSize случайных родителей берётся лучший по фиту.
    // Турнирный отбор создаёт давление отбора — слабые сети почти не размножаются,
    // тогда как равномерный случайный выбор давал им те же шансы, что и чемпионам,
    // и поиск топтался на месте.
    public final static int TOURNAMENT_SIZE = 3;
    // Доля потомков-иммигрантов: полностью случайная сеть на поколение.
    // Без иммигрантов популяция сходится к одному локальному оптимуму и перестаёт
    // исследовать; иммигрант возвращает в генофонд разнообразие.
    public final static int IMMIGRANTS = 1;

    // Совместимая перегрузка без фитов чемпионов ядра.
    public ArrayList<NeuroNet> GenerateOffspring(ArrayList<? extends BattleUnitCommon> topUnits, ArrayList<NeuroNet> coreNets, int count, BattleUnitCommon enemyUnit)
    {
        return GenerateOffspring(topUnits, coreNets, null, count, enemyUnit);
    }

    // Формирует список потомков-нейросетей из лучших юнитов и нейросетей-чемпионов ядра.
    // Каждый потомок — кроссинговер двух родителей, выбранных турниром, с последующей
    // гауссовой мутацией; первый потомок — точная копия чемпиона, последний — иммигрант.
    public ArrayList<NeuroNet> GenerateOffspring(ArrayList<? extends BattleUnitCommon> topUnits, ArrayList<NeuroNet> coreNets,
            ArrayList<Integer> coreFits, int count, BattleUnitCommon enemyUnit)
    {
        ArrayList<NeuroNet> parents = new ArrayList<NeuroNet>();
        ArrayList<Integer> parentFits = new ArrayList<Integer>();
        for (BattleUnitCommon unit : topUnits)
        {
            if (unit.getNeuroNet() != null)
            {
                parents.add(unit.getNeuroNet());
                parentFits.add(unit.getFit());
            }
        }
        if (coreNets != null)
        {
            for (int i = 0; i < coreNets.size(); i++)
            {
                NeuroNet coreNet = coreNets.get(i);
                if (coreNet != null)
                {
                    parents.add(coreNet);
                    int coreFit = (coreFits != null && i < coreFits.size()) ? coreFits.get(i) : 0;
                    parentFits.add(coreFit);
                }
            }
        }
        // Лучший вражеский юнит — горизонтальный перенос генов.
        if (enemyUnit != null && enemyUnit.getNeuroNet() != null)
        {
            parents.add(enemyUnit.getNeuroNet());
            parentFits.add(enemyUnit.getFit());
        }

        ArrayList<NeuroNet> offspring = new ArrayList<NeuroNet>();
        if (parents.isEmpty())
        {
            return offspring;
        }
        // Первый потомок — точная копия лучшего чемпиона: элитизм внутри поколения,
        // чтобы найденное не терялось даже при неудачной мутации остальных.
        NeuroNet elite = parents.get(0).Copy();
        offspring.add(elite);

        for (int i = 1; i < count; i++)
        {
            // Последние IMMIGRANTS потомков — свежие случайные сети: разнообразие
            // в генофонде против преждевременной сходимости.
            if (count > 2 && i >= count - IMMIGRANTS)
            {
                NeuroNet immigrant = parents.get(0).Copy();
                immigrant.RandomWeights();
                offspring.add(immigrant);
                continue;
            }
            NeuroNet parentA = SelectByTournament(parents, parentFits);
            NeuroNet parentB = SelectByTournament(parents, parentFits);
            NeuroNet child = Crossover(parentA, parentB);
            child.Mutate(MUTATION_PROBABILITY, sigma);
            offspring.add(child);
        }
        return offspring;
    }

    // Турнирный отбор: берём TOURNAMENT_SIZE случайных родителей и возвращаем лучшего по фиту.
    private NeuroNet SelectByTournament(ArrayList<NeuroNet> parents, ArrayList<Integer> fits)
    {
        int bestIdx = random.nextInt(parents.size());
        int bestFit = fits.get(bestIdx);
        for (int t = 1; t < TOURNAMENT_SIZE; t++)
        {
            int idx = random.nextInt(parents.size());
            if (fits.get(idx) > bestFit)
            {
                bestFit = fits.get(idx);
                bestIdx = idx;
            }
        }
        return parents.get(bestIdx);
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
