package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.neuro_net.Axon;
import ru.gigabyteartur.weights_of_war.neuro_net.Layer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.neuro_net.Neuron;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TeamsTest extends HeadlessGdxTest
{
    private File savesDir;

    @BeforeEach
    public void tempSaves() throws Exception
    {
        savesDir = Files.createTempDirectory("wow-teams-saves").toFile();
        GameWorld.SetSavesDirectoryForTests(savesDir.getAbsolutePath());
    }

    @AfterEach
    public void restoreSaves()
    {
        GameWorld.SetSavesDirectoryForTests(null);
        if (savesDir != null)
        {
            File[] fs = savesDir.listFiles();
            if (fs != null)
            {
                for (File f : fs)
                {
                    f.delete();
                }
            }
            savesDir.delete();
        }
    }

    // --- команда / IsEnemy ---

    @Test
    public void sameTeamAreAlliesDifferentTeamsAreEnemies()
    {
        Fraction a1 = new Fraction("Red", null, new Team("A"));
        Fraction a2 = new Fraction("Blue", null, new Team("A"));
        Fraction b1 = new Fraction("Cyan", null, new Team("B"));

        TestUnit red = new TestUnit(0, 0);
        red.setFraction(a1);
        TestUnit blue = new TestUnit(1, 0);
        blue.setFraction(a2);
        TestUnit cyan = new TestUnit(2, 0);
        cyan.setFraction(b1);

        assertFalse(red.IsEnemy(blue), "фракции одной команды не враги");
        assertTrue(red.IsAlly(blue), "фракции одной команды — союзники");
        assertTrue(red.IsEnemy(cyan), "разные команды — враги");
        assertFalse(red.IsAlly(cyan));
    }

    @Test
    public void nullTeamKeepsLegacyEachForSelf()
    {
        Fraction red = new Fraction("Red", null);
        Fraction red2 = new Fraction("Red", null); // другая ссылка, то же имя
        Fraction blue = new Fraction("Blue", null);

        TestUnit u = new TestUnit(0, 0);
        u.setFraction(red);
        TestUnit same = new TestUnit(1, 0);
        same.setFraction(red);
        TestUnit other = new TestUnit(2, 0);
        other.setFraction(blue);
        TestUnit sameNameOtherRef = new TestUnit(3, 0);
        sameNameOtherRef.setFraction(red2);

        assertFalse(u.IsEnemy(same), "та же фракция (ссылка) — союзник");
        assertTrue(u.IsEnemy(other), "другая фракция — враг");
        assertTrue(u.IsEnemy(sameNameOtherRef), "при team==null союз только по ссылке, как раньше");
    }

    // --- класс Team: единственный владелец отношений ---

    @Test
    public void teamNameIsIdentity()
    {
        Team a = new Team("A");
        Team aAgain = new Team("A");
        Team b = new Team("B");

        assertEquals(a, aAgain, "две команды с одним именем — одна команда");
        assertEquals(a.hashCode(), aAgain.hashCode(), "равные команды — равные хеши");
        assertNotEquals(a, b);
        assertEquals("A", a.getName(), "имя команды читается наружу");
    }

    @Test
    public void sameTeamRelationIsOwnedByTeam()
    {
        Fraction a1 = new Fraction("Red", null, new Team("A"));
        Fraction a2 = new Fraction("Blue", null, new Team("A"));
        Fraction b1 = new Fraction("Cyan", null, new Team("B"));
        Fraction solo = new Fraction("Pink", null);

        assertTrue(Team.SameTeam(a1, a2), "одна команда — союзники");
        assertFalse(Team.SameTeam(a1, b1), "разные команды — не союзники");
        assertFalse(Team.SameTeam(a1, solo), "фракция без команды никому не союзник");
        assertFalse(Team.SameTeam(null, a1), "null-фракция союзником не считается");
        assertTrue(Team.SameTeam(solo, solo), "объект не враг сам себе");

        assertTrue(Team.AreEnemies(solo, a1), "без команды — каждый сам за себя");
        assertFalse(Team.AreEnemies(a1, a2), "одна команда — не враги");
    }

    @Test
    public void sideKeyTreatsTeamlessFactionsAsSeparateSides()
    {
        Fraction a1 = new Fraction("Red", null, new Team("A"));
        Fraction a2 = new Fraction("Blue", null, new Team("A"));
        Fraction b1 = new Fraction("Cyan", null, new Team("B"));
        Fraction solo1 = new Fraction("Red", null);
        Fraction solo2 = new Fraction("Red", null);

        assertEquals(Team.KeyOf(a1), Team.KeyOf(a2), "одна команда — один ключ стороны");
        assertNotEquals(Team.KeyOf(a1), Team.KeyOf(b1), "разные команды — разные стороны");
        assertNotEquals(Team.KeyOf(solo1), Team.KeyOf(solo2),
                "две одинаково названные фракции без команды — разные стороны, а не одна");
        assertNull(Team.KeyOf(null), "у null-фракции стороны нет");
    }

    @Test
    public void winConditionTreatsSameNamedSoloFactionsAsDifferentSides()
    {
        GameWorld world = new GameWorld();
        BuildingCore c1 = new BuildingCore(0, 0);
        c1.setFraction(new Fraction("Red", null));
        BuildingCore c2 = new BuildingCore(200, 0);
        c2.setFraction(new Fraction("Red", null));
        world.AddUnit(c1);
        world.AddUnit(c2);

        assertFalse(world.IsOnlyOneTeamLeft(),
                "две разные фракции без команды воюют друг с другом, значит живы две стороны");
    }

    // --- своё ядро ---

    @Test
    public void findOwnCoreReturnsOwnCoreNotAllyCore()
    {
        Fraction mine = new Fraction("Red", null, new Team("A"));
        Fraction ally = new Fraction("Blue", null, new Team("A"));

        GameWorld world = new GameWorld();
        TestUnit unit = new TestUnit(0, 0);
        unit.setFraction(mine);
        world.AddUnit(unit);

        BuildingCore myCore = new BuildingCore(100, 100);
        myCore.setFraction(mine);
        world.AddUnit(myCore);

        BuildingCore allyCore = new BuildingCore(200, 100);
        allyCore.setFraction(ally);
        world.AddUnit(allyCore);

        assertSame(myCore, world.FindOwnCore(unit), "юнит узнаёт ИМЕННО своё ядро, а не ядро союзника");
    }

    // --- условие победы по команде ---

    @Test
    public void winConditionFiresWhenOneTeamAlive()
    {
        GameWorld world = new GameWorld();
        BuildingCore c1 = new BuildingCore(0, 0);
        c1.setFraction(new Fraction("Red", null, new Team("A")));
        BuildingCore c2 = new BuildingCore(100, 0);
        c2.setFraction(new Fraction("Blue", null, new Team("A")));
        world.AddUnit(c1);
        world.AddUnit(c2);

        // Живы ядра двух РАЗНЫХ фракций одной команды — победа по команде уже наступила.
        assertTrue(world.IsOnlyOneTeamLeft());
    }

    @Test
    public void winConditionDoesNotFireWhenMultipleTeamsAlive()
    {
        GameWorld world = new GameWorld();
        BuildingCore c1 = new BuildingCore(0, 0);
        c1.setFraction(new Fraction("Red", null, new Team("A")));
        BuildingCore c2 = new BuildingCore(100, 0);
        c2.setFraction(new Fraction("Cyan", null, new Team("B")));
        world.AddUnit(c1);
        world.AddUnit(c2);

        assertFalse(world.IsOnlyOneTeamLeft(), "живы две команды — партия не окончена");
    }

    // --- InitTeams: 9 ядер / 3 команды ---

    @Test
    public void initTeamsCreatesNineCoresThreeTeamsGreenScripted()
    {
        GameWorld world = new GameWorld();
        world.InitTeams();

        int cores = 0;
        HashSet<String> teams = new HashSet<String>();
        boolean greenScripted = false;
        int greenCores = 0;
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BuildingCore && !object.IsDead())
            {
                cores++;
                Fraction f = ((BuildingCore) object).getFraction();
                if (f.getTeamName() != null)
                {
                    teams.add(f.getTeamName());
                }
                if ("Green".equals(f.getName()))
                {
                    greenCores++;
                    greenScripted = ((BuildingCore) object).isScripted();
                }
            }
        }
        assertEquals(9, cores, "должно быть 9 ядер");
        assertEquals(3, teams.size(), "должно быть 3 команды");
        assertEquals(1, greenCores, "ровно одно ядро Green");
        assertTrue(greenScripted, "Green играет по вписанной стратегии");
    }

    // --- сохранение: сети обучающихся ядер команды-победителя ---

    @Test
    public void saveKeepsWinningTeamNetsOnly() throws Exception
    {
        GameWorld world = new GameWorld();
        Fraction a1f = new Fraction("Red", null, new Team("A"));
        Fraction a2f = new Fraction("Blue", null, new Team("A"));
        Fraction b1f = new Fraction("Cyan", null, new Team("B"));
        BuildingCore a1 = new BuildingCore(0, 0);
        a1.setFraction(a1f);
        BuildingCore a2 = new BuildingCore(120, 0);
        a2.setFraction(a2f);
        BuildingCore b1 = new BuildingCore(240, 0);
        b1.setFraction(b1f);
        world.AddUnit(a1);
        world.AddUnit(a2);
        world.AddUnit(b1);

        a1.ProduceUnits(world);
        a2.ProduceUnits(world);
        b1.ProduceUnits(world);

        // Проигравшая команда B погибла; победитель — команда A (ядра a1/a2 живы).
        b1.setHealth(0);
        for (PlacedObject o : world.GetUnits())
        {
            if (o instanceof BattleUnitCommon && ((BattleObject) o).getFraction() == b1f)
            {
                ((BattleUnitCommon) o).setHealth(0);
            }
        }

        NeuroNet a1net = a1.GetChampionNet(UnitSwordman.class);
        NeuroNet b1net = b1.GetChampionNet(UnitSwordman.class);
        assertNotNull(a1net);
        assertNotNull(b1net);
        assertNotEquals(weightsOf(a1net), weightsOf(b1net), "сети должны различаться, иначе тест ничего не проверяет");

        world.SaveBestNetworks();

        File file = new File(savesDir, "Swordman.xml");
        assertTrue(file.exists(), "сохранение должно сработать");
        NeuroNet loaded = new NeuroNet();
        assertTrue(loaded.LoadFromFile(file.getAbsolutePath()));

        assertNotEquals(weightsOf(b1net), weightsOf(loaded), "сеть проигравшей команды не должна сохраняться");

        NeuroNet a2net = a2.GetChampionNet(UnitSwordman.class);
        List<Double> loadedW = weightsOf(loaded);
        assertTrue(loadedW.equals(weightsOf(a1net)) || (a2net != null && loadedW.equals(weightsOf(a2net))),
                "в файл должна уйти сеть ядра команды-победителя");
    }

    // --- headless-прогон на 9 ядрах / 3 командах ---

    @Test
    public void headlessProbeNineCoresAlliesDoNotAttack()
    {
        Gdx.graphics = fixedGraphics(1f / 60f, 1280, 720);

        GameWorld world = new GameWorld();
        world.InitTeams();
        world.setGameSpeed(10f);
        world.setMaxGenerations(0);

        int cores = countCores(world);
        int teams = countTeams(world);
        boolean greenScripted = greenIsScripted(world);

        int frames = 0;
        int maxGen = 0;
        boolean sameTeamAttack = false;
        while (maxGen < 8 && frames < 3600 && !world.IsGameOver())
        {
            world.Update();
            frames++;
            if (anySameTeamAttack(world))
            {
                sameTeamAttack = true;
            }
            maxGen = maxGeneration(world);
        }

        System.out.println("PROBE cores=" + cores + " teams=" + teams + " greenScripted=" + greenScripted
                + " maxGen=" + maxGen + " frames=" + frames + " gameOver=" + world.IsGameOver()
                + " sameTeamAttack=" + sameTeamAttack);

        assertEquals(9, cores, "должно быть 9 ядер");
        assertEquals(3, teams, "должно быть 3 команды");
        assertTrue(greenScripted, "Green-ядро играет по правилам");
        assertFalse(sameTeamAttack, "союзники одной команды не должны атаковать друг друга");
        assertTrue(maxGen >= 2, "мир должен прожить хотя бы пару поколений");
    }

    // --- служебные методы ---

    private static List<Double> weightsOf(NeuroNet net)
    {
        List<Double> out = new ArrayList<Double>();
        for (Layer layer : net.GetLayers())
        {
            for (Neuron neuron : layer.GetNeurons())
            {
                out.add(neuron.GetBias());
                for (Axon axon : neuron.GetAxons())
                {
                    out.add(axon.GetWeight());
                }
            }
        }
        return out;
    }

    private static int countCores(GameWorld world)
    {
        int n = 0;
        for (PlacedObject o : world.GetUnits())
        {
            if (o instanceof BuildingCore && !o.IsDead())
            {
                n++;
            }
        }
        return n;
    }

    private static int countTeams(GameWorld world)
    {
        HashSet<String> teams = new HashSet<String>();
        for (PlacedObject o : world.GetUnits())
        {
            if (o instanceof BuildingCore && !o.IsDead())
            {
                Fraction f = ((BuildingCore) o).getFraction();
                if (f.getTeamName() != null)
                {
                    teams.add(f.getTeamName());
                }
            }
        }
        return teams.size();
    }

    private static boolean greenIsScripted(GameWorld world)
    {
        for (PlacedObject o : world.GetUnits())
        {
            if (o instanceof BuildingCore && "Green".equals(((BuildingCore) o).getFraction().getName()))
            {
                return ((BuildingCore) o).isScripted();
            }
        }
        return false;
    }

    private static int maxGeneration(GameWorld world)
    {
        int max = 0;
        for (PlacedObject o : world.GetUnits())
        {
            if (o instanceof BuildingCore && !o.IsDead())
            {
                max = Math.max(max, ((BuildingCore) o).getGenerationNumber());
            }
        }
        return max;
    }

    private static boolean anySameTeamAttack(GameWorld world)
    {
        for (PlacedObject o : world.GetUnits())
        {
            if (!(o instanceof BattleUnitCommon) || o.IsDead())
            {
                continue;
            }
            BattleUnitCommon unit = (BattleUnitCommon) o;
            CommandUnit command = unit.GetCurrentCommand();
            if (command instanceof CommandAttackTarget)
            {
                BattleObject target = ((CommandAttackTarget) command).getTarget();
                if (target != null && unit.IsAlly(target))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static Graphics fixedGraphics(final float delta, final int w, final int h)
    {
        return (Graphics) Proxy.newProxyInstance(
            Graphics.class.getClassLoader(),
            new Class<?>[] { Graphics.class },
            new InvocationHandler()
            {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                {
                    String name = method.getName();
                    if (name.equals("getWidth") || name.equals("getBackBufferWidth"))
                    {
                        return w;
                    }
                    if (name.equals("getHeight") || name.equals("getBackBufferHeight"))
                    {
                        return h;
                    }
                    if (name.equals("getDeltaTime"))
                    {
                        return delta;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == float.class) return 0f;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0d;
                    if (returnType == short.class) return (short) 0;
                    if (returnType == byte.class) return (byte) 0;
                    if (returnType == char.class) return (char) 0;
                    return null;
                }
            }
        );
    }
}
