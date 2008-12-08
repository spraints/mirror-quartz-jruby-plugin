package org.jruby.webapp.quartz;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jruby.Ruby;
import org.jruby.webapp.RailsContextListener;

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

  private static final int FIFTEEN_MINUTES_IN_MILLIS = 1000 * 60 * 15;

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
	  
      Ruby runtime = null;
      try {
          runtime = (Ruby) getRuntimePool(ctx).borrowObject();
          runtime.evalScriptlet(command);
          getRuntimePool(ctx).returnObject(runtime);
		  ctx.log("finished: " + command);
      } catch (Exception e) {
          ctx.log("Could not execute: " + command, e);
          if(runtime != null) getRuntimePool(ctx).invalidateObject(runtime);
          ctx.log(command + " returning JRuby runtime to pool and will restart in 15 minutes.");
          try {
              Thread.sleep(FIFTEEN_MINUTES_IN_MILLIS);
          } catch (InterruptedException ex) {
              // can't do much here ...
          }
      }
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
	}

	protected ObjectPool getRuntimePool(ServletContext ctx)
    throws ServletException 
  {
		ObjectPool runtimePool = 
      (ObjectPool)ctx.getAttribute(RailsContextListener.RUNTIME_POOL_ATTRIBUTE);
		if (runtimePool == null) {
			throw new ServletException("No runtime pool is available, please check RailsContextListener");
		}
		return runtimePool;
	}

  protected ServletContext getServletContext(JobExecutionContext ctx) 
    throws org.quartz.SchedulerException 
  {
    return (ServletContext) ctx.getScheduler().getContext().get("servlet_context");
  }
}
