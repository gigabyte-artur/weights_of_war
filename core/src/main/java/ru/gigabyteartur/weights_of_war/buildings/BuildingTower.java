package ru.gigabyteartur.weights_of_war.buildings;

import com.badlogic.gdx.Gdx;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.TextureCache;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Сторожевая башня фракции: автоматически атакует вражеских юнитов и возрождается после разрушения.
public class BuildingTower extends BattleBuildingCommon
{
    public final static int DEFAULT_WIDTH = 60;          // Ширина башни.
    public final static int DEFAULT_HEIGHT = 60;         // Высота башни.
    public final static int DEFAULT_MAX_HEALTH = 300;    // Запас здоровья башни.
    public final static int DEFAULT_SIGHT_RANGE = 300;   // Радиус обзора башни, px.
    public final static int ATTACK_DAMAGE = 20;          // Урон за выстрел.
    public final static int ATTACK_RANGE = 300;          // Радиус атаки, px.
    public final static float ATTACK_INTERVAL = 1f;      // Интервал между выстрелами, сек.
    public final static float RESPAWN_TIME = 180f;       // Время до возрождения после разрушения, сек (3 минуты).

    private float attackTimer = 0f;                      // Таймер между выстрелами.
    private float respawnTimer = 0f;                     // Таймер возрождения после разрушения.

    public BuildingTower()
    {
        super();
        SetDefaultSize();
    }

    public BuildingTower(int x, int y)
    {
        super(x, y);
        SetDefaultSize();
    }

    @Override
    public void SetMainTexture()
    {
        mainTexture = TextureCache.GetTexture("tower.png");
    }

    @Override
    public String GetClassName()
    {
        return "Сторожевая башня";
    }

    @Override
    public void Update(GameWorld world)
    {
        // Разрушенная башня возрождается через RESPAWN_TIME секунд,
        // но только если ядро её фракции ещё не разрушено.
        if (IsDead())
        {
            if (!CanRespawn(world))
            {
                return;
            }
            respawnTimer += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
            if (respawnTimer >= RESPAWN_TIME)
            {
                setHealth(getMaxHealth());
                respawnTimer = 0f;
                attackTimer = 0f;
            }
            return;
        }

        // Автоатака ближайшего вражеского юнита в радиусе действия.
        attackTimer += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        if (attackTimer >= ATTACK_INTERVAL)
        {
            attackTimer -= ATTACK_INTERVAL;
            AttackNearestEnemy(world);
        }
    }

    // Башня может возродиться, только если ядро её фракции живо.
    boolean CanRespawn(GameWorld world)
    {
        return world.FindOwnCore(this) != null;
    }

    // Наносит урон ближайшему вражескому юниту в радиусе атаки.
    private void AttackNearestEnemy(GameWorld world)
    {
        BattleUnitCommon target = FindTarget(world);
        if (target == null)
        {
            return;
        }
        target.setHealth(Math.max(0, target.getHealth() - ATTACK_DAMAGE));
        target.MarkAttacked();
    }

    // Находит ближайшего живого вражеского юнита в радиусе атаки.
    private BattleUnitCommon FindTarget(GameWorld world)
    {
        BattleUnitCommon nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject object : world.GetUnits())
        {
            if (!(object instanceof BattleUnitCommon) || object.IsDead())
            {
                continue;
            }
            BattleUnitCommon candidate = (BattleUnitCommon) object;
            if (getFraction() == null || candidate.getFraction() == null || !IsEnemy(candidate))
            {
                continue;
            }
            float distance = GetDistance(candidate);
            if (distance <= ATTACK_RANGE && distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Установка размеров и здоровья по умолчанию.
    private void SetDefaultSize()
    {
        setWidthHeight(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMaxHealth(DEFAULT_MAX_HEALTH);
        setHealth(DEFAULT_MAX_HEALTH);
        setSightRange(DEFAULT_SIGHT_RANGE);
    }
}
