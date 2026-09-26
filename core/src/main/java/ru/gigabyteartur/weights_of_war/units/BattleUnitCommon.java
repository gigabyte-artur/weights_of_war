package ru.gigabyteartur.weights_of_war.units;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.gigabyteartur.weights_of_war.BattleObject;
import ru.gigabyteartur.weights_of_war.PlacedObject;
import ru.gigabyteartur.weights_of_war.Sensors.SensorCommon;
import ru.gigabyteartur.weights_of_war.commands.CommandAttackTarget;
import ru.gigabyteartur.weights_of_war.commands.CommandMove;
import ru.gigabyteartur.weights_of_war.commands.CommandThink;
import ru.gigabyteartur.weights_of_war.commands.CommandUnit;
import ru.gigabyteartur.weights_of_war.GameWorld;
import ru.gigabyteartur.weights_of_war.neuro_net.DenseLayer;
import ru.gigabyteartur.weights_of_war.neuro_net.NeuroNet;
import java.util.ArrayList;

// Базовый класс боевого юнита.
public abstract class BattleUnitCommon extends BattleObject
{

    public final static float HEALTH_REGEN_PER_SECOND = 1f;  // Скорость восстановления здоровья, HP/сек.
    public final static float MAX_BOIDS = 0.6f;              // Максимальная длина суммарного boids-вектора (чтобы не перебивал движение к цели).
    public final static float STUCK_THRESHOLD = 4f;          // Порог застревания, после которого юнит отлипает от затора.
    public final static float ATTACKED_DECAY_PER_SECOND = 30f;  // Скорость затухания сигнала «под атакой», ед./сек.
    public final static float CRITICAL_HEALTH_FRACTION = 0.2f;  // Порог критического здоровья (менее 20% от максимума).
    public final static int KILL_FIT_BONUS = 100;              // Разовый бонус фитнесса за уничтожение вражеского юнита.

    public enum FitType {
        DAMAGE_TO_UNITS,        // урон по юнитам.
        DAMAGE_TO_BUILDINGS,    // урон по зданиям.
        BLOCKED_ENEMY_DAMAGE,   // заблокированный урон от врага.
        HEALED_ALLIES,          // вылеченные союзники (жрец).
        KILLED_ENEMY_UNIT,      // уничтожение вражеского юнита.
        SCOUTING                // разведка (разведчик).
    };

    private int target_x;                       // координаты цели.
    private int target_y;                       // координаты цели.
    private BattleObject targetObject;          // объект-цель (юнит или здание).
    private int speed = 1;                      // скорость перемещения юнита.
    private int damage = 10;                    // урон, наносимый юнитом.
    private int attackRange = 100;              // радиус атаки юнита
    private float movementRemainder = 0f;       // накопленное дробное смещение.
    private float healthRegenRemainder = 0f;    // накопленное дробное восстановление здоровья.
    private int GenerationNumber = 0;           // номер поколения юнита.
    private NeuroNet neuroNet;                  // нейросеть юнита.
    private int fit = 0;                        // Значение фитнесс-функции.
    private int lastFit = 0;                    // Фит на прошлой проверке.
    private int staleWaves = 0;                 // Сколько волн подряд фит не менялся.
    private float stuck = 0f;                   // Накопленное время застревания, сек.
    private float attacked = 0f;                // Сигнал «под атакой» (0..100, затухает со временем).
    private ArrayList<CommandUnit> commandsQueue = new ArrayList<CommandUnit>();        // очередь команд.

    protected float separationRadius = 30f;      // Радиус отталкивания от союзников (boids).
    protected float separationWeight = 1.5f;     // Вес отталкивания от союзников.
    protected float cohesionWeight = 0f;         // Вес притяжения к центру масс (0 = выключено).
    protected float alignmentWeight = 0f;        // Вес выравнивания по соседям (0 = выключено).

    public BattleUnitCommon()
    {
        super();
    }

    public BattleUnitCommon(int x, int y)
    {
        super(x, y);
    }

    public void setTarget(int x, int y)
    {
        this.target_x = x;
        this.target_y = y;
    }

    public int getTargetX()
    {
        return target_x;
    }

    public int getTargetY()
    {
        return target_y;
    }

    // Возвращает объект-цель (юнит или здание) или null, если цель не выбрана.
    public BattleObject getTargetObject()
    {
        return targetObject;
    }

    // Устанавливает объект-цель.
    public void setTargetObject(BattleObject targetObject)
    {
        this.targetObject = targetObject;
    }

    // Возвращает объект-цель, если он жив и видим фракции юнита (иначе null).
    // Используется сенсорами, чтобы нейросеть «не видела» скрывшуюся или мёртвую цель.
    // Если мир не задан (в тестах), проверка видимости пропускается.
    public BattleObject getVisibleTargetObject(GameWorld world)
    {
        if (targetObject == null || targetObject.IsDead())
        {
            return null;
        }
        if (world != null && !world.IsVisible(getFraction(), targetObject))
        {
            return null;
        }
        return targetObject;
    }

    public void setSpeed(int speed)
    {
        this.speed = speed;
    }

    public void setSeparationRadius(float separationRadius)
    {
        this.separationRadius = separationRadius;
    }

    public void setSeparationWeight(float separationWeight)
    {
        this.separationWeight = separationWeight;
    }

    public void setCohesionWeight(float cohesionWeight)
    {
        this.cohesionWeight = cohesionWeight;
    }

    public void setAlignmentWeight(float alignmentWeight)
    {
        this.alignmentWeight = alignmentWeight;
    }

    // Возвращает накопленное время застревания (сек).
    public float getStuck()
    {
        return stuck;
    }

    // Возвращает текущий сигнал «под атакой» (0..100).
    public float getAttacked()
    {
        return attacked;
    }

    // Отмечает, что юнит атакован — поднимает сигнал «под атакой» до максимума.
    public void MarkAttacked()
    {
        attacked = 100f;
    }

    // При смерти юнита его очередь команд очищается.
    @Override
    public void setHealth(int health)
    {
        super.setHealth(health);
        if (health <= 0)
        {
            commandsQueue.clear();
        }
    }

    // Обработка очереди команд.
    @Override
    public void Update(GameWorld world)
    {
        // Мёртвый юнит не выполняет команды.
        if (IsDead())
        {
            commandsQueue.clear();
            return;
        }

        // Восстановление здоровья (1 единица в секунду).
        healthRegenRemainder += HEALTH_REGEN_PER_SECOND * Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        int regen = (int) healthRegenRemainder;
        if (regen > 0)
        {
            healthRegenRemainder -= regen;
            int newHealth = Math.min(getMaxHealth(), getHealth() + regen);
            setHealth(newHealth);
        }

        // Если команд нет — автоматически добавляем команду «подумать».
        if (commandsQueue.isEmpty())
        {
            commandsQueue.add(new CommandThink(CreateSensors()));
        }

        CommandUnit command = commandsQueue.get(0);
        if (command.Execute(this, world))
        {
            commandsQueue.remove(0);
        }

        // Если застрял слишком долго — сбрасываем команду и отлипаем от затора.
        if (stuck > STUCK_THRESHOLD)
        {
            ClearCommands();
            AddCommand(EscapeCommand(world));
            stuck = 0f;
        }

        // Затухание сигнала «под атакой».
        attacked = Math.max(0f, attacked - ATTACKED_DECAY_PER_SECOND * Gdx.graphics.getDeltaTime() * world.getGameSpeed());
    }

    // Возвращает команду «отлипания» от затора: уход по вектору отталкивания от союзников (или случайно, если соседей нет).
    private CommandUnit EscapeCommand(GameWorld world)
    {
        float[] separation = CalculateSeparation(world);
        float sx = separation[0];
        float sy = separation[1];
        float length = (float) Math.sqrt(sx * sx + sy * sy);

        int escapeDistance = (int) (separationRadius * 2);
        int targetX;
        int targetY;

        if (length > 0.01f)
        {
            float ux = sx / length;
            float uy = sy / length;
            targetX = getX() + Math.round(ux * escapeDistance);
            targetY = getY() + Math.round(uy * escapeDistance);
        }
        else
        {
            // Нет близких союзников — случайное направление.
            int direction = (int) (Math.random() * 4);
            switch (direction)
            {
                case 0:  targetX = getX();               targetY = getY() + escapeDistance; break;
                case 1:  targetX = getX();               targetY = getY() - escapeDistance; break;
                case 2:  targetX = getX() - escapeDistance; targetY = getY();              break;
                default: targetX = getX() + escapeDistance; targetY = getY();              break;
            }
        }

        return new CommandMove(targetX, targetY);
    }

    // Создаёт базовый набор сенсоров юнита (22 сенсора). Подклассы могут переопределять и добавлять свои.
    public ArrayList<SensorCommon> CreateSensors()
    {
        return CommandThink.CreateBaseSensors();
    }

    // Шаг перемещения юнита к цели с учётом занятых областей и boids-коррекции направления.
    public void MoveToTarget(GameWorld world)
    {
        int dx = target_x - getX();
        int dy = target_y - getY();

        if (dx == 0 && dy == 0)
        {
            return;
        }

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Накопление дробного смещения: юнит проходит speed пикселей в секунду.
        movementRemainder += speed * Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        int step = (int) movementRemainder;
        movementRemainder -= step;

        if (step <= 0)
        {
            return;
        }

        // Цель ближе одного шага — двигаемся прямо к ней без boids-коррекции.
        if (distance <= step)
        {
            if (!TryMove(world, getX() + dx, getY() + dy))
            {
                stuck += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
            }
            return;
        }

        // Единичный вектор направления к цели (seek).
        float seekX = dx / distance;
        float seekY = dy / distance;

        // Суммарный boids-вектор.
        float boidsX = 0;
        float boidsY = 0;

        // Отталкивание от близких союзников.
        float[] separation = CalculateSeparation(world);
        boidsX += separation[0] * separationWeight;
        boidsY += separation[1] * separationWeight;

        // Во время атаки притяжение и выравнивание гасим, чтобы не стягивать юнита с цели.
        float groupFactor = IsCurrentlyAttacking(distance) ? 0.5f : 1f;

        // Притяжение к центру масс союзников (только если включено).
        if (cohesionWeight > 0)
        {
            float[] cohesion = CalculateCohesion(world);
            boidsX += cohesion[0] * cohesionWeight * groupFactor;
            boidsY += cohesion[1] * cohesionWeight * groupFactor;
        }

        // Выравнивание по направлению движения союзников (только если включено).
        if (alignmentWeight > 0)
        {
            float[] alignment = CalculateAlignment(world);
            boidsX += alignment[0] * alignmentWeight * groupFactor;
            boidsY += alignment[1] * alignmentWeight * groupFactor;
        }

        // Ограничиваем длину boids-вектора, чтобы он не перебивал движение к цели.
        float boidsLength = (float) Math.sqrt(boidsX * boidsX + boidsY * boidsY);
        if (boidsLength > MAX_BOIDS)
        {
            float scale = MAX_BOIDS / boidsLength;
            boidsX *= scale;
            boidsY *= scale;
        }

        // Итоговое направление движения: цель + boids.
        float finalX = seekX + boidsX;
        float finalY = seekY + boidsY;
        float finalLength = (float) Math.sqrt(finalX * finalX + finalY * finalY);
        if (finalLength == 0)
        {
            // seek и boids взаимоуничтожились — считаем это застреванием.
            stuck += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
            return;
        }
        finalX /= finalLength;
        finalY /= finalLength;

        int stepX = Math.round(finalX * step);
        int stepY = Math.round(finalY * step);

        // Гарантируем шаг хотя бы на 1 px по доминирующей оси, чтобы юнит не замирал.
        if (stepX == 0 && stepY == 0)
        {
            if (Math.abs(finalX) >= Math.abs(finalY))
            {
                stepX = (finalX >= 0) ? 1 : -1;
            }
            else
            {
                stepY = (finalY >= 0) ? 1 : -1;
            }
        }

        // Пытаемся переместиться с обходом препятствий (скольжением по осям).
        if (!TryMove(world, getX() + stepX, getY() + stepY))
        {
            stuck += Gdx.graphics.getDeltaTime() * world.getGameSpeed();
        }
    }

    // Определяет, атакует ли юнит сейчас: выполняет команду атаки или уже в пределах радиуса атаки цели.
    private boolean IsCurrentlyAttacking(float distanceToTarget)
    {
        if (distanceToTarget <= attackRange)
        {
            return true;
        }
        if (!commandsQueue.isEmpty() && commandsQueue.get(0) instanceof CommandAttackTarget)
        {
            return true;
        }
        return false;
    }

    // Возвращает вектор отталкивания от близких союзников {dx, dy}.
    // Учитываются только живые юниты той же фракции в пределах separationRadius.
    public float[] CalculateSeparation(GameWorld world)
    {
        float sumX = 0;
        float sumY = 0;

        for (PlacedObject object : world.GetUnits())
        {
            if (object == this || !(object instanceof BattleUnitCommon) || object.IsDead())
            {
                continue;
            }
            BattleUnitCommon other = (BattleUnitCommon) object;
            if (this.getFraction() == null || other.getFraction() == null || this.IsEnemy(other))
            {
                continue;
            }

            // Направление от соседа к себе.
            int dx = this.getX() - other.getX();
            int dy = this.getY() - other.getY();

            // Дешёвый фильтр по осям.
            if (Math.abs(dx) > separationRadius || Math.abs(dy) > separationRadius)
            {
                continue;
            }

            float distSq = (float) dx * dx + (float) dy * dy;
            float radiusSq = separationRadius * separationRadius;
            if (distSq >= radiusSq)
            {
                continue;
            }

            float dist = (float) Math.sqrt(distSq);
            if (dist == 0)
            {
                continue;
            }

            // Сила обратно пропорциональна расстоянию: (radius - dist) / radius.
            float force = (separationRadius - dist) / separationRadius;
            sumX += (dx / dist) * force;
            sumY += (dy / dist) * force;
        }

        return new float[] { sumX, sumY };
    }

    // Возвращает вектор притяжения к центру масс близких союзников (опционально, выключено по умолчанию).
    public float[] CalculateCohesion(GameWorld world)
    {
        float centerX = 0;
        float centerY = 0;
        int count = 0;

        for (PlacedObject object : world.GetUnits())
        {
            if (object == this || !(object instanceof BattleUnitCommon) || object.IsDead())
            {
                continue;
            }
            BattleUnitCommon other = (BattleUnitCommon) object;
            if (this.getFraction() == null || other.getFraction() == null || this.IsEnemy(other))
            {
                continue;
            }

            int dx = other.getX() - this.getX();
            int dy = other.getY() - this.getY();
            if (Math.abs(dx) > separationRadius || Math.abs(dy) > separationRadius)
            {
                continue;
            }
            if (GetDistanceSquared(other) >= separationRadius * separationRadius)
            {
                continue;
            }

            centerX += other.getX();
            centerY += other.getY();
            count++;
        }

        if (count == 0)
        {
            return new float[] { 0, 0 };
        }

        centerX /= count;
        centerY /= count;
        return new float[] { centerX - this.getX(), centerY - this.getY() };
    }

    // Возвращает вектор выравнивания по среднему направлению движения близких союзников (опционально).
    public float[] CalculateAlignment(GameWorld world)
    {
        float dirX = 0;
        float dirY = 0;
        int count = 0;

        for (PlacedObject object : world.GetUnits())
        {
            if (object == this || !(object instanceof BattleUnitCommon) || object.IsDead())
            {
                continue;
            }
            BattleUnitCommon other = (BattleUnitCommon) object;
            if (this.getFraction() == null || other.getFraction() == null || this.IsEnemy(other))
            {
                continue;
            }

            int dx = other.getX() - this.getX();
            int dy = other.getY() - this.getY();
            if (Math.abs(dx) > separationRadius || Math.abs(dy) > separationRadius)
            {
                continue;
            }
            if (GetDistanceSquared(other) >= separationRadius * separationRadius)
            {
                continue;
            }

            // Направление движения соседа — к его цели.
            int odx = other.getTargetX() - other.getX();
            int ody = other.getTargetY() - other.getY();
            float odist = (float) Math.sqrt(odx * odx + ody * ody);
            if (odist == 0)
            {
                continue;
            }

            dirX += odx / odist;
            dirY += ody / odist;
            count++;
        }

        if (count == 0)
        {
            return new float[] { 0, 0 };
        }

        return new float[] { dirX / count, dirY / count };
    }

    // Проверяет, свободна ли позиция (x, y) для этого юнита.
    private boolean IsFree(GameWorld world, int x, int y)
    {
        return !world.IsAreaOccupiedExcept(x, y, getWidth(), getHeight(), this);
    }

    // Проверяет, что позиция допустима: не выходит за границы мира и не занята другими объектами.
    private boolean IsPositionValid(GameWorld world, int x, int y)
    {
        return world.IsInsideWorld(x, y, getWidth(), getHeight()) && IsFree(world, x, y);
    }

    // Пытается переместиться в (x, y); при блокировке пробует скольжение по осям (обход препятствий).
    // Возвращает true и сбрасывает stuck, если юнит сдвинулся; иначе false.
    private boolean TryMove(GameWorld world, int x, int y)
    {
        // Прямой ход.
        if (IsPositionValid(world, x, y))
        {
            setXY(x, y);
            stuck = 0f;
            return true;
        }

        // Скольжение по оси X, сохраняя текущий Y.
        if (x != getX() && IsPositionValid(world, x, getY()))
        {
            setXY(x, getY());
            stuck = 0f;
            return true;
        }

        // Скольжение по оси Y, сохраняя текущий X.
        if (y != getY() && IsPositionValid(world, getX(), y))
        {
            setXY(getX(), y);
            stuck = 0f;
            return true;
        }

        return false;
    }

    public void AddCommand(CommandUnit command)
    {
        if (command != null)
            commandsQueue.add(command);
    }

    // Очищает очередь команд юнита.
    public void ClearCommands()
    {
        commandsQueue.clear();
    }

    // Возвращает количество команд в очереди (для тестов и отладки).
    public int getCommandsQueueSize()
    {
        return commandsQueue.size();
    }

    // Возвращает текущую выполняемую команду (или null, если очередь пуста).
    public CommandUnit GetCurrentCommand()
    {
        if (!commandsQueue.isEmpty())
        {
            return commandsQueue.get(0);
        }
        return null;
    }

    public int getDamage()
    {
        return damage;
    }

    public void setDamage(int damage)
    {
        this.damage = damage;
    }

    public int getAttackRange()
    {
        return attackRange;
    }

    public void setAttackRange(int attackRange)
    {
        this.attackRange = attackRange;
    }

    public int getGenerationNumber()
    {
        return GenerationNumber;
    }

    public void setGenerationNumber(int generationNumber)
    {
        this.GenerationNumber = generationNumber;
    }

    public NeuroNet getNeuroNet()
    {
        return neuroNet;
    }

    public void setNeuroNet(NeuroNet neuroNet)
    {
        this.neuroNet = neuroNet;
    }

    public int getFit()
    {
        return fit;
    }

    public void setFit(int fit)
    {
        this.fit = fit;
    }

    // Сбрасывает переходящее состояние юнита для нового или переиспользованного юнита.
    public void ResetUnitState()
    {
        fit = 0;
        lastFit = 0;
        staleWaves = 0;
        targetObject = null;
        attacked = 0f;
    }

    // Проверяет застой: если фит не менялся maxStaleWaves волн — юнит умирает.
    public void CheckStagnation(int maxStaleWaves)
    {
        if (fit == lastFit)
        {
            staleWaves++;
        }
        else
        {
            staleWaves = 0;
        }
        lastFit = fit;

        if (staleWaves >= maxStaleWaves)
        {
            setHealth(0);
            staleWaves = 0;
        }
    }

    // Возвращает true, если у объекта здоровье ниже критического порога (менее 20% от максимума).
    public static boolean IsCriticallyWounded(BattleObject object)
    {
        return object.getHealth() < object.getMaxHealth() * CRITICAL_HEALTH_FRACTION;
    }

    // Формирует нейронную сеть юнита: входной слой 26 нейронов,
    // 2 скрытых полносвязных слоя по 20 нейронов, выходной слой 10 нейронов.
    public static NeuroNet CreateNeuroNetUnit()
    {
        NeuroNet neuroNet = new NeuroNet();

        // Входной слой.
        neuroNet.GenerateAddLayer(26, true, false);

        // Скрытые полносвязные слои.
        for (int i = 0; i < 2; i++)
        {
            DenseLayer hiddenLayer = new DenseLayer();
            hiddenLayer.GenerateLayer(20, false, false);
            neuroNet.AddLayer(hiddenLayer);
        }

        // Выходной слой.
        neuroNet.GenerateAddLayer(10, false, true);
        neuroNet.Compile();
        neuroNet.RandomWeights();

        return neuroNet;
    }

    // Отрисовка юнита: цветной фон класса + базовая отрисовка + номер волны.
    @Override
    public void Show(SpriteBatch batch)
    {
        DrawAttackLine(batch);
        DrawClassBackground(batch);
        super.Show(batch);
        DrawGenerationNumber(batch);
    }

    // Отрисовка цветного фона класса юнита позади текстуры.
    private void DrawClassBackground(SpriteBatch batch)
    {
        batch.setColor(GetClassColor());
        batch.draw(getWhitePixel(), getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
    }

    // Отрисовка номера волны в правом верхнем углу юнита.
    private void DrawGenerationNumber(SpriteBatch batch)
    {
        String text = String.valueOf(GenerationNumber);
        GlyphLayout layout = new GlyphLayout();
        layout.setText(getFont(), text);
        float textX = getX() + getWidth() - layout.width - 3f;
        float textY = getY() + getHeight() - 3f;
        getFont().draw(batch, text, textX, textY);
    }

    // Шрифт для отрисовки номера волны (создаётся лениво).
    private static BitmapFont font;

    private static BitmapFont getFont()
    {
        if (font == null)
        {
            font = new BitmapFont();
        }
        return font;
    }

    // Рисует пунктирную линию от центра юнита к центру цели заданным цветом.
    protected void DrawDottedLineTo(SpriteBatch batch, BattleObject target, Color color)
    {
        if (target == null || target.IsDead())
        {
            return;
        }

        float x1 = getX() + getWidth() / 2f;
        float y1 = getY() + getHeight() / 2f;
        float x2 = target.getX() + target.getWidth() / 2f;
        float y2 = target.getY() + target.getHeight() / 2f;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0f)
        {
            return;
        }

        float dashLength = 8f;
        float gapLength = 6f;
        float thickness = 3f;

        float ux = dx / distance;
        float uy = dy / distance;
        float rotation = (float) Math.toDegrees(Math.atan2(dy, dx));

        batch.setColor(color);

        float t = 0f;
        while (t < distance)
        {
            float segment = Math.min(dashLength, distance - t);
            float cx = x1 + ux * (t + segment / 2f);
            float cy = y1 + uy * (t + segment / 2f);

            batch.draw(getWhitePixel(), cx, cy - thickness / 2f, 0f, thickness / 2f, segment, thickness, 1f, 1f, rotation, 0, 0, 1, 1, false, false);

            t += segment + gapLength;
        }

        batch.setColor(Color.WHITE);
    }

    // Отрисовка красной пунктирной линии к цели атаки (если юнит атакует) — только в режиме отладки.
    private void DrawAttackLine(SpriteBatch batch)
    {
        if (!GameWorld.getDebugMode())
        {
            return;
        }
        CommandUnit current = GetCurrentCommand();
        if (!(current instanceof CommandAttackTarget))
        {
            return;
        }
        DrawDottedLineTo(batch, ((CommandAttackTarget) current).getTarget(), Color.RED);
    }

    // Отрисовка круга видимости с прозрачностью 5%.
    public void DrawVisibilityCircle(SpriteBatch batch)
    {
        int sightRange = getSightRange();
        if (sightRange <= 0)
        {
            return;
        }

        float centerX = getX() + getWidth() / 2f;
        float centerY = getY() + getHeight() / 2f;
        float diameter = sightRange * 2f;

        batch.setColor(1f, 1f, 1f, 0.05f);
        batch.draw(getVisibilityCircle(), centerX - sightRange, centerY - sightRange, diameter, diameter);
        batch.setColor(Color.WHITE);
    }

    // Белая текстура круга для отрисовки области видимости (создаётся лениво).
    private static Texture visibilityCircle;

    private static Texture getVisibilityCircle()
    {
        if (visibilityCircle == null)
        {
            int size = 64;
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(size / 2, size / 2, size / 2);
            visibilityCircle = new Texture(pixmap);
            pixmap.dispose();
        }
        return visibilityCircle;
    }

    // Цвет фона класса юнита (для наглядного различия классов).
    public abstract Color GetClassColor();

    // Название класса юнита.
    public abstract String GetClassName();

    public abstract void IncreaseFit(int value, FitType fit_type);
}
