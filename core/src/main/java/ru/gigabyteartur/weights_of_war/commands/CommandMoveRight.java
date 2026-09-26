package ru.gigabyteartur.weights_of_war.commands;

// Перемещение на 10 единиц вправо.
public class CommandMoveRight extends CommandMoveDirection
{
    public CommandMoveRight()
    {
        super(30, 0);
    }

    @Override
    public String GetName()
    {
        return "Вправо";
    }
}
