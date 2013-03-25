package edu.berkeley.sparrow.daemon.scheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.thrift.TException;

import edu.berkeley.sparrow.daemon.SparrowConf;
import edu.berkeley.sparrow.daemon.util.Network;
import edu.berkeley.sparrow.daemon.util.TServers;
import edu.berkeley.sparrow.thrift.SchedulerService;
import edu.berkeley.sparrow.thrift.GetTaskService;
import edu.berkeley.sparrow.thrift.TFullTaskId;
import edu.berkeley.sparrow.thrift.THostPort;
import edu.berkeley.sparrow.thrift.TSchedulingRequest;
import edu.berkeley.sparrow.thrift.TTaskLaunchSpec;

/**
 * This class extends the thrift sparrow scheduler interface. It wraps the
 * {@link Scheduler} class and delegates most calls to that class.
 */
public class SchedulerThrift implements SchedulerService.Iface, GetTaskService.Iface {
  // Defaults if not specified by configuration
  public final static int DEFAULT_SCHEDULER_THRIFT_PORT = 20503;
  /**
   * Number of threads to use for the scheduler server. Each client has a
   * dedicated thread until the client closes its connection, so this number should be no less
   * than the expected number of concurrent clients!
   */
  private final static int DEFAULT_SCHEDULER_THRIFT_THREADS = 16;
  /**
   * Number of threads to use for the getTask() server. Each client has a
   * dedicated thread until the client closes its connection, so this number should be no less
   * than the expected number of concurrent clients!
   */
  private final static int DEFAULT_SCHEDULER_GET_TASK_THRIFT_THREADS = 1600;
  public final static int DEFAULT_GET_TASK_PORT = 20507;

  private Scheduler scheduler = new Scheduler();

  /**
   * Initialize this thrift service.
   *
   * This spawns a multi-threaded thrift server and listens for Sparrow
   * scheduler requests.
   */
  public void initialize(Configuration conf) throws IOException {
    SchedulerService.Processor<SchedulerService.Iface> processor =
        new SchedulerService.Processor<SchedulerService.Iface>(this);
    int port = conf.getInt(SparrowConf.SCHEDULER_THRIFT_PORT,
        DEFAULT_SCHEDULER_THRIFT_PORT);
    int schedulerThreads = conf.getInt(SparrowConf.SCHEDULER_THRIFT_THREADS,
        DEFAULT_SCHEDULER_THRIFT_THREADS);
    String hostname = Network.getHostName(conf);
    InetSocketAddress addr = new InetSocketAddress(hostname, port);
    scheduler.initialize(conf, addr);
    TServers.launchThreadedThriftServer(port, schedulerThreads, processor);

    int getTaskThreads = conf.getInt(SparrowConf.SCHEDULER_GET_TASK_THRIFT_THREADS,
        DEFAULT_SCHEDULER_GET_TASK_THRIFT_THREADS);
    int getTaskPort = conf.getInt(SparrowConf.GET_TASK_PORT,
        DEFAULT_GET_TASK_PORT);
    GetTaskService.Processor<GetTaskService.Iface> getTaskprocessor =
        new GetTaskService.Processor<GetTaskService.Iface>(this);
    TServers.launchThreadedThriftServer(getTaskPort, getTaskThreads, getTaskprocessor);
  }

  @Override
  public boolean registerFrontend(String app, String socketAddress) {
    return scheduler.registerFrontend(app, socketAddress);
  }

  @Override
  public void submitJob(TSchedulingRequest req)
      throws TException {
    scheduler.submitJob(req);
  }

  @Override
  public void sendFrontendMessage(String app, TFullTaskId taskId,
      int status, ByteBuffer message) throws TException {
    scheduler.sendFrontendMessage(app, taskId, status, message);
  }

  @Override
  public List<TTaskLaunchSpec> getTask(String requestId, THostPort nodeMonitorAddress)
      throws TException {
    return scheduler.getTask(requestId, nodeMonitorAddress);
  }
}