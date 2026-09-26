package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;
import ru.gigabyteartur.weights_of_war.units.UnitShieldman;

// Базовый класс для боевых объектов (юнитов и зданий).
public abstract class BattleObject extends PlacedObject
{
    private int health = 100;       // здоровье объекта.
    private int maxHealth = 100;    // максимальное здоровье объекта.
    private int armor = 0;          // броня объекта.
    private int sightRange = 0;     // радиус обзора объекта.
    private Fraction fraction;      // фракция объекта.
    private boolean selected = false;   // Флаг выделения объекта.

    public BattleObject()
    {
        super();
    }

    public BattleObject(int x, int y)
    {
        super(x, y);
    }

    public int getHealth()
    {
        return health;
    }

    public void setHealth(int health)
    {
        this.health = health;
    }

    public int getMaxHealth()
    {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth)
    {
        this.maxHealth = maxHealth;
    }

    public int getArmor()
    {
        return armor;
    }

    public void setArmor(int armor)
    {
        this.armor = armor;
    }

    public int getSightRange()
    {
        return sightRange;
    }

    public void setSightRange(int sightRange)
    {
        this.sightRange = sightRange;
    }

    public Fraction getFraction()
    {
        return fraction;
    }

    public void setFraction(Fraction fraction)
    {
        this.fraction = fraction;
    }

    // Возвращает, выделен ли объект.
    public boolean isSelected()
    {
        return selected;
    }

    // Устанавливает флаг выделения объекта.
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    // Объект считается мёртвым, если здоровье меньше или равно 0.
    @Override
    public boolean IsDead()
    {
        return health <= 0;
    }

    // Определяет, является ли другой объект врагом (другая фракция).
    public boolean IsEnemy(BattleObject object)
    {
        return fraction != object.fraction;
    }

    // Возвращает евклидово расстояние до другого боевого объекта.
    public float GetDistance(BattleObject object)
    {
        int dx = object.getX() - getX();
        int dy = object.getY() - getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // Возвращает кандидата как BattleObject, если это живой боевой объект (не сам this),
    // находящийся в пределах области видимости по осям (radius); иначе возвращает null.
    public BattleObject FindSightCandidate(PlacedObject unit, int radius)
    {
        if (unit == this || !(unit instanceof BattleObject) || unit.IsDead())
        {
            return null;
        }
        BattleObject candidate = (BattleObject) unit;
        if (Math.abs(getX() - candidate.getX()) > radius ||
            Math.abs(getY() - candidate.getY()) > radius)
        {
            return null;
        }
        return candidate;
    }

    // Возвращает квадрат евклидова расстояния до другого объекта (без извлечения корня,
    // для дешёвого сравнения расстояний).
    public float GetDistanceSquared(PlacedObject object)
    {
        float dx = object.getX() - getX();
        float dy = object.getY() - getY();
        return dx * dx + dy * dy;
    }

    // Отрисовка боевого объекта.
    @Override
    public void Show(SpriteBatch batch)
    {
        batch.draw(mainTexture, getX(), getY(), getWidth(), getHeight());
        DrawFactionCircle(batch);
        DrawHealthBar(batch);
        if (selected)
        {
            DrawSelectionFrame(batch);
        }
    }

    // Отрисовка белой рамки вокруг выделенного объекта.
    protected void DrawSelectionFrame(SpriteBatch batch)
    {
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();
        float thickness = 2f;

        batch.setColor(Color.WHITE);
        batch.draw(getWhitePixel(), x, y, w, thickness);
        batch.draw(getWhitePixel(), x, y + h - thickness, w, thickness);
        batch.draw(getWhitePixel(), x, y, thickness, h);
        batch.draw(getWhitePixel(), x + w - thickness, y, thickness, h);
        batch.setColor(Color.WHITE);
    }

    // Отрисовка полоски здоровья под объектом.
    protected void DrawHealthBar(SpriteBatch batch)
    {
        float barWidth = getWidth();
        float barHeight = 5f;
        float barX = getX();
        float barY = getY() - barHeight - 3f;

        float healthRatio = (getMaxHealth() > 0) ? (float) getHealth() / getMaxHealth() : 0f;
        if (healthRatio < 0f)
        {
            healthRatio = 0f;
        }
        if (healthRatio > 1f)
        {
            healthRatio = 1f;
        }

        // Тёмный фон полоски.
        batch.setColor(0.2f, 0.2f, 0.2f, 1f);
        batch.draw(getWhitePixel(), barX, barY, barWidth, barHeight);

        // Красная часть — текущее здоровье.
        batch.setColor(Color.RED);
        batch.draw(getWhitePixel(), barX, barY, barWidth * healthRatio, barHeight);

        batch.setColor(Color.WHITE);
    }

    // Отрисовка кружка цвета фракции в левом верхнем углу объекта.
    protected void DrawFactionCircle(SpriteBatch batch)
    {
        if (getFraction() == null)
        {
            return;
        }

        float circleSize = 6f;
        float circleX = getX();
        float circleY = getY() + getHeight() - circleSize;

        batch.setColor(getFraction().getColor());
        batch.draw(getWhiteCircle(), circleX, circleY, circleSize, circleSize);
        batch.setColor(Color.WHITE);
    }

    // Белая текстура 1x1 для отрисовки прямоугольников.
    protected static Texture whitePixel;

    protected static Texture getWhitePixel()
    {
        if (whitePixel == null)
        {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whitePixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return whitePixel;
    }

    // Белая текстура с кружком для отрисовки маркера фракции.
    private static Texture whiteCircle;

    private static Texture getWhiteCircle()
    {
        if (whiteCircle == null)
        {
            int size = 6;
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(size / 2, size / 2, size / 2);
            whiteCircle = new Texture(pixmap);
            pixmap.dispose();
        }
        return whiteCircle;
    }

}
