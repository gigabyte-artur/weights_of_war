package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;
import ru.gigabyteartur.weights_of_war.buildings.BuildingCore;
import ru.gigabyteartur.weights_of_war.buildings.BuildingTower;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCommon;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitArcher;
import ru.gigabyteartur.weights_of_war.units.UnitPriest;
import ru.gigabyteartur.weights_of_war.units.UnitScout;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;
import ru.gigabyteartur.weights_of_war.units.UnitSwordman;

// Командная панель слева: занимает область экрана, свободную от мира
// (область просмотра мира смещена вправо на COMMAND_PANEL_WIDTH, см. RtsCameraController).
public class CommandPanel
{
    public final static int MINIMAP_SIZE = 250;                             // Размер мини-карты (квадрат), px.
    public final static float MINIMAP_MARGIN = 10f;                         // Отступ мини-карты от краёв панели, px.
    public final static float BUTTONS_BLOCK_HEIGHT = 160f;                  // Высота блока кнопок Exit и «Отладка» под мини-картой, px.

    // Y-координата верха области под мини-картой: кнопки и информация о выбранном
    // объекте выводятся ниже этой границы, чтобы не залезать на мини-карту.
    public static float GetBelowMinimapTop()
    {
        return Gdx.graphics.getHeight() - MINIMAP_SIZE - MINIMAP_MARGIN * 2f;
    }

    // Отрисовывает фон командной панели в её области экрана (мир там не рисуется).
    public void Show(ShapeRenderer shapeRenderer)
    {
        shapeRenderer.setColor(0.12f, 0.12f, 0.18f, 1f);
        shapeRenderer.rect(0, 0, GameWorld.COMMAND_PANEL_WIDTH, Gdx.graphics.getHeight());
    }

    // Прямоугольник мини-карты в экранных координатах (верх командной панели).
    public Rectangle GetMinimapBounds()
    {
        float x = (GameWorld.COMMAND_PANEL_WIDTH - MINIMAP_SIZE) / 2f;
        float y = Gdx.graphics.getHeight() - MINIMAP_MARGIN - MINIMAP_SIZE;
        return new Rectangle(x, y, MINIMAP_SIZE, MINIMAP_SIZE);
    }

    // Мини-карта в верхней части панели: юниты — одна точка, строения — четыре точки, цвет по фракции.
    public void ShowMinimap(ShapeRenderer shapeRenderer, GameWorld world)
    {
        Rectangle bounds = GetMinimapBounds();

        // Фон и рамка мини-карты.
        shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 1f);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.setColor(0.4f, 0.4f, 0.5f, 1f);
        shapeRenderer.rectLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y, 2f);
        shapeRenderer.rectLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height, 2f);
        shapeRenderer.rectLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height, 2f);
        shapeRenderer.rectLine(bounds.x + bounds.width, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 2f);

        float pointSize = 3f;       // Размер одной точки на мини-карте, px.
        float gap = 1f;             // Зазор между точками строения, px.

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
                float step = pointSize + gap;
                shapeRenderer.rect(mapX - step, mapY - step, pointSize, pointSize);
                shapeRenderer.rect(mapX + gap, mapY - step, pointSize, pointSize);
                shapeRenderer.rect(mapX - step, mapY + gap, pointSize, pointSize);
                shapeRenderer.rect(mapX + gap, mapY + gap, pointSize, pointSize);
            }
            else
            {
                // Юнит отображается одной точкой.
                shapeRenderer.rect(mapX - pointSize / 2f, mapY - pointSize / 2f, pointSize, pointSize);
            }
        }
    }

    // Отображает на панели информацию о выделенном объекте.
    public void ShowUnitInfo(SpriteBatch batch, BitmapFont font, BattleObject selected, GameWorld world)
    {
        if (selected == null)
        {
            return;
        }

        // Уменьшаем масштаб шрифта, чтобы текст помещался в панель.
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(0.6f);

        float x = 15f;
        float lineHeight = 22f;
        // Выводим информацию под мини-картой, ниже блока кнопок Exit и «Отладка»,
        // чтобы текст не залезал на мини-карту.
        float y = GetBelowMinimapTop() - BUTTONS_BLOCK_HEIGHT;

        if (selected instanceof BuildingCore)
        {
            ShowCoreInfo(batch, font, (BuildingCore) selected, world, x, y, lineHeight);
        }
        else if (selected instanceof BuildingTower)
        {
            ShowTowerInfo(batch, font, (BuildingTower) selected, x, y, lineHeight);
        }
        else if (selected instanceof BattleUnitCommon)
        {
            ShowUnitStats(batch, font, (BattleUnitCommon) selected, world, x, y, lineHeight);
        }

        font.getData().setScale(prevScaleX, prevScaleY);
    }

    // Информация о выделенном юните.
    private void ShowUnitStats(SpriteBatch batch, BitmapFont font, BattleUnitCommon unit, GameWorld world, float x, float y, float lineHeight)
    {
        font.draw(batch, "Класс: " + unit.GetClassName(), x, y);
        font.draw(batch, "Фракция: " + FractionName(unit), x, y - lineHeight);
        font.draw(batch, "Здоровье: " + unit.getHealth() + " / " + unit.getMaxHealth(), x, y - lineHeight * 2);
        font.draw(batch, "Фитнесс: " + unit.getFit(), x, y - lineHeight * 3);
        font.draw(batch, "Действие: " + ActionName(unit), x, y - lineHeight * 4);
        font.draw(batch, "Застревание: " + Math.round(unit.getStuck()), x, y - lineHeight * 5);
        font.draw(batch, "Союзников рядом: " + world.CountAllies(unit, unit.getSightRange()), x, y - lineHeight * 6);
        font.draw(batch, "Врагов рядом: " + world.CountEnemies(unit, unit.getSightRange()), x, y - lineHeight * 7);

        // Цель юнита (если задана).
        BattleObject target = unit.getTargetObject();
        if (target != null)
        {
            font.draw(batch, "Цель: " + TargetName(target), x, y - lineHeight * 8);
        }

        // Значения сенсоров юнита.
        font.draw(batch, "Сенсоры:", x, y - lineHeight * 9);
        ArrayList<SensorCommon> sensors = unit.CreateSensors();
        for (int i = 0; i < sensors.size(); i++)
        {
            SensorCommon sensor = sensors.get(i);
            int value = sensor.CheckSensor(unit, world);
            font.draw(batch, sensor.GetName() + ": " + value, x, y - lineHeight * (10 + i));
        }
    }

    // Информация о выделенном ядре: класс, фракция, здоровье, лучший фитнесс живых юнитов и фитнесс сохранённых нейросетей.
    private void ShowCoreInfo(SpriteBatch batch, BitmapFont font, BuildingCore core, GameWorld world, float x, float y, float lineHeight)
    {
        font.draw(batch, "Класс: " + core.GetClassName(), x, y);
        font.draw(batch, "Фракция: " + FractionName(core), x, y - lineHeight);
        font.draw(batch, "Здоровье: " + core.getHealth() + " / " + core.getMaxHealth(), x, y - lineHeight * 2);
        font.draw(batch, "Лучший фитнесс:", x, y - lineHeight * 4);
        font.draw(batch, "Мечник: " + BestFit(world, UnitSwordman.class, core.getFraction()), x, y - lineHeight * 5);
        font.draw(batch, "Лучник: " + BestFit(world, UnitArcher.class, core.getFraction()), x, y - lineHeight * 6);
        font.draw(batch, "Щитовик: " + BestFit(world, UnitShieldman.class, core.getFraction()), x, y - lineHeight * 7);
        font.draw(batch, "Жрец: " + BestFit(world, UnitPriest.class, core.getFraction()), x, y - lineHeight * 8);
        font.draw(batch, "Разведчик: " + BestFit(world, UnitScout.class, core.getFraction()), x, y - lineHeight * 9);
        font.draw(batch, "Фитнесс ядра:", x, y - lineHeight * 11);
        font.draw(batch, "Мечник: " + core.getNetSwordmanFit(), x, y - lineHeight * 12);
        font.draw(batch, "Лучник: " + core.getNetArcherFit(), x, y - lineHeight * 13);
        font.draw(batch, "Щитовик: " + core.getNetShieldmanFit(), x, y - lineHeight * 14);
        font.draw(batch, "Жрец: " + core.getNetPriestFit(), x, y - lineHeight * 15);
        font.draw(batch, "Разведчик: " + core.getNetScoutFit(), x, y - lineHeight * 16);
    }

    // Информация о выделенной сторожевой башне: класс, фракция, здоровье.
    private void ShowTowerInfo(SpriteBatch batch, BitmapFont font, BuildingTower tower, float x, float y, float lineHeight)
    {
        font.draw(batch, "Класс: " + tower.GetClassName(), x, y);
        font.draw(batch, "Фракция: " + FractionName(tower), x, y - lineHeight);
        font.draw(batch, "Здоровье: " + tower.getHealth() + " / " + tower.getMaxHealth(), x, y - lineHeight * 2);
    }

    // Лучший фитнесс заданного класса юнитов указанной фракции (по живым юнитам).
    private int BestFit(GameWorld world, Class<? extends BattleUnitCommon> unitClass, Fraction fraction)
    {
        ArrayList<? extends BattleUnitCommon> top = world.GetTopAliveUnits(unitClass, 1, fraction);
        if (!top.isEmpty())
        {
            return top.get(0).getFit();
        }
        return 0;
    }

    // Возвращает имя фракции объекта или «Нет», если фракция отсутствует.
    private String FractionName(BattleObject object)
    {
        if (object.getFraction() != null)
        {
            return object.getFraction().getName();
        }
        return "Нет";
    }

    // Возвращает название выполняемого действия юнита или «—», если команд нет.
    private String ActionName(BattleUnitCommon unit)
    {
        CommandUnit command = unit.GetCurrentCommand();
        if (command != null)
        {
            return command.GetName();
        }
        return "—";
    }

    // Возвращает краткое описание цели: класс, фракция и здоровье.
    private String TargetName(BattleObject target)
    {
        String className;
        if (target instanceof BattleUnitCommon)
        {
            className = ((BattleUnitCommon) target).GetClassName();
        }
        else if (target instanceof BattleBuildingCommon)
        {
            className = ((BattleBuildingCommon) target).GetClassName();
        }
        else
        {
            className = target.getClass().getSimpleName();
        }
        return className + " (" + FractionName(target) + ", " + target.getHealth() + "/" + target.getMaxHealth() + " HP)";
    }
}
