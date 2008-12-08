package org.jruby.webapp.quartz;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jruby.Ruby;
import org.jruby.rack.RackServletContextListener;
import org.jruby.rack.RackApplicationFactory;
import org.jruby.rack.RackApplication;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.quartz.JobExecutionContext;

import org.apache.commons.pool.ObjectPool;

/**
 * Quartz job for running arbitrary Ruby code inside a Rails environment.
 *
 * JRuby runtimes are retrieved from the runtime pool created by Goldspike's
 * RailsContextListener.
 */
public class RailsJob implements Job {

  /**
   * inspired by Goldspike's RailsServlet#service
   */
  public void execute(JobExecutionContext context)
            throws JobExecutionException 
  {
    try {
      ServletContext ctx = getServletContext(context);
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String command = data.getString("command");
      ctx.log("starting: " + command);
      
      RackApplicationFactory rackFactory = (RackApplicationFactory) ctx.getAttribute(RackServletContextListener.FACTORY_KEY);
      RackApplication rackApp = null;
      try {
          rackApp = rackFactory.getApplication();
          rackApp.getRuntime().evalScriptlet(command);
          ctx.log("finished: " + command);
      } catch (Exception e) {
          ctx.log("Could not execute: " + command, e);
      } finally {
          if(rackApp != null) rackFactory.finishedWithApplication(rackApp);
      }
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }

  protected RackApplicationFactory getApplicationFactory(ServletContext ctx)
    throws ServletException 
  {
    RackApplicationFactory runtimePool = 
      (RackApplicationFactory)ctx.getAttribute(RackServletContextListener.FACTORY_KEY);
    if (runtimePool == null) {
      throw new ServletException("No application factory is available, please check RackServletContextListener");
    }
    return runtimePool;
  }

  protected ServletContext getServletContext(JobExecutionContext ctx) 
    throws org.quartz.SchedulerException 
  {
    return (ServletContext) ctx.getScheduler().getContext().get("servlet_context");
  }
}
