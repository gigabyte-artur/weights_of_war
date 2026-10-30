# -*- coding: utf-8 -*-
"""Стенд Weights of War: замеры обучения юнитов без окна.

Три задачи:

  run   — один прогон реального GameWorld на N поколений и таблица по поколениям;
  pair  — сравнение baseline (HEAD) и текущего рабочего дерева на одном наборе
          прогонов, с медианами: разброс между прогонами одного кода большой,
          одиночный прогон ничего не доказывает;
  tests — прогон :core:test через Gradle-обёртку (обходит JAVA_HOME=JRE).

Примеры:

  python tools/bench/wz_bench.py run 60
  python tools/bench/wz_bench.py run 60 --speed 60
  python tools/bench/wz_bench.py pair --seeds 7 11 23 31 47 53 --at 51
  python tools/bench/wz_bench.py tests

Зависимости: JDK 17+ (ищется сам), Gradle-обёртка проекта, установленный Gradle-кэш.
GUI не поднимается: Gdx.graphics и GL20 — заглушки, текстуры — 1x1.

Про seed: в игре Random создаётся без зерна, поэтому прогоны не детерминированы.
Флага seed здесь нет намеренно — вместо него несколько прогонов и медиана.
"""
import argparse
import glob
import io
import os
import re
import shutil
import statistics
import subprocess
import sys
import tarfile
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
BENCH_WORK = os.path.join(REPO, "build", "bench")          # build/ уже в .gitignore
CLASSPATH_FILE = os.path.join(REPO, "build", "bench-classpath.txt")
INIT_SCRIPT = os.path.join(HERE, "classpath.init.gradle")
BENCH_SRC = os.path.join(HERE, "WorldBench.java")


def log(text):
    sys.stdout.write(text + "\n")
    sys.stdout.flush()


# ---------------------------------------------------------------- окружение

def find_java():
    """Возвращает (java_exe, javac_exe, java_home). JAVA_HOME на этой машине — JRE, поэтому не верим ему слепо."""
    candidates = []
    home = os.environ.get("JAVA_HOME", "")
    if home:
        candidates.append(home)
    for pattern in (r"C:\Program Files\Java\jdk*", r"C:\Program Files\Java\jdk-*",
                    r"C:\PROGRA~1\Java\jdk*", r"C:\PROGRA~1\Java\jdk-*"):
        candidates.extend(glob.glob(pattern))

    usable = []
    for root in candidates:
        java = os.path.join(root, "bin", "java.exe")
        javac = os.path.join(root, "bin", "javac.exe")
        if os.path.isfile(java) and os.path.isfile(javac):
            usable.append((version_key(root), root, java, javac))
    if not usable:
        raise SystemExit("JDK с javac.exe не найден. Укажите путь в JAVA_HOME.")
    usable.sort(reverse=True)
    _, root, java, javac = usable[0]
    return java, javac, root


def version_key(path):
    numbers = re.findall(r"(\d+)", os.path.basename(path))
    return tuple(int(n) for n in numbers) if numbers else (0,)


def gdx_version():
    properties = os.path.join(REPO, "gradle.properties")
    if os.path.isfile(properties):
        match = re.search(r"^gdxVersion\s*=\s*(\S+)", io.open(properties, encoding="utf-8").read(), re.M)
        if match:
            return match.group(1)
    return None


def natives_jar():
    """gdx-platform natives-desktop: нужен GdxNativesLoader.load() в стенде."""
    pattern = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1",
                           "com.badlogicgames.gdx", "gdx-platform", "*", "*",
                           "gdx-platform-*-natives-desktop.jar")
    found = glob.glob(pattern)
    if not found:
        return None
    wanted = gdx_version()
    if wanted:
        exact = [p for p in found if "-%s-natives-desktop.jar" % wanted in p]
        if exact:
            return exact[0]
    return sorted(found)[-1]


def resolve_classpath(java, java_home):
    """Список jar'ов проекта через Gradle init-скрипт (build.gradle не правится)."""
    env = dict(os.environ)
    env["JAVA_HOME"] = java_home
    env["CI"] = "true"
    cmd = [java, "-cp", os.path.join(REPO, "gradle", "wrapper", "gradle-wrapper.jar"),
           "org.gradle.wrapper.GradleWrapperMain", "-I", INIT_SCRIPT,
           ":core:printBenchClasspath", "-q", "-Dorg.gradle.console=plain"]
    result = subprocess.run(cmd, cwd=REPO, env=env, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=1800)
    if not os.path.isfile(CLASSPATH_FILE):
        raise SystemExit("Не удалось получить classpath:\n" + (result.stdout + result.stderr)[-2000:])

    entries = io.open(CLASSPATH_FILE, encoding="utf-8").read().strip().split(";")
    repo_prefix = os.path.normcase(REPO)
    libs = [p for p in entries if p and not os.path.normcase(os.path.abspath(p)).startswith(repo_prefix)]
    native = natives_jar()
    if native and native not in libs:
        libs.append(native)
    log("classpath: %d элементов, стенд работает на %s" % (len(libs), java_home))
    return ";".join(libs)


# ---------------------------------------------------------------- компиляция

def java_sources(root):
    files = []
    for directory, _, names in os.walk(root):
        files.extend(os.path.join(directory, n) for n in names if n.endswith(".java"))
    return sorted(files)


def compile_tree(src_root, out_dir, libs, javac, label):
    """Компилирует все .java из src_root в out_dir. Список файлов идёт через argfile."""
    if os.path.isdir(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir)

    files = java_sources(src_root)
    if not files:
        raise SystemExit("в %s нет .java" % src_root)
    argfile = os.path.join(BENCH_WORK, "sources_%s.txt" % label)
    os.makedirs(BENCH_WORK, exist_ok=True)
    io.open(argfile, "w", encoding="utf-8").write("\n".join(files))

    result = subprocess.run([javac, "-encoding", "UTF-8", "-nowarn", "-J-Dfile.encoding=UTF-8",
                             "-cp", libs, "-d", out_dir, "@" + argfile],
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit("javac %s упал:\n%s" % (label, (result.stdout + result.stderr)[-2500:]))
    log("  собрано %d файлов (%s)" % (len(files), label))


def compile_bench(out_dir, game_classes, libs, javac):
    """Собирает WorldBench в out_dir; game_classes — собранные классы игры, они нужны на classpath."""
    os.makedirs(out_dir, exist_ok=True)
    result = subprocess.run([javac, "-encoding", "UTF-8", "-nowarn", "-J-Dfile.encoding=UTF-8",
                             "-cp", game_classes + ";" + libs, "-d", out_dir, BENCH_SRC],
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise SystemExit("javac WorldBench упал:\n" + (result.stdout + result.stderr)[-2500:])


# ---------------------------------------------------------------- прогон

ROW = re.compile(r"^(\d+),")


def run_world(classes_dir, libs, java, javac, gens, speed, max_ticks, tag):
    bench_classes = os.path.join(BENCH_WORK, "benchclasses")
    compile_bench(bench_classes, classes_dir, libs, javac)

    classpath = ";".join([bench_classes, classes_dir, libs])
    result = subprocess.run([java, "-Xmx1g", "-Dfile.encoding=UTF-8", "-cp", classpath,
                             "WorldBench", str(gens), str(speed), str(max_ticks)],
                            capture_output=True, text=True, encoding="utf-8",
                            errors="replace", timeout=3600)

    rows = []
    for line in result.stdout.splitlines():
        if ROW.match(line):
            bits = line.split(",")
            if len(bits) >= 10:
                rows.append(bits)
    log("  %s: строк %d, тиков %s%s" % (
        tag, len(rows),
        (re.search(r"ticks (\d+)", result.stdout).group(1) if "ticks" in result.stdout else "?"),
        ("" if result.returncode == 0 else "  (exit %d)" % result.returncode)))
    if result.stderr.strip():
        tail = result.stderr.strip().splitlines()[-1]
        log("  stderr: " + tail[:200])
    return rows


def metrics(rows, at_generation):
    """Метрики на выбранном поколении (или на последнем доступном)."""
    if not rows:
        return None
    chosen = None
    for bits in rows:
        if int(bits[0]) == at_generation:
            chosen = bits
            break
    if chosen is None:
        chosen = rows[-1]
    gen = int(chosen[0])
    core_sum = sum(int(x) for x in chosen[1:6])
    return {"gen": gen, "avg": float(chosen[7]), "core": core_sum, "best": int(chosen[8]),
            "alive": int(chosen[6])}


def print_run(rows):
    if not rows:
        log("нет данных")
        return
    log("")
    log("  gen  ядро(меч/лук/щит/жрец/разв)   живых   avgFit   лучший   тик")
    for bits in rows[:: max(1, len(rows) // 12)] + [rows[-1]]:
        cores = "/".join(bits[1:6])
        log("  %4s  %-27s %6s  %7s  %7s  %6s" % (bits[0], cores, bits[6], bits[7], bits[8], bits[9]))
    log("")


def cmd_run(args):
    java, javac, java_home = find_java()
    libs = resolve_classpath(java, java_home)
    classes_dir = os.path.join(BENCH_WORK, "classes_worktree")
    log("компилирую рабочее дерево")
    compile_tree(os.path.join(REPO, "core", "src", "main", "java"), classes_dir, libs, javac, "worktree")
    rows = run_world(classes_dir, libs, java, javac, args.gens, args.speed, args.max_ticks, "run")
    print_run(rows)
    point = metrics(rows, args.at)
    if point:
        print_metrics_summary({"прогон": {"avg": point["avg"], "core": point["core"],
                                            "best": point["best"], "n": 1}})


def cmd_pair(args):
    java, javac, java_home = find_java()
    libs = resolve_classpath(java, java_home)

    head_src = os.path.join(BENCH_WORK, "head_src")
    if os.path.isdir(head_src):
        shutil.rmtree(head_src)
    os.makedirs(head_src)
    log("распаковываю HEAD")
    subprocess.run(["git", "-C", REPO, "archive", "HEAD", "core/src/main/java"],
                   stdout=open(os.path.join(BENCH_WORK, "head.tar"), "wb"), check=True)
    with tarfile.open(os.path.join(BENCH_WORK, "head.tar")) as archive:
        archive.extractall(head_src)

    base_classes = os.path.join(BENCH_WORK, "classes_head")
    work_classes = os.path.join(BENCH_WORK, "classes_worktree")
    compile_tree(os.path.join(head_src, "core", "src", "main", "java"), base_classes, libs, javac, "HEAD")
    compile_tree(os.path.join(REPO, "core", "src", "main", "java"), work_classes, libs, javac, "рабочее дерево")

    results = {"BASE": [], "WORK": []}
    for run_index in range(args.runs):
        for tag, classes_dir in (("BASE", base_classes), ("WORK", work_classes)):
            rows = run_world(classes_dir, libs, java, javac, args.gens, args.speed,
                             args.max_ticks, "%s #%d" % (tag, run_index + 1))
            point = metrics(rows, args.at)
            if point:
                results[tag].append(point)

    log("")
    log("сравнение (поколение из замера; прогонов на вариант: %d)" % args.runs)
    summary = {}
    for tag, label in (("BASE", "HEAD"), ("WORK", "рабочее дерево")):
        values = results[tag]
        if not values:
            log("  %-15s нет данных" % label)
            continue
        summary[label] = {
            "avg": statistics.median(v["avg"] for v in values),
            "core": statistics.median(v["core"] for v in values),
            "best": statistics.median(v["best"] for v in values),
            "n": len(values),
        }
    print_metrics_summary(summary)

    if results["BASE"] and results["WORK"]:
        base_avg = [v["avg"] for v in results["BASE"]]
        work_avg = [v["avg"] for v in results["WORK"]]
        log("")
        log("  медиана avgFit: HEAD %.1f, рабочее дерево %.1f (разница %+.1f)"
            % (statistics.median(base_avg), statistics.median(work_avg),
               statistics.median(work_avg) - statistics.median(base_avg)))
        if len(base_avg) > 1 and len(work_avg) > 1:
            log("  разброс avgFit: HEAD %.1f..%.1f, рабочее дерево %.1f..%.1f"
                % (min(base_avg), max(base_avg), min(work_avg), max(work_avg)))
            overlap = not (min(work_avg) > max(base_avg) or min(base_avg) > max(work_avg))
            log("  диапазоны %s — при таком разбросе вывод по медиане остаётся слабым, "
                "нужно больше прогонов" % ("ПЕРЕКРЫВАЮТСЯ" if overlap else "не перекрываются"))
    return 0


def print_metrics_summary(summary):
    if not summary:
        return
    log("")
    log("  %-16s %5s %10s %12s %10s" % ("вариант", "n", "avgFit", "сумма ядер", "лучший"))
    for label, values in summary.items():
        log("  %-16s %5d %10.1f %12.0f %10.0f"
            % (label, values["n"], values["avg"], values["core"], values["best"]))


def cmd_tests(args):
    java, _, java_home = find_java()
    env = dict(os.environ)
    env["JAVA_HOME"] = java_home
    env["CI"] = "true"
    cmd = [java, "-cp", os.path.join(REPO, "gradle", "wrapper", "gradle-wrapper.jar"),
           "org.gradle.wrapper.GradleWrapperMain"] + (args.gradle_args or [":core:test"])
    result = subprocess.run(cmd, cwd=REPO, env=env, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=3600)
    tail = (result.stdout + "\n" + result.stderr).strip().splitlines()[-60:]
    log("\n".join(tail))
    log("EXIT %d" % result.returncode)
    return result.returncode


def main():
    parser = argparse.ArgumentParser(description="Стенд Weights of War (headless-замеры обучения)")
    sub = parser.add_subparsers(dest="command", required=True)

    def common(p):
        p.add_argument("gens", nargs="?", type=int, default=None, help="до какого поколения крутить (по умолчанию 60)")
        p.add_argument("--generations", dest="gens_flag", type=int, default=None,
                       help="то же, что позиционный аргумент")
        p.add_argument("--speed", type=float, default=60, help="gameSpeed (по умолчанию 60)")
        p.add_argument("--max-ticks", type=int, default=400000, help="предохранитель по тикам")
        p.add_argument("--at", type=int, default=51, help="поколение для снятия метрик (по умолчанию 51)")

    run = sub.add_parser("run", help="один прогон рабочего дерева")
    common(run)
    run.set_defaults(func=cmd_run)

    pair = sub.add_parser("pair", help="HEAD против рабочего дерева, медианы по прогонам")
    common(pair)
    pair.add_argument("--runs", type=int, default=6, help="прогонов на вариант (по умолчанию 6)")
    pair.set_defaults(func=cmd_pair)

    tests = sub.add_parser("tests", help=":core:test через Gradle-обёртку")
    tests.add_argument("gradle_args", nargs="*", help="аргументы Gradle (по умолчанию :core:test)")
    tests.set_defaults(func=cmd_tests)

    args = parser.parse_args()
    if getattr(args, "gens_flag", None) is not None:
        args.gens = args.gens_flag
    if getattr(args, "gens", None) is None:
        args.gens = 60
    return args.func(args)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main() or 0)
