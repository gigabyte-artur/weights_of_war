package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Тесты перевода мировой видимой области в координаты мини-карты (белая рамка на мини-карте).
public class MiniMapTest
{
    private final Rectangle bounds = new Rectangle(10f, 20f, 250f, 250f);

    @Test
    public void wholeMapCoversWholeMinimap()
    {
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, 0f, GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT), new Rectangle());

        assertEquals(bounds, out);
    }

    @Test
    public void worldRectangleMapsToProportionalMinimapRectangle()
    {
        // Четверть карты (левый нижний угол мира) — четверть мини-карты, с той же
        // вертикальной проекцией, что и у точек объектов (см. следующий тест).
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, 0f, GameWorld.MAP_WIDTH / 2f, GameWorld.MAP_HEIGHT / 2f), new Rectangle());

        assertEquals(10f, out.x, 0.01f);
        assertEquals(145f, out.y, 0.01f);
        assertEquals(125f, out.width, 0.01f);
        assertEquals(125f, out.height, 0.01f);
    }

    @Test
    public void viewRectUsesSameVerticalMappingAsObjectDots()
    {
        // Рамка обязана совпадать с точками объектов: та же формула Y, что и в Show(...).
        // Верхняя половина мира — нижняя половина мини-карты (так же, как сейчас рисуются точки).
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, GameWorld.MAP_HEIGHT / 2f, GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT / 2f), new Rectangle());

        assertEquals(20f, out.y, 0.01f);
        assertEquals(125f, out.height, 0.01f);
        assertEquals(250f, out.width, 0.01f);
    }

    @Test
    public void objectInsideViewLiesInsideViewRect()
    {
        // Точка объекта, отрисованная по формуле из Show(...), должна оказаться внутри рамки
        // видимой области — иначе рамка обводит не тот участок.
        Rectangle worldView = new Rectangle(1000f, 1000f, 1000f, 1000f);
        Rectangle out = MiniMap.WorldRectToMinimap(bounds, worldView, new Rectangle());

        float mapX = bounds.x + 1500f / GameWorld.MAP_WIDTH * bounds.width;
        float mapY = bounds.y + bounds.height - 1500f / GameWorld.MAP_HEIGHT * bounds.height;

        assertTrue(out.contains(mapX, mapY), "object dot must lie inside the view rectangle");
    }

    @Test
    public void objectOutsideViewLiesOutsideViewRect()
    {
        Rectangle worldView = new Rectangle(1000f, 1000f, 1000f, 1000f);
        Rectangle out = MiniMap.WorldRectToMinimap(bounds, worldView, new Rectangle());

        float mapX = bounds.x + 3500f / GameWorld.MAP_WIDTH * bounds.width;
        float mapY = bounds.y + bounds.height - 3500f / GameWorld.MAP_HEIGHT * bounds.height;

        assertFalse(out.contains(mapX, mapY), "object dot outside the camera view must be outside the rectangle");
    }

    @Test
    public void viewRectIsClampedToMinimapBounds()
    {
        // Камера не выходит за карту, но рамка не должна рисоваться за её границами.
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(-2000f, -2000f, GameWorld.MAP_WIDTH * 2f, GameWorld.MAP_HEIGHT * 2f), new Rectangle());

        assertEquals(bounds, out);
    }

    @Test
    public void zoomedInViewRectIsSmallerThanMinimap()
    {
        // Близкий зум: рамка занимает малую часть мини-карты.
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(1900f, 1900f, 200f, 200f), new Rectangle());

        assertEquals(10f + 1900f / GameWorld.MAP_WIDTH * 250f, out.x, 0.01f);
        assertEquals(12.5f, out.width, 0.01f);
        assertEquals(12.5f, out.height, 0.01f);
    }
}
