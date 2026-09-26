package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Headless-режим: запускает симуляцию без окна и отрисовки, на повышенной скорости.
// Поддерживает пакетный запуск N игр с переносом прогресса (лучшие сети сохраняются между играми).
public class HeadlessGame extends ApplicationAdapter
{
    public final static float HEADLESS_GAME_SPEED = 1000f;         // Скорость игры в headless-режиме.
    public final static int HEADLESS_SCREEN_WIDTH = 1280;          // Ширина виртуального экрана.
    public final static int HEADLESS_SCREEN_HEIGHT = 720;          // Высота виртуального экрана.
    public final static float HEADLESS_DELTA_TIME = 1f / 60f;      // Фиксированный шаг кадра, сек.
    public final static int DEFAULT_GAMES_COUNT = 1;               // Число игр по умолчанию.
    public final static int MAX_GENERATIONS = 3000;                // Лимит поколений на партию в headless-режиме.

    private final int gamesToRun;       // Сколько игр сыграть подряд.
    private int gamesFinished = 0;      // Сколько игр уже завершено.
    private boolean finished = false;   // Пакет завершён (защита от повторного вызова render после exit).
    private GameWorld world;

    public HeadlessGame()
    {
        this(DEFAULT_GAMES_COUNT);
    }

    public HeadlessGame(int gamesToRun)
    {
        this.gamesToRun = Math.max(1, gamesToRun);
    }

    @Override
    public void create()
    {
        // Виртуальный графический контекст: фиксированные размеры и шаг кадра, без GPU.
        Gdx.graphics = fixedGraphics();

        // GL-заглушка (нужна для создания текстур без GPU).
        Gdx.gl = noOpGL20();
        Gdx.gl20 = Gdx.gl;

        // Заглушки текстур (без чтения файлов и нативного Pixmap).
        TextureCache.PutTexture("core.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("tower.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("swordman.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("archer.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("shieldman.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("priest.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("scout.png", new Texture(1, 1, Pixmap.Format.RGBA8888));

        StartNewGame();
    }

    @Override
    public void render()
    {
        if (finished)
        {
            return;
        }
        world.Update();
        if (world.IsGameOver())
        {
            FinishGame();
            gamesFinished++;
            if (gamesFinished >= gamesToRun)
            {
                System.out.println("Headless: finished " + gamesFinished + " game(s).");
                finished = true;
                Gdx.app.exit();
            }
            else
            {
                StartNewGame();
            }
        }
    }

    // Начинает новую игру: создаёт мир, инициализирует (загружая сохранённые сети), ставит скорость и лимит поколений.
    private void StartNewGame()
    {
        System.out.println("Game " + (gamesFinished + 1) + " started: " + CurrentDateTime());
        world = new GameWorld();
        world.Init();
        world.setGameSpeed(HEADLESS_GAME_SPEED);
        world.setMaxGenerations(MAX_GENERATIONS);
    }

    // Завершает текущую игру: сохраняет лучшие сети и печатает результат.
    private void FinishGame()
    {
        System.out.println("Game " + (gamesFinished + 1) + " finished: " + CurrentDateTime());
        world.SaveBestNetworks();
        Fraction winner = world.GetWinner();
        if (winner == null)
        {
            System.out.println("Game " + (gamesFinished + 1) + ": draw.");
        }
        else
        {
            System.out.println("Game " + (gamesFinished + 1) + ": winner " + winner.getName() + ", generation " + GetWinnerGeneration(winner));
        }
    }

    // Возвращает номер поколения ядра победившей фракции.
    private int GetWinnerGeneration(Fraction winner)
    {
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BuildingCore)
            {
                BuildingCore core = (BuildingCore) object;
                if (core.getFraction() == winner)
                {
                    return core.getGenerationNumber();
                }
            }
        }
        return 0;
    }

    // Возвращает текущую дату и время в формате "yyyy-MM-dd HH:mm:ss".
    private static String CurrentDateTime()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Возвращает Graphics-заглушку с фиксированными размерами экрана и шагом кадра.
    private static Graphics fixedGraphics()
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
                    if (name.equals("getWidth"))
                    {
                        return HEADLESS_SCREEN_WIDTH;
                    }
                    if (name.equals("getHeight"))
                    {
                        return HEADLESS_SCREEN_HEIGHT;
                    }
                    if (name.equals("getDeltaTime"))
                    {
                        return HEADLESS_DELTA_TIME;
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

    // Возвращает GL20-заглушку, все методы которой возвращают значение по умолчанию.
    private static GL20 noOpGL20()
    {
        return (GL20) Proxy.newProxyInstance(
            GL20.class.getClassLoader(),
            new Class<?>[] { GL20.class },
            new InvocationHandler()
            {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                {
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
