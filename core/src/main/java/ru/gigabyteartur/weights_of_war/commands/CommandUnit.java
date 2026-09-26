package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Базовый класс команды для юнита.
public abstract class CommandUnit
{
    // Выполняет команду для юнита. Возвращает true, если команда завершена.
    public abstract boolean Execute(BattleUnitCommon unit, GameWorld world);

    // Название команды (выполняемое действие).
    public String GetName()
    {
        return getClass().getSimpleName();
    }
}
