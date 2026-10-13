package dan.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReminderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReminderApplication.class, args);
	}
        
        // docker run --name postgres-reminder-rest -e POSTGRES_DB=reminder-rest -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16

}
