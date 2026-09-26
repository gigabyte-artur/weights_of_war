package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.TextureCache;

// Юнит-разведчик: быстрый мобильный юнит, контролирующий видимость карты.
// Получает фитнесс за одиночную разведку — чем меньше союзников в поле зрения, тем выше прирост.
public class UnitScout extends BattleUnitCommon
{
    public final static int DEFAULT_WIDTH = 25;             // Ширина юнита.
    public final static int DEFAULT_HEIGHT = 25;            // Высота юнита.
    public final static int DEFAULT_HP = 100;               // Здоровье юнита по умолчанию.
    public final static int DEFAULT_SPEED = 130;            // Скорость юнита по умолчанию.
    public final static int DEFAULT_DAMAGE = 8;             // Урон юнита по умолчанию.
    public final static int DEFAULT_ATTACK_RANGE = 10;      // Дальность атаки.
    public final static int DEFAULT_SIGHT_RANGE = 250;      // Дальность обзора по умолчанию.
    public final static int DEFAULT_ARMOR = 2;              // Броня юнита по умолчанию.
    public final static float DEFAULT_SEPARATION_RADIUS = 50f;    // Радиус отталкивания от союзников (boids).
    public final static float DEFAULT_SEPARATION_WEIGHT = 1.2f;   // Вес отталкивания.
    public final static float DEFAULT_COHESION_WEIGHT = 0.15f;    // Вес притяжения к центру масс.
    public final static float DEFAULT_ALIGNMENT_WEIGHT = 0.10f;   // Вес выравнивания по соседям.

    public final static float FITNESS_GAIN_PER_SECOND = 1f;   // Базовый прирост фитнесса, ед/сек.
    public final static int MAX_ALLIES_FOR_FITNESS = 10;      // Максимум союзников в поле зрения, при котором ещё начисляется фитнесс.

    private float fitnessRemainder = 0f;    // Накопленный дробный прирост фитнесса.

    public UnitScout()
    {
        super();
        Init();
    }

    public UnitScout(int x, int y)
    {
        super(x, y);
        Init();
    }

    @Override
    public void SetMainTexture()
    {
        mainTexture = TextureCache.GetTexture("scout.png");
    }

    // Обновление: базовая логика + накопление фитнесса за одиночную разведку.
    @Override
    public void Update(GameWorld world)
    {
        super.Update(world);
        if (IsDead())
        {
            return;
        }
        ApplyFitnessGain(Gdx.graphics.getDeltaTime() * world.getGameSpeed(), world);
    }

    // Начисляет фитнесс за разведку: базовый прирост 1/сек, каждый союзник в поле зрения
    // уменьшает его в 2 раза. При более чем MAX_ALLIES_FOR_FITNESS союзниках прирост нулевой.
    public void ApplyFitnessGain(float deltaSeconds, GameWorld world)
    {
        int allyCount = world.CountAllies(this, getSightRange());
        if (allyCount > MAX_ALLIES_FOR_FITNESS)
        {
            return;
        }

        double rate = FITNESS_GAIN_PER_SECOND / Math.pow(1.5, allyCount);
        fitnessRemainder += (float) (rate * deltaSeconds);
        int gain = (int) fitnessRemainder;
        if (gain > 0)
        {
            fitnessRemainder -= gain;
            IncreaseFit(gain, FitType.SCOUTING);
        }
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

    private void SetDefaultBoids()
    {
        this.setSeparationRadius(DEFAULT_SEPARATION_RADIUS);
        this.setSeparationWeight(DEFAULT_SEPARATION_WEIGHT);
        this.setCohesionWeight(DEFAULT_COHESION_WEIGHT);
        this.setAlignmentWeight(DEFAULT_ALIGNMENT_WEIGHT);
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
        SetDefaultBoids();
        this.setMaxHealth(getHealth());
        this.fitnessRemainder = 0f;
    }

    // Цвет фона класса: пурпурный.
    @Override
    public Color GetClassColor()
    {
        return Color.MAGENTA;
    }

    // Название класса.
    @Override
    public String GetClassName()
    {
        return "Разведчик";
    }

    // Разведчик получает фитнесс только за разведку и урон по зданиям.
    @Override
    public void IncreaseFit(int value, FitType fit_type)
    {
        if (fit_type == FitType.SCOUTING)
            this.setFit(this.getFit() + value);
        else if (fit_type == FitType.DAMAGE_TO_BUILDINGS)
            this.setFit(this.getFit() + value * 5);
        else
        {
            // за остальные виды приспособленности разведчик фитнесса не получает.
        }
    }
}
