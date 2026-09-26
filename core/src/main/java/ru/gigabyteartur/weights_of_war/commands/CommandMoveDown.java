package ru.gigabyteartur.weights_of_war.commands;

// Перемещение на 10 единиц вниз.
public class CommandMoveDown extends CommandMoveDirection
{
    public CommandMoveDown()
    {
        super(0, -30);
    }

    @Override
    public String GetName()
    {
        return "Вниз";
    }
}
