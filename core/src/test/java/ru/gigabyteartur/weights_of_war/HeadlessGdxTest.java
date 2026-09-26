package ru.gigabyteartur.weights_of_war;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * База для тестов, которым нужна инициализированная среда libGDX (headless).
 * <p>
 * Headless-бэкенд не выставляет GL-контекст, а {@link Texture} требует {@link Gdx#gl}.
 * Поэтому здесь подставляется no-op заглушка GL20, а загрузка текстур подменяется
 * заглушками — так конструкторы юнитов и зданий не обращаются к реальному GPU и файлам.
 */
public abstract class HeadlessGdxTest
{
    private static boolean initialized = false;

    @BeforeAll
    public static void initGdx()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {}, config);

        Gdx.gl = noOpGL20();
        Gdx.gl20 = Gdx.gl;

        // Заглушки текстур (без чтения файлов и нативного Pixmap).
        TextureCache.PutTexture("core.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("tower.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("swordman.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("archer.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("shieldman.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("priest.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
        TextureCache.PutTexture("scout.png", new Texture(1, 1, Pixmap.Format.RGBA8888));
    }

    // Возвращает GL20-заглушку, все методы которой возвращают значение по умолчанию.
    private static GL20 noOpGL20()
    {
        return (GL20) Proxy.newProxyInstance(
            GL20.class.getClassLoader(),
            new Class<?>[] { GL20.class },
            new InvocationHandler()
            {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == float.class) return 0f;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0d;
                    if (returnType == short.class) return (short) 0;
                    if (returnType == byte.class) return (byte) 0;
                    if (returnType == char.class) return (char) 0;
                    return null;
                }
            }
        );
    }
}
