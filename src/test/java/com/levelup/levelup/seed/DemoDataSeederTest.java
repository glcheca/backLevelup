package com.levelup.levelup.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.levelup.levelup.course.CourseRepository;
import com.levelup.levelup.event.EventRepository;
import com.levelup.levelup.note.NoteRepository;
import com.levelup.levelup.reminder.ReminderRepository;
import com.levelup.levelup.task.TaskRepository;
import com.levelup.levelup.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.seed.enabled=true")
@ActiveProfiles("test")
class DemoDataSeederTest {

  @Autowired private DemoDataSeeder seeder;
  @Autowired private UserRepository userRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private TaskRepository taskRepository;
  @Autowired private NoteRepository noteRepository;
  @Autowired private ReminderRepository reminderRepository;

  @Test
  void seedsDemoDataOnce() {
    assertThat(userRepository.existsByEmail(DemoDataSeeder.DEMO_EMAIL)).isTrue();

    long courses = courseRepository.count();
    long events = eventRepository.count();
    long tasks = taskRepository.count();
    long notes = noteRepository.count();
    long reminders = reminderRepository.count();

    assertThat(courses).isEqualTo(5);
    assertThat(events).isEqualTo(20);
    assertThat(tasks).isEqualTo(15);
    assertThat(notes).isEqualTo(10);
    assertThat(reminders).isGreaterThanOrEqualTo(2);

    seeder.seed();

    assertThat(courseRepository.count()).isEqualTo(courses);
    assertThat(eventRepository.count()).isEqualTo(events);
    assertThat(taskRepository.count()).isEqualTo(tasks);
    assertThat(noteRepository.count()).isEqualTo(notes);
    assertThat(reminderRepository.count()).isEqualTo(reminders);
  }
}
