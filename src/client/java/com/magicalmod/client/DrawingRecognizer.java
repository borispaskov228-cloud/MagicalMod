package com.magicalmod.client;

/**
 * Распознаватель форм, нарисованных мышью (координаты экранные, Y вниз).
 * Устойчив к масштабу, сдвигу и небольшому шуму: точки нормализуются,
 * слишком близкие соседи отбрасываются, углы считаются на сглаженной кривой.
 */
public final class DrawingRecognizer {
    public record Point(float x, float y) {}

    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float SHARP_TURN = (float) (Math.PI / 3.0); // 60°
    private static final float MIN_DIRECTION = 0.12f;               // порог шума при смене знака
    private static final int MIN_POINTS = 8;

    private DrawingRecognizer() {}

    public static String recognize(java.util.List<Point> points) {
        if (points == null) return "";

        // Копия без null-точек.
        java.util.List<Point> raw = new java.util.ArrayList<>(points.size());
        for (Point p : points) if (p != null) raw.add(p);
        if (raw.size() < MIN_POINTS) return "";

        // Грубый bbox исходных точек — нужен для порога «слишком близкие соседи».
        float rMinX = Float.POSITIVE_INFINITY, rMinY = Float.POSITIVE_INFINITY;
        float rMaxX = Float.NEGATIVE_INFINITY, rMaxY = Float.NEGATIVE_INFINITY;
        for (Point p : raw) {
            rMinX = Math.min(rMinX, p.x());
            rMaxX = Math.max(rMaxX, p.x());
            rMinY = Math.min(rMinY, p.y());
            rMaxY = Math.max(rMaxY, p.y());
        }
        float rawDiag = (float) Math.hypot(rMaxX - rMinX, rMaxY - rMinY);
        if (rawDiag < 8.0f) return "";

        // Фильтр дрожания мыши: сохраняем первую и последнюю точки.
        float minStep = Math.max(1.0f, rawDiag * 0.012f);
        java.util.List<Point> f = new java.util.ArrayList<>(raw.size());
        f.add(raw.get(0));
        for (int i = 1; i < raw.size(); i++) {
            Point prev = f.get(f.size() - 1), cur = raw.get(i);
            if (distance(prev, cur) >= minStep) f.add(cur);
        }
        // Последняя точка должна быть именно последней, но не дублировать предыдущую.
        Point tail = raw.get(raw.size() - 1);
        if (distance(f.get(f.size() - 1), tail) >= minStep) {
            f.add(tail);
        } else {
            f.set(f.size() - 1, tail);
        }
        // У замкнутых форм START == END: дубль на стыке разбивает угол на два слабых.
        if (f.size() > 2 && distance(f.get(0), f.get(f.size() - 1)) < minStep) {
            f.remove(f.size() - 1);
        }

        int n = f.size();
        if (n < 6) return "";

        // Нормализация: центр bbox + деление на диагональ → характерный размер ~1.
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float pathLen = 0.0f;
        for (int i = 0; i < n; i++) {
            Point p = f.get(i);
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
            if (i > 0) pathLen += distance(f.get(i - 1), p);
        }
        float diag = (float) Math.hypot(maxX - minX, maxY - minY);
        if (diag < 8.0f || pathLen < Math.max(12.0f, diag * 0.9f)) return "";

        float cx = (minX + maxX) * 0.5f, cy = (minY + maxY) * 0.5f;
        float[] x = new float[n], y = new float[n];
        for (int i = 0; i < n; i++) {
            Point p = f.get(i);
            x[i] = (p.x() - cx) / diag;
            y[i] = (p.y() - cy) / diag;
        }

        // Замкнутость: зазор между концами исходной кривой относительно диагонали bbox.
        // берём исходные концы, т.к. дубль START==END мог быть удалён фильтром.
        float closeGap = distance(raw.get(0), raw.get(raw.size() - 1)) / diag;
        boolean closed = closeGap < 0.30f;
        boolean nearlyClosed = closeGap < 0.46f;

        // Очень слабое сглаживание: сильное (1-2-1) превращает 90° в ~53° и ломает детектор углов.
        final float smooth = 0.12f;
        float[] sx = x.clone(), sy = y.clone();
        for (int i = 1; i < n - 1; i++) {
            sx[i] += smooth * ((x[i - 1] + x[i + 1]) * 0.5f - x[i]);
            sy[i] += smooth * ((y[i - 1] + y[i + 1]) * 0.5f - y[i]);
        }
        if (closed) { // стык последней и первой точек тоже внутренняя вершина
            sx[0] += smooth * ((x[n - 1] + x[1]) * 0.5f - x[0]);
            sy[0] += smooth * ((y[n - 1] + y[1]) * 0.5f - y[0]);
            sx[n - 1] += smooth * ((x[n - 2] + x[0]) * 0.5f - x[n - 1]);
            sy[n - 1] += smooth * ((y[n - 2] + y[0]) * 0.5f - y[n - 1]);
        }

        // Повороты: величина, знак (cross), суммарный и «чистый» поворот.
        TurnStats turns = analyzeTurns(sx, sy, closed);
        // Углы считаем и по не сглаженной кривой: при быстром проведении мыши
        // сглаживание разбивает прямой угол на два слабых поворота.
        TurnStats rawTurns = analyzeTurns(x, y, closed);
        float absNet = Math.abs(turns.net);

        // Радиус относительно центроида: разброс и отношение концы/начало.
        float mx = 0.0f, my = 0.0f;
        for (int i = 0; i < n; i++) {
            mx += x[i];
            my += y[i];
        }
        mx /= n;
        my /= n;
        float[] radius = new float[n];
        float minR = Float.MAX_VALUE, maxR = 0.0f;
        for (int i = 0; i < n; i++) {
            radius[i] = (float) Math.hypot(x[i] - mx, y[i] - my);
            minR = Math.min(minR, radius[i]);
            maxR = Math.max(maxR, radius[i]);
        }
        float radiusSpread = maxR > 1e-4f ? (maxR - minR) / maxR : 0.0f;
        int k = Math.max(2, n / 6);
        float rFirst = 0.0f, rLast = 0.0f;
        for (int i = 0; i < k; i++) {
            rFirst += radius[i];
            rLast += radius[n - 1 - i];
        }
        rFirst /= k;
        rLast /= k;
        float radiusRatio = rFirst > 1e-4f ? rLast / rFirst : 1.0f;

        // Прямолинейность: отношение длины пути к расстоянию между концами.
        float endDist = distance(x[0], y[0], x[n - 1], y[n - 1]);
        float straightness = endDist > 1e-4f ? (pathLen / diag) / endDist : Float.MAX_VALUE;
        float dx = x[n - 1] - x[0], dy = y[n - 1] - y[0];

        // 1) Спирали: несколько витков и заметное изменение радиуса.
        // Число резких углов здесь не проверяем: шум даёт ложные углы, а число витков и радиус — устойчивы.
        if (radiusSpread > 0.32f && absNet > TWO_PI * 1.5f) {
            return !closed && radiusRatio < 0.65f ? "inward" : "spiral";
        }
        // Спираль в один виток: концы близки (почти замкнуто), но радиус заметно
        // меняется — раньше такой жест ошибочно читался как круг.
        boolean radiiChange = radiusRatio < 0.82f || radiusRatio > 1.16f;
        if (radiiChange && radiusSpread > 0.16f && absNet > TWO_PI * 0.85f) {
            return radiusRatio < 0.82f ? "inward" : "spiral";
        }

        // 2) Замкнутые и почти замкнутые фигуры: число резких углов задаёт форму.
        if ((closed || (nearlyClosed && absNet > TWO_PI * 0.8f)) && absNet > 3.5f) {
            int sharp = rawTurns.sharpCount > 0 ? rawTurns.sharpCount : turns.sharpCount;
            if (sharp == 3 || sharp == 6) return "triangle";
            if (sharp == 4 || sharp == 8) return "square";
            if (sharp <= 2) return "circle";
            return "";
        }

        // 3) Почти прямая незамкнутая линия: горизонталь или вертикаль вниз.
        if (turns.sharpCount == 0 && turns.total < 0.85f && straightness < 1.12f && endDist > 0.25f) {
            if (Math.abs(dy) > Math.abs(dx) * 1.25f && dy > 0.0f) return "line_down";
            return "line";
        }

        // 4) V: одна резкая вершина — ориентация значения не имеет.
        if (turns.sharpCount == 1 && turns.sharpIndex >= 0 && !closed) {
            int vi = turns.sharpIndex;
            if (distance(x[0], y[0], x[vi], y[vi]) > 0.12f
                    && distance(x[n - 1], y[n - 1], x[vi], y[vi]) > 0.12f) return "v";
        }

        // 5) Волна: плавные чередующиеся повороты. Один период даёт 2 смены знака.
        if (!closed && turns.sharpCount < 2 && turns.total > 1.2f
                && (turns.signChanges >= 2 || radiusSpread > 0.4f)) return "wave";

        // 6) Зигзаг: 2+ резких поворота на незамкнутой кривой.
        if (!closed && turns.sharpCount >= 2) return "zigzag";

        // 7) Круг с небольшим разрывом.
        if (nearlyClosed && absNet > 4.5f && turns.sharpCount <= 2) return "circle";

        return "";
    }

    /** Угол поворота в вершине b: atan2(cross, dot) ∈ (-π, π]. */
    private static Turn turn(float ax, float ay, float bx, float by, float cx, float cy, int index) {
        float v1x = bx - ax, v1y = by - ay;
        float v2x = cx - bx, v2y = cy - by;
        float l1 = (float) Math.hypot(v1x, v1y), l2 = (float) Math.hypot(v2x, v2y);
        if (l1 < 1e-6f || l2 < 1e-6f) return null;
        float cross = (v1x * v2y - v1y * v2x) / (l1 * l2);
        float dot = (v1x * v2x + v1y * v2y) / (l1 * l2);
        return new Turn((float) Math.atan2(cross, dot), index);
    }

    /** Для замкнутых форм учитываем ещё два стыка между последней и первой точками. */
    private static TurnStats analyzeTurns(float[] sx, float[] sy, boolean closed) {
        TurnStats stats = new TurnStats();
        int n = sx.length;
        for (int i = 1; i < n - 1; i++) {
            stats.add(turn(sx[i - 1], sy[i - 1], sx[i], sy[i], sx[i + 1], sy[i + 1], i));
        }
        if (closed) {
            stats.add(turn(sx[n - 2], sy[n - 2], sx[n - 1], sy[n - 1], sx[0], sy[0], n - 1));
            stats.add(turn(sx[n - 1], sy[n - 1], sx[0], sy[0], sx[1], sy[1], 0));
        }
        return stats;
    }

    private record Turn(float angle, int index) {}

    private static final class TurnStats {
        float total;
        float net;
        int sharpCount;
        int sharpIndex = -1;
        int signChanges;
        private int lastSign;
        private float sharpest;

        void add(Turn t) {
            if (t == null) return;
            float a = t.angle();
            total += Math.abs(a);
            net += a;
            if (Math.abs(a) > SHARP_TURN) {
                sharpCount++;
                if (Math.abs(a) > sharpest) {
                    sharpest = Math.abs(a);
                    sharpIndex = t.index();
                }
            }
            int sign = a > MIN_DIRECTION ? 1 : (a < -MIN_DIRECTION ? -1 : 0);
            if (sign != 0) {
                if (lastSign != 0 && sign != lastSign) signChanges++;
                lastSign = sign;
            }
        }
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    private static float distance(Point a, Point b) {
        return distance(a.x(), a.y(), b.x(), b.y());
    }
}
