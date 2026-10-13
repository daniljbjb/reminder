/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.scheduler.quartz.scheduler;

import dan.reminder.model.Reminder;
import dan.reminder.scheduler.quartz.job.EmailReminderJob;
import java.util.Date;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author danil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuartzEmailScheduler {

    private static final String JOB_GROUP = "email-jobs";

    private static final String EMAIL_KEY = "email";
    private static final String SUBJECT_KEY = "subject";
    private static final String MESSAGE_KEY = "message";
    public static final String ATTEMPT_KEY = "attempt";

    private final Scheduler scheduler;

    public void schedule(Reminder reminder, String userEmail) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(EMAIL_KEY, userEmail);
        dataMap.put(SUBJECT_KEY, reminder.getTitle());
        dataMap.put(MESSAGE_KEY, Objects.requireNonNullElse(reminder.getDescription(), "Empty description"));
        dataMap.put(ATTEMPT_KEY, 1);

        JobDetail jobDetail = JobBuilder.newJob(EmailReminderJob.class)
                .withIdentity(jobKey(reminder.getId()))
                .usingJobData(dataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(reminder.getId()))
                .startAt(Date.from(reminder.getRemind()))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled Quartz job for reminder {}", reminder.getId());
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule Quartz job for reminder " + reminder.getId(), e);
        }
    }

    public boolean delete(Long reminderId) {
        try {
            boolean deleted = scheduler.deleteJob(jobKey(reminderId));

            if (deleted) {
                log.info("Deleted Quartz job for reminder {}", reminderId);
            } else {
                log.warn("Quartz job for reminder {} was not found", reminderId);
            }

            return deleted;
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to delete Quartz job for reminder " + reminderId, e);
        }
    }

    /** Schedules the next persisted Quartz attempt after a failed SMTP delivery. */
    public void scheduleRetry(JobDetail jobDetail, int nextAttempt, java.time.Duration delay) {
        Trigger retryTrigger = TriggerBuilder.newTrigger()
                .withIdentity(retryTriggerKey(jobDetail.getKey(), nextAttempt))
                .forJob(jobDetail.getKey())
                .usingJobData(ATTEMPT_KEY, nextAttempt)
                .startAt(Date.from(java.time.Instant.now().plus(delay)))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();

        try {
            scheduler.scheduleJob(retryTrigger);
            log.warn("Scheduled email retry attempt {} for job {}", nextAttempt, jobDetail.getKey());
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule email retry for job " + jobDetail.getKey(), e);
        }
    }

    /**
     * Replaces the job and all its triggers in the same database transaction.
     * Quartz must use Spring's LocalDataSourceJobStore and the reminder DataSource
     * so a scheduling failure rolls back the deletion and the reminder update.
     */
    @Transactional
    public void recreate(Reminder reminder, String userEmail) {
        delete(reminder.getId());
        schedule(reminder, userEmail);
    }

    private JobKey jobKey(Long reminderId) {
        return JobKey.jobKey("email-job-" + reminderId, JOB_GROUP);
    }

    private TriggerKey triggerKey(Long reminderId) {
        return TriggerKey.triggerKey("email-trigger-" + reminderId, JOB_GROUP);
    }

    private TriggerKey retryTriggerKey(JobKey jobKey, int attempt) {
        return TriggerKey.triggerKey(jobKey.getName() + "-retry-" + attempt, jobKey.getGroup());
    }
}
