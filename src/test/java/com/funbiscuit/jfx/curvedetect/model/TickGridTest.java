package com.funbiscuit.jfx.curvedetect.model;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TickGridTest {
    private TickGrid grid;

    @Before
    public void setUp() {
        grid = new TickGrid();
    }

    @Test
    public void resetToBoxWithDefaultDirection() {
        grid.resetToBox(new Vec2D(200, 100), 5);
        List<TickGrid.Tick> ticks = grid.getTicks();

        assertEquals(0, ticks.get(0).getValue(), 0);
        assertEquals(1, ticks.get(1).getValue(), 0);
        assertEquals(0, ticks.get(2).getValue(), 0);
        assertEquals(1, ticks.get(3).getValue(), 0);

        assertEquals(ticks.get(0).getPosition().getX(), ticks.get(0).getStart().getX(), 0);
        assertEquals(95, ticks.get(0).getStart().getY(), 0);
        assertEquals(ticks.get(0).getPosition().getX(), ticks.get(0).getEnd().getX(), 0);
        assertEquals(5, ticks.get(0).getEnd().getY(), 0);

        assertEquals(ticks.get(1).getPosition().getX(), ticks.get(1).getStart().getX(), 0);
        assertEquals(95, ticks.get(1).getStart().getY(), 0);
        assertEquals(ticks.get(1).getPosition().getX(), ticks.get(1).getEnd().getX(), 0);
        assertEquals(5, ticks.get(1).getEnd().getY(), 0);

        assertEquals(5, ticks.get(2).getStart().getX(), 0);
        assertEquals(ticks.get(2).getPosition().getY(), ticks.get(2).getStart().getY(), 0);
        assertEquals(195, ticks.get(2).getEnd().getX(), 0);
        assertEquals(ticks.get(2).getPosition().getY(), ticks.get(2).getEnd().getY(), 0);

        assertEquals(5, ticks.get(3).getStart().getX(), 0);
        assertEquals(ticks.get(3).getPosition().getY(), ticks.get(3).getStart().getY(), 0);
        assertEquals(195, ticks.get(3).getEnd().getX(), 0);
        assertEquals(ticks.get(3).getPosition().getY(), ticks.get(3).getEnd().getY(), 0);
    }

    @Test
    public void changeDirectionChangesTicks() {
        grid.resetToBox(new Vec2D(200, 100), 5);
        // rotate tick lines by 90 degrees clockwise
        grid.setDirectionY(new Vec2D(1, 1));
        List<TickGrid.Tick> ticks = grid.getTicks();

        assertEquals(5, ticks.get(0).getStart().getX(), 0.1);
        assertEquals(65, ticks.get(0).getStart().getY(), 0.1);
        assertEquals(65, ticks.get(0).getEnd().getX(), 0.1);
        assertEquals(5, ticks.get(0).getEnd().getY(), 0.1);

        assertEquals(135, ticks.get(1).getStart().getX(), 0.1);
        assertEquals(95, ticks.get(1).getStart().getY(), 0.1);
        assertEquals(195, ticks.get(1).getEnd().getX(), 0.1);
        assertEquals(35, ticks.get(1).getEnd().getY(), 0.1);

        assertEquals(15, ticks.get(2).getStart().getX(), 0.1);
        assertEquals(5, ticks.get(2).getStart().getY(), 0.1);
        assertEquals(105, ticks.get(2).getEnd().getX(), 0.1);
        assertEquals(95, ticks.get(2).getEnd().getY(), 0.1);

        assertEquals(95, ticks.get(3).getStart().getX(), 0.1);
        assertEquals(5, ticks.get(3).getStart().getY(), 0.1);
        assertEquals(185, ticks.get(3).getEnd().getX(), 0.1);
        assertEquals(95, ticks.get(3).getEnd().getY(), 0.1);

    }
}
