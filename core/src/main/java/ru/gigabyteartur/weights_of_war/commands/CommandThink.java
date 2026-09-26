package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCommon;
import ru.gigabyteartur.weights_of_war.Sensors.SensorEnemyUp;
import ru.gigabyteartur.weights_of_war.Sensors.SensorEnemyRight;
import ru.gigabyteartur.weights_of_war.Sensors.SensorEnemyDown;
import ru.gigabyteartur.weights_of_war.Sensors.SensorEnemyLeft;
import ru.gigabyteartur.weights_of_war.Sensors.SensorAllyUp;
import ru.gigabyteartur.weights_of_war.Sensors.SensorAttacked;
import ru.gigabyteartur.weights_of_war.Sensors.SensorAllyRight;
import ru.gigabyteartur.weights_of_war.Sensors.SensorAllyDown;
import ru.gigabyteartur.weights_of_war.Sensors.SensorAllyLeft;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountEnemyUp;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountEnemyDown;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountEnemyLeft;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountEnemyRight;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountAllyUp;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountAllyDown;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountAllyLeft;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCountAllyRight;
import ru.gigabyteartur.weights_of_war.Sensors.SensorDistanceToOwnCore;
import ru.gigabyteartur.weights_of_war.Sensors.SensorOwnHealth;
import ru.gigabyteartur.weights_of_war.Sensors.SensorStuck;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetHealth;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetSelected;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetEnemyBuilding;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetEnemyUnit;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetAllyBuilding;
import ru.gigabyteartur.weights_of_war.Sensors.SensorTargetAllyUnit;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;

import java.util.ArrayList;

// Команда «подумать»: по сенсорам и нейросети выбирает следующую команду.
public class CommandThink extends CommandUnit
{
    // Выходной нейрон с этим индексом (команда CommandAttackNearest) закомментирован
    // в CreateCommand и не выдаёт команду, поэтому исключается из вероятностного розыгрыша.
    public final static int EXCLUDED_NEURON_INDEX = 8;

    // Индексы выходных нейронов команд, доступных не всем типам юнитов.
    public final static int NEURON_ATTACK_TARGET = 0;   // CommandAttackTarget («Атакует цель»).
    public final static int NEURON_HEAL_WOUNDED = 10;   // CommandHealNearestWounded («Лечит союзника»).

    private ArrayList<SensorCommon> sensors;

    public CommandThink()
    {
        this(CreateBaseSensors());
    }

    public CommandThink(ArrayList<SensorCommon> sensors)
    {
        this.sensors = sensors;
    }

    // Создаёт базовый набор из 26 сенсоров (для обычных юнитов).
    public static ArrayList<SensorCommon> CreateBaseSensors()
    {
        ArrayList<SensorCommon> sensors = new ArrayList<SensorCommon>();
        // Близость врагов.
        sensors.add(new SensorEnemyUp());
        sensors.add(new SensorEnemyRight());
        sensors.add(new SensorEnemyDown());
        sensors.add(new SensorEnemyLeft());
        // Близость союзников.
        sensors.add(new SensorAllyUp());
        sensors.add(new SensorAllyRight());
        sensors.add(new SensorAllyDown());
        sensors.add(new SensorAllyLeft());
        // Количество врагов.
        sensors.add(new SensorCountEnemyUp());
        sensors.add(new SensorCountEnemyDown());
        sensors.add(new SensorCountEnemyLeft());
        sensors.add(new SensorCountEnemyRight());
        // Количество союзников.
        sensors.add(new SensorCountAllyUp());
        sensors.add(new SensorCountAllyDown());
        sensors.add(new SensorCountAllyLeft());
        sensors.add(new SensorCountAllyRight());
        // Собственное здоровье.
        sensors.add(new SensorOwnHealth());
        // Расстояние до своего ядра.
        sensors.add(new SensorDistanceToOwnCore());
        // Застревание (не может сдвинуться).
        sensors.add(new SensorStuck());
        // Под атакой.
        sensors.add(new SensorAttacked());
        // Здоровье выбранной цели.
        sensors.add(new SensorTargetHealth());
        // Выбрана ли цель.
        sensors.add(new SensorTargetSelected());
        // Тип выбранной цели.
        sensors.add(new SensorTargetEnemyBuilding());
        sensors.add(new SensorTargetEnemyUnit());
        sensors.add(new SensorTargetAllyBuilding());
        sensors.add(new SensorTargetAllyUnit());
        return sensors;
    }

    @Override
    public boolean Execute(BattleUnitCommon unit, GameWorld world)
    {
        NeuroNet neuroNet = unit.getNeuroNet();
        if (neuroNet == null)
        {
            return true;
        }

        // Этап 1: значения сенсоров во входные нейроны (0..100 → 0..1).
        // Пишем не больше сенсоров, чем нейронов во входном слое.
        int inputCount = 0;
        if (!neuroNet.GetLayers().isEmpty())
        {
            inputCount = Math.min(sensors.size(), neuroNet.GetLayers().get(0).GetSize());
        }
        for (int i = 0; i < inputCount; i++)
        {
            int value = sensors.get(i).CheckSensor(unit, world);
            neuroNet.SetInputSignal(i, value / 100.0);
        }

        // Этап 2: рассчитываем скрытые слои.
        neuroNet.CalcSignals();

        // Этап 2.5: блокируем команды, недоступные данному типу юнита, —
        // устанавливаем их выходной сигнал меньше 0 перед выбором.
        ArrayList<Integer> blockedIndices = GetBlockedIndices(unit);
        BlockOutputSignals(neuroNet, blockedIndices);

        // Этап 3: выбираем команду вероятностно (рулетка по весам выходных сигналов).
        int bestIndex = ChooseOutputIndex(neuroNet, Math.random(), blockedIndices);
        CommandUnit command = CreateCommand(bestIndex, unit, world);
        if (command != null)
        {
            unit.AddCommand(command);
        }

        return true;
    }

    // Выбирает индекс выходного нейрона вероятностно: вес нейрона = max(0, сигнал),
    // вероятность = вес / сумма весов (рулетка). Нейрон EXCLUDED_NEURON_INDEX исключён.
    int ChooseOutputIndex(NeuroNet neuroNet)
    {
        return ChooseOutputIndex(neuroNet, Math.random());
    }

    // То же, но с явным случайным значением randomValue ∈ [0, 1) — для детерминированных тестов.
    int ChooseOutputIndex(NeuroNet neuroNet, double randomValue)
    {
        return ChooseOutputIndex(neuroNet, randomValue, new ArrayList<Integer>());
    }

    // Полная версия: дополнительно исключает из розыгрыша заблокированные индексы
    // (команды, недоступные данному типу юнита).
    int ChooseOutputIndex(NeuroNet neuroNet, double randomValue, ArrayList<Integer> blockedIndices)
    {
        ArrayList<Layer> layers = neuroNet.GetLayers();
        Layer outputLayer = layers.get(layers.size() - 1);
        ArrayList<Neuron> outputNeurons = outputLayer.GetNeurons();

        // Кандидаты: все выходные нейроны, кроме исключённого и заблокированных индексов.
        ArrayList<Integer> indices = new ArrayList<Integer>();
        ArrayList<Double> weights = new ArrayList<Double>();
        double totalWeight = 0.0;
        for (int i = 0; i < outputNeurons.size(); i++)
        {
            if (i == EXCLUDED_NEURON_INDEX || blockedIndices.contains(i))
            {
                continue;
            }
            double weight = Math.max(0.0, outputNeurons.get(i).GetSignal());
            indices.add(i);
            weights.add(weight);
            totalWeight += weight;
        }

        // Все веса нулевые — равновероятный выбор среди кандидатов.
        if (totalWeight <= 0.0)
        {
            int pick = (int) (randomValue * indices.size());
            if (pick >= indices.size())
            {
                pick = indices.size() - 1;
            }
            return indices.get(pick);
        }

        // Рулетка: накопленная сумма весов задаёт интервалы выбора.
        double threshold = randomValue * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < indices.size(); i++)
        {
            cumulative += weights.get(i);
            if (threshold < cumulative)
            {
                return indices.get(i);
            }
        }
        // Защита от погрешности округления — последний кандидат.
        return indices.get(indices.size() - 1);
    }

    // Возвращает индексы выходных нейронов команд, недоступных данному типу юнита.
    // Лечение доступно только жрецу; атака цели недоступна жрецу.
    ArrayList<Integer> GetBlockedIndices(BattleUnitCommon unit)
    {
        ArrayList<Integer> blocked = new ArrayList<Integer>();
        if (!(unit instanceof UnitPriest))
        {
            blocked.add(NEURON_HEAL_WOUNDED);
        }
        if (unit instanceof UnitPriest)
        {
            blocked.add(NEURON_ATTACK_TARGET);
        }
        return blocked;
    }

    // Устанавливает выходные сигналы заблокированных команд в отрицательное значение
    // перед подсчётом весов, чтобы они не могли быть выбраны.
    private void BlockOutputSignals(NeuroNet neuroNet, ArrayList<Integer> blockedIndices)
    {
        ArrayList<Layer> layers = neuroNet.GetLayers();
        Layer outputLayer = layers.get(layers.size() - 1);
        ArrayList<Neuron> outputNeurons = outputLayer.GetNeurons();
        for (int index : blockedIndices)
        {
            if (index >= 0 && index < outputNeurons.size())
            {
                outputNeurons.get(index).SetSignal(-1.0);
            }
        }
    }

    // Создаёт команду по индексу выходного нейрона.
    private CommandUnit CreateCommand(int index, BattleUnitCommon unit, GameWorld world)
    {
        switch (index)
        {
            case 0: return new CommandAttackTarget(world.FindNearestEnemy(unit));
            case 1: return new CommandMoveDown();
            case 2: return new CommandMoveLeft();
            case 3: return new CommandMoveUp();
            case 4: return new CommandMoveRight();
            case 5: return new CommandMoveToTarget();
            case 6: return new CommandTargetNearestEnemy();
            case 7: return new CommandTargetEnemyBuilding();
            //case 8: return new CommandAttackNearest();
            case 9: return new CommandMoveToOwnCore();
            case 10: return new CommandHealNearestWounded();
            default: return null;
        }
    }

    // Возвращает имена команд, соответствующих выходным нейронам (индексы 0..10).
    public static ArrayList<String> GetCommandNames()
    {
        ArrayList<String> names = new ArrayList<String>();
        names.add("Атакует цель");       // 0 CommandAttackTarget
        names.add("Вниз");               // 1 CommandMoveDown
        names.add("Влево");              // 2 CommandMoveLeft
        names.add("Вверх");              // 3 CommandMoveUp
        names.add("Вправо");             // 4 CommandMoveRight
        names.add("К цели");             // 5 CommandMoveToTarget
        names.add("Целится во врага");   // 6 CommandTargetNearestEnemy
        names.add("Целится в здание");   // 7 CommandTargetEnemyBuilding
        names.add("Атакует ближайшего"); // 8 CommandAttackNearest
        names.add("К ядру");             // 9 CommandMoveToOwnCore
        names.add("Лечит союзника");     // 10 CommandHealNearestWounded
        return names;
    }

    // Название команды.
    @Override
    public String GetName()
    {
        return "Думает";
    }
}
