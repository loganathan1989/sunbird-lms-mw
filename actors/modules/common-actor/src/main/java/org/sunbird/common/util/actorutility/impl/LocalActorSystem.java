package org.sunbird.common.util.actorutility.impl;

import org.sunbird.common.actors.RequestRouterActor;
import org.sunbird.common.util.actorutility.ActorSystem;

/**
 * 
 * @author Amit Kumar
 *
 */
public class LocalActorSystem implements ActorSystem{

  @Override
  public Object initializeActorSystem(String operationType) {
    return RequestRouterActor.routerMap.get(operationType);
  }

}
