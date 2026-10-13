package dan.reminder.scheduler.quartz.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dan.reminder.config.EmailRetryProperties;
import dan.reminder.exception.EmailDeliveryException;
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
        data.put(QuartzEmailScheduler.ATTEMPT_KEY, 1);
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class)
                .withIdentity("email-job-1", "email-jobs")
                .usingJobData(data)
                .build();

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getJobDetail()).thenReturn(detail);
        Mockito.doThrow(new EmailDeliveryException("SMTP is unavailable", null))
                .when(emailService).sendTextEmail("user@example.com", "Reminder", "Do something");

        assertThatCode(() -> job.execute(context)).doesNotThrowAnyException();

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
        data.put(QuartzEmailScheduler.ATTEMPT_KEY, 3);
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class).withIdentity("email-job-1").build();

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getJobDetail()).thenReturn(detail);
        Mockito.doThrow(new EmailDeliveryException("SMTP is unavailable", null))
                .when(emailService).sendTextEmail(any(), any(), any());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class);

        verify(emailService).sendTextEmail("user@example.com", "Reminder", "Do something");
        Mockito.verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void usesLinearDelayForLaterAttempts() throws Exception {
        EmailService emailService = Mockito.mock(EmailService.class);
        QuartzEmailScheduler quartzEmailScheduler = Mockito.mock(QuartzEmailScheduler.class);
        EmailReminderJob job = new EmailReminderJob(
                emailService,
                quartzEmailScheduler,
                new EmailRetryProperties(4, Duration.ofMinutes(1))
        );

        JobDataMap data = emailJobData(2);
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class).withIdentity("email-job-1").build();
        JobExecutionContext context = context(data, detail);
        Mockito.doThrow(new EmailDeliveryException("SMTP is unavailable", null))
                .when(emailService).sendTextEmail(any(), any(), any());

        job.execute(context);

        verify(quartzEmailScheduler).scheduleRetry(detail, 3, Duration.ofMinutes(2));
    }

    @Test
    void doesNotRetryUnexpectedRuntimeException() {
        EmailService emailService = Mockito.mock(EmailService.class);
        QuartzEmailScheduler quartzEmailScheduler = Mockito.mock(QuartzEmailScheduler.class);
        EmailReminderJob job = new EmailReminderJob(
                emailService,
                quartzEmailScheduler,
                new EmailRetryProperties(3, Duration.ofMinutes(1))
        );

        JobDataMap data = emailJobData(1);
        JobExecutionContext context = context(data,
                JobBuilder.newJob(EmailReminderJob.class).withIdentity("email-job-1").build());
        IllegalArgumentException failure = new IllegalArgumentException("Programming error");
        Mockito.doThrow(failure).when(emailService).sendTextEmail(any(), any(), any());

        assertThatThrownBy(() -> job.execute(context)).isSameAs(failure);
        Mockito.verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void reportsFailureWhenRetryCannotBeScheduled() {
        EmailService emailService = Mockito.mock(EmailService.class);
        QuartzEmailScheduler quartzEmailScheduler = Mockito.mock(QuartzEmailScheduler.class);
        EmailReminderJob job = new EmailReminderJob(
                emailService,
                quartzEmailScheduler,
                new EmailRetryProperties(3, Duration.ofMinutes(1))
        );

        JobDataMap data = emailJobData(1);
        JobDetail detail = JobBuilder.newJob(EmailReminderJob.class).withIdentity("email-job-1").build();
        JobExecutionContext context = context(data, detail);
        EmailDeliveryException deliveryFailure = new EmailDeliveryException("SMTP is unavailable", null);
        IllegalStateException schedulingFailure = new IllegalStateException("Quartz is unavailable");
        Mockito.doThrow(deliveryFailure).when(emailService).sendTextEmail(any(), any(), any());
        Mockito.doThrow(schedulingFailure).when(quartzEmailScheduler)
                .scheduleRetry(detail, 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCause(schedulingFailure);
        assertThat(schedulingFailure.getSuppressed()).containsExactly(deliveryFailure);
    }

    private JobDataMap emailJobData(int attempt) {
        JobDataMap data = new JobDataMap();
        data.put("email", "user@example.com");
        data.put("subject", "Reminder");
        data.put("message", "Do something");
        data.put(QuartzEmailScheduler.ATTEMPT_KEY, attempt);
        return data;
    }

    private JobExecutionContext context(JobDataMap data, JobDetail detail) {
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getJobDetail()).thenReturn(detail);
        return context;
    }
}
