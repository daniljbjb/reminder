/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.scheduler.quartz.job;

import dan.reminder.config.EmailRetryProperties;
import dan.reminder.exception.EmailDeliveryException;
import dan.reminder.service.EmailService;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 *
 * @author danil
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailReminderJob implements Job {

    private final EmailService emailService;
    private final QuartzEmailScheduler quartzEmailScheduler;
    private final EmailRetryProperties retryProperties;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        
        JobDataMap data = context.getMergedJobDataMap();
        String to = data.getString("email");
        String subject = data.getString("subject");
        String message = data.getString("message");
        int attempt = data.getInt(QuartzEmailScheduler.ATTEMPT_KEY);

        try {
            emailService.sendTextEmail(to, subject, message);
            log.info("Quartz email delivery succeeded on attempt {}", attempt);
        } catch (EmailDeliveryException ex) {
            if (attempt >= retryProperties.maxAttempts()) {
                log.error("Email delivery failed after {} attempts", attempt, ex);
                throw new JobExecutionException("Email delivery failed after all retry attempts", ex, false);
            }

            int nextAttempt = attempt + 1;
            Duration delay = retryDelay(attempt);
            try {
                quartzEmailScheduler.scheduleRetry(context.getJobDetail(), nextAttempt, delay);
            } catch (IllegalStateException schedulingFailure) {
                schedulingFailure.addSuppressed(ex);
                log.error("Email delivery retry could not be scheduled", schedulingFailure);
                throw new JobExecutionException("Email delivery failed and retry was not scheduled",
                        schedulingFailure, false);
            }
            log.warn("Email delivery attempt {} failed; retry {} is scheduled in {}", attempt, nextAttempt, delay, ex);
        }
    }

    private Duration retryDelay(int failedAttempt) {
        return retryProperties.initialDelay().multipliedBy(failedAttempt);
    }
}
