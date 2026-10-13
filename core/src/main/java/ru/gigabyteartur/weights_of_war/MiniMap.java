package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;

// Мини-карта командной панели: занимает верхнюю область панели, отображает все
// живые объекты мира (юниты — одна точка, строения — четыре точки, цвет по фракции)
// и позволяет кликом центрировать камеру мира на выбранной точке.
public class MiniMap
{
    public final static int SIZE = 250;                                     // Размер мини-карты (квадрат), px.
    public final static float MARGIN = 10f;                                 // Отступ мини-карты от краёв панели, px.

    private final static float POINT_SIZE = 3f;                             // Размер одной точки на мини-карте, px.
    private final static float POINT_GAP = 1f;                              // Зазор между точками строения, px.

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

    // Рисует мини-карту: фон, рамку и точки объектов мира.
    public void Show(ShapeRenderer shapeRenderer, GameWorld world)
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
