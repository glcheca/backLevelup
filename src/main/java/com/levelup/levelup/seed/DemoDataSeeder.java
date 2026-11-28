package com.levelup.levelup.seed;

import com.levelup.levelup.common.ReminderText;
import com.levelup.levelup.course.ClassBlock;
import com.levelup.levelup.course.ClassBlockRepository;
import com.levelup.levelup.course.Course;
import com.levelup.levelup.course.CourseRepository;
import com.levelup.levelup.event.Event;
import com.levelup.levelup.event.EventRepository;
import com.levelup.levelup.event.EventStatus;
import com.levelup.levelup.event.EventType;
import com.levelup.levelup.grade.Assessment;
import com.levelup.levelup.grade.AssessmentRepository;
import com.levelup.levelup.note.Note;
import com.levelup.levelup.note.NoteRepository;
import com.levelup.levelup.reminder.DatabaseReminderHook;
import com.levelup.levelup.reminder.Reminder;
import com.levelup.levelup.reminder.ReminderRepository;
import com.levelup.levelup.reminder.ReminderTargetType;
import com.levelup.levelup.task.Task;
import com.levelup.levelup.task.TaskRepository;
import com.levelup.levelup.task.TaskStatus;
import com.levelup.levelup.user.User;
import com.levelup.levelup.user.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo para el perfil `dev`: un usuario demo con cinco cursos, horario, eventos,
 * tareas, apuntes, evaluaciones y recordatorios. Es idempotente: si el usuario ya existe no vuelve
 * a sembrar nada.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

  public static final String DEMO_EMAIL = "demo@levelup.test";
  public static final String DEMO_PASSWORD = "Demo1234!";

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
  private static final ZoneId ZONE = ZoneId.of("America/Lima");
  private static final String PERIOD = "2026-II";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final CourseRepository courseRepository;
  private final ClassBlockRepository classBlockRepository;
  private final EventRepository eventRepository;
  private final TaskRepository taskRepository;
  private final NoteRepository noteRepository;
  private final AssessmentRepository assessmentRepository;
  private final ReminderRepository reminderRepository;
  private final DatabaseReminderHook reminderHook;

  public DemoDataSeeder(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      CourseRepository courseRepository,
      ClassBlockRepository classBlockRepository,
      EventRepository eventRepository,
      TaskRepository taskRepository,
      NoteRepository noteRepository,
      AssessmentRepository assessmentRepository,
      ReminderRepository reminderRepository,
      DatabaseReminderHook reminderHook) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.courseRepository = courseRepository;
    this.classBlockRepository = classBlockRepository;
    this.eventRepository = eventRepository;
    this.taskRepository = taskRepository;
    this.noteRepository = noteRepository;
    this.assessmentRepository = assessmentRepository;
    this.reminderRepository = reminderRepository;
    this.reminderHook = reminderHook;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seed();
  }

  @Transactional
  public void seed() {
    if (userRepository.existsByEmail(DEMO_EMAIL)) {
      log.info("Datos demo ya presentes ({}); no se vuelven a sembrar.", DEMO_EMAIL);
      return;
    }

    User user =
        userRepository.save(
            new User(
                DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD),
                "Estudiante Demo",
                "America/Lima"));

    List<Course> courses = seedCourses(user);
    seedSchedule(courses);
    seedEvents(user, courses);
    seedTasks(user, courses);
    seedNotes(user, courses);
    seedAssessments(courses);
    seedCustomReminders(user);

    log.info(
        "Datos demo sembrados para {}: 5 cursos, 10 bloques de horario, 20 eventos, 15 tareas,"
            + " 10 apuntes y 17 evaluaciones.",
        DEMO_EMAIL);
  }

  private List<Course> seedCourses(User user) {
    List<Course> courses = new ArrayList<>();
    courses.add(course(user, "Álgebra Lineal", "MAT-201", "Dra. Rojas", "#6366f1", "4"));
    courses.add(course(user, "Cálculo Diferencial", "MAT-102", "Prof. Salazar", "#0ea5e9", "5"));
    courses.add(course(user, "Física I", "FIS-101", "Ing. Torres", "#f97316", "4"));
    courses.add(course(user, "Programación I", "CS-101", "Lic. Mendoza", "#10b981", "4"));
    courses.add(course(user, "Comunicación Académica", "HUM-110", "Mg. Castro", "#a855f7", "3"));
    return courses;
  }

  private Course course(
      User user, String name, String code, String professor, String color, String credits) {
    Course course = new Course(user, name, PERIOD);
    course.setCode(code);
    course.setProfessor(professor);
    course.setColor(color);
    course.setCredits(new BigDecimal(credits));
    return courseRepository.save(course);
  }

  private void seedSchedule(List<Course> courses) {
    block(courses.get(0), 1, 8, 0, 10, 0, "Aula 201");
    block(courses.get(0), 3, 10, 0, 12, 0, "Aula 201");
    block(courses.get(1), 2, 8, 0, 10, 0, "Aula 105");
    block(courses.get(1), 4, 8, 0, 10, 0, "Aula 105");
    block(courses.get(1), 5, 10, 0, 12, 0, "Lab 3");
    block(courses.get(2), 1, 14, 0, 16, 0, "Lab Física");
    block(courses.get(2), 4, 14, 0, 16, 0, "Aula 302");
    block(courses.get(3), 2, 14, 0, 17, 0, "Lab 4");
    block(courses.get(3), 5, 14, 0, 16, 0, "Lab 4");
    block(courses.get(4), 3, 16, 0, 18, 0, "Aula 110");
  }

  private void block(
      Course course,
      int day,
      int startHour,
      int startMinute,
      int endHour,
      int endMinute,
      String room) {
    classBlockRepository.save(
        new ClassBlock(
            course,
            (short) day,
            LocalTime.of(startHour, startMinute),
            LocalTime.of(endHour, endMinute),
            room));
  }

  private void seedEvents(User user, List<Course> courses) {
    event(user, "Entrega de práctica 3", EventType.ASSIGNMENT, 0, 23, 59, courses.get(0), 120);
    event(user, "Examen parcial de Cálculo", EventType.EXAM, 1, 8, 0, courses.get(1), 1440);
    event(user, "Repaso de Física", EventType.PERSONAL, 2, 18, 0, courses.get(2), 60);
    event(user, "Clase magistral de Programación", EventType.CLASS, 3, 10, 0, courses.get(3), null);
    event(user, "Taller de escritura", EventType.PERSONAL, 4, 19, 0, null, null);
    event(user, "Entrega de laboratorio 2", EventType.ASSIGNMENT, 5, 8, 0, courses.get(2), 1440);
    event(user, "Exposición de Comunicación", EventType.ASSIGNMENT, 6, 16, 0, courses.get(4), 60);
    event(user, "Simulacro de examen de Álgebra", EventType.EXAM, 7, 9, 0, courses.get(0), null);
    event(user, "Estudiar con el grupo", EventType.PERSONAL, 8, 18, 30, null, null);
    event(user, "Asesoría con la Dra. Rojas", EventType.PERSONAL, 9, 11, 0, courses.get(0), null);
    event(user, "Examen final de Cálculo", EventType.EXAM, 10, 8, 0, courses.get(1), 1440);
    event(
        user,
        "Entrega de proyecto de Programación",
        EventType.ASSIGNMENT,
        12,
        15,
        0,
        courses.get(3),
        120);
    event(user, "Feria de ciencias", EventType.PERSONAL, 14, 10, 0, null, null);
    event(user, "Parcial de Física", EventType.EXAM, 16, 9, 0, courses.get(2), 1440);
    event(user, "Repaso general de Álgebra", EventType.EXAM, 18, 18, 0, courses.get(0), 60);
    event(user, "Entrega de ensayo", EventType.ASSIGNMENT, 20, 8, 0, courses.get(4), 1440);
    event(user, "Sustentación de proyecto", EventType.ASSIGNMENT, 22, 14, 0, courses.get(3), null);
    event(user, "Examen final de Álgebra", EventType.EXAM, 25, 9, 0, courses.get(0), 1440);
    event(user, "Cierre de notas", EventType.PERSONAL, 27, 10, 0, null, null);
    event(user, "Celebración de fin de ciclo", EventType.PERSONAL, 29, 18, 0, null, null);
  }

  private void event(
      User user,
      String title,
      EventType type,
      int days,
      int hour,
      int minute,
      Course course,
      Integer offsetMinutes) {
    Instant start = at(days, hour, minute);
    Event event = new Event(user, title, type, start, start.plus(Duration.ofHours(2)));
    event.setCourse(course);
    event.setStatus(EventStatus.scheduled);
    event.setLocation(course == null ? null : "Campus universitario");
    event.setReminderOffsetMinutes(offsetMinutes);
    Event saved = eventRepository.save(event);
    if (offsetMinutes != null) {
      reminderHook.onEventChanged(
          user.getId(),
          saved.getId(),
          saved.getTitle(),
          ReminderText.body("Empieza", saved.getStartAt(), ZONE, saved.getLocation()),
          saved.getStartAt().minus(offsetMinutes, ChronoUnit.MINUTES),
          offsetMinutes);
    }
  }

  private void seedTasks(User user, List<Course> courses) {
    task(user, "Entregar práctica de Cálculo", courses.get(1), -1, 23, 59, TaskStatus.todo, 2);
    task(user, "Leer capítulo 4 de Álgebra", courses.get(0), 0, 20, 0, TaskStatus.doing, 1);
    task(user, "Revisar apuntes de Física", courses.get(2), 0, 22, 0, TaskStatus.todo, 0);
    task(user, "Resolver ejercicios 1-10", courses.get(0), 1, 18, 0, TaskStatus.todo, 2);
    task(user, "Preparar exposición", courses.get(4), 2, 9, 0, TaskStatus.doing, 2);
    task(user, "Resumir paper de IA", courses.get(3), 3, 20, 0, TaskStatus.todo, 1);
    task(user, "Laboratorio 2: informe", courses.get(2), 4, 23, 59, TaskStatus.todo, 2);
    task(user, "Práctica calificada 3", courses.get(0), 5, 8, 0, TaskStatus.todo, 2);
    task(user, "Ensayo de Comunicación", courses.get(4), 6, 23, 59, TaskStatus.todo, 1);
    task(user, "Repasar fórmulas", courses.get(1), 7, 19, 0, TaskStatus.todo, 1);
    task(user, "Grabar video de presentación", courses.get(3), 8, 17, 0, TaskStatus.todo, 0);
    task(user, "Leer capítulo 5", courses.get(0), 9, 21, 0, TaskStatus.todo, 1);
    task(user, "Entrega de laboratorio 1", courses.get(2), -2, 23, 59, TaskStatus.done, 2);
    task(user, "Mapa conceptual de Cálculo", courses.get(1), -3, 23, 59, TaskStatus.done, 1);
    task(user, "Proyecto final de Programación", courses.get(3), 12, 23, 59, TaskStatus.todo, 2);
  }

  private void task(
      User user,
      String title,
      Course course,
      int days,
      int hour,
      int minute,
      TaskStatus status,
      int priority) {
    Task task = new Task(user, title);
    task.setCourse(course);
    task.setDueAt(at(days, hour, minute));
    task.setStatus(status);
    task.setPriority((short) priority);
    task.setReminderOffsetMinutes(status == TaskStatus.done ? null : 60);
    if (status == TaskStatus.done) {
      task.setCompletedAt(at(days, 12, 0));
    }
    Task saved = taskRepository.save(task);
    if (saved.getReminderOffsetMinutes() != null && saved.getDueAt().isAfter(Instant.now())) {
      reminderHook.onTaskChanged(
          user.getId(),
          saved.getId(),
          saved.getTitle(),
          ReminderText.body(
              "Vence", saved.getDueAt(), ZONE, course == null ? null : course.getName()),
          saved.getDueAt().minus(60, ChronoUnit.MINUTES),
          60);
    }
  }

  private void seedNotes(User user, List<Course> courses) {
    note(
        user,
        "Fórmulas de derivadas",
        courses.get(1),
        true,
        Set.of("cálculo", "fórmulas"),
        """
        # Derivadas esenciales

        | Función | Derivada |
        |---|---|
        | $x^n$ | $n x^{n-1}$ |
        | $\\sin x$ | $\\cos x$ |
        | $e^x$ | $e^x$ |
        | $\\ln x$ | $1/x$ |

        > Regla de la cadena: $(f \\circ g)'(x) = f'(g(x)) \\cdot g'(x)$.
        """);
    note(
        user,
        "Resumen Tema 3: Matrices",
        courses.get(0),
        true,
        Set.of("álgebra", "matrices"),
        """
        # Matrices

        - Suma y producto por escalar: elemento a elemento.
        - Producto: filas por columnas; **no** es conmutativo.
        - Una matriz es invertible si su determinante es distinto de cero.
        - Rango = número de filas linealmente independientes.

        ## Pendiente
        Repasar la demostración de la regla de Cramer.
        """);
    note(
        user,
        "Leyes de Newton",
        courses.get(2),
        false,
        Set.of("física", "teoría"),
        """
        # Las tres leyes

        1. **Inercia**: un cuerpo sin fuerza neta mantiene su estado de movimiento.
        2. **F = m·a**: la aceleración es proporcional a la fuerza neta.
        3. **Acción-reacción**: a toda fuerza le corresponde una igual y opuesta.

        Aplicación: diagramas de cuerpo libre antes de resolver cualquier problema.
        """);
    note(
        user,
        "Complejidad algorítmica",
        courses.get(3),
        false,
        Set.of("programación", "big-o"),
        """
        # Big-O

        - `O(1)`: acceso a un índice.
        - `O(log n)`: búsqueda binaria.
        - `O(n)`: recorrer una lista.
        - `O(n log n)`: ordenamientos eficientes.
        - `O(n²)`: bucles anidados.

        ```java
        int suma(int[] valores) {
          int total = 0;
          for (int valor : valores) total += valor;
          return total;
        }
        ```
        """);
    note(
        user,
        "Cómo escribir una tesis",
        courses.get(4),
        false,
        Set.of("escritura", "tesis"),
        """
        # Estructura básica

        1. Planteamiento del problema.
        2. Objetivos general y específicos.
        3. Marco teórico.
        4. Metodología.
        5. Resultados y discusión.
        6. Conclusiones y recomendaciones.
        """);
    note(
        user,
        "Errores comunes en integrales",
        courses.get(1),
        false,
        Set.of("cálculo", "errores"),
        """
        # Errores que evitar

        - Olvidar la constante de integración.
        - Cambiar de variable sin actualizar los límites.
        - Confundir la integral de un producto con el producto de integrales.
        """);
    note(
        user,
        "Vocabulario inglés académico",
        null,
        false,
        Set.of("inglés", "vocabulario"),
        """
        # Palabras útiles

        - **furthermore**: además
        - **whereas**: mientras que
        - **thereby**: de este modo
        - **insight**: idea o comprensión profunda
        """);
    note(
        user,
        "Checklist de laboratorio",
        courses.get(2),
        false,
        Set.of("física", "laboratorio"),
        """
        # Antes de entregar

        - [ ] Datos tabulados con unidades.
        - [ ] Incertidumbre calculada.
        - [ ] Gráficos con ejes rotulados.
        - [ ] Conclusiones que respondan al objetivo.
        """);
    note(
        user,
        "Git: comandos esenciales",
        courses.get(3),
        false,
        Set.of("programación", "git"),
        """
        # Flujo diario

        ```bash
        git status
        git add -A
        git commit -m "feat: ..."
        git push origin main
        ```

        Para revisar el historial: `git log --oneline --graph`.
        """);
    note(
        user,
        "Ideas para el proyecto final",
        courses.get(3),
        false,
        Set.of("programación", "ideas"),
        """
        # Opciones

        - Gestor de horarios con recordatorios.
        - Analizador de apuntes en markdown.
        - Tablero de hábitos con rachas.

        Criterios: utilidad real, alcance acotado y demo en menos de 5 minutos.
        """);
  }

  private void note(
      User user, String title, Course course, boolean pinned, Set<String> tags, String content) {
    Note note = new Note(user, title);
    note.setCourse(course);
    note.setContentMd(content);
    note.setPinned(pinned);
    note.setTags(new LinkedHashSet<>(tags));
    note.refreshSearchText();
    noteRepository.save(note);
  }

  private void seedAssessments(List<Course> courses) {
    assessment(courses.get(0), "Práctica 1", 30, 15, -20);
    assessment(courses.get(0), "Práctica 2", 30, 11, -6);
    assessment(courses.get(0), "Examen final", 40, null, 25);

    assessment(courses.get(1), "Práctica 1", 20, 17, -18);
    assessment(courses.get(1), "Práctica 2", 20, 14, -9);
    assessment(courses.get(1), "Parcial", 30, 12, 1);
    assessment(courses.get(1), "Final", 30, null, 10);

    assessment(courses.get(2), "Laboratorio 1", 20, 18, -14);
    assessment(courses.get(2), "Laboratorio 2", 20, null, 5);
    assessment(courses.get(2), "Parcial", 30, 13, 16);
    assessment(courses.get(2), "Final", 30, null, 28);

    assessment(courses.get(3), "Proyecto 1", 30, 19, -10);
    assessment(courses.get(3), "Proyecto 2", 30, 16, 12);
    assessment(courses.get(3), "Examen", 40, null, 26);

    assessment(courses.get(4), "Ensayo 1", 25, 15, -12);
    assessment(courses.get(4), "Exposición", 25, 17, -2);
    assessment(courses.get(4), "Ensayo final", 50, null, 20);
  }

  private void assessment(
      Course course, String name, int weight, Integer score, int dueDaysFromToday) {
    Assessment assessment = new Assessment(course, name);
    assessment.setWeight(BigDecimal.valueOf(weight));
    assessment.setMaxScore(BigDecimal.valueOf(20));
    assessment.setScore(score == null ? null : BigDecimal.valueOf(score));
    assessment.setDueAt(at(dueDaysFromToday, 23, 59));
    assessmentRepository.save(assessment);
  }

  private void seedCustomReminders(User user) {
    reminder(
        user,
        "Repasar apuntes de Cálculo",
        "Repasa derivadas e integrales antes del parcial.",
        at(1, 19, 0));
    reminder(
        user,
        "Entregar ensayo de Comunicación",
        "Revisa la rúbrica antes de subirlo al campus.",
        at(2, 8, 0));
  }

  private void reminder(User user, String title, String body, Instant triggerAt) {
    Reminder reminder =
        new Reminder(user, ReminderTargetType.CUSTOM, null, "custom:" + UUID.randomUUID());
    reminder.setTitle(title);
    reminder.setBody(body);
    reminder.setTriggerAt(triggerAt);
    reminderRepository.save(reminder);
  }

  private static Instant at(int daysFromToday, int hour, int minute) {
    return LocalDate.now(ZONE)
        .plusDays(daysFromToday)
        .atTime(hour, minute)
        .atZone(ZONE)
        .toInstant();
  }
}
