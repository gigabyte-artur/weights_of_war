package ru.gigabyteartur.weights_of_war;

import java.util.ArrayList;
import java.util.HashMap;

// Туман войны: для каждой фракции хранит набор видимых ей объектов (сетку видимости).
public class FogOfWar
{
    // Признак отношения видимого объекта к фракции-наблюдателю.
    public enum Relation
    {
        ALLY,    // союзник (та же фракция).
        NEUTRAL, // нейтральный (без фракции).
        ENEMY    // враг (другая фракция).
    }

    // Запись о видимом объекте: границы (AABB) и признак отношения.
    public static class VisibilityEntry
    {
        public int left;            // левая граница объекта.
        public int bottom;          // нижняя граница объекта.
        public int right;           // правая граница объекта.
        public int top;             // верхняя граница объекта.
        public Relation relation;   // отношение объекта к фракции-наблюдателю.

        public VisibilityEntry(PlacedObject object, Relation relation)
        {
            this.left = object.getX();
            this.bottom = object.getY();
            this.right = object.getX() + object.getWidth();
            this.top = object.getY() + object.getHeight();
            this.relation = relation;
        }
    }

    // Сетки видимости по фракциям: фракция → (видимый объект → запись).
    private HashMap<Fraction, HashMap<PlacedObject, VisibilityEntry>> grids =
            new HashMap<Fraction, HashMap<PlacedObject, VisibilityEntry>>();

    // Очищает и перестраивает сетки видимости всех фракций по текущему списку объектов.
    public void Rebuild(ArrayList<PlacedObject> units)
    {
        grids.clear();
        CreateGrids(units);
        FindAllyObjects(units);
        FindEnemyObjects(units);
    }

    // Создаёт пустую сетку для каждой живой фракции.
    private void CreateGrids(ArrayList<PlacedObject> units)
    {
        for (PlacedObject object : units)
        {
            if (!(object instanceof BattleObject) || object.IsDead())
            {
                continue;
            }
            Fraction fraction = ((BattleObject) object).getFraction();
            if (fraction == null)
            {
                continue;
            }
            if (!grids.containsKey(fraction))
            {
                grids.put(fraction, new HashMap<PlacedObject, VisibilityEntry>());
            }
        }
    }

    // Добавляет в сетки союзные объекты: объект всегда видим своей фракции, без учёта расстояния.
    private void FindAllyObjects(ArrayList<PlacedObject> units)
    {
        for (PlacedObject object : units)
        {
            if (!(object instanceof BattleObject) || object.IsDead())
            {
                continue;
            }
            Fraction fraction = ((BattleObject) object).getFraction();
            if (fraction == null)
            {
                continue;
            }
            grids.get(fraction).put(object, new VisibilityEntry(object, Relation.ALLY));
        }
    }

    // Добавляет в сетки вражеские и нейтральные объекты, видимые источниками зрения фракции.
    private void FindEnemyObjects(ArrayList<PlacedObject> units)
    {
        for (PlacedObject source : units)
        {
            if (!(source instanceof BattleObject) || source.IsDead())
            {
                continue;
            }
            BattleObject visionSource = (BattleObject) source;
            Fraction fraction = visionSource.getFraction();
            int sightRange = visionSource.getSightRange();
            if (fraction == null || sightRange <= 0)
            {
                continue;
            }

            for (PlacedObject object : units)
            {
                BattleObject candidate = visionSource.FindSightCandidate(object, sightRange);
                if (candidate == null)
                {
                    continue;
                }
                if (candidate.getFraction() == fraction)
                {
                    continue; // союзник уже добавлен в методе FindAllyObjects.
                }

                // Проверка расстояния (точный круг, а не прямоугольник по осям) — без извлечения корня.
                if (visionSource.GetDistanceSquared(candidate) > sightRange * sightRange)
                {
                    continue;
                }

                Relation relation;
                if (candidate.getFraction() == null)
                {
                    relation = Relation.NEUTRAL;
                }
                else
                {
                    relation = Relation.ENEMY;
                }
                grids.get(fraction).put(object, new VisibilityEntry(object, relation));
            }
        }
    }

    // Возвращает true, если объект видим фракции (находится в её сетке тумана войны).
    public boolean IsVisible(Fraction fraction, PlacedObject object)
    {
        HashMap<PlacedObject, VisibilityEntry> grid = grids.get(fraction);
        if (grid == null)
        {
            return false;
        }
        return grid.containsKey(object);
    }
}
