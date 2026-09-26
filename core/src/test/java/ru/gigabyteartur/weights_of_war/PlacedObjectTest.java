package ru.gigabyteartur.weights_of_war;

import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PlacedObjectTest
{
    @Test
    public void getGapXWhenTargetIsToTheRight()
    {
        TestUnit a = new TestUnit(0, 0);
        TestUnit b = new TestUnit(30, 0);
        assertEquals(20, a.GetGapX(b));
    }

    @Test
    public void getGapXWhenTargetIsToTheLeft()
    {
        TestUnit a = new TestUnit(30, 0);
        TestUnit b = new TestUnit(0, 0);
        assertEquals(20, a.GetGapX(b));
    }

    @Test
    public void getGapXIsZeroWhenOverlapping()
    {
        TestUnit a = new TestUnit(0, 0);
        TestUnit b = new TestUnit(5, 0);
        assertEquals(0, a.GetGapX(b));
    }

    @Test
    public void getGapYWhenTargetIsBelow()
    {
        TestUnit a = new TestUnit(0, 30);
        TestUnit b = new TestUnit(0, 0);
        assertEquals(20, a.GetGapY(b));
    }

    @Test
    public void setWidthHeightRejectsNonPositiveValues()
    {
        TestUnit a = new TestUnit(0, 0);
        a.setWidthHeight(0, 5);
        assertEquals(0, a.getWidth());
        assertEquals(0, a.getHeight());
    }
}
