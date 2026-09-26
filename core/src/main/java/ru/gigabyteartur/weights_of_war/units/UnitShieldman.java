package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.graphics.Color;
import ru.gigabyteartur.weights_of_war.TextureCache;

// Юнит-щитовик.
public class UnitShieldman extends BattleUnitCommon
{
    public final static int DEFAULT_WIDTH = 25;        // Размеры юнита по умолчанию.
    public final static int DEFAULT_HEIGHT = 25;       // Размеры юнита по умолчанию.
    public final static int DEFAULT_HP = 100;          // Здоровье юнита по умолчанию.
    public final static int DEFAULT_SPEED = 80;        // Скорость юнита по умолчанию.
    public final static int DEFAULT_DAMAGE = 12;        // Урон юнита по умолчанию.
    public final static int DEFAULT_ATTACK_RANGE = 10; // Дальность.
    public final static int DEFAULT_SIGHT_RANGE = 150;  // Дальность обзора по умолчанию.
    public final static int DEFAULT_ARMOR = 6;         // Броня юнита по умолчанию.
    public final static float DEFAULT_SEPARATION_RADIUS = 50f;     // Радиус отталкивания от союзников (boids).
    public final static float DEFAULT_SEPARATION_WEIGHT = 1.0f;    // Вес отталкивания.
    public final static float DEFAULT_COHESION_WEIGHT = 0.30f;     // Вес притяжения к центру масс.
    public final static float DEFAULT_ALIGNMENT_WEIGHT = 0.20f;    // Вес выравнивания по соседям.

    public UnitShieldman()
    {
        super();
        Init();
    }

    public UnitShieldman(int x, int y)
    {
        super(x, y);
        Init();
    }

    @Override
    public void SetMainTexture()
    {
        mainTexture = TextureCache.GetTexture("shieldman.png");
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
    }

    // Цвет фона класса: голубой.
    @Override
    public Color GetClassColor()
    {
        return Color.CYAN;
    }

    // Название класса.
    @Override
    public String GetClassName()
    {
        return "Щитовик";
    }

    // Повышает приспособленность на величину нанесённого урона.
    @Override
    public void IncreaseFit(int value, FitType fit_type)
    {
        if (fit_type == FitType.DAMAGE_TO_UNITS)
            this.setFit((int) (this.getFit() + Math.ceil(0.5 * value)));
        else if (fit_type == FitType.DAMAGE_TO_BUILDINGS)
            this.setFit(this.getFit() + value * 5);
        else if (fit_type == FitType.BLOCKED_ENEMY_DAMAGE)
            this.setFit((int) (this.getFit() + Math.ceil(1.5 * value)));
        else if (fit_type == FitType.KILLED_ENEMY_UNIT)
            this.setFit(this.getFit() + value);
        else
        {
            // Неизвестный тип приспособленности.
        }
    }
}
