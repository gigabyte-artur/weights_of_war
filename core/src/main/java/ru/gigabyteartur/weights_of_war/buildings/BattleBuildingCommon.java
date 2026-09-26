package ru.gigabyteartur.weights_of_war.buildings;

import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.GameWorld;

// Базовый класс здания.
public abstract class BattleBuildingCommon extends BattleObject
{
    public BattleBuildingCommon()
    {
        super();
        Init();
    }

    public BattleBuildingCommon(int x, int y)
    {
        super(x, y);
        Init();
    }

    @Override
    public void SetMainTexture()
    {

    }

    // Название класса здания.
    public abstract String GetClassName();

    @Override
    public void Update(GameWorld world)
    {

    }

    // Инициализация здания: загружаем текстуру по умолчанию.
    private void Init()
    {
        SetMainTexture();
    }
}
