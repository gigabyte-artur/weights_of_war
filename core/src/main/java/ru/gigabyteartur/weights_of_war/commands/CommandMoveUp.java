package ru.gigabyteartur.weights_of_war.commands;

// Перемещение на 10 единиц вверх.
public class CommandMoveUp extends CommandMoveDirection
{
    public CommandMoveUp()
    {
        super(0, 30);
    }

    @Override
    public String GetName()
    {
        return "Вверх";
    }
}
