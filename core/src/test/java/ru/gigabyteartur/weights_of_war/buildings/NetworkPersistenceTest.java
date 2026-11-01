package ru.gigabyteartur.weights_of_war.buildings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Перенос прогресса между партиями: донор — пул чемпионов ядра, а не лучший живой юнит.
 * <p>
 * Старое поведение {@code SaveBestNetwork} брало {@code GetBestAliveUnit}, поэтому файл
 * класса обновлялся, только если к концу партии жив хоть один обучающийся юнит этого
 * класса. Ядро гибнет задолго до конца партии — в замере на 20 партиях обновилось 47
 * файлов из 100, а в 4 партиях не обновился ни один. Эти тесты фиксируют именно тот
 * случай, который старый код терял: юнитов нет, а прогресс есть.
 */
public class NetworkPersistenceTest extends HeadlessGdxTest
{
    private final Fraction red = new Fraction("Red", null);
    private File savesDir;

    @BeforeEach
    public void useTempSavesDirectory() throws Exception
    {
        savesDir = Files.createTempDirectory("wow-saves-test").toFile();
        setSavesDir(savesDir.getAbsolutePath());
    }

    @AfterEach
    public void restoreSavesDirectory() throws Exception
    {
        setSavesDir(null);
        if (savesDir != null)
        {
            File[] files = savesDir.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    f.delete();
                }
            }
            savesDir.delete();
        }
    }

    private static void setSavesDir(String path) throws Exception
    {
        Method m = GameWorld.class.getDeclaredMethod("SetSavesDirectoryForTests", String.class);
        m.setAccessible(true);
        m.invoke(null, path);
    }

    private GameWorld worldWithCore()
    {
        GameWorld world = new GameWorld();
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(red);
        world.AddUnit(core);
        return world;
    }

    private BuildingCore aCore(GameWorld world)
    {
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BuildingCore)
            {
                return (BuildingCore) object;
            }
        }
        fail("в мире нет ядра");
        return null;
    }

    // Ядро должно выпустить хотя бы одно поколение: только тогда у него есть пул чемпионов.
    private void breedOnce(GameWorld world)
    {
        aCore(world).ProduceUnits(world);
    }

    private void killAllUnitsOfClass(GameWorld world, Class<?> unitClass)
    {
        for (PlacedObject object : world.GetUnits())
        {
            if (unitClass.isInstance(object))
            {
                ((BattleUnitCommon) object).setHealth(0);
            }
        }
    }

    private int countAliveUnitsOfClass(GameWorld world, Class<?> unitClass)
    {
        int alive = 0;
        for (PlacedObject object : world.GetUnits())
        {
            if (unitClass.isInstance(object) && !object.IsDead())
            {
                alive++;
            }
        }
        return alive;
    }

    @Test
    public void savesChampionEvenWhenEveryUnitOfThatClassIsDead() throws IOException
    {
        GameWorld world = worldWithCore();
        breedOnce(world);
        assertTrue(countAliveUnitsOfClass(world, UnitSwordman.class) > 0,
                "поколение должно было выпустить мечников");

        // Партия кончилась так, как она кончается в игре: обучающееся ядро уничтожено,
        // живых юнитов класса нет вовсе. Старый код на этом не писал ничего.
        killAllUnitsOfClass(world, UnitSwordman.class);
        assertEquals(0, countAliveUnitsOfClass(world, UnitSwordman.class));

        world.SaveBestNetworks();

        File file = new File(savesDir, "Swordman.xml");
        assertTrue(file.exists(), "пул чемпионов ядра должен сохраняться и без живых юнитов класса");
        assertTrue(file.length() > 0, "файл сохранения не должен быть пустым");

        NeuroNet loaded = new NeuroNet();
        assertTrue(loaded.LoadFromFile(file.getAbsolutePath()), "сохранённый файл должен читаться обратно");
        assertFalse(loaded.GetLayers().isEmpty(), "загруженная сеть должна иметь слои");
    }

    @Test
    public void deadCoreStillDonatesItsChampionPool()
    {
        GameWorld world = worldWithCore();
        breedOnce(world);

        BuildingCore core = aCore(world);
        assertNotNull(core.GetChampionNet(UnitSwordman.class),
                "у ядра после поколения есть чемпион мечников");

        // Ядро разрушено — в игре это обычное состояние конца партии.
        core.setHealth(0);
        assertTrue(core.IsDead());
        killAllUnitsOfClass(world, UnitSwordman.class);

        world.SaveBestNetworks();

        assertTrue(new File(savesDir, "Swordman.xml").exists(),
                "мёртвое ядро — носитель прогресса: его пул переживает и юнитов, и само ядро");
    }

    // Веса сети одним списком: сети одной топологии иначе не отличить друг от друга.
    private static java.util.List<Double> weightsOf(NeuroNet net)
    {
        java.util.List<Double> out = new java.util.ArrayList<Double>();
        for (ru.gigabyteartur.weights_of_war.neuro_net.Layer layer : net.GetLayers())
        {
            for (ru.gigabyteartur.weights_of_war.neuro_net.Neuron neuron : layer.GetNeurons())
            {
                out.add(neuron.GetBias());
                for (ru.gigabyteartur.weights_of_war.neuro_net.Axon axon : neuron.GetAxons())
                {
                    out.add(axon.GetWeight());
                }
            }
        }
        return out;
    }

    @Test
    public void championPoolBeatsALiveUnitOfAnotherFraction()
    {
        // Дефект был не только в «жив ли хоть кто-то», но и в том, КОГО брали донором:
        // GetBestAliveUnit смотрит на все фракции. Живой юнит чужой фракции с огромным
        // фитом уезжал в файл вместо чемпиона обучающегося ядра. Здесь его сеть заведомо
        // отличается от чемпионской, поэтому подмена видна по весам.
        GameWorld world = worldWithCore();
        breedOnce(world);

        Fraction blue = new Fraction("Blue", null);
        UnitSwordman foreign = null;
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof UnitSwordman && !object.IsDead())
            {
                foreign = (UnitSwordman) object;
                break;
            }
        }
        assertNotNull(foreign, "поколение выпустило мечника");
        foreign.setFraction(blue);
        foreign.setFit(999999);

        NeuroNet foreignNet = foreign.getNeuroNet().Copy();
        foreignNet.RandomWeights();
        foreign.setNeuroNet(foreignNet);

        BuildingCore core = aCore(world);
        NeuroNet champion = core.GetChampionNet(UnitSwordman.class);
        assertNotNull(champion);
        assertNotEquals(weightsOf(champion), weightsOf(foreignNet),
                "сеть чужака должна отличаться от чемпионской, иначе тест ничего не проверяет");

        world.SaveBestNetworks();

        File file = new File(savesDir, "Swordman.xml");
        assertTrue(file.exists());
        NeuroNet loaded = new NeuroNet();
        assertTrue(loaded.LoadFromFile(file.getAbsolutePath()));

        assertEquals(weightsOf(champion), weightsOf(loaded),
                "в файл должна уйти сеть чемпиона ядра, а не живого юнита чужой фракции");
    }

    @Test
    public void scriptedCoreNeverDonatesItsPool()
    {
        // Вписанное ядро не несёт генов: его пул не прогресс обучения и не должен
        // попадать в общий файл, иначе он отравит эволюцию остальных ядер.
        GameWorld world = new GameWorld();
        BuildingCore scripted = new BuildingCore(0, 0);
        scripted.setFraction(red);
        scripted.setScripted(true);
        world.AddUnit(scripted);
        scripted.ProduceUnits(world);

        assertNull(scripted.GetChampionNet(UnitSwordman.class),
                "вписанное ядро не ведёт пул чемпионов");

        world.SaveBestNetworks();

        assertFalse(new File(savesDir, "Swordman.xml").exists(),
                "вписанное ядро не должно давать донора для общего сохранения");
    }

    @Test
    public void worldWithoutAnyCoreKeepsTheOldBestAliveFallback()
    {
        // Запасной путь: мир без ядер (так устроена часть тестов и внешних прогонов)
        // должен сохранять по-прежнему лучшего живого юнита, иначе поведение сломается.
        GameWorld world = new GameWorld();
        UnitSwordman unit = new UnitSwordman(10, 10);
        unit.setFraction(red);
        unit.setFit(42);
        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        world.AddUnit(unit);

        world.SaveBestNetworks();

        assertTrue(new File(savesDir, "Swordman.xml").exists(),
                "без ядер сохраняется лучший живой юнит класса");
    }
}
