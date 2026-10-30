package ru.gigabyteartur.weights_of_war.brains;

import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Источник решения юнита. Источников ровно два, и они не смешиваются:
//
//   * эволюционный — brain == null. Решение выдаёт нейросеть через CommandThink,
//     юнит получает фит, участвует в отборе и передаёт гены потомкам;
//   * вписанный   — у юнита есть UnitBrain. Он и решает, а сеть, сенсоры и фит
//     в этом юните не участвуют вовсе.
//
// Смысл разделения в том, чтобы сравнивать обучение с простой стратегией честно:
// юнит либо учится, либо играет по правилам, но никогда не делает и то и другое
// сразу. Иначе «контрольная» стратегия тихо подкармливалась бы эволюцией.
public interface UnitBrain
{
    // Возвращает команду, которую юниту нужно выполнять прямо сейчас.
    // null — мозг намеренно ничего не делает в этот момент.
    CommandUnit Decide(BattleUnitCommon unit, GameWorld world);

    // Название стратегии (для отладочной панели и логов).
    String GetName();
}
