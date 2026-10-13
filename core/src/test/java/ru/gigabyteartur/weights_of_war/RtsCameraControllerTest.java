package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RtsCameraControllerTest extends HeadlessGdxTest
{
    private static final int SCREEN_W = 800;
    private static final int SCREEN_H = 600;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private RtsCameraController controller;

    @BeforeEach
    public void setUp()
    {
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_W, SCREEN_H, camera);
        viewport.update(SCREEN_W, SCREEN_H, true);
        controller = new RtsCameraController(camera, viewport);
    }

    @Test
    public void panUpWhenWKeyPressed()
    {
        controller.centerOn(0f, 0f);
        float y0 = camera.position.y;

        controller.keyDown(Input.Keys.W);
        controller.update(1f);

        assertEquals(y0 + controller.panSpeed, camera.position.y, 0.01f);
        assertEquals(0f, camera.position.x, 0.01f);
    }

    @Test
    public void panDownWhenSKeyPressed()
    {
        controller.centerOn(0f, 0f);
        float y0 = camera.position.y;

        controller.keyDown(Input.Keys.S);
        controller.update(1f);

        assertEquals(y0 - controller.panSpeed, camera.position.y, 0.01f);
    }

    @Test
    public void panLeftWhenAKeyPressed()
    {
        controller.centerOn(0f, 0f);
        float x0 = camera.position.x;

        controller.keyDown(Input.Keys.A);
        controller.update(1f);

        assertEquals(x0 - controller.panSpeed, camera.position.x, 0.01f);
    }

    @Test
    public void panRightWhenDKeyPressed()
    {
        controller.centerOn(0f, 0f);
        float x0 = camera.position.x;

        controller.keyDown(Input.Keys.D);
        controller.update(1f);

        assertEquals(x0 + controller.panSpeed, camera.position.x, 0.01f);
    }

    @Test
    public void stopsPanningAfterKeyReleased()
    {
        controller.centerOn(0f, 0f);

        controller.keyDown(Input.Keys.W);
        controller.update(1f);
        float yAfterPress = camera.position.y;

        controller.keyUp(Input.Keys.W);
        controller.update(1f);

        assertEquals(yAfterPress, camera.position.y, 0.01f);
    }

    @Test
    public void zoomInClampsToMinZoom()
    {
        // Многократное приближение не должно опустить zoom ниже minZoom.
        for (int i = 0; i < 100; i++)
        {
            controller.zoomAt(SCREEN_W / 2f, SCREEN_H / 2f, 1f);
        }
        assertEquals(controller.minZoom, camera.zoom, 0.0001f);
        assertTrue(Float.isFinite(camera.zoom));
        assertTrue(camera.zoom > 0f);
    }

    @Test
    public void zoomOutClampsToMaxZoom()
    {
        for (int i = 0; i < 100; i++)
        {
            controller.zoomAt(SCREEN_W / 2f, SCREEN_H / 2f, -1f);
        }
        assertEquals(controller.maxZoom, camera.zoom, 0.0001f);
        assertTrue(Float.isFinite(camera.zoom));
    }

    @Test
    public void zoomWithZeroAmountDoesNotChangeZoom()
    {
        float zoomBefore = camera.zoom;
        controller.zoomAt(SCREEN_W / 2f, SCREEN_H / 2f, 0f);
        assertEquals(zoomBefore, camera.zoom, 0.0001f);
    }

    @Test
    public void zoomToCursorKeepsWorldPointUnderCursorFixed()
    {
        controller.centerOn(0f, 0f);

        float cursorX = SCREEN_W * 0.75f;
        float cursorY = SCREEN_H * 0.25f;

        Vector2 before = new Vector2();
        viewport.unproject(before.set(cursorX, cursorY));

        controller.zoomAt(cursorX, cursorY, 1f); // приближение.

        Vector2 after = new Vector2();
        viewport.unproject(after.set(cursorX, cursorY));

        assertEquals(before.x, after.x, 0.5f);
        assertEquals(before.y, after.y, 0.5f);
        assertTrue(camera.zoom < 1f); // действительно приблизились.
    }

    @Test
    public void clampsCameraToMapBounds()
    {
        controller.setMapBounds(new Rectangle(0, 0, 4000, 4000));
        controller.centerOn(0f, 0f);

        // Видимая область 800x600 при zoom=1: половинные размеры 400 и 300.
        assertEquals(400f, camera.position.x, 0.01f);
        assertEquals(300f, camera.position.y, 0.01f);
    }

    @Test
    public void clampsCameraAtTopRightCorner()
    {
        controller.setMapBounds(new Rectangle(0, 0, 4000, 4000));
        controller.centerOn(5000f, 5000f);

        // Карта 4000x4000, видимая область 800x600: максимум 4000-400 и 4000-300.
        assertEquals(3600f, camera.position.x, 0.01f);
        assertEquals(3700f, camera.position.y, 0.01f);
    }

    @Test
    public void centersMapOnAxisWhenMapSmallerThanViewport()
    {
        // Карта 100x100 меньше видимой области 800x600 — камера центрирует карту.
        controller.setMapBounds(new Rectangle(0, 0, 100, 100));
        controller.centerOn(0f, 0f);

        assertEquals(50f, camera.position.x, 0.01f);
        assertEquals(50f, camera.position.y, 0.01f);
    }

    @Test
    public void doesNotClampWhenMapBoundsIsNull()
    {
        controller.setMapBounds(null);
        controller.centerOn(10000f, 10000f);

        assertEquals(10000f, camera.position.x, 0.01f);
        assertEquals(10000f, camera.position.y, 0.01f);
    }

    @Test
    public void zeroDeltaDoesNotMoveCamera()
    {
        controller.centerOn(0f, 0f);
        float x0 = camera.position.x;
        float y0 = camera.position.y;

        controller.keyDown(Input.Keys.W);
        controller.update(0f);

        assertEquals(x0, camera.position.x, 0.01f);
        assertEquals(y0, camera.position.y, 0.01f);
    }

    @Test
    public void getVisibleWorldRectReturnsVisibleWorldArea()
    {
        controller.centerOn(100f, 100f);

        Rectangle out = new Rectangle();
        controller.getVisibleWorldRect(out);

        // Видимая область 800x600 с центром в (100,100).
        assertEquals(-300f, out.x, 0.01f);
        assertEquals(-200f, out.y, 0.01f);
        assertEquals(800f, out.width, 0.01f);
        assertEquals(600f, out.height, 0.01f);
    }

    @Test
    public void zoomNearBoundaryKeepsCameraWithinMapBounds()
    {
        controller.setMapBounds(new Rectangle(0, 0, 2000, 2000));
        controller.centerOn(0f, 0f); // прижатие к левому-нижнему углу.
        controller.zoomAt(0f, 0f, -1f); // отдаление вблизи границы.

        Rectangle visible = new Rectangle();
        controller.getVisibleWorldRect(visible);

        assertTrue(visible.x >= 0f, "visible.x = " + visible.x);
        assertTrue(visible.y >= 0f, "visible.y = " + visible.y);
        assertTrue(visible.x + visible.width <= 2000f + 0.01f, "right edge = " + (visible.x + visible.width));
        assertTrue(visible.y + visible.height <= 2000f + 0.01f, "top edge = " + (visible.y + visible.height));
    }
}
