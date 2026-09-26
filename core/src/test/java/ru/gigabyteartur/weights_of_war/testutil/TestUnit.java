package ru.gigabyteartur.weights_of_war.testutil;

import com.badlogic.gdx.graphics.Color;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

/**
 * Тестовый юнит, не загружающий текстуры, чтобы тесты работали без libGDX.
 */
public class TestUnit extends BattleUnitCommon
{
    public TestUnit()
    {
        super();
        setWidthHeight(10, 10);
    }

    public TestUnit(int x, int y)
    {
        super(x, y);
        setWidthHeight(10, 10);
    }

    @Override
    public void SetMainTexture()
    {
        // Намеренно пусто: текстуры в тестах не нужны.
    }

    @Override
    public Color GetClassColor()
    {
        return Color.WHITE;
    }

    @Override
    public String GetClassName()
    {
        return "Тест";
    }

    @Override
    public void IncreaseFit(int value, FitType fit_type)
    {
        setFit(getFit() + value);
    }
}
