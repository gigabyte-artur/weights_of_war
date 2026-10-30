package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Тесты перевода координат мини-карты: точки объектов, белая рамка видимой области
// и обработка клика. Север мира (+Y) должен быть сверху мини-карты, а клик —
// возвращать ровно тот участок карты, который нарисован под курсором.
public class MiniMapTest extends HeadlessGdxTest
{
    private final Rectangle bounds = new Rectangle(10f, 20f, 250f, 250f);

    // Мировой X/Y -> экранный X/Y мини-карты через публичный перевод.
    private Vector2 world(float x, float y)
    {
        return MiniMap.WorldToMinimap(bounds, x, y, new Vector2());
    }

    @Test
    public void wholeMapCoversWholeMinimap()
    {
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, 0f, GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT), new Rectangle());

        assertEquals(bounds, out);
    }

    @Test
    public void northOfWorldIsAtTopOfMinimap()
    {
        // Север (большой Y мира) — верх мини-карты (большой Y экрана).
        assertTrue(world(0f, GameWorld.MAP_HEIGHT).y > world(0f, 0f).y,
                "north of the world must be drawn above its south");

        // Крайние значения совпадают с границами мини-карты.
        assertEquals(bounds.y, world(0f, 0f).y, 0.01f);
        assertEquals(bounds.y + bounds.height, world(0f, GameWorld.MAP_HEIGHT).y, 0.01f);
        assertEquals(bounds.x, world(0f, 0f).x, 0.01f);
        assertEquals(bounds.x + bounds.width, world(GameWorld.MAP_WIDTH, 0f).x, 0.01f);
    }

    @Test
    public void worldRectangleMapsToProportionalMinimapRectangle()
    {
        // Четверть карты (левый нижний угол мира) — четверть мини-карты в её левом нижнем углу.
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, 0f, GameWorld.MAP_WIDTH / 2f, GameWorld.MAP_HEIGHT / 2f), new Rectangle());

        assertEquals(10f, out.x, 0.01f);
        assertEquals(20f, out.y, 0.01f);
        assertEquals(125f, out.width, 0.01f);
        assertEquals(125f, out.height, 0.01f);
    }

    @Test
    public void viewRectUsesSameVerticalMappingAsObjectDots()
    {
        // Рамка обязана совпадать с точками объектов: та же формула Y, что и в WorldToMinimap.
        // Верхняя половина мира — верхняя половина мини-карты.
        Rectangle out = MiniMap.WorldRectToMinimap(bounds,
                new Rectangle(0f, GameWorld.MAP_HEIGHT / 2f, GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT / 2f), new Rectangle());

        assertEquals(20f + 125f, out.y, 0.01f);
        assertEquals(125f, out.height, 0.01f);
        assertEquals(250f, out.width, 0.01f);

        // И та же точка мира даёт одну и ту же координату обоими путями.
        Vector2 dot = world(0f, GameWorld.MAP_HEIGHT);
        assertEquals(out.y + out.height, dot.y, 0.01f);
    }

    @Test
    public void objectInsideViewLiesInsideViewRect()
    {
        Rectangle worldView = new Rectangle(1000f, 1000f, 1000f, 1000f);
        Rectangle out = MiniMap.WorldRectToMinimap(bounds, worldView, new Rectangle());

        Vector2 dot = world(1500f, 1500f);
        assertTrue(out.contains(dot.x, dot.y), "object dot must lie inside the view rectangle");
    }

    @Test
    public void objectOutsideViewLiesOutsideViewRect()
    {
        Rectangle worldView = new Rectangle(1000f, 1000f, 1000f, 1000f);
        Rectangle out = MiniMap.WorldRectToMinimap(bounds, worldView, new Rectangle());

        Vector2 dot = world(3500f, 3500f);
        assertFalse(out.contains(dot.x, dot.y), "object dot outside the camera view must be outside the rectangle");
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
        assertEquals(20f + 1900f / GameWorld.MAP_HEIGHT * 250f, out.y, 0.01f);
        assertEquals(12.5f, out.width, 0.01f);
        assertEquals(12.5f, out.height, 0.01f);
    }

    @Test
    public void clickReturnsTheWorldAreaDrawnUnderTheCursor()
    {
        // Клик по точке мини-карты возвращает ровно ту мировую точку, которая там нарисована:
        // обе формулы взаимно обратны, поэтому клик не может увести камеру не туда.
        Vector2[] samples = {
                new Vector2(0f, 0f),
                new Vector2(2000f, 2000f),
                new Vector2(GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT),
                new Vector2(1234f, 3800f),
        };

        for (Vector2 sample : samples)
        {
            Vector2 onMap = MiniMap.WorldToMinimap(bounds, sample.x, sample.y, new Vector2());
            Vector2 back = MiniMap.MinimapToWorld(bounds, onMap.x, onMap.y, new Vector2());

            assertEquals(sample.x, back.x, 0.01f);
            assertEquals(sample.y, back.y, 0.01f);
        }
    }

    @Test
    public void clickNearTopOfMinimapSelectsNorthOfWorld()
    {
        // Клик у верхнего края мини-карты — это север мира, а не юг.
        float nearTopY = bounds.y + bounds.height * 0.9f;
        Vector2 target = MiniMap.MinimapToWorld(bounds, bounds.x + bounds.width / 2f, nearTopY, new Vector2());

        assertTrue(target.y > GameWorld.MAP_HEIGHT * 0.5f,
                "a click near the top of the minimap must aim north of the map");
        assertTrue(target.y <= GameWorld.MAP_HEIGHT);
    }

    @Test
    public void clickCentersViewRectOnClickedPoint()
    {
        // Сквозная цепочка: клик по мини-карте -> мировая точка -> камера -> видимая область
        // -> рамка на мини-карте. Рамка обязана встать вокруг точки клика; раньше клик и
        // рамка считали Y по-разному, и рамка уезжала на противоположную половину карты.
        final int screenW = 800;
        final int screenH = 600;
        OrthographicCamera camera = new OrthographicCamera();
        FitViewport viewport = new FitViewport(screenW, screenH, camera);
        viewport.update(screenW, screenH, true);

        RtsCameraController controller = new RtsCameraController(camera, viewport);
        controller.setMapBounds(new Rectangle(0f, 0f, GameWorld.MAP_WIDTH, GameWorld.MAP_HEIGHT));

        // Клик в северной половине мини-карты, вдали от краёв (чтобы камера не прижалась к границе).
        float clickX = bounds.x + bounds.width * 0.5f;
        float clickY = bounds.y + bounds.height * 0.7f;

        Vector2 worldPoint = MiniMap.MinimapToWorld(bounds, clickX, clickY, new Vector2());
        controller.centerOn(worldPoint.x, worldPoint.y);

        Rectangle visible = controller.getVisibleWorldRect(new Rectangle());
        Rectangle rect = MiniMap.WorldRectToMinimap(bounds, visible, new Rectangle());

        float rectCenterX = rect.x + rect.width * 0.5f;
        float rectCenterY = rect.y + rect.height * 0.5f;

        assertEquals(clickX, rectCenterX, 0.5f, "view rectangle must be centred on the clicked column");
        assertEquals(clickY, rectCenterY, 0.5f, "view rectangle must be centred on the clicked row");
        assertTrue(worldPoint.y > GameWorld.MAP_HEIGHT * 0.5f,
                "a click in the upper half of the minimap must aim north of the map");
    }
}
