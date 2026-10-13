package dan.reminder.scheduler.quartz.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import dan.reminder.model.Reminder;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.transaction.annotation.Transactional;

class QuartzEmailSchedulerTest {

    @Test
    void storesInitialAttemptAsInteger() throws Exception {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        QuartzEmailScheduler emailScheduler = new QuartzEmailScheduler(scheduler);
        Reminder reminder = Mockito.mock(Reminder.class);
        Mockito.when(reminder.getId()).thenReturn(1L);
        Mockito.when(reminder.getTitle()).thenReturn("Reminder");
        Mockito.when(reminder.getDescription()).thenReturn("Do something");
        Mockito.when(reminder.getRemind()).thenReturn(java.time.Instant.now().plusSeconds(10));

        emailScheduler.schedule(reminder, "user@example.com");

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).scheduleJob(jobCaptor.capture(), Mockito.any(Trigger.class));
        assertThat(jobCaptor.getValue().getJobDataMap().get(QuartzEmailScheduler.ATTEMPT_KEY))
                .isEqualTo(1)
                .isInstanceOf(Integer.class);
    }

    @Test
    void storesRetryAttemptAsInteger() throws Exception {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        QuartzEmailScheduler emailScheduler = new QuartzEmailScheduler(scheduler);
        JobDetail jobDetail = JobBuilder.newJob(dan.reminder.scheduler.quartz.job.EmailReminderJob.class)
                .withIdentity("email-job-1", "email-jobs")
                .build();

        emailScheduler.scheduleRetry(jobDetail, 2, Duration.ofMinutes(1));

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(triggerCaptor.capture());
        assertThat(triggerCaptor.getValue().getJobDataMap().get(QuartzEmailScheduler.ATTEMPT_KEY))
                .isEqualTo(2)
                .isInstanceOf(Integer.class);
    }

    @Test
    void recreateDeletesOldJobAndRetriesBeforeSchedulingUpdatedJob() throws Exception {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        QuartzEmailScheduler emailScheduler = new QuartzEmailScheduler(scheduler);
        Reminder reminder = reminder(1L);

        emailScheduler.recreate(reminder, "user@example.com");

        InOrder order = Mockito.inOrder(scheduler);
        order.verify(scheduler).deleteJob(JobKey.jobKey("email-job-1", "email-jobs"));
        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        order.verify(scheduler).scheduleJob(jobCaptor.capture(), Mockito.any(Trigger.class));
        assertThat(jobCaptor.getValue().getJobDataMap().getString("email"))
                .isEqualTo("user@example.com");
        assertThat(jobCaptor.getValue().getJobDataMap().getString("subject"))
                .isEqualTo("Reminder");
    }

    @Test
    void recreatePropagatesSchedulingFailureForTransactionRollback() throws Exception {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        QuartzEmailScheduler emailScheduler = new QuartzEmailScheduler(scheduler);
        SchedulerException failure = new SchedulerException("Database unavailable");
        Mockito.doThrow(failure).when(scheduler)
                .scheduleJob(Mockito.any(JobDetail.class), Mockito.any(Trigger.class));

        assertThatThrownBy(() -> emailScheduler.recreate(reminder(1L), "user@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasCause(failure);
        verify(scheduler).deleteJob(JobKey.jobKey("email-job-1", "email-jobs"));
        assertThat(QuartzEmailScheduler.class.getMethod("recreate", Reminder.class, String.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    private Reminder reminder(Long id) {
        Reminder reminder = Mockito.mock(Reminder.class);
        Mockito.when(reminder.getId()).thenReturn(id);
        Mockito.when(reminder.getTitle()).thenReturn("Reminder");
        Mockito.when(reminder.getDescription()).thenReturn("Do something");
        Mockito.when(reminder.getRemind()).thenReturn(java.time.Instant.now().plusSeconds(10));
        return reminder;
    }
}
