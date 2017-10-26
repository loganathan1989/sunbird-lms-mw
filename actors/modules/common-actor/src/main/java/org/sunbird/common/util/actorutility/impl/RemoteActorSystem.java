package org.sunbird.common.util.actorutility.impl;

import org.sunbird.common.util.actorutility.ActorSystem;
import org.sunbird.common.util.actorutility.ActorUtility;

/**
 * 
 * @author Amit Kumar
 *
 */
public class RemoteActorSystem implements ActorSystem{

  @Override
  public Object initializeActorSystem(String operationType) {
    return ActorUtility.getActorSelection();
  }

}
