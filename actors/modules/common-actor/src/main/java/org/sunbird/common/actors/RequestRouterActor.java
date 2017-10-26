package org.sunbird.common.actors;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.AuditOperation;
import org.sunbird.common.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.routing.SmallestMailboxPool;
import akka.util.Timeout;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class RequestRouterActor extends UntypedAbstractActor {
  
  private static Document document;
  private static Map<String, Integer> actorMap = new HashMap<String, Integer>();
  private ExecutionContext ec;
  private static final int WAIT_TIME_VALUE = 9;
  public static Map<String, ActorRef> routerMap = new HashMap<>();

  static {
      try (InputStream inputStream = RequestRouterActor.class.getClassLoader().getResourceAsStream("actor-config.xml")) {
          DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
          document = dBuilder.parse(inputStream);
          document.getDocumentElement().normalize();
      } catch (Exception e) {
        ProjectLogger.log("Error occurred", e);
      }
  }
  
  
  public RequestRouterActor() {
      try {
          if (null != document) {
              createManagersPool();
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  
  private void createManagersPool() {
    NodeList nList = document.getElementsByTagName("actor-managers");
    for (int temp = 0; temp < nList.getLength(); temp++) {
      Node nNode = nList.item(temp);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) nNode;
        NodeList cList = eElement.getChildNodes();
        if (null != cList && cList.getLength() > 0) {
          for (int i = 0; i < cList.getLength(); i++) {
            Node cNode = cList.item(i);
            if (cNode.getNodeType() == Node.ELEMENT_NODE) {
              Element cElement = (Element) cNode;
              try {
                NamedNodeMap nodeMap = cElement.getAttributes();
                String className = nodeMap.getNamedItem("class").getNodeValue();
                String name = nodeMap.getNamedItem("name").getNodeValue();
                String strCount = nodeMap.getNamedItem("count").getNodeValue();
                int count = 8;
                try {
                  count = Integer.parseInt(strCount);
                } catch (Exception e) {
                  ProjectLogger.log("Error occured" + e.getMessage(), e);
                }
                actorMap.put(name, count);
                Class<?> cls = Class.forName(className);
                Props actorProps = Props.create(cls);
                ActorRef actor =
                    getContext().actorOf(new SmallestMailboxPool(count).props(actorProps));
                actor = getContext().actorOf(actorProps);
                routerMap.put(name, actor);
              } catch (Exception e) {
                ProjectLogger.log("Error occured" + e.getMessage(), e);
              }
            }

          }
        }
      }
    }
  }

  /*private void addActorsToPool(String[] arr, int count, String id, Address[] addresses) {
      for (String className : arr) {
          try {
              Class<?> cls = Class.forName(className);
              Props actorProps = Props.create(cls);
              ActorRef actor = null;
              if (null != addresses && addresses.length > 0) {
                  actor = getContext().actorOf(new RemoteRouterConfig(new RoundRobinPool(count), addresses).props(Props.create(cls)));
              } else {
                  actor = getContext().actorOf(new SmallestMailboxPool(count).props(actorProps));
              }
              addActorsToMap(className,actor);
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
  }*/
/*  private void addActorsToMap(String className, ActorRef actor){
    routerMap.put(className, actor);
  }*/
  @Override
  public void onReceive(Object message) throws Exception {
    if (message instanceof Request) {
      ProjectLogger.log("Actor selector onReceive called");
      Request actorMessage = (Request) message;
      org.sunbird.common.request.ExecutionContext.setRequestId(actorMessage.getRequestId());
      if (null != routerMap.get(actorMessage.getManagerName())) {
        ActorRef ref = routerMap.get(actorMessage.getManagerName());
        if (null != ref) {
          route(ref, actorMessage);
        } else {
          ProjectLogger.log("UNSUPPORTED OPERATION TYPE");
          ProjectCommonException exception =
              new ProjectCommonException(ResponseCode.invalidOperationName.getErrorCode(),
                  ResponseCode.invalidOperationName.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
          sender().tell(exception, ActorRef.noSender());
        }
      }
    } else {
      ProjectLogger.log("UNSUPPORTED MESSAGE");
      ProjectCommonException exception =
          new ProjectCommonException(ResponseCode.invalidRequestData.getErrorCode(),
              ResponseCode.invalidRequestData.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      sender().tell(exception, ActorRef.noSender());
    }

  }

  /**
   * method will route the message to corresponding router pass into the argument .
   *
   * @param router
   * @param message
   * @return boolean
   */
  private boolean route(ActorRef router, Request message) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log("Actor Service Call start  for  api ==" + message.getOperation()
        + " start time " + startTime, LoggerEnum.PERF_LOG);
    Timeout timeout = new Timeout(Duration.create(WAIT_TIME_VALUE, TimeUnit.SECONDS));
    Future<Object> future = Patterns.ask(router, message, timeout);
    ActorRef parent = sender();
    future.onComplete(new OnComplete<Object>() {
      @Override
      public void onComplete(Throwable failure, Object result) {
        if (failure != null) {
          ProjectLogger.log("Actor Service Call Ended on Failure for  api =="
              + message.getOperation() + " end time " + System.currentTimeMillis() + "  Time taken "
              + (System.currentTimeMillis() - startTime), LoggerEnum.PERF_LOG);
          // We got a failure, handle it here
          ProjectLogger.log(failure.getMessage(), failure);
          if (failure instanceof ProjectCommonException) {
            parent.tell(failure, ActorRef.noSender());
          } else {
            ProjectCommonException exception =
                new ProjectCommonException(ResponseCode.internalError.getErrorCode(),
                    ResponseCode.internalError.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
            parent.tell(exception, ActorRef.noSender());
          }
        } else {
          ProjectLogger.log("PARENT RESULT IS " + result);
          // We got a result, handle it
          ProjectLogger.log("Actor Service Call Ended on Success for  api =="
              + message.getOperation() + " end time " + System.currentTimeMillis() + "  Time taken "
              + (System.currentTimeMillis() - startTime), LoggerEnum.PERF_LOG);
          parent.tell(result, ActorRef.noSender());
          // Audit log method call
          if(result instanceof Response){
            if (Util.auditLogUrlMap.containsKey(message.getOperation())) {
              AuditOperation auditOperation =
                  (AuditOperation) Util.auditLogUrlMap.get(message.getOperation());
              Map<String, Object> map = new HashMap<>();
              map.put(JsonKey.OPERATION, auditOperation);
              map.put(JsonKey.REQUEST, message);
              map.put(JsonKey.RESPONSE, result);
              Request request = new Request();
              request.setOperation(ActorOperations.PROCESS_AUDIT_LOG.getValue());
              request.setRequest(map);
              ActorRef auditLogManagementActor = routerMap.get(ActorOperations.PROCESS_AUDIT_LOG.getKey());
              auditLogManagementActor.tell(request, self());
            }
          }
        }
      }
    }, ec);
    return true;
  }

}