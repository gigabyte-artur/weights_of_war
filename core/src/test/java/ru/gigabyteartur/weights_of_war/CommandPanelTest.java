package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;
import ru.gigabyteartur.weights_of_war.testutil.TestUnit;
import ru.gigabyteartur.weights_of_war.units.BattleUnitCommon;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Командная панель: кроме строки «Фракция» она показывает и имя команды (Team),
 * за которую фракция играет. Проверяется ровно тот текст, который панель рисует
 * (CommandPanel.FractionLine / CommandPanel.TeamLine); сама отрисовка в тестах не
 * поднимается — BitmapFont и SpriteBatch требуют GL-контекста.
 */
public class CommandPanelTest extends HeadlessGdxTest
{
    private static BattleUnitCommon UnitOf(String name, Color color, Team team)
    {
        BattleUnitCommon unit = new TestUnit(0, 0);
        unit.setFraction(new Fraction(name, color, team));
        return unit;
    }

    // Фракция в команде: рядом со строкой «Фракция» появляется строка «Команда».
    @Test
    public void panelShowsFactionAndItsTeam()
    {
        BattleUnitCommon unit = UnitOf("Red", Color.RED, new Team("A"));

        assertEquals("Фракция: Red", CommandPanel.FractionLine(unit));
        assertEquals("Команда: A", CommandPanel.TeamLine(unit));
    }

    // Команда берётся у фракции, а не выдумывается панелью: у двух фракций одной
    // команды подпись одна и та же, у фракций разных команд — разная. Смена команды
    // видна панели сразу: подпись не закеширована.
    @Test
    public void teamLineUsesTheFactionsOwnTeam()
    {
        Team teamA = new Team("A");
        BattleUnitCommon red = UnitOf("Red", Color.RED, teamA);
        BattleUnitCommon blue = UnitOf("Blue", Color.BLUE, teamA);
        BattleUnitCommon cyan = UnitOf("Cyan", Color.CYAN, new Team("B"));

        assertEquals("Команда: A", CommandPanel.TeamLine(red));
        assertEquals("Команда: A", CommandPanel.TeamLine(blue));
        assertEquals("Команда: B", CommandPanel.TeamLine(cyan));

        cyan.getFraction().setTeam(teamA);
        assertEquals("Команда: A", CommandPanel.TeamLine(cyan));
    }

    // Фракция без команды играет сама за себя (Team.SameTeam). Панель говорит это
    // прямо: по пустому месту нельзя отличить «команды нет» от «панель молчит».
    @Test
    public void teamLineSaysWhenThereIsNoTeam()
    {
        assertEquals("Команда: нет", CommandPanel.TeamLine(UnitOf("Red", Color.RED, null)));
    }

    // Объект без фракции: прежний ответ по фракции — «Нет», команды взяться неоткуда.
    @Test
    public void teamLineHandlesObjectWithoutFraction()
    {
        BattleUnitCommon unit = new TestUnit(0, 0);
        unit.setFraction(null);

        assertEquals("Фракция: Нет", CommandPanel.FractionLine(unit));
        assertEquals("Команда: нет", CommandPanel.TeamLine(unit));
    }

    // Каждая фракция боевой раскладки берётся из данных мира (TEAM_CORE_SPECS), а не
    // из выдуманных тестом: имя фракции и имя её команды доходят до панели целиком.
    @Test
    public void panelShowsTeamOfEverySpecInTheRealLayout()
    {
        for (GameWorld.CoreSpec spec : GameWorld.TEAM_CORE_SPECS)
        {
            BattleUnitCommon unit = new TestUnit(0, 0);
            unit.setFraction(new Fraction(spec.name, spec.color, spec.team));

            assertEquals("Фракция: " + spec.name, CommandPanel.FractionLine(unit));
            assertEquals("Команда: " + spec.team.getName(), CommandPanel.TeamLine(unit));
        }
    }
}
