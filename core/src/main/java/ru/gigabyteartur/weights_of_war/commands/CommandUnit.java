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

    // Может ли эта команда быть приказом — намерением, которое живёт ORDER_MIN_SEC..ORDER_MAX_SEC
    // и переиздаётся, пока приказ не сменится или не будет прерван событием.
    //
    // По умолчанию false: разовая команда (выбор цели, лечение) не должна растягиваться
    // на десять секунд. Подклассы, у которых есть смысл «держать курс», переопределяют.
    public boolean IsPersistentOrder()
    {
        return false;
    }

    // Вызывается, когда живой приказ переиздаёт эту команду: команда может обновить
    // свою точку, если её цель сдвинулась (погоня за движущимся врагом).
    public void OnOrderRepeat(BattleUnitCommon unit, GameWorld world)
    {
    }
}
