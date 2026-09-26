package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;

// Кеширует текстуры по пути, чтобы не загружать одну и ту же текстуру повторно.
public class TextureCache
{
    private static HashMap<String, Texture> textures = new HashMap<String, Texture>();

    // Возвращает текстуру по пути, загружая её при первом обращении.
    public static Texture GetTexture(String path)
    {
        Texture texture = textures.get(path);
        if (texture == null)
        {
            texture = new Texture(path);
            textures.put(path, texture);
        }
        return texture;
    }

    // Регистрирует готовую текстуру в кеше (для тестов и предзагрузки).
    public static void PutTexture(String path, Texture texture)
    {
        textures.put(path, texture);
    }

    // Освобождает все закешированные текстуры.
    public static void Dispose()
    {
        for (Texture texture : textures.values())
        {
            texture.dispose();
        }
        textures.clear();
    }
}
