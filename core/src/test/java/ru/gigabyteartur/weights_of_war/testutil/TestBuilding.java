package ru.gigabyteartur.weights_of_war.testutil;

import ru.gigabyteartur.weights_of_war.buildings.BattleBuildingCommon;

/**
 * Тестовое здание, не загружающее текстуры, чтобы тесты работали без libGDX.
 */
public class TestBuilding extends BattleBuildingCommon
{
    public TestBuilding(int x, int y)
    {
        super(x, y);
        setWidthHeight(100, 100);
    }

    @Override
    public void SetMainTexture()
    {
        // Намеренно пусто: текстуры в тестах не нужны.
    }

    @Override
    public String GetClassName()
    {
        return "Здание";
    }
}
