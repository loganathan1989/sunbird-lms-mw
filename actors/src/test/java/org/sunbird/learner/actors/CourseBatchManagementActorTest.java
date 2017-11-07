package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.EkStepRequestUtil;
import org.sunbird.learner.util.Util;


/**
 * @author Amit Kumar.
 */


@RunWith(PowerMockRunner.class)
@PrepareForTest({EkStepRequestUtil.class,ElasticSearchUtil.class,CourseEnrollmentActor.class})
@PowerMockIgnore("javax.management.*")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CourseBatchManagementActorTest {
  
  static ActorSystem system;
  static CassandraOperation operation= ServiceFactory.getInstance();
  static PropertiesCache cach = PropertiesCache.getInstance();
  final static Props props = Props.create(CourseBatchManagementActor.class);
  static Util.DbInfo batchDbInfo = null;
  static Util.DbInfo userOrgdbInfo = null;
  private String courseId = "do_212282810555342848180";
  private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
  private static String batchId = "";
  private static String hashTagId = "";
  
  @BeforeClass
  public static void setUp() {
      hashTagId = String.valueOf(System.currentTimeMillis());
      system = ActorSystem.create("system");
      Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
      batchDbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
      userOrgdbInfo = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
  }
  
  @Test
  public void runAllTestCases(){
    test1InvalidOperation();
    test2InvalidMessageType();
    testA1CreateBatch();
    testA1CreateBatchWithInvalidCorsId();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    testB2CreateBatchWithInvalidHashTagId();
    testC3getBatchDetails();
    testA1CreateBatchwithInvalidMentors();
    testA1CreateBatchWithInvalidCourseId();
    testA1CreateBatchWithInvalidOrgId();
    testC3getBatchDetailsWithInvalidId();
    testC4getCourseBatchDetails();
    testC4getCourseBatchDetailsWithInvalidId();
    
  }
  
  //@Test
  public void test1InvalidOperation(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      Request reqObj = new Request();
      reqObj.setOperation("INVALID_OPERATION");

      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(ProjectCommonException.class);
  }

  //@Test
  public void test2InvalidMessageType(){
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);

      subject.tell(new String("Invelid Type"), probe.getRef());
      probe.expectMsgClass(ProjectCommonException.class);
  }
  
  //@Test
  public void testA1CreateBatch(){
    PowerMockito.mockStatic(EkStepRequestUtil.class);
    Map<String , Object> ekstepResponse = new HashMap<String , Object>();
    ekstepResponse.put("count" , 10);
    Object[] ekstepMockResult = {ekstepResponse};
    when( EkStepRequestUtil.searchContent(Mockito.anyString() , Mockito.anyMap()) ).thenReturn(ekstepMockResult);
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, courseId);
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("1000 second"),Response.class);
    batchId = (String) response.getResult().get(JsonKey.BATCH_ID);
    System.out.println("batchId : "+batchId);
  }
  
  public void testA1CreateBatchWithInvalidCorsId(){
    PowerMockito.mockStatic(EkStepRequestUtil.class);
    Map<String , Object> ekstepResponse = new HashMap<String , Object>();
    ekstepResponse.put("count" , 10);
    Object[] ekstepMockResult = {ekstepResponse};
    when( EkStepRequestUtil.searchContent(Mockito.anyString() , Mockito.anyMap()) ).thenReturn(ekstepMockResult);
    
    PowerMockito.mockStatic(CourseEnrollmentActor.class);
    Map<String , Object> actorResponse = new HashMap<String , Object>();
    when( CourseEnrollmentActor.getCourseObjectFromEkStep(Mockito.anyString() , Mockito.anyMap()) ).thenReturn(actorResponse);
    
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, courseId+"789");
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  public void testA1CreateBatchWithInvalidCourseId(){
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, "12345");
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  public void testA1CreateBatchWithInvalidOrgId(){
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, courseId);
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    List<String> orgList = new ArrayList<>();
    orgList.add("12589");
    innerMap.put(JsonKey.COURSE_CREATED_FOR, orgList);
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  //@Test
  public void testA1CreateBatchwithInvalidMentors(){
    PowerMockito.mockStatic(EkStepRequestUtil.class);
    Map<String , Object> ekstepResponse = new HashMap<String , Object>();
    ekstepResponse.put("count" , 10);
    Object[] ekstepMockResult = {ekstepResponse};
    when( EkStepRequestUtil.searchContent(Mockito.anyString() , Mockito.anyMap()) ).thenReturn(ekstepMockResult);
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, courseId);
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    List<String> mentors = new ArrayList<>();
    mentors.add("12589");
    innerMap.put(JsonKey.MENTORS, mentors);
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  //@Test
  public void testB2CreateBatchWithInvalidHashTagId(){
    PowerMockito.mockStatic(EkStepRequestUtil.class);
    Map<String , Object> ekstepResponse = new HashMap<String , Object>();
    ekstepResponse.put("count" , 10);
    Object[] ekstepMockResult = {ekstepResponse};
    when( EkStepRequestUtil.searchContent(Mockito.anyString() , Mockito.anyMap()) ).thenReturn(ekstepMockResult);
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.COURSE_ID, courseId);
    innerMap.put(JsonKey.NAME, "DUMMY_COURSE_NAME1");
    innerMap.put(JsonKey.ENROLLMENT_TYPE, "invite-only");
    innerMap.put(JsonKey.START_DATE , (String)format.format(new Date()));
    innerMap.put(JsonKey.HASHTAGID ,hashTagId );
    Calendar now =  Calendar.getInstance();
    now.add(Calendar.DAY_OF_MONTH, 5);
    Date after5Days = now.getTime();
    innerMap.put(JsonKey.END_DATE , (String)format.format(after5Days));
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  //@Test
  public void testC3getBatchDetails(){
       
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.BATCH_ID ,batchId );
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("1000 second"),Response.class);
    String hashtagId = (String) ((Map<String,Object>)response.getResult().get(JsonKey.RESPONSE)).get(JsonKey.HASHTAGID);
    assertEquals(true,hashtagId.equalsIgnoreCase(hashTagId));
  }
  
  public void testC3getBatchDetailsWithInvalidId(){
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.BATCH_ID ,batchId+"1234" );
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("1000 second"),Response.class);
    assertEquals(true,((Map<String, Object>)res.getResult().get(JsonKey.RESPONSE)).isEmpty());
  }
  
  public void testC4getCourseBatchDetails(){
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_COURSE_BATCH_DETAIL.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.BATCH_ID ,batchId );
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("1000 second"),Response.class);
    String hashtagId = (String) (((List<Map<String, Object>>)response.getResult().get(JsonKey.RESPONSE)).get(0)).get(JsonKey.HASHTAGID);
    assertEquals(true,hashtagId.equalsIgnoreCase(hashTagId));
  }
  
 public void testC4getCourseBatchDetailsWithInvalidId(){
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_COURSE_BATCH_DETAIL.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.BATCH_ID ,batchId+"13456" );
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("1000 second"),ProjectCommonException.class);
  }
  
  @AfterClass
  public static void deleteUser() {


    operation.deleteRecord(batchDbInfo.getKeySpace(), batchDbInfo.getTableName(), batchId);

    ElasticSearchUtil.removeData(ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.course.getTypeName(), batchId);
    
    
  }
}