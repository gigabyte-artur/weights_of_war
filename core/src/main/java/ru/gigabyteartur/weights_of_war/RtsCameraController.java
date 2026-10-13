package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

// Контроллер камеры в стиле 2D RTS: управляет OrthographicCamera, панорамирует мир
// клавишами WASD и приближает/удаляет взгляд колёсиком мыши к курсору.
// Работает только с мировыми координатами; экранные координаты преобразуются через
// viewport.apply(...) + camera.unproject(...) (корректно для смещённого вьюпорта).
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

    // Смещение области просмотра по X в пикселях: мир рисуется справа от командной панели,
    // а не под ней. Обновляется при resize() из Main.
    private int viewportOffsetX;

    private Rectangle mapBounds;      // Границы карты; null — без ограничений.
    private CameraTarget followTarget; // Цель слежения (заглушка на первом этапе).
    private boolean uiHovered;         // Курсор над UI (заглушка для edge/drag pan).

    private boolean panUp;
    private boolean panDown;
    private boolean panLeft;
    private boolean panRight;

    private final Vector2 tmpBefore = new Vector2();
    private final Vector3 tmpVec3 = new Vector3();
    private final Rectangle tmpVisibleRect = new Rectangle();

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

    // Пересоздаёт область просмотра мира: она занимает только свободную от командной панели
    // часть экрана (справа от неё), чтобы панель не перекрывала мир.
    public void updateViewport(int screenWidth, int screenHeight, int panelWidth)
    {
        viewportOffsetX = Math.min(panelWidth, Math.max(0, screenWidth - 1));
        int worldWidth = Math.max(1, screenWidth - viewportOffsetX);
        // Виртуальные размеры вьюпорта ставим равными размеру области мира в пикселях:
        // тогда масштаб FitViewport равен 1 и каждая единица камеры соответствует одному
        // пикселу экрана. Иначе (как при виртуальных размерах всего окна) из-за letterbox
        // по горизонтали мировые координаты растягиваются/сжимаются неравномерно —
        // объекты выглядят «вытянутыми по вертикали», а unproject путает оси.
        camera.viewportWidth = worldWidth;
        camera.viewportHeight = screenHeight;
        viewport.update(worldWidth, screenHeight, false);
        // Смещаем вьюпорт вправо: пиксели под панелью (x < panelWidth) миром не занимают.
        viewport.setScreenBounds(viewportOffsetX, 0, worldWidth, screenHeight);
        // Не трогаем GL-состояние здесь: Main.render() сам вызывает viewport.apply(false)
        // перед отрисовкой мира; лишние apply из обработчиков ввода ломали проекцию.

        // После изменения размеров вьюпорта мировые границы видимой области смещаются —
        // прижимаем камеру к карте и центрируем её в новой видимой области.
        clampToMapBounds();
        if (mapBounds != null)
        {
            Rectangle visible = getVisibleWorldRect(tmpVisibleRect);
            camera.position.x = mapBounds.x + mapBounds.width * 0.5f;
            camera.position.y = visible.y + visible.height * 0.5f;
            clampToMapBounds();
        }
        camera.update();
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

    // Преобразует экранные координаты (Gdx.input, Y сверху вниз) в мировые.
    // Учитывает смещённую область просмотра: мир рисуется только справа от командной панели.
    public Vector2 unprojectScreenPoint(float screenX, float screenY)
    {
        toViewportNdc(screenX, screenY, tmpVec3);
        camera.unproject(tmpVec3);
        return tmpBefore.set(tmpVec3.x, tmpVec3.y);
    }

    // Переводит экранные координаты мыши (верхний левый угол окна — начало отсчёта, Y вниз)
    // в NDC-координаты (-1..1) ОБЛАСТИ ПРОСМОТРА МИРА.
    // Обязательно учитывается реальный прямоугольник вьюпорта (setScreenBounds):
    // после смещения области просмотра влево на ширину командной панели её левый край
    // находится не на x = 0, поэтому прямое camera.unproject без этой коррекции давал
    // неверные мировые координаты клика (юниты не выделялись), а игнорирование
    // соотношения сторон при вычислении NDC по вертикали приводило к «вытягиванию»
    // объектов и рассогласованию осей X/Y.
    private void toViewportNdc(float screenX, float screenY, Vector3 out)
    {
        int winW = Gdx.graphics.getWidth();
        int winH = Gdx.graphics.getHeight();
        float glX = screenX;                  // GL: Y снизу вверх, X тот же
        float glY = winH - screenY;
        float relX = (glX - viewport.getScreenX()) / Math.max(1, viewport.getScreenWidth());
        float relY = (glY - viewport.getScreenY()) / Math.max(1, viewport.getScreenHeight());
        out.set(relX * 2f - 1f, relY * 2f - 1f, 0f);
    }

    // Зум к курсору: world-точка под экранными координатами (screenX, screenY) остаётся
    // на месте. amount > 0 — приближение, amount < 0 — удаление.
    public void zoomAt(float screenX, float screenY, float amount)
    {
        if (amount == 0f)
        {
            return;
        }

        toViewportNdc(screenX, screenY, tmpVec3);
        camera.unproject(tmpVec3);
        // Вектор от центра камеры до точки под курсором (в мировых единицах).
        float dx = tmpVec3.x - camera.position.x;
        float dy = tmpVec3.y - camera.position.y;

        float scale = (float) Math.pow(zoomFactor, -amount);
        float newZoom = MathUtils.clamp(camera.zoom * scale, minZoom, maxZoom);
        float ratio = newZoom / camera.zoom; // отношение нового зума к старому
        camera.zoom = newZoom;
        camera.update();

        // Точка под курсором должна остаться на месте: вектор до неё
        // сжимается/растягивается пропорционально зуму.
        camera.position.x += dx * (1f - ratio);
        camera.position.y += dy * (1f - ratio);

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
