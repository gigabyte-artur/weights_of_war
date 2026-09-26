package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.buildings.BuildingTower;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCommon;
import ru.gigabyteartur.weights_of_war.commands.CommandThink;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

// Игровой мир: хранит и обновляет все размещённые объекты.
public class GameWorld {

    public final static int COMMAND_PANEL_WIDTH = 300;                      // Ширина командной панели слева, px.
    public final static float FOG_UPDATE_INTERVAL = 0.1f;                   // Период обновления тумана войны, сек.
    public final static float NEURO_LOG_INTERVAL = 10f;                     // Период логирования нейросети, сек.

    // Границы игрового мира (поле битвы), за которые юнитам запрещено перемещаться.
    private int worldLeft;                                                  // Левая граница мира.
    private int worldBottom;                                                // Нижняя граница мира.
    private int worldRight;                                                 // Правая граница мира.
    private int worldTop;                                                   // Верхняя граница мира.

    private float gameSpeed = 1;                                            // Скорость игры.
    private boolean gameOver = false;                                       // Флаг завершения игры.
    private static boolean debugMode = false;                               // Режим отладки: отображение кругов обзора, линий атаки и лечения.
    private Fraction winnerFraction = null;                                 // Победившая фракция.
    private int maxGenerations = 0;                                        // Лимит поколений на партию (0 = без лимита).
    private ArrayList<PlacedObject> units = new ArrayList<PlacedObject>();             // Список размещённых объектов.
    private FogOfWar fogOfWar = new FogOfWar();                             // Туман войны.
    private float fogTimer = 0f;                                            // Таймер обновления тумана войны.
    private float neuroLogTimer = 0f;                                       // Таймер логирования нейросети.

    public GameWorld()
    {

    }

    public float getGameSpeed()
    {
        return gameSpeed;
    }

    public void setGameSpeed(float gameSpeed)
    {
        this.gameSpeed = gameSpeed;
    }

    // Устанавливает лимит поколений на партию (0 = без лимита).
    public void setMaxGenerations(int maxGenerations)
    {
        this.maxGenerations = maxGenerations;
    }

    // Возвращает, включён ли режим отладки.
    public static boolean getDebugMode()
    {
        return debugMode;
    }

    // Включает/выключает режим отладки.
    public static void setDebugMode(boolean debugMode)
    {
        GameWorld.debugMode = debugMode;
    }

    public int getWorldLeft()
    {
        return worldLeft;
    }

    public int getWorldBottom()
    {
        return worldBottom;
    }

    public int getWorldRight()
    {
        return worldRight;
    }

    public int getWorldTop()
    {
        return worldTop;
    }

    public void Show(SpriteBatch batch)
    {
        for (PlacedObject unit : units)
        {
            if (unit.IsDead())
            {
                continue;
            }

            // В режиме отладки рисуем круг обзора юнита до самого объекта.
            if (debugMode && unit instanceof BattleUnitCommon)
            {
                ((BattleUnitCommon) unit).DrawVisibilityCircle(batch);
            }

            unit.Show(batch);
        }
    }

    public void Init()
    {
        // Инициализация.
        int screenHeight = Gdx.graphics.getHeight();
        int screenWidth = Gdx.graphics.getWidth();
        // Границы мира: поле битвы занимает весь экран справа от командной панели.
        worldLeft = COMMAND_PANEL_WIDTH;
        worldBottom = 0;
        worldRight = screenWidth;
        worldTop = screenHeight;
        // Скорость игры.
        setGameSpeed(2);
        // Фракции.
        Fraction redFraction = new Fraction("Red", Color.RED);
        Fraction blueFraction = new Fraction("Blue", Color.BLUE);
        Fraction yellowFraction = new Fraction("Yellow", Color.YELLOW);
        Fraction greenFraction = new Fraction("Green", Color.FOREST);
        // Ядро красной фракции в правом верхнем углу.
        BuildingCore redCore = new BuildingCore(screenWidth - BuildingCore.DEFAULT_WIDTH, screenHeight - BuildingCore.DEFAULT_HEIGHT);
        redCore.setFraction(redFraction);
        units.add(redCore);
        // Ядро синей фракции в левом нижнем углу (справа от командной панели).
        BuildingCore blueCore = new BuildingCore(COMMAND_PANEL_WIDTH, BuildingCore.DEFAULT_WIDTH);
        blueCore.setFraction(blueFraction);
        units.add(blueCore);
        // Ядро желтой фракции в правом нижнем углу.
        BuildingCore yellowCore = new BuildingCore(screenWidth - BuildingCore.DEFAULT_WIDTH, BuildingCore.DEFAULT_WIDTH);
        yellowCore.setFraction(yellowFraction);
        units.add(yellowCore);
        // Ядро зелёной фракции в левом верхнем углу (справа от командной панели).
        BuildingCore greenCore = new BuildingCore(COMMAND_PANEL_WIDTH, screenHeight - BuildingCore.DEFAULT_HEIGHT);
        greenCore.setFraction(greenFraction);
        units.add(greenCore);
        // Сторожевые башни: по одной на фракцию, по диагонали от ядра в сторону центра карты.
        PlaceTower(redCore, redFraction);
        PlaceTower(blueCore, blueFraction);
        PlaceTower(yellowCore, yellowFraction);
        PlaceTower(greenCore, greenFraction);
        // Загружаем сохранённые нейросети (если есть) во все ядра.
        LoadSavedNeuroNets();
        // Строим начальную сетку тумана войны (до первого обновления).
        UpdateFogOfWar();
    }

    // Размещает сторожевую башню фракции по диагонали от ядра в сторону центра карты.
    private void PlaceTower(BuildingCore core, Fraction fraction)
    {
        int centerX = (COMMAND_PANEL_WIDTH + Gdx.graphics.getWidth()) / 2;
        int centerY = Gdx.graphics.getHeight() / 2;

        int coreCenterX = core.getX() + core.getWidth() / 2;
        int coreCenterY = core.getY() + core.getHeight() / 2;

        int dirX = (centerX > coreCenterX) ? 1 : -1;
        int dirY = (centerY > coreCenterY) ? 1 : -1;

        int offset = 300; // диагональный отступ от центра ядра.
        int towerX = coreCenterX + dirX * offset - BuildingTower.DEFAULT_WIDTH / 2;
        int towerY = coreCenterY + dirY * offset - BuildingTower.DEFAULT_HEIGHT / 2;

        BuildingTower tower = new BuildingTower(towerX, towerY);
        tower.setFraction(fraction);
        units.add(tower);
    }

    // Возвращает абсолютный путь к каталогу сохранений (общий для GUI и headless-режимов).
    private static String SavesDirectory()
    {
        return System.getProperty("user.home") + File.separator + ".WeightsOfWar" + File.separator + "saves";
    }

    // Загружает сохранённые нейросети из каталога сохранений во все ядра фракций.
    // Если файла нет — соответствующая нейросеть остаётся пустой и создастся случайно при первой волне.
    private void LoadSavedNeuroNets()
    {
        String dir = SavesDirectory();
        NeuroNet swordmanNet = LoadNetIfExists(dir + File.separator + "Swordman.xml");
        NeuroNet archerNet = LoadNetIfExists(dir + File.separator + "Archer.xml");
        NeuroNet shieldmanNet = LoadNetIfExists(dir + File.separator + "Shieldman.xml");
        NeuroNet priestNet = LoadNetIfExists(dir + File.separator + "Priest.xml");
        NeuroNet scoutNet = LoadNetIfExists(dir + File.separator + "Scout.xml");

        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore)
            {
                ((BuildingCore) object).SetNeuroNets(swordmanNet, archerNet, shieldmanNet, priestNet, scoutNet);
            }
        }
    }

    // Загружает нейросеть из файла, если он существует; иначе возвращает null.
    private NeuroNet LoadNetIfExists(String path)
    {
        File file = new File(path);
        if (!file.exists())
        {
            return null;
        }

        NeuroNet net = new NeuroNet();
        if (net.LoadFromFile(path))
        {
            return net;
        }
        // Версия файла устарела — сеть останется пустой и создастся случайно при первой волне.
        return null;
    }

    // Добавляет объект в мир (например, юнит, произведённый зданием).
    public void AddUnit(PlacedObject unit)
    {
        if (unit != null)
            units.add(unit);
    }

    // Возвращает список всех размещённых объектов мира.
    public ArrayList<PlacedObject> GetUnits()
    {
        return units;
    }

    // Немедленно перестраивает сетку тумана войны (используется при инициализации и в тестах).
    public void UpdateFogOfWar()
    {
        fogOfWar.Rebuild(units);
    }

    // Возвращает true, если объект видим заданной фракции (находится в её сетке тумана войны).
    public boolean IsVisible(Fraction fraction, PlacedObject object)
    {
        return fogOfWar.IsVisible(fraction, object);
    }

    // Возвращает случайного живого юнита с нейросетью (или null, если таких нет).
    private BattleUnitCommon PickRandomUnit()
    {
        ArrayList<BattleUnitCommon> aliveUnits = new ArrayList<BattleUnitCommon>();
        for (PlacedObject object : units)
        {
            if (object instanceof BattleUnitCommon && !object.IsDead() && ((BattleUnitCommon) object).getNeuroNet() != null)
            {
                aliveUnits.add((BattleUnitCommon) object);
            }
        }
        if (aliveUnits.isEmpty())
        {
            return null;
        }
        return aliveUnits.get((int) (Math.random() * aliveUnits.size()));
    }

    // Возвращает имена сенсоров юнита (в порядке входных нейронов).
    private ArrayList<String> GetSensorNames(BattleUnitCommon unit)
    {
        ArrayList<String> names = new ArrayList<String>();
        for (SensorCommon sensor : unit.CreateSensors())
        {
            names.add(sensor.GetName());
        }
        return names;
    }

    // Удаляет старые файлы логов, если их больше 100 (оставляет 100 самых свежих).
    private void CleanupNeuroLogs()
    {
        File dir = new File("neuro_logs");
        if (!dir.exists() || !dir.isDirectory())
        {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length <= 100)
        {
            return;
        }
        // Сортируем по времени изменения (старые первыми).
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File a, File b)
            {
                return Long.compare(a.lastModified(), b.lastModified());
            }
        });
        // Удаляем самые старые, оставляя 100 самых свежих.
        int toDelete = files.length - 100;
        for (int i = 0; i < toDelete; i++)
        {
            files[i].delete();
        }
    }

    // Формирует имя файла лога нейросети по текущей дате и времени.
    private String BuildNeuroLogFileName()
    {
        File dir = new File("neuro_logs");
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "neuro_logs/neuro_" + format.format(new Date()) + ".xml";
    }

    public void Update()
    {
        // Игра завершена — мир больше не обновляется.
        if (gameOver)
        {
            return;
        }

        // Обновление тумана войны раз в FOG_UPDATE_INTERVAL секунд (по реальному времени).
        fogTimer += Gdx.graphics.getDeltaTime();
        if (fogTimer >= FOG_UPDATE_INTERVAL)
        {
            fogTimer -= FOG_UPDATE_INTERVAL;
            fogOfWar.Rebuild(units);
        }

        // Логирование нейросети раз в NEURO_LOG_INTERVAL секунд.
        neuroLogTimer += Gdx.graphics.getDeltaTime();
        if (neuroLogTimer >= NEURO_LOG_INTERVAL)
        {
            neuroLogTimer -= NEURO_LOG_INTERVAL;
            BattleUnitCommon unit = PickRandomUnit();
            if (unit != null)
            {
                CleanupNeuroLogs();
                NeuroNet.ScheduleLogging(unit.getNeuroNet(), BuildNeuroLogFileName(), unit.GetClassName(), GetSensorNames(unit), CommandThink.GetCommandNames());
            }
        }

        // Цикл по индексу, чтобы здания могли добавлять юнитов прямо во время
        // обновления без ConcurrentModificationException.
        for (int i = 0; i < units.size(); i++)
            units.get(i).Update(this);

        // Завершение игры: остались здания только одной фракции, и прошла генерация второго поколения.
        if (CheckGameOver())
        {
            gameOver = true;
            winnerFraction = FindWinnerFraction();
            System.out.println("Game over.");
        }
        // Завершение игры по лимиту поколений (для headless-прогонов).
        else if (maxGenerations > 0 && GetMaxGeneration() >= maxGenerations)
        {
            gameOver = true;
            winnerFraction = FindWinnerByFitness();
            System.out.println("Game over (generation limit " + maxGenerations + " reached).");
        }
    }

    // Находит ближайшего живого врага (объекта другой фракции), видимого фракции юнита
    // через туман войны (совместное зрение фракции). Возвращает null, если таких врагов нет.
    public BattleObject FindNearestEnemy(BattleUnitCommon unit)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject object : units)
        {
            if (object == unit || !(object instanceof BattleObject) || object.IsDead())
            {
                continue;
            }
            BattleObject candidate = (BattleObject) object;
            // Пропускаем союзников (та же фракция) и объекты без фракции.
            if (unit.getFraction() == null || candidate.getFraction() == null || !unit.IsEnemy(candidate))
            {
                continue;
            }
            // Пропускаем врагов, не видимых фракции юнита (туман войны).
            if (!fogOfWar.IsVisible(unit.getFraction(), candidate))
            {
                continue;
            }
            float distance = unit.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшее живое здание противника (другой фракции), видимое фракции юнита.
    // Возвращает null, если таких зданий нет.
    public BattleBuildingCommon FindNearestEnemyBuilding(BattleUnitCommon unit)
    {
        BattleBuildingCommon nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject object : units)
        {
            if (object == unit || !(object instanceof BattleBuildingCommon) || object.IsDead())
            {
                continue;
            }
            BattleBuildingCommon candidate = (BattleBuildingCommon) object;
            // Пропускаем союзников (та же фракция) и объекты без фракции.
            if (unit.getFraction() == null || candidate.getFraction() == null || !unit.IsEnemy(candidate))
            {
                continue;
            }
            // Пропускаем здания, не видимые фракции юнита (туман войны).
            if (!fogOfWar.IsVisible(unit.getFraction(), candidate))
            {
                continue;
            }
            float distance = unit.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит живое ядро (BuildingCore) своей фракции для заданного боевого объекта.
    // Возвращает null, если своего ядра нет.
    public BattleBuildingCommon FindOwnCore(BattleObject unit)
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                BuildingCore building = (BuildingCore) object;
                if (unit.getFraction() != null && building.getFraction() == unit.getFraction())
                {
                    return building;
                }
            }
        }
        return null;
    }

    // Возвращает true, если сторожевая башня заданной фракции разрушена.
    // Если башни этой фракции нет — возвращает false (штраф не применяется).
    public boolean IsTowerDestroyed(Fraction fraction)
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingTower && ((BuildingTower) object).getFraction() == fraction)
            {
                return object.IsDead();
            }
        }
        return false;
    }

    // Находит ближайшего живого врага, расположенного выше (Y больше) заданного объекта.
    // Возвращает null, если такого врага нет.
    public BattleObject FindNearestEnemyUp(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться выше (большая Y-координата).
            if (candidate.getY() <= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого врага, расположенного справа (X больше) заданного объекта.
    // Возвращает null, если такого врага нет.
    public BattleObject FindNearestEnemyRight(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться справа (большая X-координата).
            if (candidate.getX() <= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого врага, расположенного ниже (Y меньше) заданного объекта.
    // Возвращает null, если такого врага нет.
    public BattleObject FindNearestEnemyDown(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться ниже (меньшая Y-координата).
            if (candidate.getY() >= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого врага, расположенного слева (X меньше) заданного объекта.
    // Возвращает null, если такого врага нет.
    public BattleObject FindNearestEnemyLeft(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться слева (меньшая X-координата).
            if (candidate.getX() >= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);

            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    // Возвращает количество живых врагов, расположенных выше (Y больше)
    // заданного объекта в пределах области видимости.
    public int CountEnemiesUp(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться выше (большая Y-координата).
            if (candidate.getY() > object.getY())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых врагов, расположенных ниже (Y меньше)
    // заданного объекта в пределах области видимости.
    public int CountEnemiesDown(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться ниже (меньшая Y-координата).
            if (candidate.getY() < object.getY())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых врагов, расположенных слева (X меньше)
    // заданного объекта в пределах области видимости.
    public int CountEnemiesLeft(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться слева (меньшая X-координата).
            if (candidate.getX() < object.getX())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых врагов, расположенных справа (X больше)
    // заданного объекта в пределах области видимости.
    public int CountEnemiesRight(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Враг должен находиться справа (большая X-координата).
            if (candidate.getX() > object.getX())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых врагов (объектов другой фракции)
    // в радиусе обзора object (полный круг, без ограничения направления).
    public int CountEnemies(BattleObject object, int sightRange)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindCandidate(object, unit, sightRange);
            if (candidate == null)
            {
                continue;
            }
            // Точная проверка расстояния (круг, а не прямоугольник по осям).
            if (object.GetDistanceSquared(candidate) > sightRange * sightRange)
            {
                continue;
            }
            count++;
        }
        return count;
    }

    // Возвращает количество живых союзников, расположенных выше (Y больше)
    // заданного объекта в пределах области видимости.
    public int CountAlliesUp(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться выше (большая Y-координата).
            if (candidate.getY() > object.getY())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых союзников, расположенных ниже (Y меньше)
    // заданного объекта в пределах области видимости.
    public int CountAlliesDown(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться ниже (меньшая Y-координата).
            if (candidate.getY() < object.getY())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых союзников, расположенных слева (X меньше)
    // заданного объекта в пределах области видимости.
    public int CountAlliesLeft(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться слева (меньшая X-координата).
            if (candidate.getX() < object.getX())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых союзников, расположенных справа (X больше)
    // заданного объекта в пределах области видимости.
    public int CountAlliesRight(BattleObject object, int sightRange_in)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться справа (большая X-координата).
            if (candidate.getX() > object.getX())
            {
                count++;
            }
        }
        return count;
    }

    // Возвращает количество живых союзников (юнитов и зданий той же фракции)
    // в радиусе обзора object (полный круг, без ограничения направления).
    public int CountAllies(BattleObject object, int sightRange)
    {
        int count = 0;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange);
            if (candidate == null)
            {
                continue;
            }
            // Точная проверка расстояния (круг, а не прямоугольник по осям).
            if (object.GetDistanceSquared(candidate) > sightRange * sightRange)
            {
                continue;
            }
            count++;
        }
        return count;
    }

    // Находит ближайшего живого союзника, расположенного выше (Y больше) заданного объекта.
    // Возвращает null, если такого союзника нет.
    public BattleObject FindNearestAllyUp(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться выше (большая Y-координата).
            if (candidate.getY() <= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого союзника, расположенного справа (X больше) заданного объекта.
    // Возвращает null, если такого союзника нет.
    public BattleObject FindNearestAllyRight(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться справа (большая X-координата).
            if (candidate.getX() <= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого союзника, расположенного ниже (Y меньше) заданного объекта.
    // Возвращает null, если такого союзника нет.
    public BattleObject FindNearestAllyDown(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться ниже (меньшая Y-координата).
            if (candidate.getY() >= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего живого союзника, расположенного слева (X меньше) заданного объекта.
    // Возвращает null, если такого союзника нет.
    public BattleObject FindNearestAllyLeft(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            // Союзник должен находиться слева (меньшая X-координата).
            if (candidate.getX() >= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит самого раненого живого союзника-юнита своей фракции (health < maxHealth)
    // в пределах range от юнита, у которого ещё меньше maxHealersPerTarget лечащих жрецов.
    // Возвращает null, если таких нет.
    public BattleUnitCommon FindMostWoundedAlly(BattleUnitCommon unit, int range, int maxHealersPerTarget)
    {
        BattleUnitCommon best = null;
        float bestRatio = Float.MAX_VALUE;
        for (PlacedObject object : units)
        {
            if (object == unit || !(object instanceof BattleUnitCommon) || object.IsDead())
            {
                continue;
            }
            BattleUnitCommon candidate = (BattleUnitCommon) object;
            // Пропускаем врагов (другая фракция) и объекты без фракции.
            if (unit.getFraction() == null || candidate.getFraction() == null || unit.IsEnemy(candidate))
            {
                continue;
            }
            // Союзник должен быть ранен (health < maxHealth).
            if (candidate.getHealth() >= candidate.getMaxHealth())
            {
                continue;
            }
            // Цель уже лечат достаточно жрецов — пропускаем.
            if (CountHealersOn(candidate) >= maxHealersPerTarget)
            {
                continue;
            }
            // Первичный фильтр по осям.
            if (Math.abs(unit.getX() - candidate.getX()) > range || Math.abs(unit.getY() - candidate.getY()) > range)
            {
                continue;
            }
            // Самый раненый — с наименьшим процентом здоровья.
            float ratio = (float) candidate.getHealth() / candidate.getMaxHealth();
            if (ratio < bestRatio)
            {
                bestRatio = ratio;
                best = candidate;
            }
        }
        return best;
    }

    // Возвращает число живых жрецов, в данный момент лечащих заданного союзника.
    public int CountHealersOn(BattleUnitCommon target)
    {
        int count = 0;
        for (PlacedObject object : units)
        {
            if (object instanceof UnitPriest && !object.IsDead())
            {
                UnitPriest priest = (UnitPriest) object;
                if (priest.getHealTarget() == target)
                {
                    count++;
                }
            }
        }
        return count;
    }

    // Находит ближайшего раненого живого союзника, расположенного выше (Y больше) заданного объекта.
    public BattleObject FindNearestWoundedAllyUp(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindWoundedAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            if (candidate.getY() <= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего раненого живого союзника, расположенного справа (X больше) заданного объекта.
    public BattleObject FindNearestWoundedAllyRight(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindWoundedAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            if (candidate.getX() <= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего раненого живого союзника, расположенного ниже (Y меньше) заданного объекта.
    public BattleObject FindNearestWoundedAllyDown(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindWoundedAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            if (candidate.getY() >= object.getY())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Находит ближайшего раненого живого союзника, расположенного слева (X меньше) заданного объекта.
    public BattleObject FindNearestWoundedAllyLeft(BattleObject object, int sightRange_in)
    {
        BattleObject nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (PlacedObject unit : units)
        {
            BattleObject candidate = FindWoundedAllyCandidate(object, unit, sightRange_in);
            if (candidate == null)
            {
                continue;
            }
            if (candidate.getX() >= object.getX())
            {
                continue;
            }
            float distance = object.GetDistance(candidate);
            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // Возвращает раненого союзника (живой объект той же фракции с health < maxHealth),
    // если unit удовлетворяет условиям; иначе null.
    private BattleObject FindWoundedAllyCandidate(BattleObject object, PlacedObject unit, int sightRange_in)
    {
        if (unit == object || !(unit instanceof BattleObject) || unit.IsDead())
            return null;
        BattleObject candidate = (BattleObject) unit;
        if (object.getFraction() == null || candidate.getFraction() == null || object.IsEnemy(candidate))
            return null;
        // Союзник должен быть ранен.
        if (candidate.getHealth() >= candidate.getMaxHealth())
            return null;
        // Первичный фильтр по осям.
        if (Math.abs(object.getX() - candidate.getX()) > sightRange_in)
            return null;
        if (Math.abs(object.getY() - candidate.getY()) > sightRange_in)
            return null;
        return candidate;
    }

    // Находит мёртвого юнита заданного класса (для переиспользования объекта).
    // Возвращает null, если такого объекта нет.
    @SuppressWarnings("unchecked")
    public <T extends BattleUnitCommon> T FindDeadUnit(Class<T> unitClass)
    {
        for (PlacedObject object : units)
        {
            if (object.IsDead() && unitClass.isInstance(object))
            {
                return (T) object;
            }
        }
        return null;
    }

    // Возвращает до count живых юнитов заданного класса и фракции с наибольшим значением фита.
    @SuppressWarnings("unchecked")
    public <T extends BattleUnitCommon> ArrayList<T> GetTopAliveUnits(Class<T> unitClass, int count, Fraction fraction)
    {
        ArrayList<T> aliveUnits = new ArrayList<T>();
        for (PlacedObject object : units)
        {
            if (unitClass.isInstance(object) && !object.IsDead()
                    && ((BattleObject) object).getFraction() == fraction)
            {
                aliveUnits.add((T) object);
            }
        }

        aliveUnits.sort(new Comparator<T>()
        {
            @Override
            public int compare(T a, T b)
            {
                return Integer.compare(GetEffectiveFit(b), GetEffectiveFit(a));
            }
        });

        ArrayList<T> top = new ArrayList<T>();
        for (int i = 0; i < count && i < aliveUnits.size(); i++)
        {
            top.add(aliveUnits.get(i));
        }
        return top;
    }

    // Возвращает живого юнита заданного класса с наибольшим значением фита среди всех фракций.
    // Возвращает null, если таких юнитов нет.
    @SuppressWarnings("unchecked")
    public <T extends BattleUnitCommon> T GetBestAliveUnit(Class<T> unitClass)
    {
        T best = null;
        for (PlacedObject object : units)
        {
            if (unitClass.isInstance(object) && !object.IsDead())
            {
                T candidate = (T) object;
                if (best == null || GetEffectiveFit(candidate) > GetEffectiveFit(best))
                {
                    best = candidate;
                }
            }
        }
        return best;
    }

    // Сохраняет лучшую по фиту нейросеть каждого типа юнитов в каталог сохранений (для переноса прогресса между играми).
    public void SaveBestNetworks()
    {
        File dir = new File(SavesDirectory());
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        SaveBestNetwork(UnitSwordman.class, "Swordman.xml");
        SaveBestNetwork(UnitArcher.class, "Archer.xml");
        SaveBestNetwork(UnitShieldman.class, "Shieldman.xml");
        SaveBestNetwork(UnitPriest.class, "Priest.xml");
        SaveBestNetwork(UnitScout.class, "Scout.xml");
    }

    // Сохраняет нейросеть лучшего по фиту живого юнита заданного класса в файл.
    private <T extends BattleUnitCommon> void SaveBestNetwork(Class<T> unitClass, String fileName)
    {
        T best = GetBestAliveUnit(unitClass);
        if (best != null && best.getNeuroNet() != null)
        {
            best.getNeuroNet().SaveToFile(new File(SavesDirectory(), fileName).getPath());
        }
    }

    // Возвращает живого юнита заданного класса с наибольшим фитом среди вражеских
    // фракций (отличных от ownFraction). Возвращает null, если таких нет.
    @SuppressWarnings("unchecked")
    public <T extends BattleUnitCommon> T GetBestEnemyUnit(Class<T> unitClass, Fraction ownFraction)
    {
        T best = null;
        for (PlacedObject object : units)
        {
            if (unitClass.isInstance(object) && !object.IsDead())
            {
                BattleObject battleObject = (BattleObject) object;
                if (battleObject.getFraction() != null && battleObject.getFraction() != ownFraction)
                {
                    T candidate = (T) object;
                    if (best == null || GetEffectiveFit(candidate) > GetEffectiveFit(best))
                    {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    // Проверяет застой фита у всех живых юнитов (умирают при длительном застое).
    public void CheckUnitsStagnation(int maxStaleWaves)
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BattleUnitCommon && !object.IsDead())
            {
                ((BattleUnitCommon) object).CheckStagnation(maxStaleWaves);
            }
        }
    }

    // Проверяет, пересекается ли заданная область (x, y, width, height)
    // с областью какого-либо уже размещённого объекта.
    public boolean IsAreaOccupied(int x, int y, int width, int height)
    {
        return IsAreaOccupiedExcept(x, y, width, height, null);
    }

    // То же самое, но с возможностью исключить сам перемещающийся объект.
    public boolean IsAreaOccupiedExcept(int x, int y, int width, int height, PlacedObject self)
    {
        for (PlacedObject unit : units)
        {
            if (unit == self || unit.IsDead())
            {
                continue;
            }

            if (x < unit.getX() + unit.getWidth() &&
                x + width > unit.getX() &&
                y < unit.getY() + unit.getHeight() &&
                y + height > unit.getY())
            {
                return true;
            }
        }
        return false;
    }

    // Ищет ближайшую свободную позицию для размещения области (width x height),
    // спиралевидно от точки (x, y) с шагом, равным размерам области.
    // Возвращает массив {x, y}.
    public int[] FindFreePosition(int x, int y, int width, int height)
    {
        // Защита от некорректных размеров (деление на ноль в расчёте шага спирали).
        if (width <= 0 || height <= 0)
        {
            return new int[]{x, y};
        }

        if (IsInsideWorld(x, y, width, height) && !IsAreaOccupied(x, y, width, height))
        {
            return new int[]{x, y};
        }

        // Число шагов спирали, достаточное, чтобы покрыть экран по каждой оси.
        int maxRadiusX = Gdx.graphics.getWidth() / width + 1;
        int maxRadiusY = Gdx.graphics.getHeight() / height + 1;
        int maxRadius = Math.max(maxRadiusX, maxRadiusY);

        // Обходим периметр квадрата со стороной radius шагов.
        for (int radius = 1; radius <= maxRadius; radius++)
        {
            for (int stepX = -radius; stepX <= radius; stepX++)
            {
                for (int stepY = -radius; stepY <= radius; stepY++)
                {
                    if (Math.max(Math.abs(stepX), Math.abs(stepY)) != radius)
                    {
                        continue;
                    }

                    int candidateX = x + stepX * width;
                    int candidateY = y + stepY * height;

                    if (IsInsideWorld(candidateX, candidateY, width, height) &&
                        !IsAreaOccupied(candidateX, candidateY, width, height))
                    {
                        return new int[]{candidateX, candidateY};
                    }
                }
            }
        }

        return new int[]{x, y};
    }

    // Возвращает кандидата (живой объект другой фракции, не сам object),
    // если unit удовлетворяет условиям; иначе возвращает null.
    private BattleObject FindCandidate(BattleObject object, PlacedObject unit, int sightRange_in)
    {
        return FindCandidate(object, unit, sightRange_in, false);
    }

    // Возвращает союзника (живой объект той же фракции, не сам object),
    // если unit удовлетворяет условиям; иначе возвращает null.
    private BattleObject FindAllyCandidate(BattleObject object, PlacedObject unit, int sightRange_in)
    {
        return FindCandidate(object, unit, sightRange_in, true);
    }

    // Возвращает живого кандидата заданного отношения (союзник/враг) к object
    // в пределах области видимости по осям; иначе возвращает null.
    private BattleObject FindCandidate(BattleObject object, PlacedObject unit, int sightRange_in, boolean ally)
    {
        BattleObject candidate = object.FindSightCandidate(unit, sightRange_in);
        if (candidate == null)
        {
            return null;
        }
        // Пропускаем объекты без фракции.
        if (object.getFraction() == null || candidate.getFraction() == null)
        {
            return null;
        }
        // Союзник (ally=true) требует ту же фракцию, враг (ally=false) — другую.
        if (object.IsEnemy(candidate) == ally)
        {
            return null;
        }
        return candidate;
    }

    // Возвращает true, если игра завершена.
    public boolean IsGameOver()
    {
        return gameOver;
    }

    // Возвращает фракцию-победителя (или null при ничьей).
    public Fraction GetWinner()
    {
        return winnerFraction;
    }

    // Возвращает фракцию первого живого ядра (победителя), или null.
    private Fraction FindWinnerFraction()
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                return ((BuildingCore) object).getFraction();
            }
        }
        return null;
    }

    // Возвращает true, если среди живых ядер осталась только одна фракция (или ядер нет).
    private boolean IsOnlyOneFractionLeft()
    {
        Fraction firstFraction = null;
        boolean hasFirst = false;
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                Fraction f = ((BuildingCore) object).getFraction();
                if (!hasFirst)
                {
                    firstFraction = f;
                    hasFirst = true;
                }
                else if (firstFraction != f)
                {
                    return false;
                }
            }
        }
        return true;
    }

    // Возвращает true, если хотя бы одно ядро произвело второе поколение.
    private boolean IsSecondGenerationReached()
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && ((BuildingCore) object).getGenerationNumber() >= 2)
            {
                return true;
            }
        }
        return false;
    }

    // Возвращает максимальный номер поколения среди живых ядер.
    private int GetMaxGeneration()
    {
        int max = 0;
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                int generation = ((BuildingCore) object).getGenerationNumber();
                if (generation > max)
                {
                    max = generation;
                }
            }
        }
        return max;
    }

    // Возвращает текущее поколение живого ядра фракции (или 0, если такого ядра нет).
    private int GetFractionGeneration(Fraction fraction)
    {
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead() && ((BuildingCore) object).getFraction() == fraction)
            {
                return ((BuildingCore) object).getGenerationNumber();
            }
        }
        return 0;
    }

    // Возвращает эффективный фитнесс юнита с микро-бонусом за выживаемость поколений:
    // новый фит = фит * (1 + (текущее поколение фракции - поколение юнита) / 10).
    public int GetEffectiveFit(BattleUnitCommon unit)
    {
        int currentGeneration = GetFractionGeneration(unit.getFraction());
        double multiplier = 1.0 + (currentGeneration - unit.getGenerationNumber()) / 10.0;
        return (int) Math.round(unit.getFit() * multiplier);
    }

    // Возвращает фракцию с наибольшим суммарным фитнесом нейросетей ядра (для определения победителя по лимиту поколений).
    private Fraction FindWinnerByFitness()
    {
        Fraction bestFraction = null;
        int bestFit = Integer.MIN_VALUE;
        for (PlacedObject object : units)
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                BuildingCore core = (BuildingCore) object;
                int totalFit = core.getNetSwordmanFit() + core.getNetArcherFit() + core.getNetShieldmanFit()
                        + core.getNetPriestFit() + core.getNetScoutFit();
                if (totalFit > bestFit)
                {
                    bestFit = totalFit;
                    bestFraction = core.getFraction();
                }
            }
        }
        return bestFraction;
    }

    // Проверяет, выполнены ли условия завершения игры.
    private boolean CheckGameOver()
    {
        return IsOnlyOneFractionLeft() && IsSecondGenerationReached();
    }

    // Проверяет, что область (x, y, width, height) целиком находится внутри мира
    // (справа от командной панели и в пределах экрана).
    public boolean IsInsideWorld(int x, int y, int width, int height)
    {
        return x >= worldLeft && y >= worldBottom &&
               x + width <= worldRight &&
               y + height <= worldTop;
    }
}
