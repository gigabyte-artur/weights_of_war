package ru.gigabyteartur.weights_of_war.commands;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.testutil.TestBuilding;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest
{
    @Test
    public void targetNearestEnemySetsTargetToClosestEnemy()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(new Fraction("Red", null));
        me.setSightRange(1000);
        TestUnit near = new TestUnit(10, 0);
        near.setFraction(new Fraction("Blue", null));
        TestUnit far = new TestUnit(100, 0);
        far.setFraction(new Fraction("Blue", null));

        GameWorld world = new GameWorld();
        world.AddUnit(me);
        world.AddUnit(near);
        world.AddUnit(far);
        world.UpdateFogOfWar();

        CommandTargetNearestEnemy command = new CommandTargetNearestEnemy();
        assertTrue(command.Execute(me, world));
        assertEquals(10, me.getTargetX());
        assertEquals(0, me.getTargetY());
    }

    @Test
    public void targetNearestEnemyCompletesWhenNoEnemy()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(new Fraction("Red", null));
        me.setTarget(77, 88);

        GameWorld world = new GameWorld();
        world.AddUnit(me);

        CommandTargetNearestEnemy command = new CommandTargetNearestEnemy();
        assertTrue(command.Execute(me, world));
        assertEquals(77, me.getTargetX()); // цель не изменилась
        assertEquals(88, me.getTargetY());
    }

    @Test
    public void targetEnemyBuildingSetsTargetToBuilding()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(new Fraction("Red", null));
        me.setSightRange(1000);
        TestBuilding building = new TestBuilding(30, 40);
        building.setFraction(new Fraction("Blue", null));

        GameWorld world = new GameWorld();
        world.AddUnit(me);
        world.AddUnit(building);
        world.UpdateFogOfWar();

        CommandTargetEnemyBuilding command = new CommandTargetEnemyBuilding();
        assertTrue(command.Execute(me, world));
        assertEquals(30, me.getTargetX());
        assertEquals(40, me.getTargetY());
    }

    @Test
    public void attackTargetWithNullTargetCompletesImmediately()
    {
        TestUnit me = new TestUnit(0, 0);
        CommandAttackTarget command = new CommandAttackTarget(null);
        assertTrue(command.Execute(me, new GameWorld()));
    }

    @Test
    public void thinkWithoutNeuroNetCompletesImmediately()
    {
        TestUnit me = new TestUnit(0, 0);
        CommandThink command = new CommandThink();
        assertTrue(command.Execute(me, new GameWorld()));
        assertEquals(0, me.getCommandsQueueSize());
    }

    @Test
    public void thinkWithNeuroNetAddsACommand()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(new Fraction("Red", null));
        me.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());

        GameWorld world = new GameWorld();
        world.AddUnit(me);

        CommandThink command = new CommandThink();
        assertTrue(command.Execute(me, world));
        assertEquals(1, me.getCommandsQueueSize());
    }

    @Test
    public void moveToOwnCoreCompletesWhenNoOwnCore()
    {
        TestUnit me = new TestUnit(0, 0);
        me.setFraction(new Fraction("Red", null));

        GameWorld world = new GameWorld();
        world.AddUnit(me); // только юнит, своего ядра нет.

        CommandMoveToOwnCore command = new CommandMoveToOwnCore();
        assertTrue(command.Execute(me, world));
    }

    @Test
    public void chooseOutputIndexUsesRouletteProportionalToSignal()
    {
        // Сигналы: нейрон 0 = 0.5, нейрон 1 = 1.0, остальные отрицательные (обрезаются в 0).
        NeuroNet net = CreateOutputNet(11, 0.5, 1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
        CommandThink command = new CommandThink();

        // Сумма весов = 1.5: нейрон 0 имеет 0.5/1.5 ≈ 33%, нейрон 1 — 1.0/1.5 ≈ 67%.
        assertEquals(0, command.ChooseOutputIndex(net, 0.0));   // начало интервала
        assertEquals(0, command.ChooseOutputIndex(net, 0.3));   // 0.3 < 1/3 → нейрон 0
        assertEquals(1, command.ChooseOutputIndex(net, 0.34));  // 0.34 > 1/3 → нейрон 1
        assertEquals(1, command.ChooseOutputIndex(net, 0.99));  // верх интервала → нейрон 1
    }

    @Test
    public void chooseOutputIndexExcludesDisabledNeuron()
    {
        // Нейрон 8 (исключён) имеет максимальный сигнал, но выбираться не должен.
        NeuroNet net = CreateOutputNet(11, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0);
        CommandThink command = new CommandThink();

        // Веса учитывают только нейроны 0 и 1 (по 0.5), сумма = 1.0.
        assertEquals(0, command.ChooseOutputIndex(net, 0.0));
        assertEquals(0, command.ChooseOutputIndex(net, 0.49));
        assertEquals(1, command.ChooseOutputIndex(net, 0.5));
        assertEquals(1, command.ChooseOutputIndex(net, 0.99));
    }

    @Test
    public void chooseOutputIndexUniformWhenAllWeightsZero()
    {
        // Все сигналы нулевые → равновероятный выбор среди кандидатов (кроме нейрона 8).
        NeuroNet net = CreateOutputNet(11, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        CommandThink command = new CommandThink();

        // Кандидаты: [0,1,2,3,4,5,6,7,9,10].
        assertEquals(0, command.ChooseOutputIndex(net, 0.0));    // кандидат 0
        assertEquals(5, command.ChooseOutputIndex(net, 0.5));    // кандидат 5
        assertEquals(10, command.ChooseOutputIndex(net, 0.999)); // последний кандидат
    }

    @Test
    public void chooseOutputIndexWorksWithTenNeuronOutput()
    {
        // У обычного юнита выходной слой 10 нейронов (0..9); нейрон 8 также исключён.
        NeuroNet net = CreateOutputNet(10, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
        CommandThink command = new CommandThink();

        // Нейрон 8 исключён, все веса нулевые → равновероятный выбор среди [0,1,2,3,4,5,6,7,9].
        assertEquals(0, command.ChooseOutputIndex(net, 0.0));
        assertEquals(9, command.ChooseOutputIndex(net, 0.999));
    }

    @Test
    public void chooseOutputIndexExcludesBlockedIndices()
    {
        // Все сигналы нулевые → равновероятный выбор; заблокированный нейрон 10 не должен выбираться.
        NeuroNet net = CreateOutputNet(11, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        CommandThink command = new CommandThink();
        ArrayList<Integer> blocked = new ArrayList<Integer>();
        blocked.add(CommandThink.NEURON_HEAL_WOUNDED);

        // Кандидаты: [0,1,2,3,4,5,6,7,9] (8 исключён, 10 заблокирован). Последний — 9, а не 10.
        assertEquals(0, command.ChooseOutputIndex(net, 0.0, blocked));
        assertEquals(9, command.ChooseOutputIndex(net, 0.999, blocked));
    }

    // Создаёт нейросеть с единственным выходным слоем заданного размера
    // и устанавливает сигналы его нейронов (остальные — 0).
    private static NeuroNet CreateOutputNet(int outputSize, double... signals)
    {
        NeuroNet net = new NeuroNet();
        net.GenerateAddLayer(outputSize, false, true);
        ArrayList<Neuron> neurons = net.GetLayers().get(0).GetNeurons();
        for (int i = 0; i < neurons.size(); i++)
        {
            neurons.get(i).SetSignal(i < signals.length ? signals[i] : 0.0);
        }
        return net;
    }
}
