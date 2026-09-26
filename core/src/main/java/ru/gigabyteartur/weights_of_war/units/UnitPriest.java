package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.TextureCache;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.commands.CommandHealNearestWounded;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCommon;
import ru.gigabyteartur.weights_of_war.Sensors.SensorOwnMana;
import ru.gigabyteartur.weights_of_war.Sensors.SensorWoundedAllyDown;
import ru.gigabyteartur.weights_of_war.Sensors.SensorWoundedAllyLeft;
import ru.gigabyteartur.weights_of_war.Sensors.SensorWoundedAllyRight;
import ru.gigabyteartur.weights_of_war.Sensors.SensorWoundedAllyUp;
import ru.gigabyteartur.weights_of_war.neuro_net.DenseLayer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;

import java.util.ArrayList;

// Юнит-жрец: поддержка фракции, канально лечит раненых союзников, расходуя ману.
public class UnitPriest extends BattleUnitCommon
{
    public final static int DEFAULT_WIDTH = 25;             // Ширина юнита.
    public final static int DEFAULT_HEIGHT = 25;            // Высота юнита.
    public final static int DEFAULT_HP = 80;                // Здоровье юнита по умолчанию.
    public final static int DEFAULT_SPEED = 90;             // Скорость юнита по умолчанию.
    public final static int DEFAULT_DAMAGE = 0;             // Урон.
    public final static int DEFAULT_ATTACK_RANGE = 0;       // Дальность атаки.
    public final static int DEFAULT_SIGHT_RANGE = 150;      // Дальность обзора; он же радиус лечения.
    public final static int DEFAULT_ARMOR = 0;              // Броня юнита по умолчанию.

    public final static float HEAL_PER_SECOND = 2f;         // Скорость лечения, HP/сек.
    public final static float MANA_PER_HEAL = 1f;           // Расход маны за 1 HP лечения.
    public final static float MANA_MAX = 2f * UnitSwordman.DEFAULT_HP * MANA_PER_HEAL;              // Максимальная мана (200).
    public final static float MANA_REGEN_PER_SECOND = MANA_MAX / (2f * BuildingCore.PRODUCTION_INTERVAL); // Реген маны (≈6.67).
    public final static int MAX_HEALERS_PER_TARGET = 3;     // Максимум жрецов, одновременно лечащих одну цель.
    public final static float HEAL_INTERRUPT_DURATION = 0.5f; // Длительность прерывания лечения при получении урона, сек.

    private float mana = MANA_MAX;               // Текущая мана.
    private float manaRemainder = 0f;            // Накопленная дробная мана для регена.
    private boolean healingThisTick = false;     // Лечил ли жрец в текущем тике.
    private BattleUnitCommon healTarget;         // Текущая цель лечения.
    private float healInterruptTimer = 0f;       // Оставшееся время прерывания лечения после урона.

    public UnitPriest()
    {
        super();
        Init();
    }

    public UnitPriest(int x, int y)
    {
        super(x, y);
        Init();
    }

    @Override
    public void SetMainTexture()
    {
        mainTexture = TextureCache.GetTexture("priest.png");
    }

    // Текущее количество маны.
    public float getMana()
    {
        return mana;
    }

    // Устанавливает ману, ограничивая диапазоном [0, MANA_MAX].
    public void setMana(float mana)
    {
        if (mana < 0)
        {
            mana = 0;
        }
        if (mana > MANA_MAX)
        {
            mana = MANA_MAX;
        }
        this.mana = mana;
    }

    // Процент маны от 0 до 100.
    public int getManaPercent()
    {
        if (MANA_MAX <= 0)
        {
            return 0;
        }
        return (int) Math.round((mana * 100.0) / MANA_MAX);
    }

    // Помечает, что жрец лечил в текущем тике (блокирует реген маны).
    public void setHealingThisTick(boolean healingThisTick)
    {
        this.healingThisTick = healingThisTick;
    }

    // Текущая цель лечения (или null, если жрец никого не лечит).
    public BattleUnitCommon getHealTarget()
    {
        return healTarget;
    }

    // Устанавливает текущую цель лечения.
    public void setHealTarget(BattleUnitCommon healTarget)
    {
        this.healTarget = healTarget;
    }

    // Прервано ли лечение (жрец получил урон и ещё не может лечить).
    public boolean isHealInterrupted()
    {
        return healInterruptTimer > 0f;
    }

    // При получении урона прерываем лечение на заданное время.
    @Override
    public void MarkAttacked()
    {
        super.MarkAttacked();
        healInterruptTimer = HEAL_INTERRUPT_DURATION;
    }

    // Обновление: реген маны идёт только когда жрец в этом тике не лечил.
    @Override
    public void Update(GameWorld world)
    {
        healingThisTick = false;
        super.Update(world);

        if (IsDead())
        {
            return;
        }

        // Убывание таймера прерывания лечения.
        if (healInterruptTimer > 0f)
        {
            healInterruptTimer = Math.max(0f, healInterruptTimer - Gdx.graphics.getDeltaTime() * world.getGameSpeed());
        }

        if (!healingThisTick)
        {
            manaRemainder += MANA_REGEN_PER_SECOND * Gdx.graphics.getDeltaTime() * world.getGameSpeed();
            int regen = (int) manaRemainder;
            if (regen > 0)
            {
                manaRemainder -= regen;
                setMana(mana + regen);
            }
        }
    }

    // Сенсоры жреца: базовые 26 + 4 направления к раненым союзникам + процент маны.
    @Override
    public ArrayList<SensorCommon> CreateSensors()
    {
        ArrayList<SensorCommon> sensors = super.CreateSensors();
        sensors.add(new SensorWoundedAllyUp());
        sensors.add(new SensorWoundedAllyRight());
        sensors.add(new SensorWoundedAllyDown());
        sensors.add(new SensorWoundedAllyLeft());
        sensors.add(new SensorOwnMana());
        return sensors;
    }

    // Формирует нейросеть жреца: вход 31, 2 скрытых полносвязных слоя по 20, выход 11.
    public static NeuroNet CreateNeuroNetPriest()
    {
        NeuroNet neuroNet = new NeuroNet();

        // Входной слой.
        neuroNet.GenerateAddLayer(31, true, false);

        // Скрытые полносвязные слои.
        for (int i = 0; i < 2; i++)
        {
            DenseLayer hiddenLayer = new DenseLayer();
            hiddenLayer.GenerateLayer(20, false, false);
            neuroNet.AddLayer(hiddenLayer);
        }

        // Выходной слой.
        neuroNet.GenerateAddLayer(11, false, true);
        neuroNet.Compile();
        neuroNet.RandomWeights();

        return neuroNet;
    }

    // Отрисовка: базовая + полоска маны.
    @Override
    public void Show(SpriteBatch batch)
    {
        DrawHealLine(batch);
        super.Show(batch);
        DrawManaBar(batch);
    }

    // Отрисовка синей полоски маны под полоской здоровья.
    private void DrawManaBar(SpriteBatch batch)
    {
        float barWidth = getWidth();
        float barHeight = 5f;
        float barX = getX();
        float barY = getY() - barHeight - 3f - barHeight - 3f;   // ниже полоски здоровья.

        float manaRatio = (MANA_MAX > 0) ? mana / MANA_MAX : 0f;
        if (manaRatio < 0f)
        {
            manaRatio = 0f;
        }
        if (manaRatio > 1f)
        {
            manaRatio = 1f;
        }

        // Тёмный фон полоски.
        batch.setColor(0.2f, 0.2f, 0.2f, 1f);
        batch.draw(getWhitePixel(), barX, barY, barWidth, barHeight);

        // Синяя часть — текущая мана.
        batch.setColor(Color.BLUE);
        batch.draw(getWhitePixel(), barX, barY, barWidth * manaRatio, barHeight);

        batch.setColor(Color.WHITE);
    }

    // Отрисовка пунктирной линии от жреца к текущей цели лечения — только в режиме отладки.
    private void DrawHealLine(SpriteBatch batch)
    {
        if (!GameWorld.getDebugMode())
        {
            return;
        }
        if (!(GetCurrentCommand() instanceof CommandHealNearestWounded))
        {
            return;
        }
        DrawDottedLineTo(batch, healTarget, Color.GREEN);
    }

    // Установка размеров по умолчанию.
    private void SetDefaultSize()
    {
        this.setWidthHeight(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    // Установка здоровья по умолчанию.
    private void SetDefaultHP()
    {
        this.setHealth(DEFAULT_HP);
    }

    private void SetDefaultSpeed()
    {
        this.setSpeed(DEFAULT_SPEED);
    }

    private void SetDefaultDamage()
    {
        this.setDamage(DEFAULT_DAMAGE);
    }

    private void SetDefaultRange()
    {
        this.setAttackRange(DEFAULT_ATTACK_RANGE);
    }

    private void SetDefaultSightRange()
    {
        this.setSightRange(DEFAULT_SIGHT_RANGE);
    }

    private void SetDefaultArmor()
    {
        this.setArmor(DEFAULT_ARMOR);
    }

    // Инициализация юнита при создании.
    private void Init()
    {
        SetMainTexture();
        ResetDefaults();
    }

    // Переинициализирует юнита на новой позиции (для переиспользования мёртвого объекта).
    public void Reset(int x, int y)
    {
        setXY(x, y);
        ClearCommands();
        ResetDefaults();
    }

    // Сбрасывает боевые параметры юнита к значениям по умолчанию.
    private void ResetDefaults()
    {
        SetDefaultSize();
        SetDefaultHP();
        SetDefaultSpeed();
        SetDefaultDamage();
        SetDefaultRange();
        SetDefaultSightRange();
        SetDefaultArmor();
        this.setMaxHealth(getHealth());
        this.mana = MANA_MAX;
        this.manaRemainder = 0f;
        this.healTarget = null;
        this.healInterruptTimer = 0f;
    }

    // Цвет фона класса: жёлтый.
    @Override
    public Color GetClassColor()
    {
        return Color.YELLOW;
    }

    // Название класса.
    @Override
    public String GetClassName()
    {
        return "Жрец";
    }

    // Приспособленность жреца растёт только за фактически вылеченные HP.
    @Override
    public void IncreaseFit(int value, FitType fit_type)
    {
        if (fit_type == FitType.HEALED_ALLIES)
        {
            this.setFit(this.getFit() + value);
        }
        else
        {
            // Не поддерживаемый тип приспособленности для жреца.
        }
    }
}
