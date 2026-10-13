package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

// Контроллер камеры в стиле 2D RTS: управляет OrthographicCamera, панорамирует мир
// клавишами WASD и приближает/удаляет взгляд колёсиком мыши к курсору.
// Работает только с мировыми координатами; экранные координаты преобразуются через
// viewport.unproject(...) (корректно для letterbox-вьюпорта).
public final class RtsCameraController extends InputAdapter
{
    public float minZoom;
    public float maxZoom;
    public float panSpeed;
    public float edgeBorder;
    public float zoomFactor;

    public boolean edgeScrollingEnabled;
    public boolean keyScrollingEnabled;
    public boolean dragPanningEnabled;

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private Rectangle mapBounds;      // Границы карты; null — без ограничений.
    private CameraTarget followTarget; // Цель слежения (заглушка на первом этапе).
    private boolean uiHovered;         // Курсор над UI (заглушка для edge/drag pan).

    private boolean panUp;
    private boolean panDown;
    private boolean panLeft;
    private boolean panRight;

    private final Vector2 tmpBefore = new Vector2();
    private final Vector2 tmpAfter = new Vector2();

    public RtsCameraController(OrthographicCamera camera, Viewport viewport)
    {
        this.camera = camera;
        this.viewport = viewport;

        minZoom = 0.35f;
        maxZoom = 3.0f;
        panSpeed = 1000.0f;
        edgeBorder = 18.0f;
        zoomFactor = 1.15f;

        edgeScrollingEnabled = true;
        keyScrollingEnabled = true;
        dragPanningEnabled = true;
    }

    // Обновляет камеру: применяет панорамирование зажатыми клавишами и прижимает её к границам карты.
    public void update(float delta)
    {
        if (followTarget != null)
        {
            // Заглушка: активное слежение камеры на первом этапе не реализовано.
        }

        if (keyScrollingEnabled)
        {
            float dx = 0f;
            float dy = 0f;
            if (panUp) dy += 1f;
            if (panDown) dy -= 1f;
            if (panLeft) dx -= 1f;
            if (panRight) dx += 1f;

            if (dx != 0f || dy != 0f)
            {
                // Нормализация: диагональное движение не быстрее осевого.
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                camera.position.x += (dx / length) * panSpeed * delta;
                camera.position.y += (dy / length) * panSpeed * delta;
            }
        }

        clampToMapBounds();
        camera.update();
    }

    // Задаёт границы карты; null — работа без ограничений.
    public void setMapBounds(Rectangle bounds)
    {
        mapBounds = bounds;
    }

    // Центрирует камеру на точке мира (с учётом границ карты).
    public void centerOn(float x, float y)
    {
        camera.position.set(x, y, 0f);
        clampToMapBounds();
        camera.update();
    }

    public void setFollowTarget(CameraTarget target)
    {
        followTarget = target;
    }

    public void clearFollowTarget()
    {
        followTarget = null;
    }

    // Сообщает, что курсор находится над UI (заглушка: на первом этапе ни на что не влияет).
    public void setUiHovered(boolean uiHovered)
    {
        this.uiHovered = uiHovered;
    }

    // Заполняет out прямоугольником видимой области мира и возвращает его.
    public Rectangle getVisibleWorldRect(Rectangle out)
    {
        float halfWidth = camera.viewportWidth * camera.zoom * 0.5f;
        float halfHeight = camera.viewportHeight * camera.zoom * 0.5f;
        return out.set(
                camera.position.x - halfWidth,
                camera.position.y - halfHeight,
                halfWidth * 2f,
                halfHeight * 2f);
    }

    // Зум к курсору: world-точка под экранными координатами (screenX, screenY) остаётся
    // на месте. amount > 0 — приближение, amount < 0 — удаление.
    public void zoomAt(float screenX, float screenY, float amount)
    {
        if (amount == 0f)
        {
            return;
        }

        Vector2 before = viewport.unproject(tmpBefore.set(screenX, screenY));

        float scale = (float) Math.pow(zoomFactor, -amount);
        camera.zoom = MathUtils.clamp(camera.zoom * scale, minZoom, maxZoom);
        camera.update();

        Vector2 after = viewport.unproject(tmpAfter.set(screenX, screenY));
        camera.position.x += before.x - after.x;
        camera.position.y += before.y - after.y;

        clampToMapBounds();
        camera.update();
    }

    @Override
    public boolean keyDown(int keycode)
    {
        switch (keycode)
        {
            case Input.Keys.W: panUp = true; return true;
            case Input.Keys.S: panDown = true; return true;
            case Input.Keys.A: panLeft = true; return true;
            case Input.Keys.D: panRight = true; return true;
            default: return false;
        }
    }

    @Override
    public boolean keyUp(int keycode)
    {
        switch (keycode)
        {
            case Input.Keys.W: panUp = false; return true;
            case Input.Keys.S: panDown = false; return true;
            case Input.Keys.A: panLeft = false; return true;
            case Input.Keys.D: panRight = false; return true;
            default: return false;
        }
    }

    @Override
    public boolean scrolled(float amountX, float amountY)
    {
        zoomAt(Gdx.input.getX(), Gdx.input.getY(), amountY);
        return true;
    }

    // Прижимает камеру к границам карты так, чтобы видимая область не выходила за пределы мира.
    // Если карта меньше видимой области по какой-либо оси — центрирует карту по этой оси.
    private void clampToMapBounds()
    {
        if (mapBounds == null)
        {
            return;
        }

        float visibleWidth = camera.viewportWidth * camera.zoom;
        float visibleHeight = camera.viewportHeight * camera.zoom;
        float halfWidth = visibleWidth * 0.5f;
        float halfHeight = visibleHeight * 0.5f;

        if (mapBounds.width <= visibleWidth)
        {
            camera.position.x = mapBounds.x + mapBounds.width * 0.5f;
        }
        else
        {
            camera.position.x = MathUtils.clamp(
                    camera.position.x,
                    mapBounds.x + halfWidth,
                    mapBounds.x + mapBounds.width - halfWidth);
        }

        if (mapBounds.height <= visibleHeight)
        {
            camera.position.y = mapBounds.y + mapBounds.height * 0.5f;
        }
        else
        {
            camera.position.y = MathUtils.clamp(
                    camera.position.y,
                    mapBounds.y + halfHeight,
                    mapBounds.y + mapBounds.height - halfHeight);
        }
    }
}
