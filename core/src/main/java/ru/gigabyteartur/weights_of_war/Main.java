package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import ru.gigabyteartur.weights_of_war.commands.CommandMove;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import java.io.File;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private GameWorld world;
    private CommandPanel commandPanel;
    private BattleObject selectedUnit;

    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private Rectangle exitButtonBounds;
    private Rectangle debugButtonBounds;
    private float textX;
    private float textY;

    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new GameWorld();
        world.Init();
        commandPanel = new CommandPanel();

        font = createFont();
        font.getData().setScale(2f);

        shapeRenderer = new ShapeRenderer();

        float buttonWidth = 140f;
        float buttonHeight = 45f;
        float margin = 15f;
        // Кнопка Exit на командной панели слева (по центру, вверху).
        exitButtonBounds = new Rectangle(
                (GameWorld.COMMAND_PANEL_WIDTH - buttonWidth) / 2f,
                Gdx.graphics.getHeight() - buttonHeight - margin,
                buttonWidth,
                buttonHeight);

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, "Exit");
        textX = exitButtonBounds.x + (exitButtonBounds.width - layout.width) / 2f;
        textY = exitButtonBounds.y + (exitButtonBounds.height + layout.height) / 2f;

        // Кнопка-переключатель «Режим отладки» под кнопкой Exit.
        float debugButtonWidth = 220f;
        debugButtonBounds = new Rectangle(
                (GameWorld.COMMAND_PANEL_WIDTH - debugButtonWidth) / 2f,
                exitButtonBounds.y - buttonHeight - 10f,
                debugButtonWidth,
                buttonHeight);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float y = Gdx.graphics.getHeight() - screenY;
                if (exitButtonBounds.contains(screenX, y)) {
                    Gdx.app.exit();
                    return true;
                }
                if (debugButtonBounds.contains(screenX, y)) {
                    GameWorld.setDebugMode(!GameWorld.getDebugMode());
                    return true;
                }
                if (button == Input.Buttons.RIGHT)
                {
                    commandSelectedUnit(screenX, y);
                    return false;
                }
                selectUnitAt(screenX, y);
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        world.Update();

        batch.begin();
        drawGrassBackground(batch);
        world.Show(batch);
        batch.end();

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Командная панель слева — рисуется поверх боя.
        commandPanel.Show(shapeRenderer);

        // Кнопка выхода.
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(exitButtonBounds.x, exitButtonBounds.y, exitButtonBounds.width, exitButtonBounds.height);

        // Кнопка-переключатель «Режим отладки»: зелёная при включённом режиме, тёмная при выключенном.
        if (GameWorld.getDebugMode())
        {
            shapeRenderer.setColor(0.15f, 0.5f, 0.15f, 0.9f);
        }
        else
        {
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        }
        shapeRenderer.rect(debugButtonBounds.x, debugButtonBounds.y, debugButtonBounds.width, debugButtonBounds.height);
        shapeRenderer.end();

        batch.begin();
        font.draw(batch, "Exit", textX, textY);
        drawDebugButtonLabel(batch);
        commandPanel.ShowUnitInfo(batch, font, selectedUnit, world);
        batch.end();

        // Сообщение о победителе при завершении игры.
        if (world.IsGameOver())
        {
            String winnerText;
            if (world.GetWinner() == null)
            {
                winnerText = "Ничья";
            }
            else
            {
                winnerText = "Победила фракция: " + world.GetWinner().getName();
            }

            GlyphLayout layout = new GlyphLayout();
            layout.setText(font, winnerText);
            float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
            float y = Gdx.graphics.getHeight() / 2f;

            batch.begin();
            font.draw(batch, winnerText, x, y);
            batch.end();
        }
    }

    // Выделяет объект (юнита или здание) под указателем; снимает выделение с остальных.
    private void selectUnitAt(float x, float y)
    {
        BattleObject target = null;
        for (PlacedObject object : world.GetUnits())
        {
            if (object instanceof BattleObject && !object.IsDead())
            {
                BattleObject battleObject = (BattleObject) object;
                battleObject.setSelected(false);
                if (x >= battleObject.getX() && x <= battleObject.getX() + battleObject.getWidth() &&
                    y >= battleObject.getY() && y <= battleObject.getY() + battleObject.getHeight())
                {
                    target = battleObject;
                }
            }
        }
        if (target != null)
        {
            target.setSelected(true);
        }
        selectedUnit = target;
    }

    // Отправляет выделенного юнита к указанным координатам (правый клик по области боя).
    private void commandSelectedUnit(float x, float y)
    {
        if (x < GameWorld.COMMAND_PANEL_WIDTH)
        {
            return; // клик по командной панели — не область боя.
        }
        if (selectedUnit instanceof BattleUnitCommon && !selectedUnit.IsDead())
        {
            BattleUnitCommon unit = (BattleUnitCommon) selectedUnit;
            unit.ClearCommands();
            unit.AddCommand(new CommandMove((int) x, (int) y));
        }
    }

    // Отрисовывает подпись кнопки-переключателя «Режим отладки».
    private void drawDebugButtonLabel(SpriteBatch batch)
    {
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(1f);

        String text;
        if (GameWorld.getDebugMode())
        {
            text = "Отладка: вкл";
        }
        else
        {
            text = "Отладка: выкл";
        }
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        float x = debugButtonBounds.x + (debugButtonBounds.width - layout.width) / 2f;
        float y = debugButtonBounds.y + (debugButtonBounds.height + layout.height) / 2f;
        font.draw(batch, text, x, y);

        font.getData().setScale(prevScaleX, prevScaleY);
    }

    // Заполняет фон плитками текстуры травы.
    private void drawGrassBackground(SpriteBatch batch) {
        Texture grass = TextureCache.GetTexture("grass.jpg");
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        batch.setColor(Color.WHITE);
        for (int x = 0; x < screenWidth; x += grass.getWidth()) {
            for (int y = 0; y < screenHeight; y += grass.getHeight()) {
                batch.draw(grass, x, y);
            }
        }
    }

    // Создаёт шрифт с поддержкой кириллицы через FreeType.
    private BitmapFont createFont() {
        String[] fontPaths = {
            "C:\\Windows\\Fonts\\arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Helvetica.ttc"
        };

        for (String path : fontPaths) {
            try {
                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(path));
                FreeTypeFontParameter param = new FreeTypeFontParameter();
                param.size = 32;
                param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
                    + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
                BitmapFont font = generator.generateFont(param);
                generator.dispose();
                return font;
            } catch (Exception e) {
                // Пробуем следующий путь.
            }
        }

        return new BitmapFont();
    }

    @Override
    public void dispose() {
        world.SaveBestNetworks();
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();
        TextureCache.Dispose();
    }
}
