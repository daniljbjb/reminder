package dan.reminder.scheduler.quartz.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dan.reminder.config.EmailRetryProperties;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import dan.reminder.service.EmailService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

class EmailReminderJobTest {

    @Test
    void schedulesPersistedRetryWhenEmailDeliveryFails() throws Exception {
        EmailService emailService = Mockito.mock(EmailService.class);
        QuartzEmailScheduler quartzEmailScheduler = Mockito.mock(QuartzEmailScheduler.class);
        EmailReminderJob job = new EmailReminderJob(
                emailService,
                quartzEmailScheduler,
                new EmailRetryProperties(3, Duration.ofMinutes(1))
        );

        JobDataMap data = new JobDataMap();
        data.put("email", "user@example.com");
        data.put("subject", "Reminder");
        data.put("message", "Do something");
        data.put(QuartzEmailScheduler.ATTEMPT_KEY, "1");
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class)
                .withIdentity("email-job-1", "email-jobs")
                .usingJobData(data)
                .build();

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getJobDetail()).thenReturn(detail);
        Mockito.doThrow(new IllegalStateException("SMTP is unavailable"))
                .when(emailService).sendTextEmail("user@example.com", "Reminder", "Do something");

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class);

        ArgumentCaptor<Integer> attemptCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Duration> delayCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(quartzEmailScheduler).scheduleRetry(
                Mockito.eq(detail), attemptCaptor.capture(), delayCaptor.capture());
        verify(emailService).sendTextEmail("user@example.com", "Reminder", "Do something");
        assertThat(attemptCaptor.getValue()).isEqualTo(2);
        assertThat(delayCaptor.getValue()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void stopsSchedulingRetriesAfterConfiguredLimit() throws Exception {
        EmailService emailService = Mockito.mock(EmailService.class);
        QuartzEmailScheduler quartzEmailScheduler = Mockito.mock(QuartzEmailScheduler.class);
        EmailReminderJob job = new EmailReminderJob(
                emailService,
                quartzEmailScheduler,
                new EmailRetryProperties(3, Duration.ofMinutes(1))
        );

        JobDataMap data = new JobDataMap();
        data.put("email", "user@example.com");
        data.put("subject", "Reminder");
        data.put("message", "Do something");
        data.put(QuartzEmailScheduler.ATTEMPT_KEY, "3");
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class).withIdentity("email-job-1").build();

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getJobDetail()).thenReturn(detail);
        Mockito.doThrow(new IllegalStateException("SMTP is unavailable"))
                .when(emailService).sendTextEmail(any(), any(), any());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class);

        verify(emailService).sendTextEmail("user@example.com", "Reminder", "Do something");
        Mockito.verifyNoInteractions(quartzEmailScheduler);
    }
}
