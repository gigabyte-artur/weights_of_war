package ru.gigabyteartur.weights_of_war.commands;

// Перемещение на 10 единиц влево.
public class CommandMoveLeft extends CommandMoveDirection
{
    public CommandMoveLeft()
    {
        super(-30, 0);
    }

    @Override
    public String GetName()
    {
        return "Влево";
    }
}
