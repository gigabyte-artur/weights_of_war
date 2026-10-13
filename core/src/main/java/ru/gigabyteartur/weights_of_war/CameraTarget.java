package ru.gigabyteartur.weights_of_war;

// Цель слежения камеры: объект мира, за которым может следовать RtsCameraController.
// На первом этапе активное слежение не реализовано — интерфейс определён как заглушка
// для методов setFollowTarget/clearFollowTarget.
public interface CameraTarget
{
    float getX();

    float getY();
}
