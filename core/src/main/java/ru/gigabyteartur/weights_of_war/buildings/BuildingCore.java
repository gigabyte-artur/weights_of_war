package ru.gigabyteartur.weights_of_war.buildings;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.TextureCache;
import ru.gigabyteartur.weights_of_war.commands.CommandThink;
import ru.gigabyteartur.weights_of_war.evo.Evolution;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

// Ядро фракции: производит юнитов.
public class BuildingCore extends BattleBuildingCommon
{
    public final static int DEFAULT_WIDTH = 100;         // Ширина ядра по умолчанию.
    public final static int DEFAULT_HEIGHT = 100;        // Высота ядра по умолчанию.
    public final static int DEFAULT_MAX_HEALTH = 1000;   // Максимальное здоровье ядра.
    public final static int DEFAULT_SIGHT_RANGE = 400;   // Радиус обзора ядра, px.
    public final static float PRODUCTION_INTERVAL = 60f; // Интервал производства, сек.
    public final static int SWORDMEN_COUNT = 10;          // Мечников за один раз.
    public final static int ARCHERS_COUNT = 10;           // Лучников за один раз.
    public final static int SHIELDMEN_COUNT = 10;         // Щитовиков за один раз.
    public final static int PRIESTS_COUNT = 10;           // Жрецов за один раз.
    public final static int SCOUTS_COUNT = 10;            // Разведчиков за один раз.
    public final static int SPAWN_RADIUS = 400;           // Радиус случайного спавна юнитов от ядра, px.
    public final static int CENTER_SPAWN_RADIUS = 500;    // Радиус спавна юнитов от центра карты, px.
    public final static double CENTER_SPAWN_RATE = 0.0;   // Доля юнитов, спавнящихся в центре карты.
    public final static int STAGNATION_WAVES = 10;            // Волн без роста фита до смерти.
    public final static double ENEMY_GENE_TRANSFER_RATE = 0.03; // Вероятность переноса генов лучшего врага в следующее поколение.
    public final static double TOWER_DESTROYED_SKIP_RATE = 0.3;  // Вероятность пропуска генерации юнитов при разрушенной башне.
    public final static int LIMIT_BEST_UNITS = 5;                // Лимит лучших юнитов в пуле.

    private float productionTimer = 0f;                  // Таймер производства.
    private int GenerationNumber = 0;                    // Номер генерации.
    private ArrayList<NeuroNet> NetSwordmen = new ArrayList<>();    // Пул лучших нейросетей мечников (до трёх чемпионов).
    private ArrayList<NeuroNet> NetArchers = new ArrayList<>();     // Пул лучших нейросетей лучников.
    private ArrayList<NeuroNet> NetShieldmen = new ArrayList<>();   // Пул лучших нейросетей щитовиков.
    private ArrayList<NeuroNet> NetPriests = new ArrayList<>();     // Пул лучших нейросетей жрецов.
    private ArrayList<NeuroNet> NetScouts = new ArrayList<>();      // Пул лучших нейросетей разведчиков.
    private ArrayList<Integer> NetSwordmenFits = new ArrayList<>(); // Фиты чемпионов мечников (по убыванию).
    private ArrayList<Integer> NetArchersFits = new ArrayList<>();  // Фиты чемпионов лучников.
    private ArrayList<Integer> NetShieldmenFits = new ArrayList<>();// Фиты чемпионов щитовиков.
    private ArrayList<Integer> NetPriestsFits = new ArrayList<>();  // Фиты чемпионов жрецов.
    private ArrayList<Integer> NetScoutsFits = new ArrayList<>();   // Фиты чемпионов разведчиков.
    private Random random = new Random();                // Генератор случайных чисел.
    private Evolution evolutionAlgo = new Evolution();                     // Эволюционные алгоритмы.

    public BuildingCore()
    {
        super();
        SetDefaultSize();
    }

    public BuildingCore(int x, int y)
    {
        super(x, y);
        SetDefaultSize();
    }

    @Override
    public void SetMainTexture()
    {
        mainTexture = TextureCache.GetTexture("core.png");
    }

    // Название класса.
    @Override
    public String GetClassName()
    {
        return "Ядро";
    }

    @Override
    public void Update(GameWorld world)
    {
        // Разрушенное ядро не производит юнитов.
        if (IsDead())
        {
            return;
        }

        productionTimer += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        if (productionTimer >= PRODUCTION_INTERVAL)
        {
            productionTimer -= PRODUCTION_INTERVAL;
            ProduceUnits(world);
        }
    }

    // Производит отряд юнитов рядом с ядром и отправляет их в бой.
    // Для нового юнита сначала переиспользуется мёртвый объект того же класса.
    void ProduceUnits(GameWorld world)
    {
        // Если сторожевая башня фракции разрушена, с вероятностью TOWER_DESTROYED_SKIP_RATE
        // пропускаем генерацию нового поколения юнитов.
        if (world.IsTowerDestroyed(getFraction()) && random.nextDouble() < TOWER_DESTROYED_SKIP_RATE)
        {
            return;
        }

        GenerationNumber++;
        // При первой генерации создаём нейросети для каждого типа юнитов,
        // если они ещё не загружены из сохранения.
        if (GenerationNumber == 1)
        {
            if (NetSwordmen.isEmpty())
            {
                NetSwordmen.add(BattleUnitCommon.CreateNeuroNetUnit());
                NetSwordmenFits.add(0);
            }
            if (NetArchers.isEmpty())
            {
                NetArchers.add(BattleUnitCommon.CreateNeuroNetUnit());
                NetArchersFits.add(0);
            }
            if (NetShieldmen.isEmpty())
            {
                NetShieldmen.add(BattleUnitCommon.CreateNeuroNetUnit());
                NetShieldmenFits.add(0);
            }
            if (NetPriests.isEmpty())
            {
                NetPriests.add(UnitPriest.CreateNeuroNetPriest());
                NetPriestsFits.add(0);
            }
            if (NetScouts.isEmpty())
            {
                NetScouts.add(BattleUnitCommon.CreateNeuroNetUnit());
                NetScoutsFits.add(0);
            }
        }
        // Юниты с неизменным фитом умирают.
        world.CheckUnitsStagnation(STAGNATION_WAVES);

        // Эволюция для мечников.
        ArrayList<UnitSwordman> topSwordmen = world.GetTopAliveUnits(UnitSwordman.class, LIMIT_BEST_UNITS, getFraction());
        UnitSwordman enemySwordman = (random.nextDouble() < ENEMY_GENE_TRANSFER_RATE) ? world.GetBestEnemyUnit(UnitSwordman.class, getFraction()) : null;
        ArrayList<NeuroNet> swordmenOffspring = evolutionAlgo.GenerateOffspring(topSwordmen, NetSwordmen, SWORDMEN_COUNT, enemySwordman);
        evolutionAlgo.UpdateCoreNets(topSwordmen, NetSwordmen, NetSwordmenFits);

        for (int i = 0; i < SWORDMEN_COUNT; i++)
        {
            int[] position = FindSpawnPosition(world, UnitSwordman.DEFAULT_WIDTH, UnitSwordman.DEFAULT_HEIGHT);
            UnitSwordman unit = world.FindDeadUnit(UnitSwordman.class);
            if (unit == null)
            {
                unit = new UnitSwordman(position[0], position[1]);
                world.AddUnit(unit);
            }
            else
            {
                unit.Reset(position[0], position[1]);
            }
            unit.setFraction(getFraction());
            unit.setGenerationNumber(GenerationNumber);
            unit.ResetUnitState();
            unit.setNeuroNet(swordmenOffspring.get(i));
            unit.AddCommand(new CommandThink(unit.CreateSensors()));
        }

        // Эволюция для лучников.
        ArrayList<UnitArcher> topArchers = world.GetTopAliveUnits(UnitArcher.class, LIMIT_BEST_UNITS, getFraction());
        UnitArcher enemyArcher = (random.nextDouble() < ENEMY_GENE_TRANSFER_RATE) ? world.GetBestEnemyUnit(UnitArcher.class, getFraction()) : null;
        ArrayList<NeuroNet> archersOffspring = evolutionAlgo.GenerateOffspring(topArchers, NetArchers, ARCHERS_COUNT, enemyArcher);
        evolutionAlgo.UpdateCoreNets(topArchers, NetArchers, NetArchersFits);

        for (int i = 0; i < ARCHERS_COUNT; i++)
        {
            int[] position = FindSpawnPosition(world, UnitArcher.DEFAULT_WIDTH, UnitArcher.DEFAULT_HEIGHT);
            UnitArcher unit = world.FindDeadUnit(UnitArcher.class);
            if (unit == null)
            {
                unit = new UnitArcher(position[0], position[1]);
                world.AddUnit(unit);
            }
            else
            {
                unit.Reset(position[0], position[1]);
            }
            unit.setFraction(getFraction());
            unit.setGenerationNumber(GenerationNumber);
            unit.ResetUnitState();
            unit.setNeuroNet(archersOffspring.get(i));
            unit.AddCommand(new CommandThink(unit.CreateSensors()));
        }

        // Эволюция для щитовиков.
        ArrayList<UnitShieldman> topShieldmen = world.GetTopAliveUnits(UnitShieldman.class, LIMIT_BEST_UNITS, getFraction());
        UnitShieldman enemyShieldman = (random.nextDouble() < ENEMY_GENE_TRANSFER_RATE) ? world.GetBestEnemyUnit(UnitShieldman.class, getFraction()) : null;
        ArrayList<NeuroNet> shieldmenOffspring = evolutionAlgo.GenerateOffspring(topShieldmen, NetShieldmen, SHIELDMEN_COUNT, enemyShieldman);
        evolutionAlgo.UpdateCoreNets(topShieldmen, NetShieldmen, NetShieldmenFits);

        for (int i = 0; i < SHIELDMEN_COUNT; i++)
        {
            int[] position = FindSpawnPosition(world, UnitShieldman.DEFAULT_WIDTH, UnitShieldman.DEFAULT_HEIGHT);
            UnitShieldman unit = world.FindDeadUnit(UnitShieldman.class);
            if (unit == null)
            {
                unit = new UnitShieldman(position[0], position[1]);
                world.AddUnit(unit);
            }
            else
            {
                unit.Reset(position[0], position[1]);
            }
            unit.setFraction(getFraction());
            unit.setGenerationNumber(GenerationNumber);
            unit.ResetUnitState();
            unit.setNeuroNet(shieldmenOffspring.get(i));
            unit.AddCommand(new CommandThink(unit.CreateSensors()));
        }

        // Эволюция для жрецов.
        ArrayList<UnitPriest> topPriests = world.GetTopAliveUnits(UnitPriest.class, LIMIT_BEST_UNITS, getFraction());
        UnitPriest enemyPriest = (random.nextDouble() < ENEMY_GENE_TRANSFER_RATE) ? world.GetBestEnemyUnit(UnitPriest.class, getFraction()) : null;
        ArrayList<NeuroNet> priestsOffspring = evolutionAlgo.GenerateOffspring(topPriests, NetPriests, PRIESTS_COUNT, enemyPriest);
        evolutionAlgo.UpdateCoreNets(topPriests, NetPriests, NetPriestsFits);

        for (int i = 0; i < PRIESTS_COUNT; i++)
        {
            int[] position = FindSpawnPosition(world, UnitPriest.DEFAULT_WIDTH, UnitPriest.DEFAULT_HEIGHT);
            UnitPriest unit = world.FindDeadUnit(UnitPriest.class);
            if (unit == null)
            {
                unit = new UnitPriest(position[0], position[1]);
                world.AddUnit(unit);
            }
            else
            {
                unit.Reset(position[0], position[1]);
            }
            unit.setFraction(getFraction());
            unit.setGenerationNumber(GenerationNumber);
            unit.ResetUnitState();
            unit.setNeuroNet(priestsOffspring.get(i));
            unit.AddCommand(new CommandThink(unit.CreateSensors()));
        }

        // Эволюция для разведчиков.
        ArrayList<UnitScout> topScouts = world.GetTopAliveUnits(UnitScout.class, LIMIT_BEST_UNITS, getFraction());
        UnitScout enemyScout = (random.nextDouble() < ENEMY_GENE_TRANSFER_RATE) ? world.GetBestEnemyUnit(UnitScout.class, getFraction()) : null;
        ArrayList<NeuroNet> scoutsOffspring = evolutionAlgo.GenerateOffspring(topScouts, NetScouts, SCOUTS_COUNT, enemyScout);
        evolutionAlgo.UpdateCoreNets(topScouts, NetScouts, NetScoutsFits);

        for (int i = 0; i < SCOUTS_COUNT; i++)
        {
            int[] position = FindSpawnPosition(world, UnitScout.DEFAULT_WIDTH, UnitScout.DEFAULT_HEIGHT);
            UnitScout unit = world.FindDeadUnit(UnitScout.class);
            if (unit == null)
            {
                unit = new UnitScout(position[0], position[1]);
                world.AddUnit(unit);
            }
            else
            {
                unit.Reset(position[0], position[1]);
            }
            unit.setFraction(getFraction());
            unit.setGenerationNumber(GenerationNumber);
            unit.ResetUnitState();
            unit.setNeuroNet(scoutsOffspring.get(i));
            unit.AddCommand(new CommandThink(unit.CreateSensors()));
        }
    }

    // Генерирует случайную свободную позицию для юнита: 70% — в центре карты, остальные — у ядра.
    private int[] FindSpawnPosition(GameWorld world, int width, int height)
    {
        if (random.nextDouble() < CENTER_SPAWN_RATE)
        {
            return FindSpawnAtMapCenter(world, width, height);
        }
        return FindSpawnAtCore(world, width, height);
    }

    // Случайная позиция в радиусе CENTER_SPAWN_RADIUS от центра игровой области.
    private int[] FindSpawnAtMapCenter(GameWorld world, int width, int height)
    {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double radius = CENTER_SPAWN_RADIUS * Math.sqrt(random.nextDouble());
        int centerX = (GameWorld.COMMAND_PANEL_WIDTH + Gdx.graphics.getWidth()) / 2;
        int centerY = Gdx.graphics.getHeight() / 2;
        int spawnX = centerX + (int) Math.round(radius * Math.cos(angle));
        int spawnY = centerY + (int) Math.round(radius * Math.sin(angle));
        return world.FindFreePosition(spawnX, spawnY, width, height);
    }

    // Случайная позиция в радиусе SPAWN_RADIUS от центра ядра.
    private int[] FindSpawnAtCore(GameWorld world, int width, int height)
    {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double radius = SPAWN_RADIUS * Math.sqrt(random.nextDouble());
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;
        int spawnX = centerX + (int) Math.round(radius * Math.cos(angle));
        int spawnY = centerY + (int) Math.round(radius * Math.sin(angle));
        return world.FindFreePosition(spawnX, spawnY, width, height);
    }

    // Установка размеров и здоровья по умолчанию.
    private void SetDefaultSize()
    {
        this.setWidthHeight(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setMaxHealth(DEFAULT_MAX_HEALTH);
        this.setHealth(DEFAULT_MAX_HEALTH);
        this.setSightRange(DEFAULT_SIGHT_RANGE);
    }

    // Устанавливает нейросети для всех типов юнитов (для загрузки из сохранения).
    // Каждая переданная сеть становится первым (пока единственным) чемпионом своего пула.
    public void SetNeuroNets(NeuroNet swordman_in, NeuroNet archer_in, NeuroNet shieldman_in, NeuroNet priest_in, NeuroNet scout_in)
    {
        SetNetPool(NetSwordmen, NetSwordmenFits, swordman_in);
        SetNetPool(NetArchers, NetArchersFits, archer_in);
        SetNetPool(NetShieldmen, NetShieldmenFits, shieldman_in);
        SetNetPool(NetPriests, NetPriestsFits, priest_in);
        SetNetPool(NetScouts, NetScoutsFits, scout_in);
    }

    // Кладёт нейросеть в пул чемпионов (если она не null), сбрасывая прежнее содержимое.
    private void SetNetPool(ArrayList<NeuroNet> nets, ArrayList<Integer> fits, NeuroNet net)
    {
        nets.clear();
        fits.clear();
        if (net != null)
        {
            nets.add(net);
            fits.add(0);
        }
    }

    public int getGenerationNumber()
    {
        return GenerationNumber;
    }

    // Фитнесс лучшей сохранённой в ядре нейросети мечников.
    public int getNetSwordmanFit()
    {
        return NetSwordmenFits.isEmpty() ? 0 : NetSwordmenFits.get(0);
    }

    // Фитнесс лучшей сохранённой в ядре нейросети лучников.
    public int getNetArcherFit()
    {
        return NetArchersFits.isEmpty() ? 0 : NetArchersFits.get(0);
    }

    // Фитнесс лучшей сохранённой в ядре нейросети щитовиков.
    public int getNetShieldmanFit()
    {
        return NetShieldmenFits.isEmpty() ? 0 : NetShieldmenFits.get(0);
    }

    // Фитнесс лучшей сохранённой в ядре нейросети жрецов.
    public int getNetPriestFit()
    {
        return NetPriestsFits.isEmpty() ? 0 : NetPriestsFits.get(0);
    }

    // Фитнесс лучшей сохранённой в ядре нейросети разведчиков.
    public int getNetScoutFit()
    {
        return NetScoutsFits.isEmpty() ? 0 : NetScoutsFits.get(0);
    }
}
