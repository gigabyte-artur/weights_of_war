package ru.gigabyteartur.weights_of_war.commands;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Механизм приказов: выбранное сетью действие живёт ORDER_MIN_SEC..ORDER_MAX_SEC игровых
// секунд и меняется только по событию.
//
// Раньше «команда» жила до первого кадра с пустой очередью, поэтому сеть не могла связать
// своё решение с последствиями: выбор цели (один кадр) и удар (много кадров) стоили
// одинаково, а событие на поле боя не успевало стать причиной смены курса. Здесь живёт
// всё, что делает решение обязательством: срок жизни, прерывания и переиздание команды
// без повторного обращения к сети.
public class OrderTracker
{
    public final static float ORDER_MIN_SEC = 5f;                // Минимальный срок приказа, игровых секунд.
    public final static float ORDER_MAX_SEC = 10f;               // Максимальный срок приказа, игровых секунд.
    public final static float ORDER_INTERRUPT_ATTACKED = 50f;    // Порог «меня бьют», при котором приказ прерывается.

    private int index = -1;                     // Выходной нейрон, которым выбран действующий приказ (-1 = приказа нет).
    private float remaining = 0f;               // Сколько игровых секунд приказ ещё жив.
    private float threatScanCooldown = 0f;      // Через сколько игровых секунд снова проверить врага рядом.
    private boolean attackedInterruptSpent = false;  // Прерывание по удару уже израсходовано до затухания сигнала.
    private BattleObject closeEnemy = null;     // Враг, из-за которого приказ уже прерывали (чтобы не прерывать каждый тик).

    // Жив ли сейчас приказ.
    public boolean IsActive()
    {
        return index >= 0 && remaining > 0f;
    }

    // Индекс выходного нейрона действующего приказа (-1, если приказа нет).
    public int GetIndex()
    {
        return index;
    }

    // Сколько игровых секунд приказу осталось жить.
    public float GetRemaining()
    {
        return remaining;
    }

    // Начинает приказ: запоминает, каким выходом сети он выбран, и назначает срок.
    // Срок случайный в пределах ORDER_MIN_SEC..ORDER_MAX_SEC, чтобы рой не менял
    // намерение синхронно.
    //
    // Разовые команды (выбор цели) приказом не становятся — иначе юнит «обязался» бы
    // выбирать цель на десять секунд и просто стоял.
    public void Begin(int index, CommandUnit command)
    {
        if (command != null && command.IsPersistentOrder())
        {
            this.index = index;
            this.remaining = ORDER_MIN_SEC + (float) Math.random() * (ORDER_MAX_SEC - ORDER_MIN_SEC);
        }
        else
        {
            End();
        }
    }

    // Снимает приказ: следующий тик с пустой очередью снова спросит сеть.
    public void End()
    {
        index = -1;
        remaining = 0f;
        threatScanCooldown = 0f;
    }

    // Сбрасывает состояние приказа для нового или переиспользованного юнита.
    public void Reset()
    {
        index = -1;
        remaining = 0f;
        threatScanCooldown = 0f;
        attackedInterruptSpent = false;
        closeEnemy = null;
    }

    // Продвигает приказ на кадр: срок и пауза проверки угрозы тикают в игровых секундах,
    // как и кулдаун решения.
    //
    // Возвращает true, если приказ сгорел — истёк срок или событие сделало его
    // бессмысленным. Тогда юнит выбрасывает очередь команд и на следующем шаге решает
    // заново: событие важнее расписания.
    public boolean Advance(float delta, BattleUnitCommon unit, GameWorld world)
    {
        if (remaining > 0f)
        {
            remaining -= delta;
        }
        if (threatScanCooldown > 0f)
        {
            threatScanCooldown -= delta;
        }

        if (index < 0)
        {
            return false;
        }

        // Срок истёк — намерение больше не держится.
        if (remaining <= 0f)
        {
            End();
            return true;
        }

        if (ShouldInterrupt(unit, world))
        {
            End();
            return true;
        }
        return false;
    }

    // Переиздаёт команду живого приказа, не спрашивая сеть заново: «Вверх» — следующие
    // 30 px, «К цели» — следующий шаг к текущим координатам цели, «Атакует цель» —
    // продолжение погони. Иначе юнит просто встал бы столбом на остаток срока.
    //
    // Возвращает null, если приказ не держится: команда не переиздаётся или не является
    // приказом — тогда приказ снимается.
    public CommandUnit Reissue(BattleUnitCommon unit, GameWorld world)
    {
        if (!IsActive())
        {
            return null;
        }
        CommandUnit repeated = CommandThink.CreateCommand(index, unit, world);
        if (repeated == null || !repeated.IsPersistentOrder())
        {
            End();
            return null;
        }
        repeated.OnOrderRepeat(unit, world);
        return repeated;
    }

    // Прерывания приказа: события, ради которых стоит сжечь остаток срока.
    // Проверки дешёвые (кроме поиска врага в радиусе атаки — он идёт не чаще кулдауна
    // решения, то есть с той же частотой, с какой раньше жили сенсоры).
    private boolean ShouldInterrupt(BattleUnitCommon unit, GameWorld world)
    {
        // Юнита бьют — обстоятельство важнее выбранного курса. Прерываем ОДИН раз
        // на удар: сигнал «под атакой» затухает несколько секунд, и без этого флага
        // приказ сгорал бы каждый тик, пока юнит под огнём, — то есть новый приказ
        // жил бы не дольше старого кадра.
        if (unit.getAttacked() >= ORDER_INTERRUPT_ATTACKED && !attackedInterruptSpent)
        {
            attackedInterruptSpent = true;
            return true;
        }
        if (unit.getAttacked() < ORDER_INTERRUPT_ATTACKED)
        {
            attackedInterruptSpent = false;
        }
        if (index == CommandThink.NEURON_ATTACK_TARGET)
        {
            // Цель приказа умерла или ушла в туман — атаковать больше некого.
            return unit.getVisibleTargetObject(world) == null;
        }
        // Приказ не про атаку, а враг уже в радиусе удара. Обход мира стоит дорого,
        // поэтому проверка идёт не чаще DECISION_INTERVAL_SEC — реакция на событие,
        // а не сенсор каждый кадр.
        if (threatScanCooldown > 0f)
        {
            return false;
        }
        threatScanCooldown = BattleUnitCommon.DECISION_INTERVAL_SEC;

        BattleObject enemy = world.FindNearestEnemyWithin(unit, unit.getAttackRange());
        if (enemy == null)
        {
            // Угроза ушла: следующий подошедший враг снова заслуживает реакции.
            closeEnemy = null;
            return false;
        }
        if (enemy == closeEnemy)
        {
            // На этого врага уже отреагировали. Без этой памяти юнит, которому сеть
            // велела идти, стоял бы рядом с врагом и «решал» каждые 0.25 с — приказ
            // сгорал бы быстрее, чем успевал что-то сделать.
            return false;
        }
        closeEnemy = enemy;
        return true;
    }
}
