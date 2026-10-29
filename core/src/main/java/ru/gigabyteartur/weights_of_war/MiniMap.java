package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;

// Мини-карта командной панели: занимает верхнюю область панели, отображает все
// живые объекты мира (юниты — одна точка, строения — четыре точки, цвет по фракции),
// рисует белым прямоугольником видимую камерой область мира
// и позволяет кликом центрировать камеру мира на выбранной точке.
public class MiniMap
{
    public final static int SIZE = 250;                                     // Размер мини-карты (квадрат), px.
    public final static float MARGIN = 10f;                                 // Отступ мини-карты от краёв панели, px.

    private final static float POINT_SIZE = 3f;                             // Размер одной точки на мини-карте, px.
    private final static float POINT_GAP = 1f;                              // Зазор между точками строения, px.

    public final static float VIEW_RECT_LINE_WIDTH = 2f;                    // Толщина линии прямоугольника видимой области, px.

    private final Rectangle viewRect = new Rectangle();                     // Переиспользуемый прямоугольник рамки видимой области.

    // Прямоугольник мини-карты в экранных координатах (верх командной панели).
    public Rectangle GetBounds()
    {
        float x = (GameWorld.COMMAND_PANEL_WIDTH - SIZE) / 2f;
        float y = Gdx.graphics.getHeight() - MARGIN - SIZE;
        return new Rectangle(x, y, SIZE, SIZE);
    }

    // Y-координата верха области под мини-картой: кнопки и информация о выбранном
    // объекте выводятся ниже этой границы, чтобы не залезать на мини-карту.
    public static float GetBelowTop()
    {
        return Gdx.graphics.getHeight() - SIZE - MARGIN * 2f;
    }

    // Рисует мини-карту: фон, рамку, точки объектов мира и белый прямоугольник
    // видимой камере области. visibleWorld — мировой прямоугольник камеры (может быть null).
    public void Show(ShapeRenderer shapeRenderer, GameWorld world, Rectangle visibleWorld)
    {
        Rectangle bounds = GetBounds();

        // Фон и рамка мини-карты.
        shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 1f);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.setColor(0.4f, 0.4f, 0.5f, 1f);
        shapeRenderer.rectLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y, 2f);
        shapeRenderer.rectLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height, 2f);
        shapeRenderer.rectLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height, 2f);
        shapeRenderer.rectLine(bounds.x + bounds.width, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 2f);

        for (PlacedObject object : world.GetUnits())
        {
            if (object.IsDead() || !(object instanceof BattleObject))
            {
                continue;
            }

            BattleObject battleObject = (BattleObject) object;

            // Мировые координаты центра объекта -> экранные координаты на мини-карте (ось Y инвертирована).
            float mapX = bounds.x + (object.getX() + object.getWidth() / 2f) / GameWorld.MAP_WIDTH * bounds.width;
            float mapY = bounds.y + bounds.height - (object.getY() + object.getHeight() / 2f) / GameWorld.MAP_HEIGHT * bounds.height;

            if (battleObject.getFraction() != null)
            {
                shapeRenderer.setColor(battleObject.getFraction().getColor());
            }
            else
            {
                shapeRenderer.setColor(Color.WHITE);
            }

            if (object instanceof BattleBuildingCommon)
            {
                // Строение отображается четырьмя точками (квадратом 2x2).
                float step = POINT_SIZE + POINT_GAP;
                shapeRenderer.rect(mapX - step, mapY - step, POINT_SIZE, POINT_SIZE);
                shapeRenderer.rect(mapX + POINT_GAP, mapY - step, POINT_SIZE, POINT_SIZE);
                shapeRenderer.rect(mapX - step, mapY + POINT_GAP, POINT_SIZE, POINT_SIZE);
                shapeRenderer.rect(mapX + POINT_GAP, mapY + POINT_GAP, POINT_SIZE, POINT_SIZE);
            }
            else
            {
                // Юнит отображается одной точкой.
                shapeRenderer.rect(mapX - POINT_SIZE / 2f, mapY - POINT_SIZE / 2f, POINT_SIZE, POINT_SIZE);
            }
        }

        // Белая рамка видимой области мира — рисуется последней, чтобы быть поверх точек объектов.
        ShowVisibleWorldRect(shapeRenderer, bounds, visibleWorld);
    }

    // Белая рамка видимой области мира: показывает на мини-карте, какой кусок
    // карты сейчас виден камере.
    private void ShowVisibleWorldRect(ShapeRenderer shapeRenderer, Rectangle bounds, Rectangle visibleWorld)
    {
        if (visibleWorld == null)
        {
            return;
        }

        Rectangle view = WorldRectToMinimap(bounds, visibleWorld, viewRect);
        if (view.width <= 0f || view.height <= 0f)
        {
            return;
        }

        // Линия рисуется четырьмя тонкими полосами в уже открытом Filled-проходе,
        // так что рисовать её можно в том же вызове Show(...).
        float line = Math.min(VIEW_RECT_LINE_WIDTH, Math.min(view.width, view.height) / 2f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(view.x, view.y, view.width, line);
        shapeRenderer.rect(view.x, view.y + view.height - line, view.width, line);
        shapeRenderer.rect(view.x, view.y, line, view.height);
        shapeRenderer.rect(view.x + view.width - line, view.y, line, view.height);
    }

    // Переводит мировой прямоугольник (видимую область камеры) в координаты мини-карты:
    // X масштабируется по ширине карты, ось Y инвертирована (верх мира — верх мини-карты).
    // Часть прямоугольника за пределами карты обрезается по границам мини-карты.
    public static Rectangle WorldRectToMinimap(Rectangle bounds, Rectangle worldRect, Rectangle out)
    {
        float left = bounds.x + worldRect.x / GameWorld.MAP_WIDTH * bounds.width;
        float right = bounds.x + (worldRect.x + worldRect.width) / GameWorld.MAP_WIDTH * bounds.width;
        float bottom = bounds.y + bounds.height - worldRect.y / GameWorld.MAP_HEIGHT * bounds.height;
        float top = bounds.y + bounds.height - (worldRect.y + worldRect.height) / GameWorld.MAP_HEIGHT * bounds.height;

        float maxX = bounds.x + bounds.width;
        float maxY = bounds.y + bounds.height;
        left = Math.max(bounds.x, Math.min(left, maxX));
        right = Math.max(bounds.x, Math.min(right, maxX));
        bottom = Math.max(bounds.y, Math.min(bottom, maxY));
        top = Math.max(bounds.y, Math.min(top, maxY));

        return out.set(Math.min(left, right), Math.min(bottom, top),
                Math.abs(right - left), Math.abs(top - bottom));
    }

    // Обрабатывает клик по мини-карте: если клик попал в её границы, возвращает
    // мировые координаты соответствующей точки карты, иначе null.
    public Vector2 ScreenClickToWorld(int screenX, int flippedY)
    {
        Rectangle bounds = GetBounds();
        if (!bounds.contains(screenX, flippedY))
        {
            return null;
        }
        float worldX = (screenX - bounds.x) / bounds.width * GameWorld.MAP_WIDTH;
        float worldY = (bounds.y + bounds.height - flippedY) / bounds.height * GameWorld.MAP_HEIGHT;
        return new Vector2(worldX, worldY);
    }
}
