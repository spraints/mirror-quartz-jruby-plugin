package org.jruby.webapp.quartz;

import org.jruby.Ruby;

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.quartz.impl.StdSchedulerFactory;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SchedulerContext;
import org.quartz.xml.JobSchedulingDataProcessor;

/** 
 * Initializer for the Quartz-Scheduler and configured jobs
 */
public class QuartzContextListener implements ServletContextListener {

  private String command;
  private int waitBeforeStartSeconds = 30; // wait 30 seconds
  private boolean stop = false; // wait 30 seconds
  private Thread thread = null;
  private int interval = 60;
  private int minIdle = 0;
  private Scheduler scheduler;
  private SchedulerContext schedulerContext;
  private ServletContext context;

  /**
   * Initializes the scheduler, and schedules all configured jobs.
   */
  public void contextInitialized(ServletContextEvent event) {
    context = event.getServletContext();
    context.log("intializing Quartz Scheduler...");
    try {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
      schedulerContext = scheduler.getContext();
      schedulerContext.put("servlet_context", context);
      Map commands = getCommands(context);
      for (Iterator entries = commands.entrySet().iterator(); entries.hasNext(); ) {
        Map.Entry entry = (Map.Entry) entries.next();
        scheduleCronJob((String) entry.getKey(), (String[]) entry.getValue());
      }
      scheduler.start();
      context.log("Finished initializing quartz scheduler");
    } catch (Exception e) {
      context.log("Quartz Scheduler failed to initialize: " + e.toString());
    }
  }

  /**
   * shuts down the scheduler
   */
    public void contextDestroyed(ServletContextEvent event) {
    try {
      if (scheduler != null) scheduler.shutdown();
    } catch (Exception e) {
      context.log("Quartz Scheduler failed to shut down cleanly: " + e.toString());
      e.printStackTrace();
    }
    context.log("Quartz Scheduler successful shut down.");
  }

  /**
   * Parses command names and cron patterns from the context's init parameters
   * InitParameters have to follow these conventions in order to be recognized:
   *
   * yourJobNameCommand     - piece of Ruby code to execute
   * yourJobNameCronPattern - cron pattern saying when to execute this job.
   */
  protected Map getCommands(ServletContext context) {
    
    Map commands = new HashMap();
    for  (Enumeration initParams = context.getInitParameterNames(); initParams.hasMoreElements(); ) {
      String key = (String) initParams.nextElement();
      if (key.endsWith("Command")) {
        String command = context.getInitParameter(key);
        key = key.replaceFirst("Command$", "");
        String cron = context.getInitParameter(key + "CronPattern");
        if (cron != null) {
          commands.put(key, new String[] { command, cron });
        }
      }
    }
    return commands;
  }

  /**
   * schedules a single job
   */
  protected void scheduleCronJob(String name, String[] commandAndCronPattern) throws Exception {
    JobDetail job = new JobDetail(name+"Job", "railsQuartz", 
                                  org.jruby.webapp.quartz.RailsJob.class);
    job.getJobDataMap().put("command", commandAndCronPattern[0]);
    Trigger trigger = new CronTrigger(name+"Trigger", "railsQuartz", commandAndCronPattern[1]);
    scheduler.scheduleJob(job, trigger);
  }

}
