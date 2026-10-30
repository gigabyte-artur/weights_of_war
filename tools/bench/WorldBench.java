// Стенд Weights of War: реальный GameWorld без окна и отрисовки.
//
// Зачем: измерять, что происходит с обучением за N поколений, не поднимая GUI.
// Печатает по строке на поколение: фиты чемпионов ядра по типам юнитов,
// число живых юнитов, средний и лучший фит. Этим же стендом мерились эффекты
// гауссовой мутации, турнирного отбора и экспериментов с памятью сети.
//
// Использование (обычно через wz_bench.py, он собирает classpath):
//   java -cp <classpath> WorldBench <gens> [speed] [maxTicks]
//     gens     — поколение, на котором остановиться
//     speed    — gameSpeed (по умолчанию 60; в headless ставят 60..1000)
//     maxTicks — предохранитель по тикам (по умолчанию 400000)
//     dt       — шаг кадра в секундах (по умолчанию 1/60)
//
// ВАЖНО про dt и gameSpeed: игровое время за тик = dt * speed. Поколение наступает
// раз в 60 игровых секунд. При speed=60 и dt=1/60 один тик равен ЦЕЛОЙ игровой
// секунде — любая механика короче секунды (кулдаун решения, удар, регенерация)
// в таком прогоне выключена по построению. Мерить такие механики можно только
// при малом gameSpeed: при speed=1 и dt=1/60 поколение стоит 3600 тиков,
// при speed=10 — 360 тиков (тик = 1/6 секунды).
//
// ВАЖНО про случайность: флага seed здесь нет намеренно. В коде игры Random
// создаётся без зерна (Neuron.Mutate, NeuroNet.RandomWeights, Evolution,
// BuildingCore), поэтому прогон детерминированно не воспроизводится.
// Разброс между прогонами одного и того же кода велик (по avgFit наблюдалось
// 67..89 на 60 поколениях), поэтому сравнивать варианты надо по медиане
// нескольких прогонов — это делает `wz_bench.py pair`.

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxNativesLoader;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.TextureCache;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

import java.lang.reflect.Proxy;
import java.util.Locale;

public class WorldBench
{
    static final int SCREEN_WIDTH = 1280;
    static final int SCREEN_HEIGHT = 720;

    static float frameDt = 1f / 60f;   // шаг кадра, сек (по умолчанию 1/60).

    /** Заглушка Gdx.graphics: фиксированный размер кадра и заданный шаг dt. */
    static Graphics graphics()
    {
        return (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(),
            new Class<?>[]{Graphics.class}, (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("getWidth")) return SCREEN_WIDTH;
                if (name.equals("getHeight")) return SCREEN_HEIGHT;
                if (name.equals("getDeltaTime")) return frameDt;
                return defaultOf(method.getReturnType());
            });
    }

    /** Заглушка GL20: всё no-op, ничего не рисуем. */
    static GL20 gl()
    {
        return (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(),
            new Class<?>[]{GL20.class}, (proxy, method, args) -> defaultOf(method.getReturnType()));
    }

    static Object defaultOf(Class<?> type)
    {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == float.class) return 0f;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }

    public static void main(String[] args) throws Exception
    {
        int gens = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        float speed = args.length > 1 ? Float.parseFloat(args[1]) : 60f;
        int maxTicks = args.length > 2 ? Integer.parseInt(args[2]) : 400000;
        frameDt = args.length > 3 ? Float.parseFloat(args[3]) : 1f / 60f;

        GdxNativesLoader.load();
        Gdx.graphics = graphics();
        Gdx.gl = gl();
        Gdx.gl20 = Gdx.gl;

        for (String texture : new String[]{"core.png", "tower.png", "swordman.png", "archer.png",
                                           "shieldman.png", "priest.png", "scout.png"})
        {
            TextureCache.PutTexture(texture, new Texture(1, 1, Pixmap.Format.RGBA8888));
        }

        GameWorld world = new GameWorld();
        world.Init();
        world.setGameSpeed(speed);
        world.setMaxGenerations(0);   // лимит не нужен — цикл останавливаем сами

        System.out.println("# gs=" + speed + " dt=" + frameDt + " maxTicks=" + maxTicks);
        System.out.println("gen,coreSword,coreArcher,coreShield,corePriest,coreScout,alive,avgFit,maxFit,tick");
        int lastGen = -1;
        int tick = 0;
        long startedAt = System.currentTimeMillis();

        while (tick < maxTicks)
        {
            if (world.IsGameOver()) { System.out.println("GAMEOVER at tick " + tick); break; }
            world.Update();
            tick++;

            int currentGen = maxCoreGeneration(world);
            if (currentGen != lastGen && currentGen > 0)
            {
                lastGen = currentGen;
                int[] coreFits = coreFits(world);

                int alive = 0;
                double sum = 0;
                int best = 0;
                for (PlacedObject object : world.GetUnits())
                {
                    if (object instanceof BattleUnitCommon && !object.IsDead())
                    {
                        BattleUnitCommon unit = (BattleUnitCommon) object;
                        alive++;
                        sum += unit.getFit();
                        best = Math.max(best, unit.getFit());
                    }
                }
                double avg = alive > 0 ? sum / alive : 0;

                StringBuilder line = new StringBuilder();
                line.append(currentGen).append(',');
                for (int fit : coreFits) line.append(fit).append(',');
                line.append(alive).append(',')
                    .append(String.format(Locale.ROOT, "%.1f", avg)).append(',')
                    .append(best).append(',').append(tick);
                System.out.println(line);

                if (currentGen >= gens) break;
            }
            if (tick % 20000 == 0) System.err.println("...tick " + tick + " gen " + lastGen);
        }
        System.out.println("ELAPSED_MS " + (System.currentTimeMillis() - startedAt) + " ticks " + tick);
    }

    static int maxCoreGeneration(GameWorld world)
    {
        int max = 0;
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BuildingCore) max = Math.max(max, ((BuildingCore) object).getGenerationNumber());
        }
        return max;
    }

    static int[] coreFits(GameWorld world)
    {
        int[] best = new int[5];
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BuildingCore)
            {
                BuildingCore core = (BuildingCore) object;
                best[0] = Math.max(best[0], core.getNetSwordmanFit());
                best[1] = Math.max(best[1], core.getNetArcherFit());
                best[2] = Math.max(best[2], core.getNetShieldmanFit());
                best[3] = Math.max(best[3], core.getNetPriestFit());
                best[4] = Math.max(best[4], core.getNetScoutFit());
            }
        }
        return best;
    }
}
