package ru.gigabyteartur.weights_of_war.brains;

import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.Fraction;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.HeadlessGdxTest;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

import java.lang.reflect.Proxy;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

// Изоляция «тривиальной» стратегии от эволюционной.
//
// Требование владельца: вписанная стратегия должна существовать в обычном прогоне,
// но не должна участвовать в отборе. Иначе обучение сравнивалось бы не с правилом,
// а с гибридом: вписанный юнит мог бы попасть в родители, его фит — в оценку
// чемпионов, а его гены — в следующее поколение.
//
// Тесты ниже проверяют ровно это: ядро с флагом не рождает сети и не трогает пул
// чемпионов, его юниты невидимы для отбора и для переноса генов, снятие флага
// возвращает обычную схему, а ResetUnitState не тащит мозг в переиспользованный юнит.
public class ScriptedBrainIsolationTest extends HeadlessGdxTest
{
    private static final float DT = 1f / 60f;

    private GameWorld world;
    private final Fraction red = new Fraction("Red", null);
    private final Fraction blue = new Fraction("Blue", null);

    @BeforeEach
    public void setUp()
    {
        Gdx.graphics = (com.badlogic.gdx.Graphics) Proxy.newProxyInstance(
                com.badlogic.gdx.Graphics.class.getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.Graphics.class},
                (proxy, method, args) ->
                {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("getDeltaTime")) return DT;
                    Class<?> t = method.getReturnType();
                    if (t == int.class) return 0;
                    if (t == float.class) return 0f;
                    if (t == boolean.class) return false;
                    if (t == long.class) return 0L;
                    return null;
                });
        world = new GameWorld();
    }

    // 1. Вписанное ядро производит юнитов без нейросети, но с мозгом.
    @Test
    public void scriptedCoreSpawnsUnitsWithBrainAndNoNet()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(red);
        core.setScripted(true);
        world.AddUnit(core);

        core.ProduceUnits(world);

        int checked = 0;
        for (Object object : world.GetUnits())
        {
            if (object instanceof UnitSwordman)
            {
                BattleUnitCommon unit = (BattleUnitCommon) object;
                assertTrue(unit.IsScripted(), "юнит вписанного ядра должен иметь мозг");
                assertNull(unit.getNeuroNet(), "юнит вписанного ядра не должен нести сеть");
                checked++;
            }
        }
        assertTrue(checked > 0, "ядро должно было произвести мечников");
    }

    // 2. Обычное ядро при этом работает как раньше: сеть есть, мозга нет.
    @Test
    public void normalCoreStillSpawnsEvolvingUnits()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(red);
        world.AddUnit(core);

        core.ProduceUnits(world);

        int checked = 0;
        for (Object object : world.GetUnits())
        {
            if (object instanceof UnitSwordman)
            {
                BattleUnitCommon unit = (BattleUnitCommon) object;
                assertFalse(unit.IsScripted(), "обычный юнит не должен играть по правилам");
                assertNotNull(unit.getNeuroNet(), "обычный юнит должен нести сеть");
                checked++;
            }
        }
        assertTrue(checked > 0);
    }

    // 3. Вписанные юниты не попадают в выбор родителей для следующего поколения.
    @Test
    public void scriptedUnitsAreInvisibleToSelection()
    {
        TestUnit scripted = new TestUnit(100, 100);
        scripted.setFraction(red);
        scripted.setBrain(new ScriptedBrain());
        scripted.setFit(9999);
        world.AddUnit(scripted);

        TestUnit evolving = new TestUnit(200, 200);
        evolving.setFraction(red);
        evolving.setFit(10);
        world.AddUnit(evolving);

        ArrayList<TestUnit> top = world.GetTopAliveUnits(TestUnit.class, 10, red);

        assertEquals(1, top.size(), "в отбор должен попасть только обучаемый юнит");
        assertSame(evolving, top.get(0));
    }

    // 4. Вписанный юнит не может стать чемпионом, даже с огромным фитом:
    //    иначе его сети (которой нет) нечего сохранять, а пул травится.
    @Test
    public void scriptedUnitNeverBecomesChampion()
    {
        TestUnit scripted = new TestUnit(100, 100);
        scripted.setFraction(red);
        scripted.setBrain(new ScriptedBrain());
        scripted.setFit(99999);
        world.AddUnit(scripted);

        assertNull(world.GetBestAliveUnit(TestUnit.class),
                "вписанный юнит не должен считаться лучшим кандидатом в чемпионы");
    }

    // 5. Чужой вписанный юнит не служит источником генов при переносе.
    @Test
    public void scriptedEnemyIsNotAGeneSource()
    {
        TestUnit scriptedEnemy = new TestUnit(100, 100);
        scriptedEnemy.setFraction(blue);
        scriptedEnemy.setBrain(new ScriptedBrain());
        scriptedEnemy.setFit(99999);
        world.AddUnit(scriptedEnemy);

        assertNull(world.GetBestEnemyUnit(TestUnit.class, red),
                "вписанный враг не должен передавать гены: генов у него нет");
    }

    // 6. Снятие флага возвращает обычное поведение ядра.
    @Test
    public void clearingFlagRestoresEvolution()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(red);
        core.setScripted(true);
        assertTrue(core.isScripted());
        assertNotNull(core.getScriptedBrain());

        core.setScripted(false);

        assertFalse(core.isScripted());
        assertNull(core.getScriptedBrain(), "у эволюционного ядра вписанной стратегии быть не должно");

        world.AddUnit(core);
        core.ProduceUnits(world);
        for (Object object : world.GetUnits())
        {
            if (object instanceof UnitSwordman)
            {
                assertNotNull(((BattleUnitCommon) object).getNeuroNet(), "после снятия флага юниты снова учатся");
            }
        }
    }

    // 7. Переиспользованный юнит не наследует мозг прошлой жизни.
    @Test
    public void reusedUnitDoesNotInheritBrain()
    {
        TestUnit unit = new TestUnit(100, 100);
        unit.setBrain(new ScriptedBrain());
        assertTrue(unit.IsScripted());

        unit.ResetUnitState();

        assertFalse(unit.IsScripted(), "ResetUnitState должен снимать мозг");
        assertNull(unit.getBrain());
    }

    // 8. Вписанное ядро не создаёт нейросети чемпионов при первой генерации.
    @Test
    public void scriptedCoreCreatesNoNets()
    {
        BuildingCore core = new BuildingCore(0, 0);
        core.setFraction(red);
        core.setScripted(true);
        world.AddUnit(core);

        core.ProduceUnits(world);

        // Все фиты чемпионов остаются нулевыми: пул не заполнялся сетями.
        assertEquals(0, core.getNetSwordmanFit());
        assertEquals(0, core.getNetArcherFit());
        assertEquals(0, core.getNetPriestFit());
        assertEquals(0, core.getNetScoutFit());
        assertEquals(0, core.getNetShieldmanFit());
    }

    // 9. Источник решения ровно один. Юнит с мозгом спрашивает мозг и не спрашивает
    //    сеть, даже если сеть ему присвоили: счётчик вызовов мозга это показывает,
    //    а пустая очередь — то, что сеть решение не подменила.
    @Test
    public void brainIsTheOnlyDecisionSourceWhenBothPresent()
    {
        TestUnit unit = new TestUnit(100, 100);
        unit.setFraction(red);
        unit.setSightRange(1000);
        unit.setMaxHealth(100);
        unit.setHealth(80);
        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());

        CountingBrain spy = new CountingBrain();
        unit.setBrain(spy);
        world.AddUnit(unit);
        world.UpdateFogOfWar();

        unit.Update(world);

        assertEquals(1, spy.calls, "мозг должен быть спрошен один раз");
        assertNull(unit.GetCurrentCommand(),
                "мозг, вернувший null, оставляет юнита без команды: сеть решение не подменяет");
    }

    // 10. Юнит без мозга по-прежнему думает сетью: решение появляется само,
    //     хотя никто его не спрашивал.
    @Test
    public void normalUnitStillThinksWithNet()
    {
        TestUnit unit = new TestUnit(100, 100);
        unit.setFraction(red);
        unit.setSightRange(1000);
        unit.setMaxHealth(100);
        unit.setHealth(80);
        unit.setNeuroNet(BattleUnitCommon.CreateNeuroNetUnit());
        world.AddUnit(unit);
        world.UpdateFogOfWar();

        assertFalse(unit.IsScripted());
        unit.Update(world);

        assertNotNull(unit.GetCurrentCommand(),
                "обучаемый юнит должен получить команду от сети, хотя мозгом никто не управляет");
        assertTrue(unit.GetCurrentCommand() instanceof ru.gigabyteartur.weights_of_war.commands.CommandUnit);
    }

    // Мозг-счётчик: ничего не решает, но фиксирует, что его спросили.
    private static class CountingBrain implements UnitBrain
    {
        int calls = 0;

        @Override
        public ru.gigabyteartur.weights_of_war.commands.CommandUnit Decide(BattleUnitCommon unit, GameWorld world)
        {
            calls++;
            return null;
        }

        @Override
        public String GetName()
        {
            return "Счётчик";
        }
    }
}
