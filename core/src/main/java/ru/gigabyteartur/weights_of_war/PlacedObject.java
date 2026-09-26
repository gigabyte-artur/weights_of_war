package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

// Базовый класс объекта, размещённого на игровом поле.
public abstract class PlacedObject
{
    private int x;                  // координаты объекта
    private int y;                  // координаты объекта
    private int width;              // ширина объекта
    private int height;             // высота объекта
    private String name;            // имя объекта
    protected Texture mainTexture;  // основная текстура объекта

    public PlacedObject()
    {
        x = 0;
        y = 0;
    }

    public PlacedObject(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public void setXY(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public void setWidthHeight(int width, int height)
    {
        if (width > 0 && height > 0)
        {
            this.width = width;
            this.height = height;
        }
        else
        {
            this.width = 0;
            this.height = 0;
            System.out.println("Error: width or height is less than 0");
        }
    }

    public String toString()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    // установка основной текстуры объекта.
    public abstract void SetMainTexture();

    // получение основной текстуры объекта.
    public Texture getMainTexture()
    {
        return mainTexture;
    }

    // отрисовка объекта.
    public abstract void Show(SpriteBatch batch);

    // обновление состояния объекта (например, перемещение).
    public abstract void Update(GameWorld world);

    // Возвращает true, если объект мёртв (не отображается и не занимает место).
    public boolean IsDead()
    {
        return false;
    }

    // Вычисляет зазор по горизонтали между ближайшими краями юнита и цели.
    public int GetGapX(PlacedObject target)
    {
        if (this.getX() + this.getWidth() <= target.getX())
        {
            // Цель правее — сравниваем правый край атакующего и левый край цели.
            return target.getX() - (this.getX() + this.getWidth());
        }
        if (this.getX() >= target.getX() + target.getWidth())
        {
            // Цель левее — сравниваем левый край атакующего и правый край цели.
            return this.getX() - (target.getX() + target.getWidth());
        }
        // Перекрытие по горизонтали.
        return 0;
    }

    // Вычисляет зазор по вертикали между ближайшими краями юнита и цели.
    public int GetGapY(PlacedObject target)
    {
        if (this.getY() + this.getHeight() <= target.getY())
        {
            // Цель выше — сравниваем верхний край атакующего и нижний край цели.
            return target.getY() - (this.getY() + this.getHeight());
        }
        if (this.getY() >= target.getY() + target.getHeight())
        {
            // Цель ниже — сравниваем нижний край атакующего и верхний край цели.
            return this.getY() - (target.getY() + target.getHeight());
        }
        // Перекрытие по вертикали.
        return 0;
    }
}
