package org.sunbird.common.util;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.actorutility.ActorSystemFactory;

/**
 * 
 * @author Amit Kumar
 *
 */
public class ActorUtil {
  
  private ActorUtil(){}
  
  public static void tell(Request request){
    Object obj = ActorSystemFactory.getActorSystem().initializeActorSystem(request.getOperation());;
     if(obj instanceof ActorRef){
       ((ActorRef)obj).tell(request, ActorRef.noSender());
     } else {
       ((ActorSelection)obj).tell(request, ActorRef.noSender());
     }
  }
}
