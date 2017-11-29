package org.sunbird.learner.util.actorutility;

import org.sunbird.common.models.util.ConfigUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.learner.util.actorutility.impl.LocalActorSystem;
import org.sunbird.learner.util.actorutility.impl.RemoteActorSystem;

/**
 * 
 * @author Amit Kumar
 *
 */
public class ActorSystemFactory {

  private static ActorSystem actorSystem = null;

  private ActorSystemFactory() {}

  static {
    if ("remote"
        .equalsIgnoreCase(ConfigUtil.config.getString(JsonKey.BACKGROUND_ACTOR_PROVIDER))) {
      ProjectLogger.log("Initializing Remote Actor System");
      createRemoteActorSystem();
    } else {
      createLocalActorSystem();
    }
  }
  
  /**
   * This method will initialize the local actor system.
   */
  private static void createLocalActorSystem () {
    ProjectLogger.log("Initializing Local Actor System");
    if (null == actorSystem) {
      actorSystem = LocalActorSystem.getInstance();
    }
  }
  
  /**
   * This method will initialize the remote actor system.
   */
  public static void createRemoteActorSystem() {
    if (null == actorSystem) {
      actorSystem = RemoteActorSystem.getInstance();
    }
  }

  public static ActorSystem getActorSystem() {
    return actorSystem;
  }
}
