package ru.gigabyteartur.weights_of_war.commands;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

// Тесты блокировки команд, недоступных определённым типам юнитов.
public class CommandBlockingTest extends HeadlessGdxTest
{
    // Лечение недоступно мечнику, лучнику, щитовику и разведчику.
    @Test
    public void nonPriestUnitsBlockHealCommand()
    {
        CommandThink command = new CommandThink();

        assertTrue(command.GetBlockedIndices(new UnitSwordman(0, 0)).contains(CommandThink.NEURON_HEAL_WOUNDED));
        assertTrue(command.GetBlockedIndices(new UnitArcher(0, 0)).contains(CommandThink.NEURON_HEAL_WOUNDED));
        assertTrue(command.GetBlockedIndices(new UnitShieldman(0, 0)).contains(CommandThink.NEURON_HEAL_WOUNDED));
        assertTrue(command.GetBlockedIndices(new UnitScout(0, 0)).contains(CommandThink.NEURON_HEAL_WOUNDED));
    }

    // Атака недоступна жрецу.
    @Test
    public void priestBlocksAttackCommand()
    {
        CommandThink command = new CommandThink();
        ArrayList<Integer> blocked = command.GetBlockedIndices(new UnitPriest(0, 0));

        assertTrue(blocked.contains(CommandThink.NEURON_ATTACK_TARGET));
        assertFalse(blocked.contains(CommandThink.NEURON_HEAL_WOUNDED));
    }

    // Жрец сохраняет доступ к лечению; не-жрецы сохраняют доступ к атаке.
    @Test
    public void nonPriestKeepsAttackAndPriestKeepsHeal()
    {
        CommandThink command = new CommandThink();

        assertFalse(command.GetBlockedIndices(new UnitSwordman(0, 0)).contains(CommandThink.NEURON_ATTACK_TARGET));
        assertFalse(command.GetBlockedIndices(new UnitPriest(0, 0)).contains(CommandThink.NEURON_HEAL_WOUNDED));
    }
}
